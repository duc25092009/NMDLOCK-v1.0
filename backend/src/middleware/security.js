/**
 * NMDLock Security Middleware
 * Request validation, header inspection, and basic threat detection.
 */
const logger = require('../utils/logger');
const response = require('../utils/response');

/**
 * Validates required security headers from client.
 */
function validateSecurityHeaders(req, res, next) {
  const requiredHeaders = ['x-app-version', 'x-platform'];
  const missing = requiredHeaders.filter(h => !req.headers[h]);
  
  if (missing.length > 0) {
    logger.security('Missing security headers', { 
      missing, 
      ip: req.ip, 
      path: req.originalUrl 
    });
    return response.unauthorized(res, 'Missing required headers');
  }
  next();
}

/**
 * Detects common malicious patterns in request body.
 */
function detectMaliciousInput(req, res, next) {
  if (!req.body || Object.keys(req.body).length === 0) {
    return next();
  }

  const bodyStr = JSON.stringify(req.body).toLowerCase();
  const maliciousPatterns = [
    /<script/i,
    /javascript:/i,
    /onerror/i,
    /onload/i,
    /alert\(/i,
    /--\s*$/m,
    /'\s*OR\s*'1'='1/i,
    /'\s*OR\s*1\s*=\s*1/i,
    /;\s*DROP\s+TABLE/i,
    /;\s*DELETE\s+FROM/i,
    /;\s*UPDATE.*SET/i,
    /exec\s*\(/i,
    /sp_executesql/i,
    /xp_cmdshell/i,
  ];

  for (const pattern of maliciousPatterns) {
    if (pattern.test(bodyStr)) {
      logger.security('Malicious input detected', {
        pattern: pattern.source,
        ip: req.ip,
        path: req.originalUrl,
      });
      return response.badRequest(res, 'Invalid input detected');
    }
  }
  next();
}

/**
 * Adds security headers to all responses.
 */
function securityHeaders(req, res, next) {
  res.set({
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'X-XSS-Protection': '1; mode=block',
    'Strict-Transport-Security': 'max-age=31536000; includeSubDomains',
    'Cache-Control': 'no-store, no-cache, must-revalidate',
    'Pragma': 'no-cache',
  });
  next();
}

/**
 * Logs all API requests for audit trail.
 */
function auditLog(req, res, next) {
  const startTime = Date.now();
  const originalEnd = res.end;
  
  res.end = function(...args) {
    const duration = Date.now() - startTime;
    
    // Log slow requests (> 2s)
    if (duration > 2000) {
      logger.warn('Slow request detected', {
        method: req.method,
        path: req.originalUrl,
        duration: `${duration}ms`,
        ip: req.ip,
      });
    }

    // Log all errors (4xx, 5xx)
    if (res.statusCode >= 400) {
      logger.warn('Request error', {
        method: req.method,
        path: req.originalUrl,
        status: res.statusCode,
        duration: `${duration}ms`,
      });
    }

    originalEnd.apply(res, args);
  };
  
  next();
}

module.exports = {
  validateSecurityHeaders,
  detectMaliciousInput,
  securityHeaders,
  auditLog,
};
