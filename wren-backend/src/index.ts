import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { initDb } from './db/database.js';
import { PORT, assertSecureConfig } from './config.js';
import { rateLimit } from './middleware/rateLimit.js';

// Load environment variables
dotenv.config();

// Fail fast on insecure defaults in production
assertSecureConfig();

// Initialize express app
const app = express();
app.set('trust proxy', 1);

// Enable CORS and JSON parsing (bounded body size to prevent abuse)
app.use(cors());
app.use(express.json({ limit: '2mb' }));

// Import feature routers
import authRouter from './routes/auth.js';
import projectsRouter from './routes/projects.js';
import aiRouter from './routes/ai.js';
import creditsRouter from './routes/credits.js';
import ownerRouter from './routes/owner.js';

// Rate limiters: strict on auth (brute-force protection), broad on the rest
const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 40, keyPrefix: 'auth' });
const apiLimiter = rateLimit({ windowMs: 60 * 1000, max: 120, keyPrefix: 'api' });
const aiLimiter = rateLimit({ windowMs: 60 * 1000, max: 30, keyPrefix: 'ai' });

// Mount routers
app.use('/api/auth', authLimiter, authRouter);
app.use('/api/projects', apiLimiter, projectsRouter);
app.use('/api/ai', aiLimiter, aiRouter);
app.use('/api/credits', apiLimiter, creditsRouter);
app.use('/api/owner', apiLimiter, ownerRouter);

// Basic health check route
app.get('/health', (req, res) => {
  res.json({
    status: 'ONLINE',
    system: 'Wren Secure Cloud Backend',
    timestamp: new Date().toISOString(),
    uptime: process.uptime()
  });
});

// Bootstrapping function
async function startServer() {
  try {
    console.log('Initializing secure storage engine...');
    await initDb();
    
    app.listen(PORT, () => {
      console.log(`==================================================`);
      console.log(` WREN SECURE CLOUD BACKEND ACTIVE               `);
      console.log(` Server Listening at: http://localhost:${PORT} `);
      console.log(` Secure API services ready for compile-time sync`);
      console.log(`==================================================`);
    });
  } catch (err) {
    console.error('Fatal backend bootstrap crash:', err);
    process.exit(1);
  }
}

startServer();
