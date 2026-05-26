import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { initDb } from './db/database.js';

// Load environment variables
dotenv.config();

// Initialize express app
const app = express();
const PORT = process.env.PORT || 3000;

// Enable CORS and JSON parsing
app.use(cors());
app.use(express.json());

// Import feature routers
import authRouter from './routes/auth.js';
import projectsRouter from './routes/projects.js';
import aiRouter from './routes/ai.js';
import creditsRouter from './routes/credits.js';
import ownerRouter from './routes/owner.js';

// Mount routers
app.use('/api/auth', authRouter);
app.use('/api/projects', projectsRouter);
app.use('/api/ai', aiRouter);
app.use('/api/credits', creditsRouter);
app.use('/api/owner', ownerRouter);

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
