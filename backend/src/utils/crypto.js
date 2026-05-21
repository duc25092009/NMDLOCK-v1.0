/**
 * NMDLock Crypto Utilities
 * Provides encryption, hashing, and token generation functions.
 */
const crypto = require('crypto');
const config = require('../config');

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 16;
const AUTH_TAG_LENGTH = 16;

/**
 * Encrypts a plaintext string using AES-256-GCM.
 */
function encrypt(plaintext) {
  const key = crypto.scryptSync(config.encryption.key, 'nmdlock_salt', 32);
  const iv = crypto.randomBytes(IV_LENGTH);
  const cipher = crypto.createCipheriv(ALGORITHM, key, iv);
  let encrypted = cipher.update(plaintext, 'utf8', 'hex');
  encrypted += cipher.final('hex');
  const authTag = cipher.getAuthTag().toString('hex');
  return `${iv.toString('hex')}:${authTag}:${encrypted}`;
}

/**
 * Decrypts a string encrypted with encrypt().
 */
function decrypt(ciphertext) {
  const key = crypto.scryptSync(config.encryption.key, 'nmdlock_salt', 32);
  const parts = ciphertext.split(':');
  if (parts.length !== 3) return null;
  const iv = Buffer.from(parts[0], 'hex');
  const authTag = Buffer.from(parts[1], 'hex');
  const encrypted = parts[2];
  const decipher = crypto.createDecipheriv(ALGORITHM, key, iv);
  decipher.setAuthTag(authTag);
  let decrypted = decipher.update(encrypted, 'hex', 'utf8');
  decrypted += decipher.final('utf8');
  return decrypted;
}

/**
 * Generates a unique license key with format: NMDLOCK-XXXX-XXXX-XXXX
 */
function generateLicenseKey() {
  const segments = [];
  for (let i = 0; i < 3; i++) {
    segments.push(crypto.randomBytes(4).toString('hex').toUpperCase());
  }
  return `NMDLOCK-${segments.join('-')}`;
}

/**
 * Creates a SHA-256 hash of the input string.
 */
function sha256(input) {
  return crypto.createHash('sha256').update(input).digest('hex');
}

/**
 * Generates a secure random token.
 */
function generateToken(bytes = 32) {
  return crypto.randomBytes(bytes).toString('hex');
}

/**
 * Creates a device fingerprint hash from device info.
 */
function createDeviceFingerprint(deviceId, androidVersion, model) {
  const raw = `${deviceId}|${androidVersion}|${model}|nmdlock_v1`;
  return sha256(raw);
}

/**
 * Generates a request signature for API security.
 */
function signRequest(method, path, timestamp, nonce, secret) {
  const message = `${method}:${path}:${timestamp}:${nonce}:${secret}`;
  return crypto.createHmac('sha256', secret).update(message).digest('hex');
}

/**
 * Verifies a request signature with timing-safe comparison.
 * Checks buffer lengths first to prevent errors from timingSafeEqual.
 */
function verifySignature(signature, method, path, timestamp, nonce, secret) {
  const expected = signRequest(method, path, timestamp, nonce, secret);
  const sigBuf = Buffer.from(signature);
  const expBuf = Buffer.from(expected);
  if (sigBuf.length !== expBuf.length) return false;
  return crypto.timingSafeEqual(sigBuf, expBuf);
}

module.exports = {
  encrypt,
  decrypt,
  generateLicenseKey,
  sha256,
  generateToken,
  createDeviceFingerprint,
  signRequest,
  verifySignature,
};
