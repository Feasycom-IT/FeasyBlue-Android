# FeasyBlue Android App（SDK + Demo）

FeasyBlue 是飞易通蓝牙模块的配置、透传与升级 App。蓝牙协议与设备操作能力由 **FeasyBlueLibrary** 提供；本仓库**不包含 FeasyBlueLibrary 源码**，只以预编译文件形式放在应用模块：

```text
feasyblue/libs/feasyblue.jar
feasyblue/libs/arm64-v8a/libFeasyBlueLibrary.so
feasyblue/libs/armeabi-v7a/libFeasyBlueLibrary.so
```

> FeasyBlue Android app source. The BLE/SPP/WiFi/TCP/OTA layer is provided by FeasyBlueLibrary as prebuilt JAR and SO files. The SDK source is not included in this repository.

---

## 下载安装包 / Download APK

| 版本        | 文件                                                   | 大小                        | SHA-256                                                            |
| --------- | ---------------------------------------------------- | ------------------------- | ------------------------------------------------------------------ |
| `3.5.7.5` | [apk/FeasyBlue3.5.7.5.apk](apk/FeasyBlue3.5.7.5.apk) | 17,323,420 B（约 16.52 MiB） | `A29369F3001FDF8E6B5F2C7886DDF44726445AD5F313D8655F3CA1A2FB0C643E` |

- 适用系统：Android 10 及以上（`minSdk 29`）；
- APK 为当前示例 App 的构建产物，正式发布前请核对签名和版本；
- 当前应用版本为 `3.5.7.5`，SDK 内部 `BuildConfig.VERSION_NAME` 为 `3.5.6.2`。

---

## 1. 引入 SDK / Add the SDK

### 1.1 仓库内的 SDK 文件

`feasyblue/libs/` 已经包含：

```text
feasyblue/libs/
├── feasyblue.jar
├── arm64-v8a/
│   └── libFeasyBlueLibrary.so
└── armeabi-v7a/
    └── libFeasyBlueLibrary.so
```

接入方在自己的 App 中使用时，把这三个文件复制到自己的 `app/libs/` 相同结构即可。

当前 `feasyblue.jar` 校验值：

- 大小：436,351 B
- SHA-256：`06F5F14A5F671EBE8A0596EC8A1A5BD534453DA4D65A18BB0BEA9EFEB08EF533`

### 1.2 Gradle 配置

```groovy
android {
    defaultConfig {
        minSdk 26
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a'
        }
    }

    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    // The JAR has no POM; the integrator must provide these dependencies.
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

完整的权限、混淆规则、回调签名和 WiFi/BW/TCP 接入示例见 [docs/FeasyBlue使用文档.md](docs/FeasyBlue使用文档.md)。

---

## 2. 编译 / Build

环境要求：**Android Studio（JDK 17）**，Android SDK **API 35**，minSdk 29。

```bash
./gradlew :feasyblue:assembleDebug
```

Windows 使用 `gradlew.bat`。

如果需要重新生成 `feasyblue.jar`，请在包含 `FeasyBlueLibrary` 源码的项目工程中构建，然后替换 `feasyblue/libs/feasyblue.jar`。

---

## 3. 应用信息 / App info

| 项                  | 值                         |
| ------------------ | ------------------------- |
| applicationId      | `com.feasycom.feasyblue`  |
| versionName        | `3.5.7.5`                 |
| versionCode        | `3575`                    |
| SDK BuildConfig    | `3.5.6.2 / 62`            |
| minSdk / targetSdk | `29` / `35`               |
| compileSdk         | `35`                      |
| ABI                | `arm64-v8a`、`armeabi-v7a` |
| UI                 | XML + ViewBinding         |

---

## 4. FeasyBlue SDK 使用文档 / SDK documents

| 语言      | 文档                                                                               |
| ------- | -------------------------------------------------------------------------------- |
| 中文      | [docs/FeasyBlue使用文档.md](docs/FeasyBlue使用文档.md)                                   |
| English | [docs/FeasyBlue_Integration_Guide_EN.md](docs/FeasyBlue_Integration_Guide_EN.md) |

文档包含 jar + so 接入、权限、OTA Service、ProGuard、BLE/SPP/WiFi-BW/TCP 初始化与回调、OTA/配网流程、数据模型、工具类、常量和常见问题。

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
│   │   ├── feasyblue.jar
│   │   ├── arm64-v8a/libFeasyBlueLibrary.so
│   │   └── armeabi-v7a/libFeasyBlueLibrary.so
│   └── src/main/...
├── build.gradle
├── settings.gradle          # 只包含 :feasyblue
├── gradle/wrapper/
├── gradlew / gradlew.bat
└── README.md
```

---

## 6. 说明 / Notes

- 本仓库只发布**对外资源**：应用层源码、预编译 SDK 二进制、示例 APK 和中英文接入文档。
- 不包含 `FeasyBlueLibrary` SDK 源码，不包含签名密钥。
- `feasyblue.jar` 与 `libFeasyBlueLibrary.so` 必须一起使用；只放 jar 会在运行时抛 `UnsatisfiedLinkError`。
- APK 位于根目录 `apk/`，根 `.gitignore` 使用 `!apk/*.apk` 允许发布 APK。
- BLE、SPP、WiFi/BW、TCP 是四条不同的链路，扫描结果和回调类型不同，不要混用。
- 正式发布前请复核权限、混淆规则、日志开关和 OTA Service 声明。
