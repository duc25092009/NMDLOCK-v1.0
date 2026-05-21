/**
 * NMDLock License Service
 * Handles license activation, validation, and device binding.
 */
const { v4: uuidv4 } = require('uuid');
const { License, Device, Assignment, AuditLog } = require('../models');
const crypto = require('../utils/crypto');
const logger = require('../utils/logger');

const LICENSE_CACHE_TTL = 5 * 60 * 1000; // 5 minutes
const licenseCache = new Map();

/**
 * Activates a license key for a specific device.
 */
async function activateLicense(keyValue, deviceInfo, ipAddress) {
  const license = License.findByKey(keyValue);
  if (!license) {
    AuditLog.create({
      action: 'LICENSE_ACTIVATE_FAILED',
      entityType: 'license',
      details: JSON.stringify({ key: keyValue, device: deviceInfo, reason: 'Key not found' }),
      severity: 'warning',
    });
    throw new Error('Invalid license key');
  }

  // Check license status
  if (license.status === 'revoked' || license.status === 'banned') {
    AuditLog.create({
      action: 'LICENSE_ACTIVATE_BLOCKED',
      entityType: 'license',
      entityId: license.id,
      details: JSON.stringify({ key: keyValue, status: license.status }),
      severity: 'warning',
    });
    throw new Error('License key has been revoked');
  }

  if (license.status === 'expired') {
    throw new Error('License key has expired');
  }

  // Check if license has expired by date
  if (license.end_at && !license.is_permanent) {
    const endDate = new Date(license.end_at);
    if (endDate < new Date()) {
      License.updateStatus(license.id, 'expired');
      throw new Error('License key has expired');
    }
  }

  // Find or create device
  let device = Device.findByDeviceId(deviceInfo.deviceId);
  if (!device) {
    const deviceResult = Device.create({
      deviceId: deviceInfo.deviceId,
      deviceName: deviceInfo.deviceName || 'Unknown',
      deviceModel: deviceInfo.deviceModel || 'Unknown',
      androidVersion: deviceInfo.androidVersion || 'Unknown',
      firstActivationAt: new Date().toISOString(),
      lastSeenAt: new Date().toISOString(),
    });
    device = Device.findById(deviceResult.lastInsertRowid);
  } else {
    Device.updateLastSeen(device.id);
  }

  // Check if device is locked
  if (device.is_locked) {
    throw new Error('Device is locked. Reason: ' + (device.lock_reason || 'Unknown'));
  }

  // Check if device already has an active assignment for this license
  const existingAssignment = Assignment.findByLicenseAndDevice(license.id, device.id);
  if (existingAssignment) {
    // Already activated on this device - just update validation
    existingAssignment.last_validated_at = new Date().toISOString();
    License.incrementActivation(license.id);
    Device.updateLastSeen(device.id);
    
    AuditLog.create({
      action: 'LICENSE_REVALIDATED',
      entityType: 'license',
      entityId: license.id,
      deviceId: device.device_id,
      severity: 'info',
    });

    return {
      activated: true,
      revalidated: true,
      license: buildLicenseResponse(license, device),
      device: buildDeviceResponse(device),
    };
  }

  // Check device limit
  const activeCount = Assignment.countActiveByLicense(license.id);
  if (activeCount.count >= license.max_devices) {
    AuditLog.create({
      action: 'LICENSE_DEVICE_LIMIT',
      entityType: 'license',
      entityId: license.id,
      deviceId: device.device_id,
      details: JSON.stringify({ maxDevices: license.max_devices, activeCount: activeCount.count }),
      severity: 'warning',
    });
    throw new Error(`License key has reached its device limit (${license.max_devices} devices)`);
  }

  // Check max activations (one-time keys)
  if (license.is_one_time && license.activation_count >= (license.max_activations || 1)) {
    License.updateStatus(license.id, 'expired');
    throw new Error('This license key has reached its activation limit');
  }

  // Create assignment
  Assignment.create(license.id, device.id);
  License.incrementActivation(license.id);
  Device.updateLastSeen(device.id);

  // Update user assignment if not set
  if (!license.assigned_to_user_id) {
    // License is now bound to this device session
  }

  AuditLog.create({
    action: 'LICENSE_ACTIVATED',
    entityType: 'license',
    entityId: license.id,
    deviceId: device.device_id,
    ipAddress: ipAddress,
    details: JSON.stringify({ type: license.type, maxDevices: license.max_devices }),
    severity: 'info',
  });

  return {
    activated: true,
    revalidated: false,
    license: buildLicenseResponse(license, device),
    device: buildDeviceResponse(device),
  };
}

/**
 * Validates a currently active license for a device.
 * Uses caching to reduce server load.
 */
