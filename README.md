# FeasyBlue Android App（SDK + Demo）

FeasyBlue 是飞易通蓝牙模块的配置、透传与升级 App。蓝牙协议与设备操作能力由 **FeasyBlueLibrary** 提供；本仓库**不包含 FeasyBlueLibrary 源码**，只以预编译 **AAR** 形式放在应用模块：

```text
feasyblue/libs/FeasyBlueLibrary-3.6.0.9.aar
```

> FeasyBlue Android app source. The BLE/SPP/WiFi/TCP/OTA layer is provided by FeasyBlueLibrary as a prebuilt AAR (both `arm64-v8a` and `armeabi-v7a` native libraries are bundled inside). The SDK source is not included in this repository.

---

## 下载安装包 / Download APK

| 版本        | 文件                                                   | 大小                        | SHA-256                                                            |
| --------- | ---------------------------------------------------- | ------------------------- | ------------------------------------------------------------------ |
| `3.6.1.2` | [apk/FeasyBlue3.6.1.2_2026-11-26.apk](apk/FeasyBlue3.6.1.2_2026-11-26.apk) | 16,682,920 B（约 15.91 MiB） | `98AC65B988A25F298F189D9F01493627A6ACBD3A6F2F49219BB73C4D58DEBFFA` |

- 适用系统：Android 10 及以上（`minSdk 29`）；
- APK 为当前示例 App 的构建产物，正式发布前请核对签名和版本；
- 当前应用版本为 `3.6.1.2`，SDK 内部 `BuildConfig.VERSION_NAME` 为 `3.6.0.9`。

---

## 1. 引入 SDK / Add the SDK

### 1.1 仓库内的 SDK 文件

`feasyblue/libs/` 已经包含：

```text
feasyblue/libs/
└── FeasyBlueLibrary-3.6.0.9.aar
```

AAR 内部结构：

```text
FeasyBlueLibrary-3.6.0.9.aar
├── classes.jar
├── jni/arm64-v8a/libFeasyBlueLibrary.so
├── jni/armeabi-v7a/libFeasyBlueLibrary.so
├── proguard.txt        # consumer rules（JNI + 对外 API keep 规则）
└── AndroidManifest.xml # 已声明蓝牙/网络权限
```

接入方在自己的 App 中使用时，把 **这一个 AAR** 复制到 `app/libs/` 即可，不需要再单独拷贝 `.so`。

当前 `FeasyBlueLibrary-3.6.0.9.aar` 校验值：

- 大小：447,616 B
- SHA-256：`89174CCB6C388DD4900AB753926F341D015341CD11D643360F896547743B6AA4`
- 16 KB 页对齐：已适配，两个 ABI 的 `libFeasyBlueLibrary.so` 所有 LOAD 段 `p_align = 0x4000`

> **3.6.0.9 变更**：SPP 扫描不再把已配对的 BLE 设备混入结果（`startScan` / `getBondDevices` 过滤 `DEVICE_TYPE_LE`，`ACTION_FOUND` 排除 LE 设备）；修复 Android 13+ 收不到经典蓝牙发现广播的问题（扫描期间使用 `RECEIVER_EXPORTED` 注册接收器，停止扫描立即注销）；AAR 公开 API 无破坏性变更。

> **3.6.0.0 变更（破坏性）**：`minSdk` 提升到 **29（Android 10）**；移除公开方法 `BwNetUtils.isKitkatOrAbove()`（恒为 true，删掉该判断即可）；BLE 扫描时长改为可按次指定 `startScan(long timeoutMs)`，`ScannerFilter`（RSSI 阈值 + 设备名包含）现已真正生效。

### 1.2 Gradle 配置

```groovy
android {
    defaultConfig {
        minSdk 29
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a'
        }
    }
}

dependencies {
    implementation files('libs/FeasyBlueLibrary-3.6.0.9.aar')

    // AAR 不自带 POM，以下依赖需要接入方提供
    implementation "org.jetbrains.kotlin:kotlin-stdlib:2.0.0"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2"
    implementation "androidx.annotation:annotation:1.3.0"
    implementation "androidx.lifecycle:lifecycle-livedata-core:2.3.1"
    implementation "androidx.room:room-runtime:2.2.6"
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    implementation "com.squareup.okhttp3:logging-interceptor:4.9.1"
    implementation "com.google.code.gson:gson:2.8.6"
    implementation "org.nanohttpd:nanohttpd:2.3.1"
    implementation "io.netty:netty-all:5.0.0.Alpha2"
}
```

