/**
 * NMDLock Logger
 * Production/debug logging with sensitive data masking.
 */
const config = require('../config');

const SENSITIVE_PATTERNS = [
  /password[^:]*:[^,}"]+/gi,
  /token[^:]*:[^,}"]+/gi,
  /secret[^:]*:[^,}"]+/gi,
  /key_value[^:]*:[^,}"]+/gi,
  /authorization[^:]*:[^,}"]+/gi,
];

const SENSITIVE_FIELDS = ['password', 'token', 'secret', 'authorization', 'key_value'];

function maskSensitive(obj) {
  if (typeof obj === 'string') {
    let masked = obj;
    for (const pattern of SENSITIVE_PATTERNS) {
      masked = masked.replace(pattern, (match) => {
        const [key] = match.split(':');
        return `${key}:***MASKED***`;
      });
    }
    return masked;
  }
  if (obj && typeof obj === 'object') {
    const masked = Array.isArray(obj) ? [...obj] : { ...obj };
    for (const key of Object.keys(masked)) {
      if (SENSITIVE_FIELDS.some(f => key.toLowerCase().includes(f.toLowerCase()))) {
        masked[key] = '***MASKED***';
      } else if (typeof masked[key] === 'object') {
        masked[key] = maskSensitive(masked[key]);
      }
    }
    return masked;
  }
  return obj;
}

const logger = {
  debug: (...args) => {
    if (!config.isDev) return;
    const masked = args.map(maskSensitive);
    console.log('[DEBUG]', ...masked);
  },
  info: (...args) => {
    const masked = args.map(maskSensitive);
    console.log('[INFO]', ...masked);
  },
  warn: (...args) => {
    const masked = args.map(maskSensitive);
    console.warn('[WARN]', ...masked);
  },
  error: (...args) => {
    const masked = args.map(maskSensitive);
    console.error('[ERROR]', ...masked);
  },
  security: (msg, details = {}) => {
    console.warn('[SECURITY]', msg, maskSensitive(details));
  },
};

module.exports = logger;
