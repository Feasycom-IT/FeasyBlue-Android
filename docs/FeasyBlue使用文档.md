# FeasyBlue Android SDK 接入文档（AAR 版）

> **交付物**：`FeasyBlueLibrary-3.5.6.4.aar`（内含 arm64-v8a / armeabi-v7a 的 .so）+ 示例 APK + 接入文档
> **交付包版本**：`3.5.7.5`
> **SDK BuildConfig**：`3.5.6.4 / 64`
> **包名**：`com.feasycom`
> **最低系统版本**：Android 9（API 28）
> **支持 ABI**：`arm64-v8a`、`armeabi-v7a`
> **Demo 最低版本**：Android 10（API 29）
>
> 本文按当前 FeasyBlue 3.5.7.5 交付包结构整理。示例以 Kotlin 为主，Java 工程可以直接调用同名接口。
> English version: [FeasyBlue_Integration_Guide_EN.md](FeasyBlue_Integration_Guide_EN.md)

---

## 0. 先选对 API

FeasyBlue 不是一个“万能蓝牙接口”，而是按设备类型分成了几套链路。接入前先确认目标设备属于哪一种：

| 你的设备/场景 | 应该使用的 API | 扫描结果类型 |
|---|---|---|
| 普通 BLE 模块：BLE 透传、参数修改、BLE OTA | `FscBleCentralApi` / `FscBleCentralApiImp` | `com.feasycom.common.bean.FscDevice` |
| 经典蓝牙 SPP 模块：串口透传、SDP、SPP OTA | `FscSppCentralApi` / `FscSppCentralApiImp` | `com.feasycom.common.bean.FscDevice` |
| WiFi/BW 模块：BLE 配网、AT 参数、WiFi OTA | `FscBleCentralBwApi` / `FscBleCentralBwApiImp` | `com.feasycom.wifi.bean.BluetoothDeviceWrapper` |
| BW246 等模块的 ESP Touch 简易配网 | `ConfigNetworkBwApi` / `ConfigNetworkBwApiImp` | `StateBwResult` |
| TCP / BW256B 高速透传 | `TCPClientManager` / `FscClientBwBwApiImp` | 无扫描结果 |
| 云端固件、协议、广告图 | `DeviceNetwork` | 无扫描结果 |

最容易混的是下面两套：

- `com.feasycom.ble` 是**通用 BLE 中心设备**，可以扫描、连接、读写 GATT、升级 BLE 模块。
- `com.feasycom.wifi.ble` 是**WiFi/BW 模块专用的 BLE 配网通道**，使用固定 UUID `fff0/fff1/fff2`，通过 AT 指令控制模块，不是通用 BLE 扫描器。

不要把这两套的扫描结果、回调接口和连接对象混用。

---

## 1. 交付内容

当前 GitHub 仓库的目录结构如下：

```text
FeasyBlue-Android/
├── apk/
│   └── FeasyBlue3.5.7.5.apk       # 示例 APK
├── docs/
│   ├── FeasyBlue使用文档.md       # 本文（中文）
│   └── FeasyBlue_Integration_Guide_EN.md   # English version
├── feasyblue/                     # 应用层源码
│   ├── build.gradle
│   ├── libs/
│   │   └── FeasyBlueLibrary-3.5.6.4.aar   # 内含两个 ABI 的 .so
│   └── src/main/...
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
├── gradle/wrapper/
└── README.md
```

各文件作用：

| 文件 | 作用 |
|---|---|
| `feasyblue/libs/FeasyBlueLibrary-3.5.6.4.aar` | SDK 的 Java/Kotlin 类 + JNI 原生库（arm64-v8a、armeabi-v7a） |
| `apk/FeasyBlue3.5.7.5.apk` | 示例 APK，可直接安装体验 |
| `README.md` | 仓库说明、APK 大小和 SHA-256 |
| `docs/FeasyBlue使用文档.md` | 本文 |
| `docs/FeasyBlue_Integration_Guide_EN.md` | English integration guide |
| `feasyblue` | 已接入 AAR 的应用工程，可直接用 Android Studio 打开 |

注意：

- **AAR 内已包含两个 ABI 的 `.so`**，外部接入方只需要拷贝这一个 AAR 到自己的 `app/libs/`，不需要再单独拷贝 so。
- AAR 不是 fat AAR，第三方依赖不会自动带进来，接入方需要按第 3.1 节补充依赖。
- 只支持 `arm64-v8a` / `armeabi-v7a`，x86/x86_64 模拟器无法加载 `.so`。
- 不要重命名 AAR 内的 `libFeasyBlueLibrary.so`。SDK 内部使用 `System.loadLibrary("FeasyBlueLibrary")`，改名后 native 方法找不到。
- AAR 内的 `.so` 已按 Android 15 要求做 16 KB 页对齐（两个 ABI 的 LOAD 段 `p_align = 0x4000`）。
- AAR 内置 `proguard.txt`（consumer rules），接入方开启 R8/ProGuard 无需再手写 JNI keep 规则。
- 本交付包不包含 `FeasyBlueLibrary` SDK 源码；需要重新生成 AAR 时，请在源码工程执行 `:FeasyBlueLibrary:assembleRelease`。

---

## 2. 环境要求

| 项 | 要求 |
|---|---|
| `minSdk` | ≥ 28（Android 9） |
| `compileSdk` | 36（与当前 SDK 源码一致） |
| `targetSdk` | 36 |
| Java/Kotlin | Java 17、Kotlin 2.0 工具链 |
| ABI | `arm64-v8a`、`armeabi-v7a` |
| 蓝牙 | 普通 BLE 设备需要 BLE；SPP 设备需要经典蓝牙 |
| Kotlin | AAR 内有 Kotlin 类；纯 Java 工程需要加入 `kotlin-stdlib` |
| UI | SDK 不含 UI 和资源，界面由 App 自己实现 |

> Demo 的 `minSdk` 是 29，SDK 本体是 28。接入方以自己的 `minSdk` 为准，但不要低于 28。

---

## 3. 集成步骤

### 3.1 引入 AAR

下面是从本仓库 `feasyblue/libs/` 复制到**接入方 App** 的步骤。本仓库的 `feasyblue` 模块内已经包含一份，外部接入方只需要拷贝一个 AAR 到自己的 `app/libs/`：

```text
app/
└── libs/
    └── FeasyBlueLibrary-3.5.6.4.aar
```

