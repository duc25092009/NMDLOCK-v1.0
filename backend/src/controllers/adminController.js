/**
 * NMDLock Admin Controller
 * Handles HTTP layer for admin management endpoints.
 */
const adminService = require('../services/adminService');
const response = require('../utils/response');
const logger = require('../utils/logger');

/**
 * GET /admin/dashboard
 * Gets dashboard statistics.
 */
function dashboard(req, res) {
  try {
    const stats = adminService.getDashboardStats();
    return response.success(res, stats);
  } catch (err) {
    logger.error('Admin dashboard error:', err.message);
    return response.error(res, 'Failed to load dashboard');
  }
}

/**
 * GET /admin/licenses
 * Lists all licenses with pagination.
 */
function listLicenses(req, res) {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const result = adminService.listLicenses(page, limit);
    return response.success(res, result);
  } catch (err) {
    logger.error('Admin list licenses error:', err.message);
    return response.error(res, 'Failed to list licenses');
  }
}

/**
 * POST /admin/licenses/create
 * Creates a new license key.
 */
function createLicense(req, res) {
  try {
    const data = req.body;
    if (!data.type) {
      return response.badRequest(res, 'License type is required');
    }
    const adminId = req.user ? req.user.userId : null;
    const result = adminService.createLicense(data, adminId);
    return response.created(res, result, 'License created');
  } catch (err) {
    logger.error('Admin create license error:', err.message);
    return response.badRequest(res, err.message);
  }
}

/**
 * POST /admin/licenses/revoke
 * Revokes a license key.
 */
function revokeLicense(req, res) {
  try {
    const { keyValue } = req.body;
    if (!keyValue) {
      return response.badRequest(res, 'License key is required');
    }
    const result = adminService.revokeLicense(keyValue);
    return response.success(res, result, 'License revoked');
  } catch (err) {
    logger.error('Admin revoke license error:', err.message);
    return response.badRequest(res, err.message);
  }
}

/**
 * POST /admin/licenses/extend
 * Extends a license by additional days.
 */
function extendLicense(req, res) {
  try {
    const { keyValue, days } = req.body;
    if (!keyValue || !days) {
      return response.badRequest(res, 'License key and days are required');
    }
    if (days < 1 || days > 3650) {
      return response.badRequest(res, 'Days must be between 1 and 3650');
    }
    const result = adminService.extendLicense(keyValue, days);
    return response.success(res, result, 'License extended');
  } catch (err) {
    logger.error('Admin extend license error:', err.message);
    return response.badRequest(res, err.message);
  }
}

/**
 * GET /admin/devices
 * Lists all devices with pagination.
 */
function listDevices(req, res) {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const result = adminService.listDevices(page, limit);
    return response.success(res, result);
  } catch (err) {
    logger.error('Admin list devices error:', err.message);
    return response.error(res, 'Failed to list devices');
  }
}

/**
 * POST /admin/devices/reset
 * Resets a device's license assignments.
 */
function resetDevice(req, res) {
  try {
    const { deviceId } = req.body;
    if (!deviceId) {
      return response.badRequest(res, 'Device ID is required');
    }
    const result = adminService.resetDevice(deviceId);
    return response.success(res, result, 'Device reset');
  } catch (err) {
    logger.error('Admin reset device error:', err.message);
    return response.badRequest(res, err.message);
  }
}

/**
 * POST /admin/devices/lock
 * Locks or unlocks a device.
 */
function lockDevice(req, res) {
  try {
    const { deviceId, locked, reason } = req.body;
    if (!deviceId) {
      return response.badRequest(res, 'Device ID is required');
    }
    const result = adminService.setDeviceLock(deviceId, locked, reason || '');
    return response.success(res, result, locked ? 'Device locked' : 'Device unlocked');
  } catch (err) {
    logger.error('Admin lock device error:', err.message);
    return response.badRequest(res, err.message);
  }
}

/**
 * GET /admin/audit
 * Gets audit logs with pagination.
 */
function getAuditLogs(req, res) {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 50;
    const result = adminService.getAuditLogs(page, limit);
    return response.success(res, result);
  } catch (err) {
    logger.error('Admin audit logs error:', err.message);
    return response.error(res, 'Failed to get audit logs');
  }
}

/**
 * GET /admin/settings
 * Gets all app settings.
 */
function getSettings(req, res) {
  try {
    const settings = adminService.getSettings();
    return response.success(res, settings);
  } catch (err) {
    logger.error('Admin settings error:', err.message);
    return response.error(res, 'Failed to get settings');
  }
}

/**
 * POST /admin/settings
 * Updates an app setting.
 */
function updateSetting(req, res) {
  try {
    const { key, value, type } = req.body;
    if (!key) {
      return response.badRequest(res, 'Setting key is required');
    }
    const result = adminService.updateSetting(key, value, type || 'string');
    return response.success(res, result, 'Setting updated');
  } catch (err) {
    logger.error('Admin update setting error:', err.message);
    return response.error(res, 'Failed to update setting');
  }
}

module.exports = {
  dashboard,
  listLicenses,
  createLicense,
  revokeLicense,
  extendLicense,
  listDevices,
  resetDevice,
  lockDevice,
  getAuditLogs,
  getSettings,
  updateSetting,
};
