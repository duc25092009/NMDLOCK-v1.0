/**
 * NMDLock Device Controller
 * Handles HTTP layer for device endpoints.
 */
const deviceService = require('../services/deviceService');
const response = require('../utils/response');
const logger = require('../utils/logger');

/**
 * POST /device/register
 * Registers a device with the server.
 */
function register(req, res) {
  try {
    const { deviceId, deviceName, deviceModel, androidVersion } = req.body;
    if (!deviceId) {
      return response.badRequest(res, 'Device ID is required');
    }
    const device = deviceService.registerDevice(
      { deviceId, deviceName, deviceModel, androidVersion },
      req.ip
    );
    return response.success(res, device, 'Device registered');
  } catch (err) {
    logger.error('Device register error:', err.message);
    return response.error(res, 'Device registration failed');
  }
}

/**
 * GET /device/status
 * Gets device status and license info.
 */
function status(req, res) {
  try {
    const deviceId = req.headers['x-device-id'] || req.query.deviceId;
    if (!deviceId) {
      return response.badRequest(res, 'Device ID is required');
    }
    const device = deviceService.getDeviceStatus(deviceId);
    return response.success(res, device);
  } catch (err) {
    logger.error('Device status error:', err.message);
    return response.error(res, 'Failed to get device status');
  }
}

/**
 * GET /device/history
 * Gets device login/activity history.
 */
function history(req, res) {
  try {
    const deviceId = req.headers['x-device-id'] || req.query.deviceId;
    if (!deviceId) {
      return response.badRequest(res, 'Device ID is required');
    }
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const result = deviceService.getDeviceHistory(deviceId, page, limit);
    return response.success(res, result);
  } catch (err) {
    logger.error('Device history error:', err.message);
    return response.error(res, 'Failed to get device history');
  }
}

module.exports = {
  register,
  status,
  history,
};
