/**
 * NMDLock Authentication Service
 * Handles user registration, login, token refresh, and device binding.
 */
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const config = require('../config');
const { nowSQL, toSQL, User, Device, Session, AuditLog } = require('../models');
const logger = require('../utils/logger');

const SALT_ROUNDS = 12;

/**
 * Registers a new user account.
 */
async function register(username, email, password) {
  const existing = User.findByUsername(username);
  if (existing) {
    throw new Error('Username already exists');
  }
  if (email) {
    const emailExists = User.findByEmail(email);
    if (emailExists) {
      throw new Error('Email already registered');
    }
  }

  const hash = await bcrypt.hash(password, SALT_ROUNDS);
  const result = User.create({
    username,
    email: email || null,
    passwordHash: hash,
    role: 'user',
  });

  AuditLog.create({
    action: 'USER_REGISTER',
    entityType: 'user',
    entityId: result.lastInsertRowid,
    details: JSON.stringify({ username }),
    severity: 'info',
  });

  return { id: result.lastInsertRowid, username, email };
}

/**
 * Authenticates a user and returns JWT tokens with device binding.
 */
async function login(username, password, deviceInfo, ipAddress, userAgent) {
  const user = User.findByUsername(username);
  if (!user) {
    throw new Error('Invalid credentials');
  }
  if (!user.is_active) {
    AuditLog.create({
      action: 'LOGIN_DISABLED',
      entityType: 'user',
      entityId: user.id,
      details: JSON.stringify({ username }),
      severity: 'warning',
    });
    throw new Error('Account is disabled');
  }

  const valid = await bcrypt.compare(password, user.password_hash);
  if (!valid) {
    AuditLog.create({
      action: 'LOGIN_FAILED',
      entityType: 'user',
      entityId: user.id,
      details: JSON.stringify({ username, ip: ipAddress }),
      severity: 'warning',
    });
    throw new Error('Invalid credentials');
  }

  // Register or update device
  let device = null;
  if (deviceInfo && deviceInfo.deviceId) {
    device = Device.findByDeviceId(deviceInfo.deviceId);
    if (!device) {
      const deviceResult = Device.create({
        deviceId: deviceInfo.deviceId,
        deviceName: deviceInfo.deviceName || 'Unknown',
        deviceModel: deviceInfo.deviceModel || 'Unknown',
        androidVersion: deviceInfo.androidVersion || 'Unknown',
        firstActivationAt: nowSQL(),
        lastSeenAt: nowSQL(),
      });
      device = Device.findById(deviceResult.lastInsertRowid);
    } else {
      Device.updateLastSeen(device.id);
    }
  }

  // Generate tokens
  const accessToken = jwt.sign(
    { 
      userId: user.id, 
      username: user.username, 
      role: user.role,
      deviceId: device ? device.device_id : null,
    },
    config.jwt.secret,
    { expiresIn: config.jwt.expiresIn }
  );

  const refreshToken = jwt.sign(
    { userId: user.id, type: 'refresh' },
    config.jwt.refreshSecret,
    { expiresIn: config.jwt.refreshExpiresIn }
  );

  // Store session
  const expiresAt = toSQL(new Date(Date.now() + 7 * 86400000));
  Session.create({
    userId: user.id,
    deviceId: device ? device.id : null,
    token: accessToken,
    refreshToken: refreshToken,
    tokenType: 'access',
    ipAddress: ipAddress || null,
    userAgent: userAgent || null,
    expiresAt: expiresAt,
  });

  AuditLog.create({
    action: 'LOGIN_SUCCESS',
    entityType: 'user',
    entityId: user.id,
    deviceId: device ? device.device_id : null,
    ipAddress: ipAddress,
    details: JSON.stringify({ username, device: deviceInfo }),
    severity: 'info',
  });

  return {
    accessToken,
    refreshToken,
    expiresIn: 900, // 15 minutes in seconds
    user: {
      id: user.id,
      username: user.username,
      role: user.role,
    },
    device: device ? {
      id: device.device_id,
      name: device.device_name,
      model: device.device_model,
      isLocked: !!device.is_locked,
    } : null,
  };
}

/**
 * Refreshes an access token using a valid refresh token.
 */
async function refreshToken(refreshTokenStr, deviceId) {
  let decoded;
  try {
    decoded = jwt.verify(refreshTokenStr, config.jwt.refreshSecret);
  } catch (err) {
    throw new Error('Invalid or expired refresh token');
  }

  const session = Session.findByRefreshToken(refreshTokenStr);
  if (!session) {
    throw new Error('Refresh token has been revoked');
  }

  // Revoke old session
  Session.revoke(session.id);

  const user = User.findById(decoded.userId);
  if (!user || !user.is_active) {
    throw new Error('User account is disabled');
  }

  const device = deviceId ? Device.findByDeviceId(deviceId) : null;

  const newAccessToken = jwt.sign(
    { 
      userId: user.id, 
      username: user.username, 
      role: user.role,
      deviceId: device ? device.device_id : null,
    },
    config.jwt.secret,
    { expiresIn: config.jwt.expiresIn }
  );

  const newRefreshToken = jwt.sign(
    { userId: user.id, type: 'refresh' },
    config.jwt.refreshSecret,
    { expiresIn: config.jwt.refreshExpiresIn }
  );

  const expiresAt2 = toSQL(new Date(Date.now() + 7 * 86400000));
  Session.create({
    userId: user.id,
    deviceId: device ? device.id : null,
    token: newAccessToken,
    refreshToken: newRefreshToken,
    tokenType: 'access',
    ipAddress: null,
    userAgent: null,
    expiresAt: expiresAt2,
  });

  AuditLog.create({
    action: 'TOKEN_REFRESH',
    entityType: 'user',
    entityId: user.id,
    deviceId: device ? device.device_id : null,
    severity: 'info',
  });

  return {
    accessToken: newAccessToken,
    refreshToken: newRefreshToken,
    expiresIn: 900,
  };
}

/**
 * Logs out a user by revoking all their sessions.
 */
async function logout(userId, deviceId) {
  if (deviceId) {
    Session.revokeByDevice(deviceId);
  } else {
    Session.revokeByUser(userId);
  }

  AuditLog.create({
    action: 'LOGOUT',
    entityType: 'user',
    entityId: userId,
    deviceId: deviceId,
    severity: 'info',
  });

  return true;
}

/**
 * Validates that a token is still valid and not revoked.
 */
function validateToken(token) {
  try {
    const decoded = jwt.verify(token, config.jwt.secret);
    const session = Session.findByToken(token);
    if (!session) {
      return { valid: false, reason: 'Session not found' };
    }
    return { valid: true, user: decoded };
  } catch (err) {
    return { valid: false, reason: err.message };
  }
}

module.exports = {
  register,
  login,
  refreshToken,
  logout,
  validateToken,
};
