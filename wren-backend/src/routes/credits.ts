import express from 'express';
import crypto from 'crypto';
import { getDb } from '../db/database.js';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth.js';

const router = express.Router();

// Get active point balance
router.get('/balance', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });

  try {
    const db = getDb();
    const creditRecord = await db.get('SELECT balance, updated_at FROM credits WHERE user_id = ?', [req.user.id]);
    return res.json({
      userId: req.user.id,
      balance: creditRecord ? creditRecord.balance : 0,
      updatedAt: creditRecord ? creditRecord.updated_at : new Date().toISOString()
    });
  } catch (err) {
    console.error('Fetch balance failure:', err);
    return res.status(500).json({ error: 'Server error retrieving points balance.' });
  }
});

// Get points usage and topup log history
router.get('/history', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });

  try {
    const db = getDb();
    const logs = await db.all(
      'SELECT * FROM credit_logs WHERE user_id = ? ORDER BY timestamp DESC LIMIT 100',
      [req.user.id]
    );
    return res.json({ logs });
  } catch (err) {
    console.error('Fetch transaction logs failure:', err);
    return res.status(500).json({ error: 'Server error retrieving credits logs.' });
  }
});

// Recharge credits (Mock Payment / Upgrades Tier)
router.post('/recharge', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });
  const { packageId } = req.body; // e.g. "basic_100", "premium_500", "tier_upgrade"

  if (!packageId) {
    return res.status(400).json({ error: 'Package ID is required.' });
  }

  let creditIncrement = 0;
  let reason = '';
  let makePremium = false;

  switch (packageId) {
    case 'basic_100':
      creditIncrement = 100;
      reason = 'TOPUP_BASIC_100';
      break;
    case 'premium_500':
      creditIncrement = 500;
      reason = 'TOPUP_PREMIUM_500';
      break;
    case 'subscription_monthly':
      creditIncrement = 1000;
      reason = 'SUBSCRIBE_PREMIUM_MONTHLY';
      makePremium = true;
      break;
    default:
      return res.status(400).json({ error: 'Invalid package ID requested.' });
  }

  try {
    const db = getDb();
    const timestamp = new Date().toISOString();

    await db.run('BEGIN TRANSACTION;');
    let newBalance = 0;

    try {
      // 1. Get current balance
      const current = await db.get('SELECT balance FROM credits WHERE user_id = ?', [req.user.id]);
      const currentBalance = current ? current.balance : 0;
      newBalance = currentBalance + creditIncrement;

      // 2. Update credits table
      await db.run(
        'INSERT INTO credits (user_id, balance, updated_at) VALUES (?, ?, ?) ON CONFLICT(user_id) DO UPDATE SET balance = ?, updated_at = ?',
        [req.user.id, newBalance, timestamp, newBalance, timestamp]
      );

      // 3. Log credit transaction
      const logId = crypto.randomUUID();
      await db.run(
        'INSERT INTO credit_logs (id, user_id, amount, reason, timestamp) VALUES (?, ?, ?, ?, ?)',
        [logId, req.user.id, creditIncrement, reason, timestamp]
      );

      // 4. Update user tier if needed
      if (makePremium) {
        await db.run('UPDATE users SET tier = ? WHERE id = ?', ['PREMIUM', req.user.id]);
      }

      await db.run('COMMIT;');
    } catch (txError) {
      await db.run('ROLLBACK;');
      throw txError;
    }

    return res.json({
      message: `Successfully recharged your account with ${creditIncrement} points.`,
      newBalance,
      tier: makePremium ? 'PREMIUM' : req.user.tier,
      timestamp
    });

  } catch (err) {
    console.error('Recharge credits failure:', err);
    return res.status(500).json({ error: 'Server error during credit recharge request.' });
  }
});

export default router;
