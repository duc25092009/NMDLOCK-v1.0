/**
 * NMDLock Device Service
 * Handles device registration, status, and binding management.
 */
const { Device, AuditLog } = require('../models');
const logger = require('../utils/logger');

/**
 * Registers or updates a device.
 */
function registerDevice(deviceInfo, ipAddress) {
  let device = Device.findByDeviceId(deviceInfo.deviceId);
  
  if (!device) {
    const result = Device.create({
      deviceId: deviceInfo.deviceId,
      deviceName: deviceInfo.deviceName || 'Unknown',
      deviceModel: deviceInfo.deviceModel || 'Unknown',
      androidVersion: deviceInfo.androidVersion || 'Unknown',
      firstActivationAt: new Date().toISOString(),
      lastSeenAt: new Date().toISOString(),
    });
    device = Device.findById(result.lastInsertRowid);

    AuditLog.create({
      action: 'DEVICE_REGISTERED',
      entityType: 'device',
      entityId: device.id,
      deviceId: device.device_id,
      ipAddress: ipAddress,
      details: JSON.stringify(deviceInfo),
      severity: 'info',
    });
  } else {
    Device.updateLastSeen(device.id);
    
    if (device.is_locked) {
      AuditLog.create({
        action: 'DEVICE_LOCKED_ACCESS',
        entityType: 'device',
        entityId: device.id,
        deviceId: device.device_id,
        ipAddress: ipAddress,
        details: JSON.stringify({ reason: device.lock_reason }),
        severity: 'warning',
      });
    }

    AuditLog.create({
      action: 'DEVICE_SEEN',
      entityType: 'device',
      entityId: device.id,
      deviceId: device.device_id,
      ipAddress: ipAddress,
      severity: 'info',
    });
  }

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

/**
 * Gets device status including license info.
 */
function getDeviceStatus(deviceId) {
  const device = Device.findByDeviceId(deviceId);
  if (!device) {
    return { registered: false, message: 'Device not registered' };
  }

  Device.updateLastSeen(device.id);

  return {
    registered: true,
    deviceId: device.device_id,
    deviceName: device.device_name,
    deviceModel: device.device_model,
    androidVersion: device.android_version,
    firstActivationAt: device.first_activation_at,
    lastSeenAt: device.last_seen_at,
    isLocked: !!device.is_locked,
    lockReason: device.lock_reason,
    verifiedCount: device.verified_count,
    status: device.is_locked ? 'locked' : 'active',
  };
}

/**
 * Gets login history for a device.
 */
function getDeviceHistory(deviceId, page = 1, limit = 20) {
  return AuditLog.listByDevice(deviceId, page, limit);
}

module.exports = {
  registerDevice,
  getDeviceStatus,
  getDeviceHistory,
};
