/**
 * NMDLock Config Routes
 * GET /config/latest  - Get latest app configuration
 * GET /config/:key    - Get specific configuration
 */
const express = require('express');
const router = express.Router();
const configController = require('../controllers/configController');
const { optionalAuth } = require('../middleware/auth');

router.get('/latest', configController.getLatest);
router.get('/:key', optionalAuth, configController.getByKey);

module.exports = router;