`app/build.gradle`：

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

说明：

- `kotlin-stdlib` 建议使用接入方工程已有的 Kotlin 版本；上面的 `2.0.0` 只是与当前 SDK 构建版本对齐。
- `androidx.room:room-runtime` 是因为 SDK 内 `DeviceInfo` 使用了 `@Entity`、`@PrimaryKey` 注解；如果完全不使用该数据类，也建议保留，避免类加载/反射时的兼容问题。
- `appcompat`、`material` **不是 SDK 本体必需依赖**，它们主要是 demo UI 使用；只有你的界面需要时才加。
- `androidx.room:room-compiler` / KAPT 只在你自己的 App 使用 Room 数据库时才需要。
- 如果使用 Maven 私服分发，请提供包含上述依赖坐标的 POM；否则建议按 `fileTree` 方式接入。

### 3.2 AndroidManifest 配置

SDK 自身的 manifest 已经声明了：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

**App 必须自己声明 OTA 服务**，否则 BLE/SPP 升级会没有进度或直接失败：

```xml
<service
    android:name="com.feasycom.ble.service.OTAService"
    android:enabled="true"
    android:exported="false" />

<service
    android:name="com.feasycom.spp.service.OTAService"
    android:enabled="true"
    android:exported="false" />
```

这两个服务由 SDK 内部 `bindService` 使用，接入方**不需要手动启动**。

建议补充的权限如下（按你的功能裁剪）：

```xml
<!-- Android 11 及以下：BLE 扫描需要定位权限 -->
<uses-permission
    android:name="android.permission.ACCESS_FINE_LOCATION"
    android:maxSdkVersion="30" />
<uses-permission
    android:name="android.permission.ACCESS_COARSE_LOCATION"
    android:maxSdkVersion="30" />

<!-- Android 12 及以上：扫描/连接运行时权限 -->
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- WiFi/BW 配网、TCP、云端接口 -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission
    android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="33" />
```

如果不需要某个权限，可以用 `tools:node="remove"` 移除 SDK 合并进来的权限；不要在不理解影响的情况下直接删除蓝牙/网络权限。

### 3.3 运行时权限

SDK **不负责申请权限**。未授权时扫描通常不会回调，很容易被误判成 SDK 故障。

推荐逻辑：

- Android 12（API 31）及以上：动态申请 `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`。
- Android 11（API 30）及以下：BLE 扫描申请 `ACCESS_FINE_LOCATION` 或 `ACCESS_COARSE_LOCATION`。
- WiFi 配网：Android 13（API 33）及以上可能需要 `NEARBY_WIFI_DEVICES`；读取 SSID/BSSID 时还要注意定位权限。
- 调用前先检查蓝牙开关；SPP 还需要设备已经配对或能够配对。

示例：

```kotlin
private val permissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            startScan()
        } else {
            // 提示用户去设置页开启权限
        }
    }

private fun requestScanPermission() {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    permissionLauncher.launch(permissions)
}
```

### 3.4 混淆规则

SDK 的 `consumer-rules.pro` 目前是空的。你的 App 如果开启 `minifyEnabled true`，请自行加入：

```proguard
-keep class com.feasycom.common.** { *; }
-keep class com.feasycom.ble.** { *; }
-keep class com.feasycom.spp.** { *; }
-keep class com.feasycom.wifi.** { *; }
-keep class com.feasycom.ota.** { *; }
-keep class com.feasycom.encrypted.** { *; }
-keep class com.feasycom.network.** { *; }
-keep class com.feasycom.acquisition.** { *; }
-keep class com.feasycom.BuildConfig { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}
```

如果不加，常见现象是 Release 包能编译但运行时报 `NoSuchMethodError`、native 方法找不到，或者 OTA/回调不触发。

### 3.5 打包注意

- 不要只打包 `arm64-v8a` 后又把 `abiFilters` 写成 `armeabi-v7a`；ABI 要和实际 so 一致。
- AAR 内已经包含两个 ABI 的 so，不要再用 `packagingOptions` 排除 `libFeasyBlueLibrary.so`。
- 如果使用 App Bundle，确认 `jniLibs` 没有被裁剪；发布前检查 APK/AAB 内是否包含两个 ABI 的 so。
- 需要排查 native 崩溃时，可以保留符号：

```groovy
android {
    packagingOptions {
        jniLibs {
            keepDebugSymbols += ['**/*.so']
        }
    }
}
```

---

## 4. 初始化与最小示例

### 4.1 通用 BLE

```kotlin
import android.bluetooth.BluetoothGatt
import android.content.Context
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.ble.controler.FscBleCentralCallbacksImp
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice

class BleClient(context: Context) : FscBleCentralCallbacksImp() {

    private val api = FscBleCentralApiImp.getInstance(context.applicationContext).apply {
        initialize()
        setCallbacks(this@BleClient)
    }

    fun startScan() = api.startScan()
    fun stopScan() = api.stopScan()
    fun connect(address: String) = api.connect(address)
    fun disconnect() = api.disconnect()
    fun send(bytes: ByteArray) = api.send(bytes)

    override fun blePeripheralFound(
        device: FscDevice,
        rssi: Int,
        record: ByteArray?
    ) {
        // device.address / device.name / rssi / record
    }

    override fun blePeripheralConnected(
        gatt: BluetoothGatt?,
        address: String,
        connectType: ConnectType
    ) {
        // 连接成功，可以开始发送
    }

    override fun blePeripheralDisconnected(
        gatt: BluetoothGatt?,
        address: String,
        status: Int
    ) {
        // 连接断开
    }

    override fun packetReceived(
        address: String,
        strValue: String,
        hexString: String,
        data: ByteArray
    ) {
        // 收到数据；二进制数据请优先使用 data
    }

    override fun otaProgressUpdate(address: String, percentage: Float, status: Int) {
        // OTA 进度
    }
}
```

初始化顺序固定为：

1. `getInstance(context)`
2. `initialize()`
3. `setCallbacks(...)`

建议把 `BleClient` 封成全局单例或 Repository，不要把回调和页面生命周期绑在一起。

### 4.2 SPP

