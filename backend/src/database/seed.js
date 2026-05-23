/**
 * NMDLock Database Seeder
 * Creates initial admin user and test data.
 */
require('dotenv').config({ path: require('path').join(__dirname, '../../.env') });
const path = require('path');
const fs = require('fs');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const { setupDatabase } = require('./setup');

function seed() {
  const db = setupDatabase();
  
  // Create admin user
  const adminUsername = process.env.ADMIN_USERNAME || 'admin';
  const adminPassword = process.env.ADMIN_PASSWORD || 'Admin@123456';
  const adminHash = bcrypt.hashSync(adminPassword, 12);

  const existingAdmin = db.prepare('SELECT id FROM users WHERE username = ?').get(adminUsername);
  if (!existingAdmin) {
    db.prepare(
      'INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)'
    ).run(adminUsername, adminHash, 'admin');
    console.log(`[SEED] Admin user created: ${adminUsername}`);
  } else {
    console.log(`[SEED] Admin user already exists`);
  }

  // Create test license key
  const testKey = 'NMDLOCK-TEST-' + uuidv4().substring(0, 8).toUpperCase();
  const existingKey = db.prepare('SELECT id FROM licenses WHERE key_value = ?').get(testKey);
  if (!existingKey) {
    db.prepare(
      `INSERT INTO licenses (key_value, type, duration_days, max_devices, max_activations, is_trial, status, notes)
       VALUES (?, 'trial', 7, 1, 1, 1, 'active', 'Auto-generated test trial key')`
    ).run(testKey);
    console.log(`[SEED] Test license created: ${testKey}`);
  }

  // Create plan templates
  const plans = [
    { name: '1 Day', type: 'daily', duration_days: 1, max_devices: 1, price: 1.99 },
    { name: '7 Days', type: 'weekly', duration_days: 7, max_devices: 1, price: 4.99 },
    { name: '30 Days', type: 'monthly', duration_days: 30, max_devices: 1, price: 9.99 },
    { name: '90 Days', type: 'monthly', duration_days: 90, max_devices: 1, price: 19.99 },
    { name: '180 Days', type: 'monthly', duration_days: 180, max_devices: 1, price: 29.99 },
    { name: '365 Days', type: 'yearly', duration_days: 365, max_devices: 1, price: 49.99 },
    { name: 'Permanent', type: 'permanent', max_devices: 1, is_permanent: 1, price: 99.99 },
    { name: '3 Devices - 30 Days', type: 'monthly', duration_days: 30, max_devices: 3, price: 19.99 },
    { name: '5 Devices - 30 Days', type: 'monthly', duration_days: 30, max_devices: 5, price: 29.99 },
  ];

  const insertPlan = db.prepare(
    `INSERT OR IGNORE INTO plan_templates (name, type, duration_days, max_devices, price, description)
     VALUES (?, ?, ?, ?, ?, ?)`
  );

  for (const plan of plans) {
    const desc = `${plan.name} - ${plan.max_devices} device(s)${plan.price ? ' - $' + plan.price : ''}`;
    insertPlan.run(plan.name, plan.type, plan.duration_days || null, plan.max_devices, plan.price || null, desc);
  }
  console.log(`[SEED] ${plans.length} plan templates created`);

  // Create default app settings
  const settings = [
    { key: 'app_name', value: 'NMDLock 1.0', type: 'string' },
    { key: 'app_version', value: '1.0.0', type: 'string' },
    { key: 'max_login_attempts', value: '5', type: 'number' },
    { key: 'lockout_duration_minutes', value: '30', type: 'number' },
    { key: 'license_cache_ttl_minutes', value: '5', type: 'number' },
    { key: 'maintenance_mode', value: 'false', type: 'boolean' },
    { key: 'allowed_android_versions', value: '["10","11","12","13","14"]', type: 'json' },
    { key: 'features', value: '{"optimization":true,"network":true,"game_profile":true,"sos":false}', type: 'json' },
  ];

  const insertSetting = db.prepare(
    `INSERT OR IGNORE INTO app_settings (setting_key, setting_value, setting_type, description)
     VALUES (?, ?, ?, ?)`
  );

  for (const s of settings) {
    insertSetting.run(s.key, s.value, s.type, `Default ${s.key} setting`);
  }
  console.log(`[SEED] ${settings.length} app settings created`);

  console.log('[SEED] Database seeding complete.');
  if (!process.env.ADMIN_PASSWORD) {
    console.warn('[SEED] ⚠️  Using default admin password! Set ADMIN_PASSWORD env var in production.');
  }
  console.log(`[SEED] Admin credentials: ${adminUsername} / ${adminPassword}`);
  console.log(`[SEED] Test key: ${testKey}`);

  db.close();
}

seed();
