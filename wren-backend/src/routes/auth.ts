import express from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import crypto from 'crypto';
import { getDb } from '../db/database.js';
import { JWT_SECRET, JWT_EXPIRES_IN } from '../config.js';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth.js';

const router = express.Router();

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateCredentials(email: unknown, password: unknown): string | null {
  if (typeof email !== 'string' || typeof password !== 'string') {
    return 'Email and password must be strings.';
  }
  if (!EMAIL_REGEX.test(email)) {
    return 'A valid email address is required.';
  }
  if (password.length < 8) {
    return 'Password must be at least 8 characters long.';
  }
  return null;
}

router.post('/register', async (req, res) => {
  const { email, password } = req.body;

  const validationError = validateCredentials(email, password);
  if (validationError) {
    return res.status(400).json({ error: validationError });
  }

  const normalizedEmail = (email as string).trim().toLowerCase();

  try {
    const db = getDb();

    // Check if user already exists
    const existingUser = await db.get('SELECT * FROM users WHERE email = ?', [normalizedEmail]);
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
        [userId, normalizedEmail, passwordHash, 'USER', 'FREE', timestamp]
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
      { id: userId, email: normalizedEmail, role: 'USER', tier: 'FREE' },
      JWT_SECRET,
      { expiresIn: JWT_EXPIRES_IN } as jwt.SignOptions
    );

    return res.status(201).json({
      message: 'Account registered successfully.',
      token,
      user: { id: userId, email: normalizedEmail, role: 'USER', tier: 'FREE', balance: 50 }
    });
  } catch (err) {
    console.error('Registration failure:', err);
    return res.status(500).json({ error: 'Server error during user registration.' });
  }
});

router.post('/login', async (req, res) => {
  const { email, password } = req.body;

  if (typeof email !== 'string' || typeof password !== 'string' || !email || !password) {
    return res.status(400).json({ error: 'Email and password are required fields.' });
  }

  const normalizedEmail = email.trim().toLowerCase();

  try {
    const db = getDb();

    // Fetch user and credit balance in single join query
    const user = await db.get(`
      SELECT u.*, c.balance
      FROM users u
      LEFT JOIN credits c ON u.id = c.user_id
      WHERE u.email = ?
    `, [normalizedEmail]);

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
      { expiresIn: JWT_EXPIRES_IN } as jwt.SignOptions
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

// Return the authenticated user's live profile and balance
router.get('/me', authenticateToken, async (req: AuthenticatedRequest, res) => {
  if (!req.user) return res.status(401).json({ error: 'Unauthorized.' });

  try {
    const db = getDb();
    const user = await db.get(`
      SELECT u.id, u.email, u.role, u.tier, u.created_at, c.balance
      FROM users u
      LEFT JOIN credits c ON u.id = c.user_id
      WHERE u.id = ?
    `, [req.user.id]);

    if (!user) {
      return res.status(404).json({ error: 'User account not found.' });
    }

    return res.json({
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        tier: user.tier,
        balance: user.balance || 0,
        created_at: user.created_at,
      },
    });
  } catch (err) {
    console.error('Profile fetch failure:', err);
    return res.status(500).json({ error: 'Server error retrieving profile.' });
  }
});

export default router;
