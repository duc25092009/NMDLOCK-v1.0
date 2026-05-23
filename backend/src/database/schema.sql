-- NMDLock 1.0 Database Schema
-- SQLite-compatible schema

-- Users table: stores admin and user accounts
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user' CHECK(role IN ('admin', 'user')),
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Devices table: stores device information and binding
CREATE TABLE IF NOT EXISTS devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT UNIQUE NOT NULL,
    device_name TEXT,
    device_model TEXT,
    android_version TEXT,
    first_activation_at TEXT,
    last_seen_at TEXT NOT NULL DEFAULT (datetime('now')),
    is_locked INTEGER NOT NULL DEFAULT 0,
    lock_reason TEXT,
    verified_count INTEGER NOT NULL DEFAULT 0,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Licenses table: stores license keys and their properties
CREATE TABLE IF NOT EXISTS licenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    key_value TEXT UNIQUE NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('hourly', 'daily', 'weekly', 'monthly', 'yearly', 'permanent', 'trial', 'custom')),
    duration_hours INTEGER,
    duration_days INTEGER,
    max_devices INTEGER NOT NULL DEFAULT 1,
    max_activations INTEGER,
    activation_count INTEGER NOT NULL DEFAULT 0,
    start_at TEXT,
    end_at TEXT,
    is_permanent INTEGER NOT NULL DEFAULT 0,
    is_trial INTEGER NOT NULL DEFAULT 0,
    is_one_time INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'active' CHECK(status IN ('active', 'expired', 'revoked', 'banned', 'pending', 'trial')),
    created_by INTEGER REFERENCES users(id),
    assigned_to_user_id INTEGER REFERENCES users(id),
    notes TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- License assignments: tracks which device uses which license
CREATE TABLE IF NOT EXISTS license_assignments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    license_id INTEGER NOT NULL REFERENCES licenses(id),
    device_id INTEGER NOT NULL REFERENCES devices(id),
    assigned_at TEXT NOT NULL DEFAULT (datetime('now')),
    released_at TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    last_validated_at TEXT
);

-- Sessions table: manages user auth sessions
CREATE TABLE IF NOT EXISTS sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(id),
    device_id INTEGER REFERENCES devices(id),
    token TEXT NOT NULL,
    refresh_token TEXT,
    token_type TEXT NOT NULL DEFAULT 'access' CHECK(token_type IN ('access', 'refresh')),
    ip_address TEXT,
    user_agent TEXT,
    is_revoked INTEGER NOT NULL DEFAULT 0,
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Audit logs: tracks all important actions
CREATE TABLE IF NOT EXISTS audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id INTEGER,
    user_id INTEGER,
    device_id TEXT,
    ip_address TEXT,
    details TEXT,
    severity TEXT NOT NULL DEFAULT 'info' CHECK(severity IN ('info', 'warning', 'error', 'critical')),
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- App settings: syncs configuration from server
CREATE TABLE IF NOT EXISTS app_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    setting_key TEXT UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    setting_type TEXT NOT NULL DEFAULT 'string' CHECK(setting_type IN ('string', 'number', 'boolean', 'json')),
    description TEXT,
    is_public INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Plan templates: predefined license plans
CREATE TABLE IF NOT EXISTS plan_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    type TEXT NOT NULL,
    duration_hours INTEGER,
    duration_days INTEGER,
    max_devices INTEGER NOT NULL DEFAULT 1,
    max_activations INTEGER,
    price REAL,
    description TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_licenses_key_value ON licenses(key_value);
CREATE INDEX IF NOT EXISTS idx_licenses_status ON licenses(status);
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices(device_id);
CREATE INDEX IF NOT EXISTS idx_license_assignments_device ON license_assignments(device_id);
CREATE INDEX IF NOT EXISTS idx_license_assignments_license ON license_assignments(license_id);
CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(token);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at);
