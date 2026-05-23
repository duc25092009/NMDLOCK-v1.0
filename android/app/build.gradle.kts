// File: app/build.gradle.kts

android {
    // ... existing config ...
    
    // FIX: Add namespace để tránh warning
    namespace = "com.nmdlock.app"
    
    // Nếu dùng TensorFlow Lite, thêm exclusion để tránh conflict
    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // FIX: Chỉ dùng tensorflow-lite, không dùng tensorflow-lite-api riêng
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // REMOVE: implementation("org.tensorflow:tensorflow-lite-api:2.14.0") // Duplicate!
}