```kotlin
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.feasycom.spp.controler.FscSppCentralCallbacksImp

class SppClient(context: Context) : FscSppCentralCallbacksImp() {

    private val api = FscSppCentralApiImp.getInstance(context.applicationContext).apply {
        initialize()
        setCallbacks(this@SppClient)
    }

    fun startScan() = api.startScan()
    fun stopScan() = api.stopScan()
    fun connect(address: String) = api.connect(address)
    fun disconnect() = api.disconnect()
    fun send(bytes: ByteArray) = api.send(bytes)

    override fun sppPeripheralFound(device: FscDevice, rssi: Int) {
        // 已配对设备会先回调，rssi 可能为 0
    }

    override fun sppPeripheralConnected(device: BluetoothDevice, type: ConnectType) {
        // 连接成功
    }

    override fun sppPeripheralDisconnected(address: String) {
        // 连接断开
    }

    override fun packetReceived(
        address: String,
        strValue: String,
        hexString: String,
        data: ByteArray
    ) {
        // 收到数据
    }
}
```

注意：如果直接调用无参数的 `FscSppCentralApiImp.getInstance()`，在还没有用 Context 初始化时会抛 `RuntimeException("Context is null")`。请先调用带 Context 的版本。

### 4.3 WiFi/BW 模块

```kotlin
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import com.feasycom.wifi.bean.BluetoothDeviceWrapper
import com.feasycom.wifi.ble.FscBleCentralBwApiImp
import com.feasycom.wifi.ble.FscBleCentralBwCallbacks

class BwClient(context: Context) : FscBleCentralBwCallbacks {

    private val api = FscBleCentralBwApiImp.getInstance(context.applicationContext).apply {
        initialize()
        setCallbacks(this@BwClient)
    }

    fun startScan() = api.startScan(15_000L)
    fun stopScan() = api.stopScan()
    fun connect(address: String) = api.connect(address)
    fun disconnect() = api.disconnect()

    override fun blePeripheralFound(
        device: BluetoothDeviceWrapper?,
        rssi: Int,
        record: ByteArray?
    ) {
        // 注意：这里是 wifi.bean.BluetoothDeviceWrapper，不是 FscDevice
    }

    override fun blePeripheralConnected(gatt: BluetoothGatt?, device: BluetoothDevice?) {
        // 连接成功
    }

    override fun blePeripheralDisconnected(
        gatt: BluetoothGatt?,
        device: BluetoothDevice?,
        isPinError: Boolean
    ) {
        // 断开
    }

    override fun packetReceived(
        device: BluetoothDevice?,
        strValue: String?,
        hexString: String?,
        rawValue: ByteArray?
    ) {
        // AT 回复
    }

    override fun packetSend(
        device: BluetoothDevice?,
        strValue: String?,
        hexString: String?,
        rawValue: ByteArray?
    ) {
    }

    override fun ipInformation(ip: String?) {
        // AT+LIP 的回复
    }

    override fun versionInformation(version: String?) {
        // AT+VER 的回复
    }
}
```

`FscBleCentralBwApiImp` 的特点是：

- 同一时间只维护一个 BLE 连接；
- 使用固定服务 `fff0`、通知特征 `fff1`、写特征 `fff2`；
- 连接后先做 AUTH 认证，再走 AT 指令；
- 适用于 WiFi/BW 模块，不适合普通 BLE 外设。

### 4.4 TCP / BW256B

```kotlin
import com.feasycom.wifi.tcp.TCPClientManager
import com.feasycom.wifi.tcp.FscClientBwBwCallback

class TcpClient : FscClientBwBwCallback {

    fun connect(host: String, port: Int) {
        TCPClientManager.initClient(this)
        TCPClientManager.connect(host, port)
    }

    fun send(bytes: ByteArray) {
        TCPClientManager.sendMessage(bytes)
    }

    fun disconnect() {
        TCPClientManager.disconnect()
    }

    override fun connected() {
        // TCP 已连接
    }

    override fun disconnected() {
        // TCP 已断开
    }

    override fun connectFailed() {
        // 连接失败
    }

    override fun packetReceived(strValue: String, rawValue: ByteArray) {
        // 收到数据
    }
}
```

`TCPClientManager` 是全局单例门面；`initClient()` 不是线程安全的重复初始化保护，建议在页面/Repository 初始化时只调用一次。

---

## 5. API 参考

### 5.1 公共接口 `FscApi<T>`

`FscBleCentralApi` 和 `FscSppCentralApi` 都继承 `FscApi<T>`。公共能力如下：

| 方法 | 说明 |
|---|---|
| `boolean initialize()` | 初始化蓝牙管理器/Adapter，必须先调用 |
| `boolean isEnabled()` | 蓝牙是否开启 |
| `void isShowLog(boolean enable)` | 日志开关；BLE 实现会联动 `MsgLogger` |
| `void setCallbacks(T callback)` | 注册回调 |
| `void startScan()` / `void stopScan()` | 开始/停止扫描 |
| `void connect(String address)` | 普通数据连接 |
| `void connectToModify(String address)` | AT/参数模式连接 |
| `void connectToOTAWithFactory(String address, byte[] dfuFile, boolean reset)` | 连接并进入 OTA |
| `void disconnect()` / `void disconnect(String address)` | 断开连接 |
| `boolean isConnected()` / `boolean isConnected(String address)` | 查询连接状态 |
| `List<FscDevice> getBondDevices()` | 获取已配对设备 |
| `boolean clearDevice(String address)` | 删除指定配对记录 |
| `boolean send(String data)` / `boolean send(byte[] data)` | 发送字符串/字节 |
| `boolean send(String address, String data)` / `boolean send(String address, byte[] data)` | 向指定地址发送 |
| `boolean sendFile(...)` | 发送测试文件/字节/输入流 |
| `void sendATCommand(Set<String> commands)` | 发送 AT 指令集合 |
| `void stopSend()` / `void stopSend(String address)` | 停止发送 |
| `boolean pauseSend(String address)` / `boolean continueSend(String address)` | 暂停/继续发送 |
| `void setSendInterval(String address, Long interval)` | 设置发送间隔 |

> `connectToVerifyOTAWithFactory` 也在这个接口体系中，但 **BLE 实现目前是空方法**；BLE 请使用 `connectToOTAWithFactory`。SPP 实现可用。

### 5.2 `FscBleCentralApi` / `FscBleCentralApiImp`

包名：`com.feasycom.ble.controler`

单例：

```java
static FscBleCentralApi getInstance(Context context)
static FscBleCentralApi getInstance()
```

在公共接口之外，BLE 还提供：

