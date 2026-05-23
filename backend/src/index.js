/**
 * NMDLock 1.0 Backend Server
 * License & Device Management API Server
 * 
 * Run: npm start
 * Setup: npm run setup && npm run seed
 */
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const cron = require('node-cron');
const path = require('path');
const fs = require('fs');

const config = require('./config');
const { getDb, Session, AuditLog } = require('./models');
const { securityHeaders, auditLog } = require('./middleware/security');
const { defaultLimiter } = require('./middleware/rateLimiter');
const logger = require('./utils/logger');

// Initialize database
const dbDir = path.dirname(config.db.path);
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}
getDb(); // Initialize DB
logger.info('Database initialized');

// Create Express app
const app = express();

// Security middleware
app.use(helmet());
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Device-ID', 'X-Signature', 'X-Timestamp', 'X-Nonce', 'X-App-Version', 'X-Platform'],
}));
app.use(securityHeaders);
app.use(auditLog);

// Request parsing
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true }));

// Logging (mask sensitive data in production)
if (config.isDev) {
  app.use(morgan('dev'));
}

// Rate limiting
app.use('/api/', defaultLimiter);

// API Routes
app.use('/api/auth', require('./routes/auth'));
app.use('/api/device', require('./routes/device'));
app.use('/api/license', require('./routes/license'));
app.use('/api/config', require('./routes/config'));
app.use('/api/logs', require('./routes/logs'));
app.use('/api/admin', require('./routes/admin'));

// Admin web interface
app.use('/admin', express.static(path.join(__dirname, '../admin')));

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: 'Endpoint not found',
    path: req.originalUrl,
  });
});

// Error handler
app.use((err, req, res, next) => {
  logger.error('Unhandled error:', err.message);
  
  AuditLog.create({
    action: 'SERVER_ERROR',
    severity: 'error',
    details: JSON.stringify({ 
      error: err.message,
      stack: config.isDev ? err.stack : undefined,
      path: req.originalUrl,
    }),
  });

  res.status(500).json({
    success: false,
    message: config.isDev ? err.message : 'Internal server error',
  });
});

// Scheduled cleanup of expired sessions (every hour)
cron.schedule('0 * * * *', () => {
  try {
    const result = Session.cleanup();
    if (result.changes > 0) {
      logger.info(`Cleaned up ${result.changes} expired sessions`);
    }
  } catch (err) {
    logger.error('Session cleanup failed:', err.message);
  }
});

// Start server
app.listen(config.port, () => {
  logger.info(`NMDLock Server v1.0.0 running on port ${config.port}`);
  logger.info(`Environment: ${config.nodeEnv}`);
  logger.info(`Admin panel: http://localhost:${config.port}/admin`);
  logger.info(`API health: http://localhost:${config.port}/api/health`);
});

// Graceful shutdown
process.on('SIGINT', () => {
  logger.info('Shutting down...');
  const { closeDb } = require('./models');
  closeDb();
  process.exit(0);
});

process.on('SIGTERM', () => {
  logger.info('SIGTERM received. Shutting down...');
  const { closeDb } = require('./models');
  closeDb();
  process.exit(0);
});

module.exports = app;
