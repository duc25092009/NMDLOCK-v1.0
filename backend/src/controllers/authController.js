/**
 * NMDLock Auth Controller
 * Handles HTTP layer for authentication endpoints.
 */
const authService = require('../services/authService');
const response = require('../utils/response');
const logger = require('../utils/logger');

/**
 * POST /auth/register
 * Registers a new user account.
 */
async function register(req, res) {
  try {
    const { username, email, password } = req.body;
    if (!username || !password) {
      return response.badRequest(res, 'Username and password are required');
    }
    if (password.length < 6) {
      return response.badRequest(res, 'Password must be at least 6 characters');
    }
    const user = await authService.register(username, email, password);
    return response.created(res, user, 'User registered successfully');
  } catch (err) {
    logger.error('Register error:', err.message);
    if (err.message.includes('already')) {
      return response.badRequest(res, err.message);
    }
    return response.error(res, 'Registration failed');
  }
}

/**
 * POST /auth/login
 * Authenticates user and returns JWT tokens.
 */
async function login(req, res) {
  try {
    const { username, password, device } = req.body;
    if (!username || !password) {
      return response.badRequest(res, 'Username and password are required');
    }
    const result = await authService.login(
      username, 
      password, 
      device || null, 
      req.ip,
      req.headers['user-agent']
    );
    return response.success(res, result, 'Login successful');
  } catch (err) {
    logger.error('Login error:', err.message);
    return response.unauthorized(res, err.message);
  }
}

/**
 * POST /auth/refresh
 * Refreshes an access token.
 */
async function refresh(req, res) {
  try {
    const { refreshToken, deviceId } = req.body;
    if (!refreshToken) {
      return response.badRequest(res, 'Refresh token is required');
    }
    const result = await authService.refreshToken(refreshToken, deviceId || null);
    return response.success(res, result, 'Token refreshed');
  } catch (err) {
    logger.error('Refresh error:', err.message);
    return response.unauthorized(res, err.message);
  }
}

/**
 * POST /auth/logout
 * Logs out user and revokes tokens.
 */
async function logout(req, res) {
  try {
    const userId = req.user ? req.user.userId : null;
    const deviceId = req.headers['x-device-id'] || null;
    await authService.logout(userId, deviceId);
    return response.success(res, null, 'Logged out successfully');
  } catch (err) {
    logger.error('Logout error:', err.message);
    return response.error(res, 'Logout failed');
  }
}

module.exports = {
  register,
  login,
  refresh,
  logout,
};
