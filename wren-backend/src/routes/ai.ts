import express from 'express';
import crypto from 'crypto';
import { getDb } from '../db/database.js';
import { authenticateToken, AuthenticatedRequest, checkCreditsCircuitBreaker } from '../middleware/auth.js';
import { generate, AiMode, AiFileContext } from '../services/llm.js';

const router = express.Router();

const VALID_MODES: AiMode[] = ['chat', 'planificador', 'editor', 'terminal', 'automatizador'];

/**
 * Atomically debit one credit inside a transaction. Returns the new balance,
 * or null when the user has insufficient credits.
 */
async function debitOneCredit(userId: string, reason: string): Promise<number | null> {
  const db = getDb();
  const timestamp = new Date().toISOString();
  await db.run('BEGIN TRANSACTION;');
  try {
    const record = await db.get('SELECT balance FROM credits WHERE user_id = ?', [userId]);
    if (!record || record.balance <= 0) {
      await db.run('ROLLBACK;');
      return null;
    }
    const newBalance = record.balance - 1;
    await db.run('UPDATE credits SET balance = ?, updated_at = ? WHERE user_id = ?', [newBalance, timestamp, userId]);
    await db.run(
      'INSERT INTO credit_logs (id, user_id, amount, reason, timestamp) VALUES (?, ?, ?, ?, ?)',
      [crypto.randomUUID(), userId, -1, reason, timestamp]
    );
    await db.run('COMMIT;');
    return newBalance;
  } catch (err) {
    await db.run('ROLLBACK;');
    throw err;
  }
}

async function loadProjectContext(projectId: string | undefined, userId: string): Promise<{ name?: string; files: AiFileContext[] }> {
  if (!projectId) return { files: [] };
  const db = getDb();
  const project = await db.get('SELECT name FROM projects WHERE id = ? AND user_id = ?', [projectId, userId]);
  if (!project) return { files: [] };
  const files = await db.all(
    'SELECT path, content FROM files WHERE project_id = ? AND is_directory = 0 ORDER BY path LIMIT 40',
    [projectId]
  );
  return { name: project.name, files: files as AiFileContext[] };
}

async function saveChatTurn(
  userId: string,
  projectId: string | undefined,
  role: 'user' | 'assistant',
  mode: string,
  content: string,
  actions: unknown[] | null,
  provider: string | null
): Promise<void> {
  const db = getDb();
  await db.run(
    'INSERT INTO chat_history (id, user_id, project_id, role, mode, content, actions, provider, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
    [
      crypto.randomUUID(),
      userId,
      projectId || null,
      role,
      mode,
      content,
      actions ? JSON.stringify(actions) : null,
      provider,
      new Date().toISOString(),
    ]
  );
}

// AI Chat endpoint (Deducts 1 credit) — routes to a real LLM provider or local engine
router.post('/chat', authenticateToken, checkCreditsCircuitBreaker, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { prompt, mode, projectId } = req.body;

  if (typeof prompt !== 'string' || prompt.trim().length === 0) {
    return res.status(400).json({ error: 'Prompt content is required.' });
  }
  if (prompt.length > 16000) {
    return res.status(400).json({ error: 'Prompt exceeds the maximum allowed length.' });
  }

  const activeMode: AiMode = VALID_MODES.includes(mode) ? mode : 'chat';

  try {
    const userId = req.user.id;

    // 1. Debit a credit atomically before doing work
    const remainingCredits = await debitOneCredit(userId, `AI_${activeMode.toUpperCase()}`);
    if (remainingCredits === null) {
      return res.status(402).json({ error: 'Insufficient credits.' });
    }

    // 2. Gather project context and generate a response
    const context = await loadProjectContext(projectId, userId);
    const result = await generate({
      prompt: prompt.trim(),
      mode: activeMode,
      projectName: context.name,
      files: context.files,
    });

    // 3. Persist the conversation turn (best-effort)
    try {
      await saveChatTurn(userId, projectId, 'user', activeMode, prompt.trim(), null, null);
      await saveChatTurn(userId, projectId, 'assistant', activeMode, result.response, result.actions, result.provider);
    } catch (histErr) {
      console.error('Chat history persistence failed (non-fatal):', histErr);
    }

    return res.json({
      success: true,
      mode: activeMode,
      response: result.response,
      actions: result.actions,
      provider: result.provider,
      model: result.model,
      remainingCredits,
      timestamp: new Date().toISOString(),
    });
  } catch (err) {
    console.error('AI Chat failure:', err);
    return res.status(500).json({ error: 'Server error during AI request processing.' });
  }
});

