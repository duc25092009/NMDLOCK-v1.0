# NMDLock APK Builder v1.0
# PowerShell script - build APK tu source code Android

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  NMDLock APK Builder v1.0" -ForegroundColor Cyan
Write-Host "  Tu dong build APK - can Java + Android SDK" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# === Buoc 1: Kiem tra Java ===
Write-Host " [1/5] Kiem tra Java..." -ForegroundColor Yellow
try {
    $javaTest = java -version 2>&1
    if ($javaTest -match "version") {
        Write-Host " [OK] Da tim thay Java." -ForegroundColor Green
    } else {
        throw "Java khong phan hoi"
    }
} catch {
    Write-Host " [!] LOI: Khong tim thay Java!" -ForegroundColor Red
    Write-Host "     Hay cai Java JDK 17 tai: https://adoptium.net/" -ForegroundColor Red
    Read-Host "Nhan Enter de thoat..."
    exit 1
}

# === Buoc 2: Kiem tra thu muc android ===
Write-Host " [2/5] Kiem tra thu muc android..." -ForegroundColor Yellow
if (-not (Test-Path "android")) {
    Write-Host " [!] LOI: Khong tim thay thu muc android!" -ForegroundColor Red
    Read-Host "Nhan Enter de thoat..."
    exit 1
}
if (-not (Test-Path "android/app/build.gradle.kts")) {
    Write-Host " [!] LOI: Thieu file android/app/build.gradle.kts" -ForegroundColor Red
    Read-Host "Nhan Enter de thoat..."
    exit 1
}
Write-Host " [OK] Tim thay project Android." -ForegroundColor Green

# === Buoc 3: Kiem tra Android SDK ===
Write-Host " [3/5] Kiem tra Android SDK..." -ForegroundColor Yellow

$androidHome = $env:ANDROID_HOME
$androidSdkRoot = $env:ANDROID_SDK_ROOT

if ([string]::IsNullOrEmpty($androidHome)) {
    if ([string]::IsNullOrEmpty($androidSdkRoot)) {
        Write-Host " [!] CHUA CAI ANDROID SDK!" -ForegroundColor Red
        Write-Host "     Tool se tu dong tai SDK tu Google (~150MB)..." -ForegroundColor Yellow
        Write-Host "     Vui long doi, tuy toc do mang..." -ForegroundColor Yellow
        
        $sdkPath = "$env:USERPROFILE\AppData\Local\Android\Sdk"
        $env:ANDROID_HOME = $sdkPath
        New-Item -ItemType Directory -Force -Path $sdkPath | Out-Null
        
        $ProgressPreference = 'SilentlyContinue'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        
        # Tai cmdline-tools
        $zipUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
        $zipPath = "$env:TEMP\cmdline-tools.zip"
        Write-Host "     Dang tai SDK tu Google..."
        try {
            Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath -UseBasicParsing
        } catch {
            Write-Host " [!] LOI: Khong tai duoc SDK!" -ForegroundColor Red
            Write-Host "     Kiem tra internet hoac cai Android Studio tai:" -ForegroundColor Red
            Write-Host "     https://developer.android.com/studio" -ForegroundColor Red
            Read-Host "Nhan Enter de thoat..."
            exit 1
        }
        
        if (-not (Test-Path $zipPath)) {
            Write-Host " [!] LOI: File tai ve khong ton tai!" -ForegroundColor Red
            exit 1
        }
        
        Write-Host "     Giai nen SDK Tools..."
        $extractPath = "$env:TEMP\cmdline-extract"
        Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force
        
        $latestPath = "$sdkPath\cmdline-tools\latest"
        New-Item -ItemType Directory -Force -Path $latestPath | Out-Null
        Copy-Item -Path "$extractPath\cmdline-tools\*" -Destination $latestPath -Recurse -Force
        
        Write-Host "     Dang cai SDK Platform 34..."
        $sdkManager = "$latestPath\bin\sdkmanager.bat"
        cmd.exe /c "echo y | `"$sdkManager`" --sdk_root=`"$sdkPath`" platforms;android-34 build-tools;34.0.0"
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host " [!] LOI: Cai SDK that bai!" -ForegroundColor Red
            exit 1
        }
        
        Write-Host " [OK] Da cai Android SDK xong!" -ForegroundColor Green
    } else {
        $env:ANDROID_HOME = $androidSdkRoot
        Write-Host " [OK] Da co Android SDK (ANDROID_SDK_ROOT)." -ForegroundColor Green
    }
} else {
    Write-Host " [OK] Da co Android SDK." -ForegroundColor Green
}