| 方法 | 说明 |
|---|---|
| `void startScan(ScannerFilter filter)` | 带过滤器参数的扫描；当前实现没有真正应用该过滤器，建议在回调里自行过滤 |
| `void connect(String address, Boolean facpSwitchOpen)` | 指定是否开启 FACP |
| `int getMtu()` / `int getMtu(String address)` | 获取 MTU |
| `int getMaximumPacketByte()` / `int getMaximumPacketByte(String address)` | 获取当前每包最大发送字节数 |
| `BluetoothDevice getConnectedBleDevice()` | 获取当前连接设备 |
| `boolean setCharacteristic(ch, properties)` | 设置通知/读写特征 |
| `boolean read(String address, BluetoothGattCharacteristic ch)` | 读取特征 |
| `void requestMtu(int mtu)` / `void requestMtu(String address, int mtu)` | 请求 MTU |
| `List<BluetoothGattService> getBluetoothGattServices(String address)` | 获取 GATT 服务 |
| `BluetoothGattCharacteristic getWriteCharacteristic(String address)` | 获取写特征 |
| `List<BluetoothGattCharacteristic> getNotifyCharacteristicList(String address)` | 获取通知特征列表 |
| `void createBond()` | 发起配对 |
| `void startOTA(String address, byte[] dfuFile, boolean reset)` | **当前空实现，不要使用** |

BLE 使用要点：

- 最多同时连接 6 个设备（`MAX_CONNECT_NUMBER = 6`）。
- `startScan()` 默认 10 秒后自动停止。
- 回调线程可能来自 GATT/Binder 线程，更新 UI 前请切到主线程。
- 普通连接、参数模式、OTA 分别使用 `connect`、`connectToModify`、`connectToOTAWithFactory`。

### 5.3 `FscSppCentralApi` / `FscSppCentralApiImp`

包名：`com.feasycom.spp.controler`

除公共 `FscApi` 外，SPP 还提供：

| 方法 | 说明 |
|---|---|
| `boolean isEnabledSDP()` | SDP 服务是否开启 |
| `void openSdpService(String customSppUuid)` | 开启 SDP 服务，等待模块反向连接；传入完整 UUID 字符串 |
| `void closeSdpService()` | 关闭 SDP 服务 |
| `void connect(String address)` | 连接 SPP 设备 |

SPP 使用要点：

- `startScan()` 会先把已配对设备回调给 `sppPeripheralFound`，然后再执行经典蓝牙发现。
- 已配对设备的 `rssi` 可能是 0。
- `connectToOTAWithFactory` 和 `connectToVerifyOTAWithFactory` 都可用。
- 从普通连接切换到 OTA 前，建议先 `disconnect()`，避免连接状态冲突。

### 5.4 `FscBleCentralBwApi` / `FscBleCentralBwApiImp`

包名：`com.feasycom.wifi.ble`

这是 WiFi/BW 模块专用接口，**不继承 `FscApi`**。

单例：

```java
static FscBleCentralBwApiImp getInstance()
static FscBleCentralBwApiImp getInstance(Context context)
```

主要方法：

| 方法 | 说明 |
|---|---|
| `void initialize()` | 初始化蓝牙管理器和 Adapter |
| `boolean isBtEnabled()` | 蓝牙是否开启 |
| `boolean enabled()` | 请求开启蓝牙 |
| `void setCallbacks(FscBleCentralBwCallbacks callback)` | 注册 BLE 回调 |
| `boolean connect(String address)` | 普通连接 |
| `boolean connect(String address, String pin)` | 带 PIN 连接；返回值不代表最终成功，以回调为准 |
| `void disconnect()` | 断开 |
| `void startScan(Long time)` | 扫描；`time` 为毫秒，0 使用默认 15 秒 |
| `void stopScan()` | 停止扫描 |
| `boolean isConnect()` | 是否已连接 |
| `boolean send(byte[] data)` | 发送数据（自动分包） |
| `void setNetwork(String ssid, String password, FscNetworkCentralBwCallbacks callback)` | BLE 配网 |
| `void getIp()` | 查询模块 IP（`AT+LIP`） |
| `void getVersion()` | 查询版本（`AT+VER`） |
| `void reset()` | 恢复出厂（`AT+RESTORE`） |
| `void setBroker(String broker)` | 设置 MQTT Broker |
| `void setPort(String port)` | 设置 MQTT 端口 |
| `void setTopic(String topic)` | 设置 MQTT Topic |
| `void setCert(String cert)` | 设置证书 |
| `void setKey(String key)` | 设置密钥 |
| `void getMac()` | 查询 MAC |
| `void getVer()` | 查询版本（另一条 AT 命令） |
| `void startOTA(String otaName, FscOtaCentralBwCallbacks callback)` | 开始 WiFi/BW OTA |
| `void setReboot()` | 重启模块 |
| `void getDhcp()` / `void setDhcp(boolean)` | 查询/设置 DHCP |
| `void setIp(String ip)` / `void setGw(String ip)` / `void setMask(String ip)` / `void setDns(String ip)` | 设置网络参数 |

注意：

- 固定使用 `fff0/fff1/fff2` UUID，只适配使用这套服务的 WiFi/BW 模块。
- 单连接模型；不要同时对多个设备调用 `connect`。
- `setNetwork` 之后 SDK 会轮询 IP，约 20 秒失败超时。

### 5.5 `ConfigNetworkBwApi` / `ConfigNetworkBwApiImp`

包名：`com.feasycom.wifi.simple`

用于 ESP Touch 简易配网：

```java
void startConfig(int model, StateBwResult result, String password)
void stopConfig()
void setCallback(ConfigNetworkBwCallback callback)
StateBwResult getStateResult()
```

`ConfigNetworkBwCallback`：

```java
void success(String ip)
void failure(int status)
```

当前实现中：

- `BwCommont.BW_246` 会走 ESP Touch；
- `BwCommont.BW_226`、`BwCommont.BW_236` 的方法是空的，不能直接使用。

`StateBwResult` 主要字段：

```java
String ssid;
byte[] ssidBytes;
String bssid;
boolean wifiConnected;
boolean is5G;
InetAddress address;
```

### 5.6 TCP / BW256B

包名：`com.feasycom.wifi.tcp`

`TCPClientManager` 是最常用的门面：

