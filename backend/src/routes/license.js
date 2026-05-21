/**
 * NMDLock License Routes
 * POST /license/activate  - Activate license key
 * POST /license/validate  - Validate current license
 * POST /license/redeem    - Redeem license (first-time)
 * GET  /license/me        - Get current license info
 * GET  /license/history   - Get license usage history
 */
const express = require('express');
const router = express.Router();
const licenseController = require('../controllers/licenseController');
const { authenticate } = require('../middleware/auth');
const { licenseLimiter } = require('../middleware/rateLimiter');

router.post('/activate', licenseLimiter, licenseController.activate);
router.post('/validate', licenseLimiter, licenseController.validate);
router.post('/redeem', licenseLimiter, licenseController.redeem);
router.get('/me', authenticate, licenseController.getMyLicense);
router.get('/history', authenticate, licenseController.getHistory);

module.exports = router;
