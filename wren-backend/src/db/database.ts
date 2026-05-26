import sqlite3 from 'sqlite3';
import { open, Database } from 'sqlite';
import bcrypt from 'bcryptjs';
import path from 'path';
import fs from 'fs';

let db: Database<sqlite3.Database, sqlite3.Statement>;

export async function initDb(): Promise<Database<sqlite3.Database, sqlite3.Statement>> {
  const dbDir = path.join(__dirname, '..', '..', 'data');
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  const dbPath = path.join(dbDir, 'wren.sqlite');

  db = await open({
    filename: dbPath,
    driver: sqlite3.Database
  });

  // Enable foreign key constraints
  await db.run('PRAGMA foreign_keys = ON;');

  // Create Users Table
  await db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      role TEXT CHECK(role IN ('USER', 'OWNER', 'SUPER_ADMIN')) NOT NULL DEFAULT 'USER',
      tier TEXT CHECK(tier IN ('FREE', 'PREMIUM')) NOT NULL DEFAULT 'FREE',
      created_at TEXT NOT NULL
    );
  `);

  // Create Sessions Table
  await db.exec(`
    CREATE TABLE IF NOT EXISTS sessions (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      token TEXT NOT NULL,
      expires_at TEXT NOT NULL,
      ip_address TEXT,
      user_agent TEXT,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
  `);

  // Create Projects Table
  await db.exec(`
    CREATE TABLE IF NOT EXISTS projects (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      name TEXT NOT NULL,
      description TEXT,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
  `);

  // Create Files Table
  await db.exec(`
    CREATE TABLE IF NOT EXISTS files (
      id TEXT PRIMARY KEY,
      project_id TEXT NOT NULL,
      name TEXT NOT NULL,
      path TEXT NOT NULL,
      is_directory INTEGER NOT NULL CHECK(is_directory IN (0, 1)),
      content TEXT,
      parent_id TEXT,
      FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
      FOREIGN KEY (parent_id) REFERENCES files(id) ON DELETE SET NULL
    );
  `);

  // Create Credits Table
  await db.exec(`
    CREATE TABLE IF NOT EXISTS credits (
      user_id TEXT PRIMARY KEY,
      balance INTEGER NOT NULL DEFAULT 50,
      updated_at TEXT NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
  `);

  // Create Credit Logs Table
  await db.exec(`
    CREATE TABLE IF NOT EXISTS credit_logs (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      amount INTEGER NOT NULL,
      reason TEXT NOT NULL,
      timestamp TEXT NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
  `);

  // Create Audit Logs Table
  await db.exec(`
    CREATE TABLE IF NOT EXISTS audit_logs (
      id TEXT PRIMARY KEY,
      actor_id TEXT NOT NULL,
      action TEXT NOT NULL,
      details TEXT,
      timestamp TEXT NOT NULL,
      FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE CASCADE
    );
  `);

  // Create API Keys Table
  await db.exec(`
    CREATE TABLE IF NOT EXISTS api_keys (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      key_hash TEXT NOT NULL,
      provider TEXT CHECK(provider IN ('OPENAI', 'ANTHROPIC', 'GEMINI')) NOT NULL,
      created_at TEXT NOT NULL,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
  `);

  // Seed default OWNER user if it does not exist
  const adminEmail = process.env.OWNER_EMAIL || 'owner@wren.ide';
  const adminPassword = process.env.OWNER_PASSWORD || 'WrenOwner2026!';
  
  const existingOwner = await db.get('SELECT * FROM users WHERE role = ?', 'OWNER');
  if (!existingOwner) {
    const ownerId = 'default-owner-uuid-2026';
    const passwordHash = await bcrypt.hash(adminPassword, 12);
    const timestamp = new Date().toISOString();

    await db.run(
      'INSERT INTO users (id, email, password_hash, role, tier, created_at) VALUES (?, ?, ?, ?, ?, ?)',
      [ownerId, adminEmail, passwordHash, 'OWNER', 'PREMIUM', timestamp]
    );

    // Initialise owner credits
    await db.run(
      'INSERT INTO credits (user_id, balance, updated_at) VALUES (?, ?, ?)',
      [ownerId, 999999, timestamp]
    );

    console.log(`[SEED] Default owner account created successfully: ${adminEmail}`);
    console.log(`[SEED] Initial credits for owner set to 999999 points.`);
  }

  return db;
}

export function getDb() {
  if (!db) {
    throw new Error('Database not initialized! Call initDb() first.');
  }
  return db;
}