```kotlin
TCPClientManager.initClient(callback)
TCPClientManager.connect(host, port)
TCPClientManager.sendMessage(message: String)
TCPClientManager.sendMessage(byteArray: ByteArray)
TCPClientManager.sendFile(inputStream, isWifiMode)
TCPClientManager.sendFile(size, isWifiMode)
TCPClientManager.ping(ip)
TCPClientManager.pauseSend()
TCPClientManager.continueSend()
TCPClientManager.stopSend()
TCPClientManager.disconnect()
```

`FscClientBwBwApiImp` 的主要方法：

```kotlin
FscClientBwBwApiImp.getInstance()
setCallback(callback: FscClientBwBwCallback)
connect(host: String, servicePort: Int)
disconnect()
send(bytes: ByteArray, isWifiMode: Boolean)
sendFile(inputStream: InputStream, isWifiMode: Boolean)
pauseSend(): Boolean
continueSend(): Boolean
stopSend()
ping(ip: String): Boolean
```

`isWifiMode` 的含义：

- `false`：普通 TCP 透传，分片更小、间隔更大；
- `true`：WiFi/BW256B 高速透传，分片更大、间隔更小。

具体选哪种要和模块/示例工程保持一致。

### 5.7 `FirmwareHttpSdk`

包名：`com.feasycom.wifi.tcp`

用于 BW256B 等模块的本地固件 HTTP 服务：

```kotlin
FirmwareHttpSdk.init(context)
FirmwareHttpSdk.setFirmwareUri(uri)
FirmwareHttpSdk.startServer(fileName, type): String?
FirmwareHttpSdk.stopServer()
```

`startServer` 返回手机本地固件 URL；`type` 用于选择本机 IP，具体取值参考 demo。

### 5.8 `DeviceNetwork`

包名：`com.feasycom.network`

这是云端接口门面，主要方法都是 `suspend`：

| 方法 | 说明 |
|---|---|
| `downloadDFU(name)` | 下载 `https://dfu.feasycom.com/{name}.dfu` |
| `downloadBIN(name)` | 下载 `https://bin.feasycom.com/{name}.bin` |
| `downImg(path, fileName)` | 下载图片 |
| `getLanch(parameter)` | 获取启动页配置 |
| `getProtocol(params, local)` | 获取协议/隐私文本 |
| `getAllDevice(deviceInfo)` | 获取设备列表 |
| `getAdData(adParams)` | 获取广告数据 |

注意：

- `downloadDFU("ABC")` 里的 `ABC` **不要带 `.dfu` 后缀**；
- `downloadBIN("ABC")` 同理，不要带 `.bin`；
- 返回的是 `ResponseBody`，需要自己写入文件；
- 网络请求必须在协程/后台线程执行。

### 5.9 回调接口

#### `FscCentralCallbacks`（BLE/SPP 公共）

| 方法 | 触发时机 |
|---|---|
| `startScan()` / `stopScan()` | 扫描开始/结束 |
| `packetReceived(address, strValue, dataHexString, data)` | 收到数据 |
| `packetSend(address, strValue, data)` | 发送数据完成/发送回调 |
| `sendPacketProgress(address, percentage, data)` | 文件发送进度 |
| `otaProgressUpdate(address, percentage, status)` | OTA 进度/状态 |
| `atCommandCallBack(command, parameter, type, status)` | AT 指令结果 |
| `startATCommand()` / `endATCommand()` | AT 指令开始/结束 |

`atCommandCallBack` 的 `type`/`status` 常用值：

- `type`：`COMMAND_STATE_QUERY = 5`、`COMMAND_STATE_SET = 6`
- `status`：`COMMAND_SUCCESSFUL = 2`、`COMMAND_FAILED = 3`、`COMMAND_TIME_OUT = 4`

#### `FscBleCentralCallbacks`

| 方法 | 触发时机 |
|---|---|
| `blePeripheralFound(FscDevice device, int rssi, byte[] record)` | 扫描到 BLE 设备 |
| `blePeripheralConnected(BluetoothGatt gatt, String address, ConnectType type)` | BLE 连接成功 |
| `blePeripheralDisconnected(BluetoothGatt gatt, String address, int status)` | BLE 断开 |
| `servicesFound(...)` | 发现服务 |
| `characteristicForService(...)` | 发现特征 |
| `readResponse(...)` | 读取特征结果 |
| `bleMtuChanged(int mtu, int status)` | MTU 变化 |
| `readModuleModel(String model)` | 读取模块型号 |
| `readModuleVersion(String version)` | 读取模块版本 |
| `onReadRemoteRssi(int rssi)` | 读取远端 RSSI |

#### `FscSppCentralCallbacks`

| 方法 | 触发时机 |
|---|---|
| `sppPeripheralFound(FscDevice device, int rssi)` | 扫描到 SPP 设备 |
| `sppPeripheralConnected(BluetoothDevice device, ConnectType type)` | SPP 连接成功 |
| `sppPeripheralDisconnected(String address)` | SPP 断开 |
| `connectToModifyFail()` | 参数模式连接失败 |
| `bondIng()` | 正在配对 |
| `connectSuccess()` / `connectFailed()` | 连接结果 |
| `packetSend(address, strValue, hexString, data)` | 发送回调 |

#### `FscBleCentralBwCallbacks`

| 方法 | 触发时机 |
|---|---|
| `blePeripheralFound(wifi.bean.BluetoothDeviceWrapper, rssi, record)` | 扫描到 BW 模块 |
| `blePeripheralConnected(BluetoothGatt, BluetoothDevice)` | BW 连接成功 |
| `blePeripheralDisconnected(BluetoothGatt, BluetoothDevice, boolean isPinError)` | BW 断开 |
| `packetReceived(BluetoothDevice, strValue, hexString, rawValue)` | 收到 AT 回复 |
| `packetSend(BluetoothDevice, strValue, hexString, rawValue)` | 发送回调 |
| `ipInformation(String ip)` | `AT+LIP` 结果 |
| `versionInformation(String version)` | `AT+VER` 结果 |

#### `FscNetworkCentralBwCallbacks` / `FscOtaCentralBwCallbacks`

```java
// 配网
void success(String ip);
void failure();

// WiFi/BW OTA
void onOtaSuccess();
void onOtaFailure();
void onOtaProgress(String progress);
void onOtaNetworkIsNotConfigured();
void onOtaStartConfig();
```

#### `FscBwCallback`（TCP）

```kotlin
connected()
disconnected()
connectFailed()
packetSend(strValue: String, data: ByteArray)
sendPacketProgress(percentage: Int, data: ByteArray)
sendPacketProgress(percentage: Float, data: ByteArray)
packetReceived(strValue: String, rawValue: ByteArray)
readResponse(...)
```

