/**
 * NMDLock Database Models
 * Provides a clean API over raw SQLite queries.
 */
const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');
const config = require('../config');
const logger = require('../utils/logger');

let db = null;

/**
 * Gets or initializes the database connection.
 */
function getDb() {
  if (db) return db;

  const dbDir = path.dirname(config.db.path);
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  db = new Database(config.db.path);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');

  // Initialize schema if needed
  const schemaPath = path.join(__dirname, '../database/schema.sql');
  if (fs.existsSync(schemaPath)) {
    const schema = fs.readFileSync(schemaPath, 'utf8');
    db.exec(schema);
    logger.info('Database schema initialized');
  }

  return db;
}

/**
 * Closes the database connection.
 */
function closeDb() {
  if (db) {
    db.close();
    db = null;
  }
}

// User model
const User = {
  findById: (id) => getDb().prepare('SELECT * FROM users WHERE id = ? AND is_active = 1').get(id),
  findByUsername: (username) => getDb().prepare('SELECT * FROM users WHERE username = ?').get(username),
  findByEmail: (email) => getDb().prepare('SELECT * FROM users WHERE email = ?').get(email),
  create: (data) => {
    const stmt = getDb().prepare(
      'INSERT INTO users (username, email, password_hash, role) VALUES (@username, @email, @passwordHash, @role)'
    );
    return stmt.run(data);
  },
  list: (page = 1, limit = 20) => {
    const offset = (page - 1) * limit;
    const items = getDb().prepare('SELECT id, username, email, role, is_active, created_at FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?').all(limit, offset);
    const total = getDb().prepare('SELECT COUNT(*) as count FROM users').get();
    return { items, total: total.count, page, limit };
  },
};

// Device model
const Device = {
  findByDeviceId: (deviceId) => getDb().prepare('SELECT * FROM devices WHERE device_id = ?').get(deviceId),
  findById: (id) => getDb().prepare('SELECT * FROM devices WHERE id = ?').get(id),
  create: (data) => {
    const stmt = getDb().prepare(
      `INSERT INTO devices (device_id, device_name, device_model, android_version, first_activation_at, last_seen_at)
       VALUES (@deviceId, @deviceName, @deviceModel, @androidVersion, @firstActivationAt, @lastSeenAt)`
    );
    return stmt.run(data);
  },
  updateLastSeen: (id) => getDb().prepare('UPDATE devices SET last_seen_at = datetime(\'now\'), verified_count = verified_count + 1 WHERE id = ?').run(id),
  lock: (id, reason) => getDb().prepare('UPDATE devices SET is_locked = 1, lock_reason = ? WHERE id = ?').run(reason, id),
  unlock: (id) => getDb().prepare('UPDATE devices SET is_locked = 0, lock_reason = NULL WHERE id = ?').run(id),
  list: (page = 1, limit = 20) => {
    const offset = (page - 1) * limit;
    const items = getDb().prepare('SELECT * FROM devices ORDER BY last_seen_at DESC LIMIT ? OFFSET ?').all(limit, offset);
    const total = getDb().prepare('SELECT COUNT(*) as count FROM devices').get();
    return { items, total: total.count, page, limit };
  },
};

// License model
const License = {
  findByKey: (keyValue) => getDb().prepare('SELECT * FROM licenses WHERE key_value = ?').get(keyValue),
  findById: (id) => getDb().prepare('SELECT * FROM licenses WHERE id = ?').get(id),
  create: (data) => {
    const stmt = getDb().prepare(
      `INSERT INTO licenses (key_value, type, duration_hours, duration_days, max_devices, max_activations, is_permanent, is_trial, is_one_time, status, created_by, notes)
       VALUES (@keyValue, @type, @durationHours, @durationDays, @maxDevices, @maxActivations, @isPermanent, @isTrial, @isOneTime, @status, @createdBy, @notes)`
    );
    return stmt.run(data);
  },
  updateStatus: (id, status) => getDb().prepare('UPDATE licenses SET status = ?, updated_at = datetime(\'now\') WHERE id = ?').run(status, id),
  extend: (id, additionalDays) => {
    const license = License.findById(id);
    if (!license) return null;
    const currentEnd = license.end_at ? new Date(license.end_at) : new Date();
    const newEnd = new Date(currentEnd.getTime() + additionalDays * 86400000);
    return getDb().prepare('UPDATE licenses SET end_at = ?, updated_at = datetime(\'now\') WHERE id = ?').run(newEnd.toISOString(), id);
  },
  incrementActivation: (id) => getDb().prepare('UPDATE licenses SET activation_count = activation_count + 1 WHERE id = ?').run(id),
  list: (page = 1, limit = 20) => {
    const offset = (page - 1) * limit;
    const items = getDb().prepare('SELECT * FROM licenses ORDER BY created_at DESC LIMIT ? OFFSET ?').all(limit, offset);
    const total = getDb().prepare('SELECT COUNT(*) as count FROM licenses').get();
    return { items, total: total.count, page, limit };
  },
  listByUser: (userId) => getDb().prepare('SELECT * FROM licenses WHERE assigned_to_user_id = ? ORDER BY created_at DESC').all(userId),
};

