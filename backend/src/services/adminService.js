/**
 * NMDLock Admin Service
 * Handles admin operations: license CRUD, device management, audit logs.
 */
const { getDb, toSQL, User, License, Device, Assignment, AuditLog, AppSetting } = require('../models');
const crypto = require('../utils/crypto');
const { clearLicenseCache } = require('./licenseService');
const logger = require('../utils/logger');

/**
 * Creates a new license key.
 */
function createLicense(data, adminId) {
  let keyValue = data.keyValue;
  if (!keyValue) {
    keyValue = crypto.generateLicenseKey();
  }

  const existing = License.findByKey(keyValue);
  if (existing) {
    throw new Error('License key already exists');
  }

  // Calculate end date if duration provided
  let endAt = null;
  if (data.durationDays) {
    const start = new Date();
    endAt = toSQL(new Date(start.getTime() + data.durationDays * 86400000));
  } else if (data.durationHours) {
    const start = new Date();
    endAt = toSQL(new Date(start.getTime() + data.durationHours * 3600000));
  } else if (data.endAt) {
    endAt = data.endAt;
  }

  // If permanent, set a far future date
  if (data.isPermanent) {
    endAt = toSQL(new Date('2099-12-31'));
  }

  const result = License.create({
    keyValue: keyValue,
    type: data.type || 'custom',
    durationHours: data.durationHours || null,
    durationDays: data.durationDays || null,
    maxDevices: data.maxDevices || 1,
    maxActivations: data.maxActivations || null,
    isPermanent: data.isPermanent ? 1 : 0,
    isTrial: data.isTrial ? 1 : 0,
    isOneTime: data.isOneTime ? 1 : 0,
    status: data.status || 'active',
    createdBy: adminId,
    notes: data.notes || null,
  });

  AuditLog.create({
    action: 'LICENSE_CREATED',
    entityType: 'license',
    entityId: result.lastInsertRowid,
    details: JSON.stringify({ key: keyValue, type: data.type, maxDevices: data.maxDevices }),
    severity: 'info',
  });

  return { id: result.lastInsertRowid, keyValue, type: data.type };
}

/**
 * Revokes a license key.
 */
function revokeLicense(keyValue) {
  const license = License.findByKey(keyValue);
  if (!license) throw new Error('License not found');

  License.updateStatus(license.id, 'revoked');
  
  // Release all active assignments
  // In production, we'd notify connected devices
  clearLicenseCacheAll(license.id);

  AuditLog.create({
    action: 'LICENSE_REVOKED',
    entityType: 'license',
    entityId: license.id,
    details: JSON.stringify({ key: keyValue }),
    severity: 'warning',
  });

  return { revoked: true, keyValue };
}

/**
 * Extends a license by additional days.
 */
function extendLicense(keyValue, additionalDays) {
  const license = License.findByKey(keyValue);
  if (!license) throw new Error('License not found');

  if (license.is_permanent) {
    throw new Error('Cannot extend a permanent license');
  }

  const result = License.extend(license.id, additionalDays);
  if (!result) throw new Error('Failed to extend license');

  // Reactivate if expired
  if (license.status === 'expired') {
    License.updateStatus(license.id, 'active');
  }

  AuditLog.create({
    action: 'LICENSE_EXTENDED',
    entityType: 'license',
    entityId: license.id,
    details: JSON.stringify({ key: keyValue, additionalDays }),
    severity: 'info',
  });

  return { extended: true, keyValue, additionalDays };
}

/**
 * Lists all licenses with pagination.
 */
function listLicenses(page = 1, limit = 20, filter = {}) {
  return License.list(page, limit);
}

/**
 * Lists all devices with pagination.
 */
function listDevices(page = 1, limit = 20) {
  return Device.list(page, limit);
}

/**
 * Resets a device's license assignments.
 */
function resetDevice(deviceId) {
  const device = Device.findByDeviceId(deviceId);
  if (!device) throw new Error('Device not found');

  const count = Assignment.releaseByDevice(device.id);
  
  AuditLog.create({
    action: 'DEVICE_RESET',
    entityType: 'device',
    entityId: device.id,
    deviceId: device.device_id,
    details: JSON.stringify({ assignmentsReleased: count.changes }),
    severity: 'info',
  });

  return { reset: true, deviceId, assignmentsReleased: count.changes };
}

/**
 * Locks or unlocks a device.
 */
function setDeviceLock(deviceId, locked, reason = '') {
  const device = Device.findByDeviceId(deviceId);
  if (!device) throw new Error('Device not found');

  if (locked) {
    Device.lock(device.id, reason);
  } else {
    Device.unlock(device.id);
  }

  AuditLog.create({
    action: locked ? 'DEVICE_LOCKED' : 'DEVICE_UNLOCKED',
    entityType: 'device',
    entityId: device.id,
    deviceId: device.device_id,
    details: JSON.stringify({ reason }),
    severity: locked ? 'warning' : 'info',
  });

  return { locked, deviceId };
}

/**
 * Gets audit logs with pagination.
 */
function getAuditLogs(page = 1, limit = 50) {
  return AuditLog.list(page, limit);
}

/**
 * Gets dashboard statistics.
 */
function getDashboardStats() {
  const db = require('../models').getDb();
  
  const totalUsers = db.prepare('SELECT COUNT(*) as count FROM users').get();
  const totalDevices = db.prepare('SELECT COUNT(*) as count FROM devices').get();
  const totalLicenses = db.prepare('SELECT COUNT(*) as count FROM licenses').get();
  const activeLicenses = db.prepare("SELECT COUNT(*) as count FROM licenses WHERE status = 'active'").get();
  const lockedDevices = db.prepare('SELECT COUNT(*) as count FROM devices WHERE is_locked = 1').get();
  const activeSessions = db.prepare("SELECT COUNT(*) as count FROM sessions WHERE is_revoked = 0 AND expires_at > datetime('now')").get();
  
  const recentFailures = db.prepare(
    "SELECT COUNT(*) as count FROM audit_logs WHERE severity IN ('error', 'critical') AND created_at > datetime('now', '-1 hour')"
  ).get();

  return {
    totalUsers: totalUsers.count,
    totalDevices: totalDevices.count,
    totalLicenses: totalLicenses.count,
    activeLicenses: activeLicenses.count,
    lockedDevices: lockedDevices.count,
    activeSessions: activeSessions.count,
    recentFailures: recentFailures.count,
  };
}

/**
 * Gets all app settings.
 */
function getSettings() {
  return AppSetting.getAllAdmin();
}

/**
 * Updates an app setting.
 */
function updateSetting(key, value, type = 'string') {
  AppSetting.set(key, value, type);
  
  AuditLog.create({
    action: 'SETTING_UPDATED',
    entityType: 'setting',
    details: JSON.stringify({ key, type }),
    severity: 'info',
  });
  
  return { updated: true, key };
}

function clearLicenseCacheAll(licenseId) {
  // Clear license cache for all devices assigned to this license
  const assignments = getDb().prepare(
    'SELECT d.device_id FROM license_assignments la JOIN devices d ON la.device_id = d.id WHERE la.license_id = ? AND la.is_active = 1'
  ).all(licenseId);
  for (const assignment of assignments) {
    clearLicenseCache(assignment.device_id);
  }
}

module.exports = {
  createLicense,
  revokeLicense,
  extendLicense,
  listLicenses,
  listDevices,
  resetDevice,
  setDeviceLock,
  getAuditLogs,
  getDashboardStats,
  getSettings,
  updateSetting,
};