> 回调线程不一定在主线程。GATT、Binder、Netty、协程 IO 都可能触发回调，更新 UI 前请切主线程。

---

## 6. 典型流程

### 6.1 BLE 扫描、连接、收发

```kotlin
if (!api.isEnabled) {
    // 提示用户打开蓝牙
    return
}

api.setCallbacks(callbacks)
api.startScan()

// 扫描回调里根据名称/RSSI/广播数据筛选
override fun blePeripheralFound(device: FscDevice, rssi: Int, record: ByteArray?) {
    if (rssi < -80) return
    // addToList(device)
}

api.stopScan()
api.connect(device.address)
```

连接结果以 `blePeripheralConnected` 为准；`connect()` 本身不返回成功/失败。连接成功后调用 `send()`；二进制数据建议用 `send(ByteArray)`，不要用字符串转换。

### 6.2 BLE 参数模式 / AT 指令

```kotlin
api.connectToModify(device.address)

// 连接成功后再发
api.sendATCommand(setOf(
    "AT+NAME=FeasyBlue",
    "AT+ADVIN=200"
))
```

AT 指令会由 SDK 追加 `\r\n`；传入完整 `AT+...` 字符串。结果通过 `atCommandCallBack(command, parameter, type, status)` 返回。

### 6.3 BLE OTA

```kotlin
val info = OtaUtils.checkDfuFile(dfuBytes) ?: return
// info.bootloader / info.versionStart / info.type_model

api.disconnect()
api.connectToOTAWithFactory(
    address = device.address,
    dfuFile = dfuBytes,
    reset = false
)

override fun otaProgressUpdate(address: String, percentage: Float, status: Int) {
    when (status) {
        XmodemUtils.OTA_STATUS_BEGIN -> {}
        XmodemUtils.OTA_STATUS_PROCESSING -> {}
        XmodemUtils.OTA_STATUS_FINISH -> {}
        XmodemUtils.OTA_STATUS_FAILED -> {}
    }
}
```

必须注意：

- BLE 请使用 `connectToOTAWithFactory`；
- `startOTA()` 和 `connectToVerifyOTAWithFactory()` 在 BLE 实现里是空的，不要调用；
- 升级前先断开普通连接；
- App 必须声明 `com.feasycom.ble.service.OTAService`；
- `reset = true` 表示升级后恢复出厂设置，按产品需求传。

### 6.4 SPP 扫描、连接、收发

```kotlin
if (!api.isEnabled) return
api.startScan()

override fun sppPeripheralFound(device: FscDevice, rssi: Int) {
    // 已配对设备先回调，随后是发现到的设备
}

api.connect(device.address)
api.send(byteArrayOf(0x01, 0x02))
```

SPP 通常需要先配对；配对过程可能触发 `bondIng()`。连接结果以 `sppPeripheralConnected` / `sppPeripheralDisconnected` 为准。

### 6.5 SPP OTA / SDP

```kotlin
// SDP：等待模块反向连接
api.openSdpService("00001101-0000-1000-8000-00805F9B34FB")

// 普通 OTA
api.disconnect()
api.connectToOTAWithFactory(device.address, dfuBytes, false)

// 需要校验模块版本时
api.connectToVerifyOTAWithFactory(device.address, dfuBytes, false, true)
```

SPP OTA 也会使用 `com.feasycom.spp.service.OTAService`，同样必须在 App manifest 中声明。升级中的 NAK 重发、部分模块 EOT 特例由 SDK 内部处理。

### 6.6 WiFi/BW 配网

```kotlin
// 先连接 BW 模块的 BLE 通道
api.connect(device.address)

// 连接成功后配网
api.setNetwork(ssid, password, object : FscNetworkCentralBwCallbacks {
    override fun success(ip: String) {
        // 配网成功，拿到模块 IP
    }

    override fun failure() {
        // 约 20 秒超时或配网失败
    }
})
```

建议：

- 手机连接 2.4GHz WiFi；ESP Touch/BW 模块通常不支持 5GHz；
- 先确认 `ACCESS_WIFI_STATE`、`CHANGE_WIFI_STATE`、定位/附近 WiFi 权限；
- 成功后可以调用 `api.getVersion()`，结果走 `versionInformation`。

### 6.7 WiFi/BW OTA

```kotlin
api.startOTA(otaUrl, object : FscOtaCentralBwCallbacks {
    override fun onOtaStartConfig() {
        // 开始配置/准备升级
    }

    override fun onOtaProgress(progress: String?) {
        // progress 可能是 "10" 这样的字符串
    }

    override fun onOtaSuccess() {
    }

    override fun onOtaFailure() {
    }

    override fun onOtaNetworkIsNotConfigured() {
        // 先配网
    }
})
```

如果是 BW256B 这类“手机本地 HTTP 服务 + 模块下载”的方案，还需要配合：

```kotlin
FirmwareHttpSdk.init(context)
FirmwareHttpSdk.setFirmwareUri(uri)
val url = FirmwareHttpSdk.startServer(fileName, type)
// 把 url / AT 指令交给模块
FirmwareHttpSdk.stopServer()
```

具体 `otaName` 是 URL 还是 AT 指令，取决于模块协议；示例工程 `WFOtaActivity` 传的是 URL。

### 6.8 TCP / BW256B 透传

```kotlin
TCPClientManager.initClient(callback)
TCPClientManager.connect("192.168.1.100", 8080)
TCPClientManager.sendMessage("hello".toByteArray())
TCPClientManager.sendFile(inputStream, isWifiMode = true)
TCPClientManager.pauseSend()
TCPClientManager.continueSend()
TCPClientManager.stopSend()
TCPClientManager.disconnect()
```

TCP 是独立于 BLE/SPP 的链路，不要和 `FscBleCentralApi` 混用。页面销毁时记得 `disconnect()`，避免 Netty 线程和连接残留。

### 6.9 从云端下载固件

```kotlin
lifecycleScope.launch(Dispatchers.IO) {
    val body = DeviceNetwork.downloadDFU("FeasyBlue_3.5.7.5")
    val file = File(filesDir, "firmware.dfu")
    body.byteStream().use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}
```

`downloadDFU` / `downloadBIN` 的参数不带扩展名。下载完成后把 `file.readBytes()` 传给 OTA 接口。

---

## 7. 数据模型