Write-Host " [OK] SDK: $env:ANDROID_HOME" -ForegroundColor Green

# === Buoc 4: Chuan bi build ===
Write-Host " [4/5] Dang chuan bi build..." -ForegroundColor Yellow

# Tao gradle wrapper folder
New-Item -ItemType Directory -Force -Path "android/gradle/wrapper" | Out-Null

# Tao gradle-wrapper.properties
$wrapperProps = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@
$wrapperProps | Out-File -FilePath "android/gradle/wrapper/gradle-wrapper.properties" -Encoding ascii -Force

# Tao local.properties
$sdkDir = $env:ANDROID_HOME -replace '\\', '/'
"sdk.dir=$sdkDir" | Out-File -FilePath "android/local.properties" -Encoding ascii -Force

# Tai gradle-wrapper.jar truc tiep (file nho ~57KB) thay vi tai ca Gradle zip (~130MB)
$wrapperJar = "android/gradle/wrapper/gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    Write-Host "     Dang tai gradle-wrapper.jar tu GitHub..." -ForegroundColor Yellow
    $ProgressPreference = 'SilentlyContinue'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    
    try {
        $jarUrl = "https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
        $jarPath = "$env:TEMP\gradle-wrapper.jar"
        Invoke-WebRequest -Uri $jarUrl -OutFile $jarPath -UseBasicParsing
        
        if ((Test-Path $jarPath) -and ((Get-Item $jarPath).Length -gt 10000)) {
            Copy-Item $jarPath -Destination $wrapperJar -Force
            Write-Host " [OK] Da tai gradle-wrapper.jar thanh cong!" -ForegroundColor Green
        } else {
            Write-Host " [!] File tai ve qua nho hoac khong hop le!" -ForegroundColor Red
            Write-Host "     Thu tai thu cong: mo trinh duyet, vao link:" -ForegroundColor Yellow
            Write-Host "     $jarUrl" -ForegroundColor Yellow
            Write-Host "     Luu file vao: $wrapperJar" -ForegroundColor Yellow
            Read-Host "Nhan Enter de tiep tuc neu ban da tai file thu cong..."
        }
    } catch {
        Write-Host " [!] LOI tai gradle-wrapper.jar: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "     Vui long tai thu cong bang trinh duyet:" -ForegroundColor Yellow
        Write-Host "     $jarUrl" -ForegroundColor Yellow
        Write-Host "     Luu vao: $wrapperJar" -ForegroundColor Yellow
        Read-Host "Nhan Enter de tiep tuc sau khi tai xong..."
    }
}

# Kiem tra lai gradle-wrapper.jar
if (-not (Test-Path $wrapperJar)) {
    Write-Host " [!] LOI: Thieu file gradle-wrapper.jar!" -ForegroundColor Red
    Write-Host "     Hay tai ve va dat vao: $wrapperJar" -ForegroundColor Red
    exit 1
}

# === Buoc 5: Build APK ===
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  BAT DAU BUILD APK (5-15 phut lan dau)..." -ForegroundColor Cyan
Write-Host "  Vui long doi, DUNG TAT cua so nay!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$androidPath = Resolve-Path "android"
Push-Location $androidPath
Write-Host " > gradlew.bat assembleDebug --no-daemon" -ForegroundColor Gray
Write-Host "     (Dang o: $androidPath)" -ForegroundColor Gray
cmd.exe /c "gradlew.bat assembleDebug --no-daemon"
$exitCode = $LASTEXITCODE
Pop-Location

if ($exitCode -ne 0) {
    Write-Host ""
    Write-Host " [!] LOI: Build that bai!" -ForegroundColor Red
    Write-Host "     Kiem tra loi phia tren." -ForegroundColor Red
    Write-Host "     Gui anh loi cho toi de duoc giup." -ForegroundColor Red
    Read-Host "Nhan Enter de thoat..."
    exit 1
}

# === Hoan thanh ===
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "         BUILD THANH CONG!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

$currentDir = (Get-Location).Path
$apkPath = "$currentDir\android\app\build\outputs\apk\debug\app-debug.apk"
Write-Host " File APK: $apkPath" -ForegroundColor White

if (Test-Path $apkPath) {
    Copy-Item $apkPath -Destination "$currentDir\NMDLock.apk" -Force
    Write-Host " Da copy ra: $currentDir\NMDLock.apk" -ForegroundColor Green
}

Write-Host ""
Read-Host "Nhan Enter de thoat..."
exit 0