async function validateLicense(deviceId) {
  // Check cache first
  const cacheKey = `license_${deviceId}`;
  const cached = licenseCache.get(cacheKey);
  if (cached && (Date.now() - cached.timestamp) < LICENSE_CACHE_TTL) {
    return cached.data;
  }

  const device = Device.findByDeviceId(deviceId);
  if (!device) {
    throw new Error('Device not registered');
  }

  if (device.is_locked) {
    throw new Error('Device is locked: ' + (device.lock_reason || 'No reason'));
  }

  const assignments = Assignment.findByDevice(device.id);
  if (!assignments || assignments.length === 0) {
    return {
      valid: false,
      reason: 'No active license found',
      device: buildDeviceResponse(device),
    };
  }

  // Find first valid license
  for (const assignment of assignments) {
    if (assignment.status === 'expired' || assignment.status === 'revoked' || assignment.status === 'banned') {
      Assignment.release(assignment.id);
      continue;
    }

    // Check date expiration
    if (!assignment.is_permanent && assignment.end_at) {
      const endDate = new Date(assignment.end_at);
      if (endDate < new Date()) {
        License.updateStatus(assignment.license_id, 'expired');
        Assignment.release(assignment.id);
        continue;
      }
    }

    // Valid license found
    Device.updateLastSeen(device.id);
    
    // Update cache
    const result = {
      valid: true,
      license: {
        key: assignment.key_value,
        type: assignment.type,
        isPermanent: !!assignment.is_permanent,
        maxDevices: assignment.max_devices,
        assignedAt: assignment.assigned_at,
        expiresAt: assignment.end_at,
      },
      device: buildDeviceResponse(device),
    };

    licenseCache.set(cacheKey, { data: result, timestamp: Date.now() });
    return result;
  }

  // No valid license found after checking all
  return {
    valid: false,
    reason: 'No valid license found',
    device: buildDeviceResponse(device),
  };
}

/**
 * Gets license history for a device.
 */
function getLicenseHistory(deviceId) {
  const device = Device.findByDeviceId(deviceId);
  if (!device) return [];
  return Assignment.history(device.id);
}

/**
 * Gets current license info for a device.
 */
function getCurrentLicense(deviceId) {
  const device = Device.findByDeviceId(deviceId);
  if (!device) return null;

  const assignments = Assignment.findByDevice(device.id);
  if (!assignments || assignments.length === 0) return null;

  return assignments[0]; // Return most recent active
}

/**
 * Redeems a license (first-time setup).
 */
async function redeemLicense(keyValue, deviceInfo) {
  return activateLicense(keyValue, deviceInfo, null);
}

/**
 * Clears license cache for a device.
 */
function clearLicenseCache(deviceId) {
  licenseCache.delete(`license_${deviceId}`);
}

/**
 * Builds a standardized license response.
 */
function buildLicenseResponse(license, device) {
  const now = new Date();
  let expiresAt = license.end_at;
  
  // Calculate expiration for duration-based licenses
  if (!expiresAt && !license.is_permanent) {
    if (license.duration_hours) {
      const start = license.created_at ? new Date(license.created_at) : now;
      expiresAt = new Date(start.getTime() + license.duration_hours * 3600000).toISOString();
    } else if (license.duration_days) {
      const start = license.created_at ? new Date(license.created_at) : now;
      expiresAt = new Date(start.getTime() + license.duration_days * 86400000).toISOString();
    }
  }

  const endDate = expiresAt ? new Date(expiresAt) : null;
  let remainingMs = endDate ? endDate.getTime() - now.getTime() : null;
  if (remainingMs && remainingMs < 0) remainingMs = 0;

  const isExpired = endDate ? endDate < now : false;

  return {
    keyValue: license.key_value,
    type: license.type,
    status: isExpired ? 'expired' : license.status,
    isPermanent: !!license.is_permanent,
    isTrial: !!license.is_trial,
    maxDevices: license.max_devices,
    activationCount: license.activation_count,
    activatedAt: license.created_at,
    expiresAt: expiresAt,
    remainingMs: remainingMs,
    remainingDays: remainingMs ? Math.floor(remainingMs / 86400000) : null,
    remainingHours: remainingMs ? Math.floor((remainingMs % 86400000) / 3600000) : null,
    deviceCount: device ? 1 : 0,
  };
}

/**
 * Builds a standardized device response.
 */
function buildDeviceResponse(device) {
  if (!device) return null;
  return {
    deviceId: device.device_id,
    deviceName: device.device_name,
    deviceModel: device.device_model,
    androidVersion: device.android_version,
    firstActivationAt: device.first_activation_at,
    lastSeenAt: device.last_seen_at,
    isLocked: !!device.is_locked,
    lockReason: device.lock_reason,
    verifiedCount: device.verified_count,
  };
}

module.exports = {
  activateLicense,
  validateLicense,
  getLicenseHistory,
  getCurrentLicense,
  redeemLicense,
  clearLicenseCache,
  buildLicenseResponse,
  buildDeviceResponse,
};