// Autonomous Agent Action Executor (Deducts 1 credit) — persists file changes when possible
router.post('/agent/execute', authenticateToken, checkCreditsCircuitBreaker, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { action, projectId, path: filePath, content, command, details } = req.body;

  if (!action) {
    return res.status(400).json({ error: 'Agent action parameter is required.' });
  }

  try {
    const userId = req.user.id;
    const db = getDb();

    const remainingCredits = await debitOneCredit(userId, 'AGENT_ACTION_EXEC');
    if (remainingCredits === null) {
      return res.status(402).json({ error: 'Insufficient credits.' });
    }

    let applied = false;
    let executionLog = `Agent executed action: ${action}.`;

    // When a project + file are supplied, actually persist the change server-side.
    if (projectId && (action === 'CREATE_FILE' || action === 'EDIT_FILE') && typeof filePath === 'string') {
      const project = await db.get('SELECT id FROM projects WHERE id = ? AND user_id = ?', [projectId, userId]);
      if (project) {
        const cleanContent = typeof content === 'string' ? stripDiffMarkers(content) : '';
        const existing = await db.get('SELECT id FROM files WHERE project_id = ? AND path = ?', [projectId, filePath]);
        const timestamp = new Date().toISOString();
        if (existing) {
          await db.run('UPDATE files SET content = ? WHERE id = ?', [cleanContent, existing.id]);
        } else {
          const name = filePath.split('/').pop() || filePath;
          await db.run(
            'INSERT INTO files (id, project_id, name, path, is_directory, content, parent_id) VALUES (?, ?, ?, ?, 0, ?, NULL)',
            [crypto.randomUUID(), projectId, name, filePath, cleanContent]
          );
        }
        await db.run('UPDATE projects SET updated_at = ? WHERE id = ?', [timestamp, projectId]);
        applied = true;
        executionLog = `${existing ? 'Updated' : 'Created'} file ${filePath} in project.`;
      }
    } else if (action === 'EXECUTE_COMMAND' && typeof command === 'string') {
      executionLog = `Command queued for client-side execution: ${command}`;
    }

    return res.json({
      success: true,
      action,
      applied,
      status: 'COMPLETED',
      remainingCredits,
      executionLog: `${executionLog}${details ? ' Details: ' + JSON.stringify(details) : ''}`,
      timestamp: new Date().toISOString(),
    });
  } catch (err) {
    console.error('AI Agent execution failure:', err);
    return res.status(500).json({ error: 'Server error executing autonomous agent action.' });
  }
});

// Fetch persisted chat history for the authenticated user (optionally scoped to a project)
router.get('/history', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { projectId } = req.query;

  try {
    const db = getDb();
    const rows = projectId
      ? await db.all(
          'SELECT id, role, mode, content, actions, provider, created_at FROM chat_history WHERE user_id = ? AND project_id = ? ORDER BY created_at ASC LIMIT 200',
          [req.user.id, projectId]
        )
      : await db.all(
          'SELECT id, role, mode, content, actions, provider, created_at FROM chat_history WHERE user_id = ? ORDER BY created_at DESC LIMIT 200',
          [req.user.id]
        );

    const messages = rows.map((r) => ({
      ...r,
      actions: r.actions ? safeJsonParse(r.actions) : [],
    }));
    return res.json({ messages });
  } catch (err) {
    console.error('Fetch chat history failure:', err);
    return res.status(500).json({ error: 'Server error retrieving chat history.' });
  }
});

function safeJsonParse(value: string): unknown {
  try {
    return JSON.parse(value);
  } catch {
    return [];
  }
}

/** Removes leading unified-diff markers so stored file content is clean source. */
function stripDiffMarkers(content: string): string {
  return content
    .split('\n')
    .filter((line) => !line.startsWith('-'))
    .map((line) => (line.startsWith('+') ? line.slice(1) : line))
    .join('\n');
}

export default router;
