/**
 * NMDLock Logs Controller
 * Handles HTTP layer for client log submission.
 */
const { AuditLog } = require('../models');
const response = require('../utils/response');
const logger = require('../utils/logger');

/**
 * POST /logs/client
 * Receives client-side logs for remote diagnostics.
 */
function submitClientLog(req, res) {
  try {
    const { level, message, details, deviceId } = req.body;
    
    if (!message) {
      return response.badRequest(res, 'Log message is required');
    }

    const severity = level === 'error' ? 'error' : 
                     level === 'warn' ? 'warning' : 'info';

    AuditLog.create({
      action: 'CLIENT_LOG',
      entityType: 'client_log',
      deviceId: deviceId || req.headers['x-device-id'] || null,
      ipAddress: req.ip,
      details: JSON.stringify({ level, message, details }),
      severity: severity,
    });

    return response.success(res, null, 'Log recorded');
  } catch (err) {
    logger.error('Client log error:', err.message);
    return response.error(res, 'Failed to record log');
  }
}

module.exports = {
  submitClientLog,
};