> 不再需要在 `sourceSets` 里配置 `jniLibs.srcDirs`，AAR 会自带 `jni/` 目录。

完整的权限、混淆规则、回调签名和 WiFi/BW/TCP 接入示例见 [docs/FeasyBlue使用文档.md](docs/FeasyBlue使用文档.md)。

---

## 2. 编译 / Build

环境要求：**Android Studio（JDK 17）**，Android SDK **API 36**，`minSdk 29`。

```bash
./gradlew :feasyblue:assembleDebug
```

Windows 使用 `gradlew.bat`。

如果需要重新生成 `FeasyBlueLibrary-3.6.0.9.aar`，请在包含 `FeasyBlueLibrary` 源码的项目工程中执行 `./gradlew :FeasyBlueLibrary:assembleRelease`，然后替换 `feasyblue/libs/` 下的 AAR。

---

## 3. 应用信息 / App info

| 项                  | 值                         |
| ------------------ | ------------------------- |
| applicationId      | `com.feasycom.feasyblue`  |
| versionName        | `3.6.1.2`                 |
| versionCode        | `3612`                    |
| SDK BuildConfig    | `3.6.0.9 / 67`            |
| SDK minSdk         | `29`（Android 10）           |
| SDK compileSdk     | `36`                      |
| App minSdk / targetSdk | `29` / `36`           |
| ABI                | `arm64-v8a`、`armeabi-v7a` |
| UI                 | XML + ViewBinding         |

---

## 4. FeasyBlue SDK 使用文档 / SDK documents

| 语言      | 文档                                                                               |
| ------- | -------------------------------------------------------------------------------- |
| 中文      | [docs/FeasyBlue使用文档.md](docs/FeasyBlue使用文档.md)                                   |
| English | [docs/FeasyBlue_Integration_Guide_EN.md](docs/FeasyBlue_Integration_Guide_EN.md) |

文档包含 AAR 接入、权限、OTA Service、ProGuard、BLE/SPP/WiFi-BW/TCP 初始化与回调、OTA/配网流程、数据模型、工具类、常量和常见问题。

---

## 5. 目录结构 / Layout

```text
.
├── apk/
│   └── FeasyBlue3.6.1.2_2026-11-23.apk
├── docs/
│   ├── FeasyBlue使用文档.md
│   └── FeasyBlue_Integration_Guide_EN.md
├── feasyblue/
│   ├── build.gradle
│   ├── libs/
│   │   └── FeasyBlueLibrary-3.6.0.9.aar
│   └── src/main/...
├── build.gradle
├── settings.gradle          # 只包含 :feasyblue
├── gradle/wrapper/
├── gradlew / gradlew.bat
└── README.md
```

---

## 6. 说明 / Notes

- 本仓库只发布**对外资源**：应用层源码、预编译 SDK 二进制（AAR）、示例 APK 和中英文接入文档。
- 不包含 `FeasyBlueLibrary` SDK 源码，不包含签名密钥。
- **AAR 已包含两个 ABI 的 `.so`**，只引一个 AAR 即可，不需要再单独拷贝 so。
- AAR 内的 `.so` 已按 Android 15 要求做 **16 KB 页对齐**（`p_align = 0x4000`）。
- AAR 内置 `proguard.txt`（consumer rules），接入方开启 R8/ProGuard 无需再手写 JNI keep 规则。
- APK 位于根目录 `apk/`，根 `.gitignore` 使用 `!apk/*.apk` 允许发布 APK。
- BLE、SPP、WiFi/BW、TCP 是四条不同的链路，扫描结果和回调类型不同，不要混用。
- 正式发布前请复核权限、混淆规则、日志开关和 OTA Service 声明。
