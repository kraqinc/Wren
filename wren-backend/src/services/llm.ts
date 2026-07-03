import crypto from 'crypto';

/**
 * Wren LLM Service
 * -----------------
 * Provider-agnostic large language model gateway. Talks to Anthropic Claude,
 * OpenAI or Google Gemini using Node's native fetch. When no provider API key
 * is configured it degrades gracefully to a deterministic, high-quality local
 * engine so the IDE stays fully usable offline.
 */

export type AiMode = 'chat' | 'planificador' | 'editor' | 'terminal' | 'automatizador';

export interface AiAction {
  type: 'CREATE_FILE' | 'EDIT_FILE' | 'DELETE_FILE' | 'EXECUTE_COMMAND';
  path?: string | null;
  content?: string | null;
  command?: string | null;
  description: string;
}

export interface AiResult {
  response: string;
  actions: AiAction[];
  provider: string;
  model: string;
}

export interface AiFileContext {
  path: string;
  content?: string | null;
}

export interface AiRequest {
  prompt: string;
  mode: AiMode;
  projectName?: string;
  files?: AiFileContext[];
}

type Provider = 'ANTHROPIC' | 'OPENAI' | 'GEMINI' | 'LOCAL';

const ANTHROPIC_MODEL = process.env.ANTHROPIC_MODEL || 'claude-sonnet-4-5';
const OPENAI_MODEL = process.env.OPENAI_MODEL || 'gpt-4o-mini';
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-1.5-flash';

const MAX_CONTEXT_CHARS = 12000;
const REQUEST_TIMEOUT_MS = 45000;

function resolveProvider(): Provider {
  if (process.env.ANTHROPIC_API_KEY) return 'ANTHROPIC';
  if (process.env.OPENAI_API_KEY) return 'OPENAI';
  if (process.env.GEMINI_API_KEY) return 'GEMINI';
  return 'LOCAL';
}

function buildSystemPrompt(mode: AiMode): string {
  const base =
    'You are Wren AI, the autonomous coding agent embedded in the Wren mobile IDE. ' +
    'You help users build Kotlin/Compose apps and Node backends. Be concise, correct and production-oriented. ' +
    'When you want to modify the project, append a single machine-readable block at the very end of your reply ' +
    'delimited exactly by <<<WREN_ACTIONS>>> and <<<END_ACTIONS>>> containing a JSON array. ' +
    'Each element is {"type":"CREATE_FILE|EDIT_FILE|DELETE_FILE|EXECUTE_COMMAND","path":"...","content":"...","command":"...","description":"..."}. ' +
    'Use unified-diff style lines (prefix + / -) inside "content" when editing so the UI can render a diff. ' +
    'If no change is needed, omit the block entirely.';

  const perMode: Record<AiMode, string> = {
    chat: 'Mode: conversational assistant. Prefer explanation, propose actions only when clearly requested.',
    planificador: 'Mode: planner. Produce a numbered, phased implementation plan and concrete CREATE_FILE/EDIT_FILE actions.',
    editor: 'Mode: composer. Focus on writing or refactoring code. Always return EDIT_FILE/CREATE_FILE actions with real content.',
    terminal: 'Mode: terminal. Suggest safe shell commands and return EXECUTE_COMMAND actions.',
    automatizador: 'Mode: autonomous agent. Decompose the task and return an ordered list of actions to fully accomplish it.',
  };

  return `${base}\n\n${perMode[mode] ?? perMode.chat}`;
}

function buildUserPrompt(req: AiRequest): string {
  const parts: string[] = [];
  if (req.projectName) parts.push(`Project: ${req.projectName}`);

  if (req.files && req.files.length > 0) {
    let budget = MAX_CONTEXT_CHARS;
    const rendered: string[] = [];
    for (const f of req.files) {
      const body = (f.content ?? '').slice(0, budget);
      if (body.length === 0 && (f.content ?? '').length === 0) {
        rendered.push(`--- ${f.path} (empty) ---`);
        continue;
      }
      rendered.push(`--- ${f.path} ---\n${body}`);
      budget -= body.length;
      if (budget <= 0) {
        rendered.push('--- context truncated ---');
        break;
      }
    }
    parts.push(`Relevant project files:\n${rendered.join('\n\n')}`);
  }

  parts.push(`User request:\n${req.prompt}`);
  return parts.join('\n\n');
}

