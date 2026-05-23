/**
 * NMDLock Backend Configuration
 * Loads environment variables with safe defaults.
 */
require('dotenv').config();
const path = require('path');

const config = {
  port: parseInt(process.env.PORT, 10) || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  isDev: (process.env.NODE_ENV || 'development') === 'development',
  isProd: process.env.NODE_ENV === 'production',

  jwt: {
    secret: process.env.JWT_SECRET || (process.env.NODE_ENV === 'production' ? (() => { throw new Error('JWT_SECRET must be set in production!'); })() : 'nmdlock_jwt_secret_default'),
    refreshSecret: process.env.JWT_REFRESH_SECRET || (process.env.NODE_ENV === 'production' ? (() => { throw new Error('JWT_REFRESH_SECRET must be set in production!'); })() : 'nmdlock_refresh_secret_default'),
    expiresIn: process.env.JWT_EXPIRES_IN || '15m',
    refreshExpiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '7d',
  },

  db: {
    path: process.env.DB_PATH || path.join(__dirname, '../../data/nmdlock.db'),
  },

  admin: {
    username: process.env.ADMIN_USERNAME || 'admin',
    password: process.env.ADMIN_PASSWORD || (process.env.NODE_ENV === 'production' ? (() => { throw new Error('ADMIN_PASSWORD must be set in production!'); })() : 'Admin@123456'),
  },

  rateLimit: {
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS, 10) || 60000,
    maxRequests: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS, 10) || 30,
  },

  encryption: {
    key: process.env.ENCRYPTION_KEY || (process.env.NODE_ENV === 'production' ? (() => { throw new Error('ENCRYPTION_KEY must be set in production!'); })() : 'nmdlock_enc_key_default_32bytes!!'),
  },
};

// Warning: Using default secrets in development (not safe for production)
if (!config.isProd) {
  const usedDefaults = [];
  if (!process.env.JWT_SECRET) usedDefaults.push('JWT_SECRET');
  if (!process.env.JWT_REFRESH_SECRET) usedDefaults.push('JWT_REFRESH_SECRET');
  if (!process.env.ENCRYPTION_KEY) usedDefaults.push('ENCRYPTION_KEY');
  if (!process.env.ADMIN_PASSWORD) usedDefaults.push('ADMIN_PASSWORD');
  if (usedDefaults.length > 0) {
    console.warn(`[CONFIG] Warning: Using default values for ${usedDefaults.join(', ')}. Set these via environment variables for security.`);
  }
}

module.exports = config;
