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
    secret: process.env.JWT_SECRET || 'nmdlock_jwt_secret_default',
    refreshSecret: process.env.JWT_REFRESH_SECRET || 'nmdlock_refresh_secret_default',
    expiresIn: process.env.JWT_EXPIRES_IN || '15m',
    refreshExpiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '7d',
  },

  db: {
    path: process.env.DB_PATH || path.join(__dirname, '../../data/nmdlock.db'),
  },

  admin: {
    username: process.env.ADMIN_USERNAME || 'admin',
    password: process.env.ADMIN_PASSWORD || 'Admin@123456',
  },

  rateLimit: {
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS, 10) || 60000,
    maxRequests: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS, 10) || 30,
  },

  encryption: {
    key: process.env.ENCRYPTION_KEY || 'nmdlock_enc_key_default_32bytes!!',
  },
};

module.exports = config;