// License Assignment model
const Assignment = {
  findByLicenseAndDevice: (licenseId, deviceId) => 
    getDb().prepare('SELECT * FROM license_assignments WHERE license_id = ? AND device_id = ? AND is_active = 1').get(licenseId, deviceId),
  findByDevice: (deviceId) => 
    getDb().prepare(`
      SELECT la.*, l.key_value, l.type, l.status, l.end_at, l.is_permanent, l.max_devices
      FROM license_assignments la
      JOIN licenses l ON la.license_id = l.id
      WHERE la.device_id = ? AND la.is_active = 1
    `).all(deviceId),
  create: (licenseId, deviceId) => {
    const stmt = getDb().prepare(
      'INSERT INTO license_assignments (license_id, device_id) VALUES (?, ?)'
    );
    return stmt.run(licenseId, deviceId);
  },
  release: (id) => getDb().prepare(
    'UPDATE license_assignments SET is_active = 0, released_at = datetime(\'now\') WHERE id = ?'
  ).run(id),
  releaseByDevice: (deviceId) => getDb().prepare(
    'UPDATE license_assignments SET is_active = 0, released_at = datetime(\'now\') WHERE device_id = ? AND is_active = 1'
  ).run(deviceId),
  countActiveByLicense: (licenseId) => 
    getDb().prepare('SELECT COUNT(*) as count FROM license_assignments WHERE license_id = ? AND is_active = 1').get(licenseId),
  history: (deviceId) => getDb().prepare(`
    SELECT la.*, l.key_value, l.type
    FROM license_assignments la
    JOIN licenses l ON la.license_id = l.id
    WHERE la.device_id = ?
    ORDER BY la.assigned_at DESC
  `).all(deviceId),
};

// Session model
const Session = {
  create: (data) => {
    const stmt = getDb().prepare(
      `INSERT INTO sessions (user_id, device_id, token, refresh_token, token_type, ip_address, user_agent, expires_at)
       VALUES (@userId, @deviceId, @token, @refreshToken, @tokenType, @ipAddress, @userAgent, @expiresAt)`
    );
    return stmt.run(data);
  },
  findByToken: (token) => getDb().prepare('SELECT * FROM sessions WHERE token = ? AND is_revoked = 0').get(token),
  findByRefreshToken: (token) => getDb().prepare('SELECT * FROM sessions WHERE refresh_token = ? AND is_revoked = 0').get(token),
  revoke: (id) => getDb().prepare('UPDATE sessions SET is_revoked = 1 WHERE id = ?').run(id),
  revokeByUser: (userId) => getDb().prepare('UPDATE sessions SET is_revoked = 1 WHERE user_id = ?').run(userId),
  revokeByDevice: (deviceId) => getDb().prepare('UPDATE sessions SET is_revoked = 1 WHERE device_id = ?').run(deviceId),
  cleanup: () => getDb().prepare('DELETE FROM sessions WHERE expires_at < datetime(\'now\')').run(),
};

// Audit Log model
const AuditLog = {
  create: (data) => {
    const stmt = getDb().prepare(
      `INSERT INTO audit_logs (action, entity_type, entity_id, user_id, device_id, ip_address, details, severity)
       VALUES (@action, @entityType, @entityId, @userId, @deviceId, @ipAddress, @details, @severity)`
    );
    return stmt.run(data);
  },
  list: (page = 1, limit = 50) => {
    const offset = (page - 1) * limit;
    const items = getDb().prepare('SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ? OFFSET ?').all(limit, offset);
    const total = getDb().prepare('SELECT COUNT(*) as count FROM audit_logs').get();
    return { items, total: total.count, page, limit };
  },
  listByDevice: (deviceId, page = 1, limit = 20) => {
    const offset = (page - 1) * limit;
    const items = getDb().prepare('SELECT * FROM audit_logs WHERE device_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?').all(deviceId, limit, offset);
    const total = getDb().prepare('SELECT COUNT(*) as count FROM audit_logs WHERE device_id = ?').get(deviceId);
    return { items, total: total.count, page, limit };
  },
  getRecentFailures: (minutes = 5) => {
    const since = new Date(Date.now() - minutes * 60000).toISOString();
    return getDb().prepare(
      "SELECT * FROM audit_logs WHERE severity IN ('error', 'critical') AND created_at > ? ORDER BY created_at DESC"
    ).all(since);
  },
};

// App Settings model
const AppSetting = {
  get: (key) => getDb().prepare('SELECT * FROM app_settings WHERE setting_key = ?').get(key),
  getAll: () => getDb().prepare('SELECT * FROM app_settings WHERE is_public = 1').all(),
  getAllAdmin: () => getDb().prepare('SELECT * FROM app_settings').all(),
  set: (key, value, type = 'string') => {
    const existing = AppSetting.get(key);
    if (existing) {
      return getDb().prepare('UPDATE app_settings SET setting_value = ?, updated_at = datetime(\'now\') WHERE setting_key = ?').run(value, key);
    }
    return getDb().prepare('INSERT INTO app_settings (setting_key, setting_value, setting_type) VALUES (?, ?, ?)').run(key, value, type);
  },
};

module.exports = {
  getDb,
  closeDb,
  User,
  Device,
  License,
  Assignment,
  Session,
  AuditLog,
  AppSetting,
};
