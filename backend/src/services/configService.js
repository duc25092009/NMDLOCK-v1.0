/**
 * NMDLock Config Service
 * Manages app configuration synchronization from server to client.
 */
const { AppSetting } = require('../models');

/**
 * Gets all public app settings for client sync.
 */
function getPublicConfig() {
  const settings = AppSetting.getAll();
  const result = {};
  
  for (const setting of settings) {
    let value = setting.setting_value;
    
    // Parse JSON and boolean values
    if (setting.setting_type === 'json') {
      try { value = JSON.parse(value); } catch (e) { /* keep as string */ }
    } else if (setting.setting_type === 'boolean') {
      value = value === 'true';
    } else if (setting.setting_type === 'number') {
      value = parseFloat(value);
    }
    
    result[setting.setting_key] = value;
  }
  
  return result;
}

/**
 * Gets a specific setting value.
 */
function getSetting(key) {
  const setting = AppSetting.get(key);
  if (!setting) return null;
  
  let value = setting.setting_value;
  if (setting.setting_type === 'json') {
    try { value = JSON.parse(value); } catch (e) {}
  } else if (setting.setting_type === 'boolean') {
    value = value === 'true';
  } else if (setting.setting_type === 'number') {
    value = parseFloat(value);
  }
  
  return { key: setting.setting_key, value, type: setting.setting_type };
}

module.exports = {
  getPublicConfig,
  getSetting,
};
