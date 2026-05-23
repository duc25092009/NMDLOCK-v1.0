// File: app/src/main/java/com/nmdlock/app/data/local/dao/LicenseDao.kt

@Dao
interface LicenseDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicense(license: License)
    
    @Query("SELECT * FROM licenses LIMIT 1")
    suspend fun getLicense(): License?
    
    // FIX: Thêm hàm getLicenseHistory nếu chưa có
    @Query("SELECT * FROM license_history ORDER BY timestamp DESC")
    suspend fun getLicenseHistory(): List<LicenseHistory>
    
    // ... existing code ...
}
