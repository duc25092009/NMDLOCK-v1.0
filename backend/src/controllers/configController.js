/**
 * NMDLock Config Controller
 * Handles HTTP layer for configuration endpoints.
 */
const configService = require('../services/configService');
const response = require('../utils/response');
const logger = require('../utils/logger');

/**
 * GET /config/latest
 * Gets latest public configuration for client sync.
 */
function getLatest(req, res) {
  try {
    const config = configService.getPublicConfig();
    return response.success(res, {
      config,
      timestamp: new Date().toISOString(),
      version: '1.0.0',
    });
  } catch (err) {
    logger.error('Config error:', err.message);
    return response.error(res, 'Failed to get configuration');
  }
}

/**
 * GET /config/:key
 * Gets a specific configuration value.
 */
function getByKey(req, res) {
  try {
    const { key } = req.params;
    const setting = configService.getSetting(key);
    if (!setting) {
      return response.notFound(res, 'Setting not found');
    }
    return response.success(res, setting);
  } catch (err) {
    logger.error('Config by key error:', err.message);
    return response.error(res, 'Failed to get setting');
  }
}

module.exports = {
  getLatest,
  getByKey,
};
