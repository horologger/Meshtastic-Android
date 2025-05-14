# Initialize submodules
git submodule update --init --recursive

# Clean prior to build
./gradlew clean

# Generate Protocol Buffers
gradlew generateProto

# Build with gradle
./gradlew assemble

# Check for connected device
adb devices -l

# Install APK on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Uninstall APK from device
adb uninstall com.geeksville.mesh

# Display all gradle tasks
./gradlew tasks

# Other commands
export ANDROID_HOME=/Users/i830671/Library/Android/sdk
export ANDROID_SDK_ROOT=/Users/i830671/Library/Android/sdk
export ANDROID_AVD_HOME=/Users/i830671/Library/Android/avd
export ANDROID_NDK_HOME=/Users/i830671/Library/Android/sdk/ndk-bundle
export ANDROID_NDK_ROOT=/Users/i830671/Library/Android/sdk/ndk-bundle
export ANDROID_NDK_PATH=/Users/i830671/Library/Android/sdk/ndk-bundle
export ANDROID_NDK_PLATFORM=android-21