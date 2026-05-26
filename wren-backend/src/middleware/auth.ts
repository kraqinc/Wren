import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { getDb } from '../db/database.js';

const JWT_SECRET = process.env.JWT_SECRET || 'wren-super-secret-key-2026-mobile-ide';

export interface AuthenticatedRequest extends Request {
  user?: {
    id: string;
    email: string;
    role: 'USER' | 'OWNER' | 'SUPER_ADMIN';
    tier: 'FREE' | 'PREMIUM';
  };
}

export function authenticateToken(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'Access denied. No authentication token provided.' });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET) as AuthenticatedRequest['user'];
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(403).json({ error: 'Invalid or expired authentication token.' });
  }
}

export function requireRole(roles: ('USER' | 'OWNER' | 'SUPER_ADMIN')[]) {
  return (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Unauthorized.' });
    }

    if (!roles.includes(req.user.role)) {
      // Securely log unauthorized administrative attempts
      const db = getDb();
      const timestamp = new Date().toISOString();
      const auditId = crypto.randomUUID();
      
      db.run(
        'INSERT INTO audit_logs (id, actor_id, action, details, timestamp) VALUES (?, ?, ?, ?, ?)',
        [auditId, req.user.id, 'UNAUTHORIZED_ACCESS_ATTEMPT', `Attempted to access role-restricted endpoint. User role: ${req.user.role}`, timestamp]
      ).catch(console.error);

      return res.status(403).json({ error: 'Access forbidden. Insufficient privileges.' });
    }

    next();
  };
}

export async function checkCreditsCircuitBreaker(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  if (!req.user) {
    return res.status(401).json({ error: 'Unauthorized.' });
  }

  try {
    const db = getDb();
    const creditRecord = await db.get('SELECT balance FROM credits WHERE user_id = ?', [req.user.id]);

    if (!creditRecord || creditRecord.balance <= 0) {
      return res.status(402).json({
        error: 'Payment Required',
        message: 'Insufficient points balance. Please purchase more points or top up your account.',
        balance: 0
      });
    }

    next();
  } catch (err) {
    console.error('Credits verification check failed:', err);
    return res.status(500).json({ error: 'Internal system validation error during credits check.' });
  }
}
