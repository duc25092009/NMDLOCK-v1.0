/**
 * NMDLock License Controller
 * Handles HTTP layer for license endpoints.
 */
const licenseService = require('../services/licenseService');
const response = require('../utils/response');
const logger = require('../utils/logger');

/**
 * POST /license/activate
 * Activates a license key for the device.
 */
async function activate(req, res) {
  try {
    const { keyValue, device } = req.body;
    if (!keyValue) {
      return response.badRequest(res, 'License key is required');
    }
    if (!device || !device.deviceId) {
      return response.badRequest(res, 'Device information is required');
    }
    const result = await licenseService.activateLicense(keyValue, device, req.ip);
    return response.success(res, result, 'License activated');
  } catch (err) {
    logger.error('License activate error:', err.message);
    return response.badRequest(res, err.message);
  }
}

/**
 * POST /license/validate
 * Validates current license status for a device.
 */
async function validate(req, res) {
  try {
    const deviceId = req.headers['x-device-id'] || req.body.deviceId;
    if (!deviceId) {
      return response.badRequest(res, 'Device ID is required');
    }
    const result = await licenseService.validateLicense(deviceId);
    return response.success(res, result);
  } catch (err) {
    logger.error('License validate error:', err.message);
    return response.badRequest(res, err.message);
  }
}

/**
 * POST /license/redeem
 * Redeems a license key (first-time setup).
 */
async function redeem(req, res) {
  try {
    const { keyValue, device } = req.body;
    if (!keyValue) {
      return response.badRequest(res, 'License key is required');
    }
    if (!device || !device.deviceId) {
      return response.badRequest(res, 'Device information is required');
    }
    const result = await licenseService.redeemLicense(keyValue, device);
    return response.success(res, result, 'License redeemed');
  } catch (err) {
    logger.error('License redeem error:', err.message);
    return response.badRequest(res, err.message);
  }
}

/**
 * GET /license/me
 * Gets current license information for the device.
 */
function getMyLicense(req, res) {
  try {
    const deviceId = req.headers['x-device-id'] || req.query.deviceId;
    if (!deviceId) {
      return response.badRequest(res, 'Device ID is required');
    }
    const license = licenseService.getCurrentLicense(deviceId);
    if (!license) {
      return response.success(res, { active: false, message: 'No active license' });
    }
    return response.success(res, license);
  } catch (err) {
    logger.error('Get license error:', err.message);
    return response.error(res, 'Failed to get license info');
  }
}

/**
 * GET /license/history
 * Gets license activation history for the device.
 */
function getHistory(req, res) {
  try {
    const deviceId = req.headers['x-device-id'] || req.query.deviceId;
    if (!deviceId) {
      return response.badRequest(res, 'Device ID is required');
    }
    const history = licenseService.getLicenseHistory(deviceId);
    return response.success(res, history);
  } catch (err) {
    logger.error('License history error:', err.message);
    return response.error(res, 'Failed to get license history');
  }
}

module.exports = {
  activate,
  validate,
  redeem,
  getMyLicense,
  getHistory,
};
