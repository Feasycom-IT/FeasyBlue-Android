# FeasyBlue Android App（SDK + Demo）

FeasyBlue 是飞易通蓝牙模块的配置、透传与升级 App。蓝牙协议与设备操作能力由 **FeasyBlueLibrary** 提供；本仓库**不包含 FeasyBlueLibrary 源码**，只以预编译 **AAR** 形式放在应用模块：

```text
feasyblue/libs/FeasyBlueLibrary-3.5.6.4.aar
```

> FeasyBlue Android app source. The BLE/SPP/WiFi/TCP/OTA layer is provided by FeasyBlueLibrary as a prebuilt AAR (both `arm64-v8a` and `armeabi-v7a` native libraries are bundled inside). The SDK source is not included in this repository.

---

## 下载安装包 / Download APK

| 版本        | 文件                                                   | 大小                        | SHA-256                                                            |
| --------- | ---------------------------------------------------- | ------------------------- | ------------------------------------------------------------------ |
| `3.5.7.5` | [apk/FeasyBlue3.5.7.5.apk](apk/FeasyBlue3.5.7.5.apk) | 17,380,719 B（约 16.58 MiB） | `613A0244BE545C1D8A1F0CD85211F17B10FB20BF545099303007D9601EFF184C` |

- 适用系统：Android 10 及以上（`minSdk 29`）；
- APK 为当前示例 App 的构建产物，正式发布前请核对签名和版本；
- 当前应用版本为 `3.5.7.5`，SDK 内部 `BuildConfig.VERSION_NAME` 为 `3.5.6.4`。

---

## 1. 引入 SDK / Add the SDK

### 1.1 仓库内的 SDK 文件

`feasyblue/libs/` 已经包含：

```text
feasyblue/libs/
└── FeasyBlueLibrary-3.5.6.4.aar
```

AAR 内部结构：

```text
FeasyBlueLibrary-3.5.6.4.aar
├── classes.jar
├── jni/arm64-v8a/libFeasyBlueLibrary.so
├── jni/armeabi-v7a/libFeasyBlueLibrary.so
├── proguard.txt        # consumer rules（JNI + 对外 API keep 规则）
└── AndroidManifest.xml # 已声明蓝牙/网络权限
```

接入方在自己的 App 中使用时，把 **这一个 AAR** 复制到 `app/libs/` 即可，不需要再单独拷贝 `.so`。

当前 `FeasyBlueLibrary-3.5.6.4.aar` 校验值：

- 大小：456,704 B
- SHA-256：`C0D60B8CE26DCD558B2B3ED1F229929765F1D9EEA6B2CC4932C8C3E279379518`
- 16 KB 页对齐：已适配，两个 ABI 的 `libFeasyBlueLibrary.so` 所有 LOAD 段 `p_align = 0x4000`

### 1.2 Gradle 配置

```groovy
android {
    defaultConfig {
        minSdk 28
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a'
        }
    }
}

dependencies {
    implementation files('libs/FeasyBlueLibrary-3.5.6.4.aar')

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

环境要求：**Android Studio（JDK 17）**，Android SDK **API 36**，`minSdk 28`。

```bash
./gradlew :feasyblue:assembleDebug
```

Windows 使用 `gradlew.bat`。

如果需要重新生成 `FeasyBlueLibrary-3.5.6.4.aar`，请在包含 `FeasyBlueLibrary` 源码的项目工程中执行 `./gradlew :FeasyBlueLibrary:assembleRelease`，然后替换 `feasyblue/libs/` 下的 AAR。

---

## 3. 应用信息 / App info

| 项                  | 值                         |
| ------------------ | ------------------------- |
| applicationId      | `com.feasycom.feasyblue`  |
| versionName        | `3.5.7.5`                 |
| versionCode        | `3575`                    |
| SDK BuildConfig    | `3.5.6.4 / 64`            |
| SDK minSdk         | `28`（Android 9）           |
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
│   └── FeasyBlue3.5.7.5.apk
├── docs/
│   ├── FeasyBlue使用文档.md
│   └── FeasyBlue_Integration_Guide_EN.md
├── feasyblue/
│   ├── build.gradle
│   ├── libs/
│   │   └── FeasyBlueLibrary-3.5.6.4.aar
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