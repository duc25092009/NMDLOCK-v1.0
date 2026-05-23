/**
 * NMDLock Database Setup
 * Creates tables and initializes the database.
 */
const path = require('path');
const fs = require('fs');
const Database = require('better-sqlite3');
const config = require('../config');

function setupDatabase() {
  const dbDir = path.dirname(config.db.path);
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  const db = new Database(config.db.path);
  
  // Enable WAL mode for better performance
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');

  const schema = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8');
  db.exec(schema);

  console.log(`[DB] Database initialized at ${config.db.path}`);
  return db;
}

if (require.main === module) {
  setupDatabase();
  console.log('[DB] Setup complete.');
}

module.exports = { setupDatabase };
