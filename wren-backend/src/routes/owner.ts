import express from 'express';
import crypto from 'crypto';
import { getDb } from '../db/database.js';
import { authenticateToken, AuthenticatedRequest, requireRole } from '../middleware/auth.js';

const router = express.Router();

// Apply administrative auth barrier to all sub-routes in this router
router.use(authenticateToken);
router.use(requireRole(['OWNER', 'SUPER_ADMIN']));

// 1. Get Global Server Metrics
router.get('/stats', async (req: AuthenticatedRequest, res) => {
  try {
    const db = getDb();
    
    // Aggregation queries
    const totalUsers = await db.get('SELECT COUNT(*) as count FROM users');
    const totalCredits = await db.get('SELECT SUM(balance) as sum FROM credits');
    const totalProjects = await db.get('SELECT COUNT(*) as count FROM projects');
    const auditLogsCount = await db.get('SELECT COUNT(*) as count FROM audit_logs');
    
    // Calculate total points consumed historically (sum of negative logs)
    const pointsSpent = await db.get('SELECT SUM(amount) as spent FROM credit_logs WHERE amount < 0');

    return res.json({
      metrics: {
        totalUsers: totalUsers?.count || 0,
        totalProjects: totalProjects?.count || 0,
        circulatingCredits: totalCredits?.sum || 0,
        historicalSpentCredits: Math.abs(pointsSpent?.spent || 0),
        auditLogsLogged: auditLogsCount?.count || 0,
        serverUptime: process.uptime()
      }
    });
  } catch (err) {
    console.error('Owner dashboard stats aggregation failure:', err);
    return res.status(500).json({ error: 'Server failure during administrative metrics aggregation.' });
  }
});

// 2. List all registered users
router.get('/users', async (req: AuthenticatedRequest, res) => {
  try {
    const db = getDb();
    const users = await db.all(`
      SELECT u.id, u.email, u.role, u.tier, u.created_at, c.balance as credits
      FROM users u
      LEFT JOIN credits c ON u.id = c.user_id
      ORDER BY u.created_at DESC
    `);
    return res.json({ users });
  } catch (err) {
    console.error('Owner fetch users failure:', err);
    return res.status(500).json({ error: 'Server error retrieving registered users.' });
  }
});

// 3. Edit user credits directly (Audited override)
router.post('/users/:targetUserId/credits', async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { targetUserId } = req.params;
  const { amount, reason } = req.body; // e.g. amount: 100 or -50, reason: "Refund"

  if (amount === undefined || !reason) {
    return res.status(400).json({ error: 'Adjustment amount and reason are required.' });
  }

  try {
    const db = getDb();
    const targetUser = await db.get('SELECT * FROM users WHERE id = ?', [targetUserId]);

    if (!targetUser) {
      return res.status(404).json({ error: 'Target user account not found.' });
    }

    const timestamp = new Date().toISOString();
    const auditLogId = crypto.randomUUID();
    const creditLogId = crypto.randomUUID();

    await db.run('BEGIN TRANSACTION;');
    let newBalance = 0;

    try {
      const current = await db.get('SELECT balance FROM credits WHERE user_id = ?', [targetUserId]);
      const currentBalance = current ? current.balance : 0;
      newBalance = currentBalance + amount;

      if (newBalance < 0) {
        await db.run('ROLLBACK;');
        return res.status(400).json({ error: 'Credit adjustments cannot result in negative balances.' });
      }

      // Update Credits Table
      await db.run(
        'INSERT INTO credits (user_id, balance, updated_at) VALUES (?, ?, ?) ON CONFLICT(user_id) DO UPDATE SET balance = ?, updated_at = ?',
        [targetUserId, newBalance, timestamp, newBalance, timestamp]
      );

      // Log credit adjustment in history
      await db.run(
        'INSERT INTO credit_logs (id, user_id, amount, reason, timestamp) VALUES (?, ?, ?, ?, ?)',
        [creditLogId, targetUserId, amount, `ADMIN_OVERRIDE: ${reason}`, timestamp]
      );

      // Create secure Audit Log entry
      await db.run(
        'INSERT INTO audit_logs (id, actor_id, action, details, timestamp) VALUES (?, ?, ?, ?, ?)',
        [
          auditLogId,
          req.user.id,
          'ADMIN_CREDIT_OVERRIDE',
          `Adjusted user credits for ${targetUser.email} (${targetUserId}) by ${amount}. Reason: ${reason}. Old balance: ${currentBalance}, New balance: ${newBalance}`,
          timestamp
        ]
      );

      await db.run('COMMIT;');
    } catch (txError) {
      await db.run('ROLLBACK;');
      throw txError;
    }

    return res.json({
      message: 'User credits overridden successfully.',
      userId: targetUserId,
      newBalance,
      timestamp
    });

  } catch (err) {
    console.error('Owner credit override failure:', err);
    return res.status(500).json({ error: 'Server error editing target user points balance.' });
  }
});

// 4. View Audit Logs History
router.get('/audit-logs', async (req: AuthenticatedRequest, res) => {
  try {
    const db = getDb();
    const logs = await db.all(`
      SELECT a.id, a.action, a.details, a.timestamp, u.email as actor_email
      FROM audit_logs a
      JOIN users u ON a.actor_id = u.id
      ORDER BY a.timestamp DESC
      LIMIT 200
    `);
    return res.json({ logs });
  } catch (err) {
    console.error('Owner fetch audit logs failure:', err);
    return res.status(500).json({ error: 'Server error retrieving system audit log files.' });
  }
});

export default router;