### `FscDevice`（BLE/SPP 扫描结果）

`com.feasycom.common.bean.FscDevice`

| 字段/方法 | 说明 |
|---|---|
| `name` | 设备名 |
| `address` | MAC 地址 |
| `device` | 原始 `BluetoothDevice` |
| `rssi` | 信号强度 |
| `mode` | `BLE` 或 `SPP` |
| `scanRecord` | 原始广播数据 |
| `flag` / `serviceData` / `manufacturerSpecificData` | 解析后的广播字段 |
| `completeLocalName` | 完整本地名 |
| `txPowerLevel` | 广播发射功率 |
| `incompleteServiceUUIDs_16bit/128bit` | 广播中的服务 UUID |

### `wifi.bean.BluetoothDeviceWrapper`（BW 扫描结果）

`com.feasycom.wifi.bean.BluetoothDeviceWrapper`

| 字段 | 说明 |
|---|---|
| `device` | 原始 `BluetoothDevice` |
| `name` | 名称 |
| `address` | 地址 |
| `rssi` | 信号强度 |
| `locationName` | 位置名 |
| `beacon` | `FeasyBeaconBw`，包含 `sensorBw`、`gateway`、`battery` |

### `DfuFileInfo`

`com.feasycom.ota.bean.DfuFileInfo`

```java
public int bootloader;
public int versionStart;       // 注意拼写是 versionStart
public int version_soft_end;
public int type_model;
```

### `StateBwResult`

`com.feasycom.wifi.bean.StateBwResult`

```java
String ssid;
byte[] ssidBytes;
String bssid;
boolean wifiConnected;
boolean is5G;
InetAddress address;
CharSequence message;
```

### 其他

- `ConnectType`：`CONNECT`、`MODIFY`、`OTA`，用于区分连接模式。
- `DeviceInfo`：`androidx.room` 实体，可用于保存设备信息；SDK 不自动创建数据库。
- `ScannerFilter`：`rssi`、`deviceName`，目前 BLE 扫描实现没有真正应用，建议回调里自行过滤。

---

## 8. 工具类

| 类 | 主要能力 |
|---|---|
| `com.feasycom.ble.utils.FeasycomUtil` | `parseAdvData(FscDevice, byte[])` 解析 BLE 广播 |
| `com.feasycom.ota.utils.OtaUtils` | `checkDfuFile`、`getDfuFileInformation`、`getModelName`、`needsReconnect` |
| `com.feasycom.ota.utils.XmodemUtils` | Xmodem 1K 分包、OTA 状态常量 |
| `com.feasycom.ota.utils.FileUtil` | 字节/十六进制/文件工具、`stringToInt` |
| `com.feasycom.wifi.utils.BwUtils` | `bytesToHex`、`getBeacon`、`getLocationName`、十六进制转换 |
| `com.feasycom.wifi.utils.BwNetUtils` | SSID、IP、BSSID、广播地址等 WiFi 工具 |
| `com.feasycom.common.utils.FileUtils` | `createTestData` 等文件/测试数据工具 |
| `com.feasycom.common.utils.MsgLogger` | SDK 日志；受 `FscBleCentralApiImp.isShowLog` 控制 |
| `com.feasycom.ota.utils.CRC16` | CRC16 校验 |
| `com.feasycom.ota.utils.BT1038Utils` | BT1038 系列协议辅助 |
| `com.feasycom.spp.utils.ConstantUtil` | SPP 模块型号常量与 `isDeviceModel` |
| `com.feasycom.wifi.tcp.FirmwareHttpSdk` | 本地固件 HTTP 服务门面 |

---

## 9. 常量速查

### 9.1 `Constant`

包名：`com.feasycom.common.utils.Constant`

| 常量 | 值/含义 |
|---|---|
| `PACKAGE_SEND_FINISH` | `100`，包发送完成 |
| `FIFO_SEND_FINISH` | `101`，FIFO 发送完成 |
| `ENABLE_CHARACTERISTIC_NOTIFICATION` | `3` |
| `ENABLE_CHARACTERISTIC_INDICATE` | `5` |
| `COMMAND_HEADER` | `"AT+"` |
| `COMMAND_NAME` / `COMMAND_LENAME` | `NAME` / `LENAME` |
| `COMMAND_ADVIN` / `COMMAND_TX_POWER` | `ADVIN` / `TXPOWER` |
| `COMMAND_PIN` / `COMMAND_LED` / `COMMAND_BUZ` | `PIN` / `LED` / `BUZ` |
| `COMMAND_RAP` / `COMMAND_OTA` | `RAP` / `OTA` |
| `COMMAND_BWMODE` / `COMMAND_EXTEND` | `BWMODE` / `EXTEND` |
| `COMMAND_BADVDATA` / `COMMAND_END` | `BADVDATA` / `END` |
| `COMMAND_SUCCESSFUL` | `0x02` |
| `COMMAND_FAILED` | `0x03` |
| `COMMAND_TIME_OUT` | `0x04` |
| `COMMAND_STATE_QUERY` | `0x05` |
| `COMMAND_STATE_SET` | `0x06` |
| `BLE_MODE` / `SPP_MODE` | `"BLE"` / `"SPP"` |

### 9.2 OTA 状态 `XmodemUtils`

| 常量 | 值 | 含义 |
|---|---|---|
| `OTA_STATUS_BEGIN` | `110` | 开始 |
| `OTA_STATUS_PROCESSING` | `121` | 进行中 |
| `OTA_STATUS_FINISH` | `10086` | 完成 |
| `OTA_STATUS_FAILED` | `120` | 失败 |
| `OTA_STATUS_VERIFY_FAILED` | `122` | 校验失败 |
| `OTA_STATUS_VERIFY_MODEL_FAILED` | `124` | 型号不匹配 |
| `OTA_STATUS_VERIFY_APP_VERSION_FAILED` | `125` | App 版本不匹配 |
| `OTA_STATUS_AT_FAILED` | `123` | AT 查询失败 |

### 9.3 `BwCommont`

包名：`com.feasycom.wifi.BwCommont`

| 常量 | 值 |
|---|---|
| `BW_226` | `0` |
| `BW_236` | `1` |
| `BW_246` | `2` |
| `PORT_OCCUPATION` | `4` |
| `FAILED_TO_CONFIGURE_NETWORK` | `5` |

### 9.4 SPP 型号

