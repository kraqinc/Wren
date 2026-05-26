import express from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import crypto from 'crypto';
import { getDb } from '../db/database.js';

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'wren-super-secret-key-2026-mobile-ide';

router.post('/register', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required fields.' });
  }

  try {
    const db = getDb();
    
    // Check if user already exists
    const existingUser = await db.get('SELECT * FROM users WHERE email = ?', [email]);
    if (existingUser) {
      return res.status(409).json({ error: 'An account with this email address already exists.' });
    }

    const userId = crypto.randomUUID();
    const passwordHash = await bcrypt.hash(password, 12);
    const timestamp = new Date().toISOString();

    // Begin database transaction for safety
    await db.run('BEGIN TRANSACTION;');

    try {
      // Insert User
      await db.run(
        'INSERT INTO users (id, email, password_hash, role, tier, created_at) VALUES (?, ?, ?, ?, ?, ?)',
        [userId, email, passwordHash, 'USER', 'FREE', timestamp]
      );

      // Insert Initial Credits (50 Points)
      await db.run(
        'INSERT INTO credits (user_id, balance, updated_at) VALUES (?, 50, ?)',
        [userId, timestamp]
      );

      // Log credit injection in transaction history
      const logId = crypto.randomUUID();
      await db.run(
        'INSERT INTO credit_logs (id, user_id, amount, reason, timestamp) VALUES (?, ?, ?, ?, ?)',
        [logId, userId, 50, 'SIGNUP_BONUS', timestamp]
      );

      await db.run('COMMIT;');
    } catch (txError) {
      await db.run('ROLLBACK;');
      throw txError;
    }

    const token = jwt.sign(
      { id: userId, email, role: 'USER', tier: 'FREE' },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    return res.status(201).json({
      message: 'Account registered successfully.',
      token,
      user: { id: userId, email, role: 'USER', tier: 'FREE', balance: 50 }
    });
  } catch (err) {
    console.error('Registration failure:', err);
    return res.status(500).json({ error: 'Server error during user registration.' });
  }
});

router.post('/login', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required fields.' });
  }

  try {
    const db = getDb();
    
    // Fetch user and credit balance in single join query
    const user = await db.get(`
      SELECT u.*, c.balance 
      FROM users u
      LEFT JOIN credits c ON u.id = c.user_id
      WHERE u.email = ?
    `, [email]);

    if (!user) {
      return res.status(401).json({ error: 'Invalid email or password.' });
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ error: 'Invalid email or password.' });
    }

    const token = jwt.sign(
      { id: user.id, email: user.email, role: user.role, tier: user.tier },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    // Track login session
    const sessionId = crypto.randomUUID();
    const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
    await db.run(
      'INSERT INTO sessions (id, user_id, token, expires_at, ip_address, user_agent) VALUES (?, ?, ?, ?, ?, ?)',
      [sessionId, user.id, token, expiresAt, req.ip, req.headers['user-agent'] || 'unknown']
    );

    return res.json({
      message: 'Login successful.',
      token,
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        tier: user.tier,
        balance: user.balance || 0
      }
    });
  } catch (err) {
    console.error('Login failure:', err);
    return res.status(500).json({ error: 'Server error during user authentication.' });
  }
});

export default router;
