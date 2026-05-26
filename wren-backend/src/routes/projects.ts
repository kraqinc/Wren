import express from 'express';
import crypto from 'crypto';
import { getDb } from '../db/database.js';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth.js';

const router = express.Router();

// Get all projects owned by authenticated user
router.get('/', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });

  try {
    const db = getDb();
    const projects = await db.all('SELECT * FROM projects WHERE user_id = ? ORDER BY updated_at DESC', [req.user.id]);
    return res.json({ projects });
  } catch (err) {
    console.error('Fetch projects failure:', err);
    return res.status(500).json({ error: 'Server error retrieving projects.' });
  }
});

// Create new project
router.post('/', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { name, description } = req.body;

  if (!name) {
    return res.status(400).json({ error: 'Project name is required.' });
  }

  try {
    const db = getDb();
    const projectId = crypto.randomUUID();
    const timestamp = new Date().toISOString();

    await db.run(
      'INSERT INTO projects (id, user_id, name, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)',
      [projectId, req.user.id, name, description || '', timestamp, timestamp]
    );

    return res.status(201).json({
      message: 'Project created successfully.',
      project: { id: projectId, name, description, created_at: timestamp, updated_at: timestamp }
    });
  } catch (err) {
    console.error('Create project failure:', err);
    return res.status(500).json({ error: 'Server error creating project.' });
  }
});

// Get project details and all its files (in flat list to build tree)
router.get('/:projectId/files', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { projectId } = req.params;

  try {
    const db = getDb();
    
    // Verify project ownership
    const project = await db.get('SELECT * FROM projects WHERE id = ? AND user_id = ?', [projectId, req.user.id]);
    if (!project) {
      return res.status(404).json({ error: 'Project not found or access denied.' });
    }

    const files = await db.all('SELECT * FROM files WHERE project_id = ?', [projectId]);
    return res.json({ project, files });
  } catch (err) {
    console.error('Fetch files failure:', err);
    return res.status(500).json({ error: 'Server error retrieving project files.' });
  }
});

// Create a file or directory inside a project
router.post('/:projectId/files', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { projectId } = req.params;
  const { name, path: filePath, isDirectory, content, parentId } = req.body;

  if (!name || filePath === undefined || isDirectory === undefined) {
    return res.status(400).json({ error: 'Name, path, and isDirectory indicators are required.' });
  }

  try {
    const db = getDb();
    
    // Verify project ownership
    const project = await db.get('SELECT * FROM projects WHERE id = ? AND user_id = ?', [projectId, req.user.id]);
    if (!project) {
      return res.status(404).json({ error: 'Project not found or access denied.' });
    }

    const fileId = crypto.randomUUID();
    const isDirNum = isDirectory ? 1 : 0;
    const timestamp = new Date().toISOString();

    await db.run('BEGIN TRANSACTION;');
    try {
      await db.run(
        'INSERT INTO files (id, project_id, name, path, is_directory, content, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?)',
        [fileId, projectId, name, filePath, isDirNum, isDirectory ? null : (content || ''), parentId || null]
      );

      // Update project modification timestamp
      await db.run('UPDATE projects SET updated_at = ? WHERE id = ?', [timestamp, projectId]);
      await db.run('COMMIT;');
    } catch (txError) {
      await db.run('ROLLBACK;');
      throw txError;
    }

    return res.status(201).json({
      message: 'File record created successfully.',
      file: { id: fileId, name, path: filePath, isDirectory, content: isDirectory ? null : (content || ''), parentId }
    });
  } catch (err) {
    console.error('Create file record failure:', err);
    return res.status(500).json({ error: 'Server error saving file details.' });
  }
});

// Update file content
router.put('/:projectId/files/:fileId', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { projectId, fileId } = req.params;
  const { content } = req.body;

  try {
    const db = getDb();
    
    // Verify project ownership
    const project = await db.get('SELECT * FROM projects WHERE id = ? AND user_id = ?', [projectId, req.user.id]);
    if (!project) {
      return res.status(404).json({ error: 'Project not found or access denied.' });
    }

    const timestamp = new Date().toISOString();

    await db.run('BEGIN TRANSACTION;');
    try {
      await db.run(
        'UPDATE files SET content = ? WHERE id = ? AND project_id = ?',
        [content || '', fileId, projectId]
      );
      await db.run('UPDATE projects SET updated_at = ? WHERE id = ?', [timestamp, projectId]);
      await db.run('COMMIT;');
    } catch (txError) {
      await db.run('ROLLBACK;');
      throw txError;
    }

    return res.json({ message: 'File saved successfully.' });
  } catch (err) {
    console.error('Update file failure:', err);
    return res.status(500).json({ error: 'Server error saving file.' });
  }
});

// Delete file or folder
router.delete('/:projectId/files/:fileId', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { projectId, fileId } = req.params;

  try {
    const db = getDb();
    
    // Verify project ownership
    const project = await db.get('SELECT * FROM projects WHERE id = ? AND user_id = ?', [projectId, req.user.id]);
    if (!project) {
      return res.status(404).json({ error: 'Project not found or access denied.' });
    }

    const timestamp = new Date().toISOString();

    await db.run('BEGIN TRANSACTION;');
    try {
      // If it's a directory, this simple schema relies on cascade or setting null for kids.
      // We will perform recursive deletion or standard delete.
      await db.run('DELETE FROM files WHERE id = ? AND project_id = ?', [fileId, projectId]);
      await db.run('UPDATE projects SET updated_at = ? WHERE id = ?', [timestamp, projectId]);
      await db.run('COMMIT;');
    } catch (txError) {
      await db.run('ROLLBACK;');
      throw txError;
    }

    return res.json({ message: 'File deleted successfully.' });
  } catch (err) {
    console.error('Delete file failure:', err);
    return res.status(500).json({ error: 'Server error deleting file.' });
  }
});

export default router;