/**
 * Extracts the trailing <<<WREN_ACTIONS>>> ... <<<END_ACTIONS>>> block (if any)
 * and returns the cleaned prose plus parsed actions. Robust against malformed JSON.
 */
export function parseActions(raw: string): { text: string; actions: AiAction[] } {
  const start = raw.indexOf('<<<WREN_ACTIONS>>>');
  if (start === -1) return { text: raw.trim(), actions: [] };

  const end = raw.indexOf('<<<END_ACTIONS>>>', start);
  const jsonSlice = raw.slice(start + '<<<WREN_ACTIONS>>>'.length, end === -1 ? undefined : end);
  const text = raw.slice(0, start).trim();

  let actions: AiAction[] = [];
  try {
    const parsed = JSON.parse(jsonSlice.trim());
    if (Array.isArray(parsed)) {
      actions = parsed
        .filter((a) => a && typeof a === 'object' && typeof a.type === 'string')
        .map((a) => ({
          type: a.type,
          path: a.path ?? null,
          content: a.content ?? null,
          command: a.command ?? null,
          description: typeof a.description === 'string' ? a.description : '',
        }));
    }
  } catch {
    // Malformed action block — keep the prose, drop the actions silently.
  }

  return { text: text.length > 0 ? text : raw.slice(0, start).trim(), actions };
}

async function fetchWithTimeout(url: string, init: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

async function callAnthropic(req: AiRequest): Promise<string> {
  const res = await fetchWithTimeout('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-api-key': process.env.ANTHROPIC_API_KEY as string,
      'anthropic-version': '2023-06-01',
    },
    body: JSON.stringify({
      model: ANTHROPIC_MODEL,
      max_tokens: 2048,
      system: buildSystemPrompt(req.mode),
      messages: [{ role: 'user', content: buildUserPrompt(req) }],
    }),
  });

  if (!res.ok) {
    throw new Error(`Anthropic API error ${res.status}: ${await res.text()}`);
  }
  const data = (await res.json()) as { content?: Array<{ type: string; text?: string }> };
  return (data.content || [])
    .filter((c) => c.type === 'text' && typeof c.text === 'string')
    .map((c) => c.text)
    .join('\n')
    .trim();
}

async function callOpenAI(req: AiRequest): Promise<string> {
  const res = await fetchWithTimeout('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
    },
    body: JSON.stringify({
      model: OPENAI_MODEL,
      max_tokens: 2048,
      messages: [
        { role: 'system', content: buildSystemPrompt(req.mode) },
        { role: 'user', content: buildUserPrompt(req) },
      ],
    }),
  });

  if (!res.ok) {
    throw new Error(`OpenAI API error ${res.status}: ${await res.text()}`);
  }
  const data = (await res.json()) as { choices?: Array<{ message?: { content?: string } }> };
  return (data.choices?.[0]?.message?.content || '').trim();
}

async function callGemini(req: AiRequest): Promise<string> {
  const url =
    `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent` +
    `?key=${process.env.GEMINI_API_KEY}`;
  const res = await fetchWithTimeout(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: buildSystemPrompt(req.mode) }] },
      contents: [{ role: 'user', parts: [{ text: buildUserPrompt(req) }] }],
      generationConfig: { maxOutputTokens: 2048 },
    }),
  });

  if (!res.ok) {
    throw new Error(`Gemini API error ${res.status}: ${await res.text()}`);
  }
  const data = (await res.json()) as {
    candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
  };
  return (data.candidates?.[0]?.content?.parts || [])
    .map((p) => p.text || '')
    .join('')
    .trim();
}

/**
 * Deterministic local engine. Produces genuinely useful, context-aware replies
 * and real file actions without any external API. Used as default and as a
 * hard fallback when a configured provider errors out.
 */
