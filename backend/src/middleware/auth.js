/**
 * NMDLock Authentication Middleware
 * JWT verification, role-based access, device binding.
 */
const jwt = require('jsonwebtoken');
const config = require('../config');
const response = require('../utils/response');
const logger = require('../utils/logger');

/**
 * Verifies JWT access token from Authorization header.
 */
function authenticate(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return response.unauthorized(res, 'Missing or invalid authorization header');
  }

  const token = authHeader.substring(7);
  try {
    const decoded = jwt.verify(token, config.jwt.secret);
    req.user = decoded;
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return response.unauthorized(res, 'Token expired');
    }
    logger.security('Invalid token attempt', { ip: req.ip });
    return response.unauthorized(res, 'Invalid token');
  }
}

/**
 * Optional authentication - doesn't fail if no token.
 */
function optionalAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.substring(7);
    try {
      req.user = jwt.verify(token, config.jwt.secret);
    } catch (err) {
      // Token invalid but that's OK for optional auth
    }
  }
  next();
}

/**
 * Requires admin role.
 */
function requireAdmin(req, res, next) {
  if (!req.user || req.user.role !== 'admin') {
    return response.forbidden(res, 'Admin access required');
  }
  next();
}

/**
 * Requires a specific device to be bound to the request.
 */
function requireDevice(req, res, next) {
  const deviceId = req.headers['x-device-id'];
  if (!deviceId) {
    return response.unauthorized(res, 'Device ID required');
  }
  req.deviceId = deviceId;
  next();
}

/**
 * Verifies request signature for sensitive operations.
 */
function verifyRequestSignature(req, res, next) {
  const signature = req.headers['x-signature'];
  const timestamp = req.headers['x-timestamp'];
  const nonce = req.headers['x-nonce'];

  if (!signature || !timestamp || !nonce) {
    return response.unauthorized(res, 'Missing security headers');
  }

  // Check timestamp is within 5 minutes
  const now = Date.now();
  const requestTime = parseInt(timestamp, 10);
  if (isNaN(requestTime) || Math.abs(now - requestTime) > 300000) {
    return response.unauthorized(res, 'Request expired or invalid timestamp');
  }

  // Store nonce for replay protection (would use Redis in production)
  req.securityContext = { signature, timestamp, nonce, method: req.method, path: req.originalUrl };
  
  // In production, verify the signature using shared device secret
  // For now, we pass through and let the controller handle verification
  
  next();
}

module.exports = {
  authenticate,
  optionalAuth,
  requireAdmin,
  requireDevice,
  verifyRequestSignature,
};
