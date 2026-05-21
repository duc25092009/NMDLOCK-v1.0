/**
 * NMDLock Rate Limiter
 * Protects API from abuse with configurable limits.
 */
const rateLimit = require('express-rate-limit');
const config = require('../config');

/**
 * Default API rate limiter.
 */
const defaultLimiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: config.rateLimit.maxRequests,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: 'Too many requests, please try again later.',
  },
});

/**
 * Strict limiter for auth endpoints.
 */
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: 'Too many authentication attempts. Please try again later.',
  },
});

/**
 * Very strict limiter for license validation.
 */
const licenseLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: 'Too many license requests. Please slow down.',
  },
});

/**
 * Admin endpoint rate limiter.
 */
const adminLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 60,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: 'Too many admin requests.',
  },
});

module.exports = {
  defaultLimiter,
  authLimiter,
  licenseLimiter,
  adminLimiter,
};
