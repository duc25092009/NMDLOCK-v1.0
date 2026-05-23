/**
 * NMDLock Log Routes
 * POST /logs/client  - Submit client-side logs
 */
const express = require('express');
const router = express.Router();
const logsController = require('../controllers/logsController');

router.post('/client', logsController.submitClientLog);

module.exports = router;
