/**
 * NMDLock Auth Routes
 * POST /auth/register  - Create account
 * POST /auth/login     - Sign in (returns JWT)
 * POST /auth/refresh   - Refresh access token
 * POST /auth/logout    - Sign out
 */
const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { authenticate } = require('../middleware/auth');
const { authLimiter } = require('../middleware/rateLimiter');

router.post('/register', authLimiter, authController.register);
router.post('/login', authLimiter, authController.login);
router.post('/refresh', authLimiter, authController.refresh);
router.post('/logout', authenticate, authController.logout);

module.exports = router;