`com.feasycom.spp.utils.ConstantUtil` 提供 `BT401`、`BT826`、`BT1036`、`BT1116FC` 等型号常量，以及 `isDeviceModel(int)`。具体型号值以该类和设备协议为准。

---

## 10. 注意事项与常见问题

1. **扫描没有任何回调**
   - Android 12+ 是否动态授予 `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`；
   - Android 11 及以下是否授予定位权限；
   - 蓝牙是否开启；
   - 是否调用了 `initialize()` 和 `setCallbacks()`；
   - 是否把 `FscBleCentralBwApiImp` 当成通用 BLE API 使用（它只适配固定 UUID 的 BW 模块）。

2. **BLE 升级没有进度 / 直接失败**
   - App manifest 是否声明了 `com.feasycom.ble.service.OTAService`；
   - 是否误用了 `startOTA()`（BLE 当前是空实现）；
   - 是否在升级前 `disconnect()`；
   - DFU 文件是否完整、型号/版本是否匹配。

3. **SPP 升级没有进度 / 直接失败**
   - App manifest 是否声明了 `com.feasycom.spp.service.OTAService`；
   - 从普通连接切 OTA 前是否先 `disconnect()`；
   - 设备是否已经配对；
   - 是否使用了 `connectToOTAWithFactory` / `connectToVerifyOTAWithFactory`。

4. **`UnsatisfiedLinkError`**
   - AAR 是否正常引入（是否只解压出 `classes.jar`，丢了 AAR 内的 `jni/`）；
   - so 是否放在正确的 ABI 目录；
   - 是否重命名了 so；
   - `abiFilters` 是否与 so 的 ABI 一致；
   - 打包工具是否裁剪了 so。

5. **Release 包回调不触发 / native 方法找不到**
   - 是否开启混淆但没有加第 3.4 节的 keep 规则；
   - 是否保留了 `native <methods>`。

6. **SPP `getInstance()` 抛 `Context is null`**
   - 先调用 `FscSppCentralApiImp.getInstance(context)`，再使用无参 `getInstance()`。

7. **BLE 连接没有结果**
   - `connect()` 不返回结果，必须等 `blePeripheralConnected` / `blePeripheralDisconnected`；
   - 确保 `setCallbacks()` 在 `connect()` 之前；
   - 确认设备在范围内、未被其他 App 占用。

8. **BW `connect(address, pin)` 返回 false**
   - 该方法的返回值不能作为最终连接结果，以 `blePeripheralConnected` / `blePeripheralDisconnected` 回调为准。

9. **WiFi 配网失败**
   - 手机是否连接 2.4GHz WiFi；
   - 是否授予 WiFi/定位/附近 WiFi 权限；
   - SSID/BSSID 是否正确；
   - `ConfigNetworkBwApiImp` 当前只有 `BW_246` 的 ESP Touch 路径可用。

10. **TCP 连不上**
    - 是否先调用 `TCPClientManager.initClient(callback)`；
    - IP/端口是否正确；
    - 手机与设备是否在同一网络；
    - 页面退出时是否调用了 `disconnect()`，导致连接被提前关闭。

11. **OTA 状态值怎么看**
    - BLE/SPP 使用 `XmodemUtils.OTA_STATUS_*`；
    - WiFi/BW 使用 `FscOtaCentralBwCallbacks` 的 `onOtaProgress` / `onOtaSuccess` / `onOtaFailure`；
    - 如果状态是 `120/122/124/125`，优先检查固件型号、版本、校验和连接稳定性。

12. **多设备连接**
    - 通用 BLE 最多 6 个连接；
    - WiFi/BW 是单连接；
    - TCP 也是单连接；
    - 不要用同一个 `FscBleCentralBwApiImp` 同时管理多个 BW 设备。

13. **回调线程**
    - SDK 回调可能来自 GATT、Binder、Netty 或协程 IO 线程；
    - 更新 UI 前使用 `runOnUiThread`、`Handler(Looper.getMainLooper())` 或 `lifecycleScope.launch(Dispatchers.Main)`。

14. **生命周期**
    - 页面销毁时停止扫描、断开连接、停止 TCP；
    - 不要长期持有 Activity Context；SDK 内部有静态单例和静态 Context，建议统一传 `applicationContext`；
    - SPP 的 SDP 服务用完要 `closeSdpService()`。

15. **日志与安全**
    - 正式包建议调用 `FscBleCentralApiImp.isShowLog(false)`，避免 SDK 日志过多；
    - SDK 内部网络层包含历史兼容配置，正式产品发布前请按公司安全/合规要求复核；
    - 不要把 `Acquisition` 之类的历史统计类当作新功能依赖。

16. **扫描过滤器**
    - `startScan(ScannerFilter)` 的过滤器参数在当前 BLE 实现中没有真正应用；
    - 建议在 `blePeripheralFound` 回调里根据 RSSI、名称自行过滤；
    - 通用 BLE 扫描默认 10 秒自动停止，BW 扫描默认 15 秒。

---

## 11. 版本记录

| 库版本 | 说明 |
|---|---|
| `3.5.6.4` | 当前 AAR 版本（SDK BuildConfig `3.5.6.4 / 64`）；示例 App 版本为 `3.5.7.5`。包含 BLE/SPP OTA、WiFi/BW 配网、TCP/BW256B、云端固件下载；`.so` 已做 16 KB 页对齐 |

> 历史版本的变更请参考项目源码仓库的 `README.md` 版本记录；本交付包不包含 SDK 源码。

---

## 附：最小接入检查清单

- [ ] `FeasyBlueLibrary-3.5.6.4.aar` 已放入 `app/libs`
- [ ] 未再用 `packagingOptions` 排除 `libFeasyBlueLibrary.so`（AAR 已包含两个 ABI）
- [ ] 未再单独拷贝 `.so`（AAR 已包含，重复拷贝会冲突）
- [ ] `abiFilters` 只包含 `arm64-v8a`、`armeabi-v7a`
- [ ] 已声明 `com.feasycom.ble.service.OTAService`
- [ ] 已声明 `com.feasycom.spp.service.OTAService`
- [ ] Android 12+ 已申请 `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`
- [ ] Android 11 及以下已申请定位权限
- [ ] 已添加 ProGuard keep 规则
- [ ] 初始化顺序为 `getInstance(context)` → `initialize()` → `setCallbacks()`
- [ ] 更新 UI 前已切主线程
- [ ] 页面退出时已停止扫描/断开连接/停止 TCP
