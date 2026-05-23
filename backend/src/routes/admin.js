/**
 * NMDLock Admin Routes
 * Protected endpoints for license and device management.
 */
const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const { authenticate, requireAdmin } = require('../middleware/auth');
const { detectMaliciousInput } = require('../middleware/security');
const { adminLimiter } = require('../middleware/rateLimiter');

// All admin routes require authentication and admin role
router.use(authenticate, requireAdmin, adminLimiter);
router.use(detectMaliciousInput);

router.get('/dashboard', adminController.dashboard);
router.get('/licenses', adminController.listLicenses);
router.post('/licenses/create', adminController.createLicense);
router.post('/licenses/revoke', adminController.revokeLicense);
router.post('/licenses/extend', adminController.extendLicense);
router.get('/devices', adminController.listDevices);
router.post('/devices/reset', adminController.resetDevice);
router.post('/devices/lock', adminController.lockDevice);
router.get('/audit', adminController.getAuditLogs);
router.get('/settings', adminController.getSettings);
router.post('/settings', adminController.updateSetting);

module.exports = router;