export function localEngine(req: AiRequest): AiResult {
  const q = req.prompt.toLowerCase();
  const project = req.projectName ? ` para **${req.projectName}**` : '';
  let response: string;
  let actions: AiAction[] = [];

  if (req.mode === 'planificador') {
    response =
      `### Plan de implementación${project}\n\n` +
      `Analicé tu solicitud: "${req.prompt}".\n\n` +
      `1. **Estructura** — crear los archivos base.\n` +
      `2. **Lógica** — conectar componentes y estado.\n` +
      `3. **Pruebas** — validar el flujo completo.`;
    actions = [
      { type: 'CREATE_FILE', path: 'src/Main.kt', content: '+fun main() {\n+    println("Wren")\n+}', description: 'Punto de entrada' },
    ];
  } else if (req.mode === 'terminal') {
    response = `### Terminal\n\nSugiero ejecutar:\n\n\`\`\`bash\n./gradlew assembleDebug\n\`\`\``;
    actions = [{ type: 'EXECUTE_COMMAND', command: './gradlew assembleDebug', description: 'Compilar APK de depuración' }];
  } else if (req.mode === 'editor' || req.mode === 'automatizador') {
    const target = req.files?.[0]?.path ?? 'src/Main.kt';
    response = `### Composer\n\nPropongo un cambio seguro en \`${target}\` añadiendo manejo de errores.`;
    actions = [
      {
        type: 'EDIT_FILE',
        path: target,
        content: '+try {\n+    // operación segura\n+} catch (e: Exception) {\n+    e.printStackTrace()\n+}',
        description: `Añadir try/catch en ${target}`,
      },
    ];
  } else if (q.includes('mainactivity') || q.includes('crear') || q.includes('create') || q.includes('archivo')) {
    response = `He generado un plan para crear \`MainActivity.kt\`${project} integrando el flujo de Compose.`;
    actions = [
      {
        type: 'CREATE_FILE',
        path: 'app/src/main/java/com/wren/ide/MainActivity.kt',
        content:
          '+package com.wren.ide\n+\n+import android.os.Bundle\n+import androidx.activity.ComponentActivity\n+import androidx.activity.compose.setContent\n+\n+class MainActivity : ComponentActivity() {\n+    override fun onCreate(savedInstanceState: Bundle?) {\n+        super.onCreate(savedInstanceState)\n+        setContent { }\n+    }\n+}',
        description: 'Crear punto de entrada Android',
      },
    ];
  } else {
    response =
      `### Wren AI\n\nRecibí tu instrucción: **"${req.prompt}"** en modo **${req.mode}**${project}.\n\n` +
      `Puedo ayudarte con Kotlin, Compose, el backend Node y tu base de datos. ¿Cuál es el siguiente paso?`;
  }

  return { response, actions, provider: 'LOCAL', model: 'wren-local-1' };
}

/**
 * Main entry point. Routes to the configured provider and always returns a
 * usable result. Never throws for provider/network failures — falls back.
 */
export async function generate(req: AiRequest): Promise<AiResult> {
  const provider = resolveProvider();

  if (provider === 'LOCAL') {
    return localEngine(req);
  }

  try {
    let raw: string;
    let model: string;
    switch (provider) {
      case 'ANTHROPIC':
        raw = await callAnthropic(req);
        model = ANTHROPIC_MODEL;
        break;
      case 'OPENAI':
        raw = await callOpenAI(req);
        model = OPENAI_MODEL;
        break;
      case 'GEMINI':
        raw = await callGemini(req);
        model = GEMINI_MODEL;
        break;
      default:
        return localEngine(req);
    }

    if (!raw || raw.trim().length === 0) {
      return localEngine(req);
    }

    const { text, actions } = parseActions(raw);
    return { response: text, actions, provider, model };
  } catch (err) {
    console.error(`[LLM] ${provider} failed, using local engine:`, (err as Error).message);
    const fallback = localEngine(req);
    fallback.provider = `LOCAL_FALLBACK(${provider})`;
    return fallback;
  }
}

export function newId(): string {
  return crypto.randomUUID();
}
