# FeasyBlue Android SDK Integration Guide (JAR + SO)

> **Deliverables**: `feasyblue.jar` + `libFeasyBlueLibrary.so` + demo APK + integration documents
> **Delivery package version**: `3.5.7.5`
> **SDK BuildConfig**: `3.5.6.2 / 62`
> **Package name**: `com.feasycom`
> **Minimum OS version**: Android 8.0 (API 26)
> **Supported ABIs**: `arm64-v8a`, `armeabi-v7a`
> **Minimum demo version**: Android 10 (API 29)
>
> This guide is based on the current FeasyBlue 3.5.7.5 delivery package. Examples are primarily written in Kotlin; Java projects can call the same APIs.
> Chinese version: [FeasyBlue使用文档.md](FeasyBlue使用文档.md)

---

## 0. Choose the Correct API First

FeasyBlue is not a one-size-fits-all Bluetooth API. It provides several communication paths for different device types. Before integrating, identify which type of device you are working with:

| Device / scenario | API to use | Scan result type |
|---|---|---|
| Standard BLE modules: BLE transparent transmission, parameter configuration, BLE OTA | `FscBleCentralApi` / `FscBleCentralApiImp` | `com.feasycom.common.bean.FscDevice` |
| Classic Bluetooth SPP modules: serial transparent transmission, SDP, SPP OTA | `FscSppCentralApi` / `FscSppCentralApiImp` | `com.feasycom.common.bean.FscDevice` |
| WiFi/BW modules: BLE provisioning, AT parameters, WiFi OTA | `FscBleCentralBwApi` / `FscBleCentralBwApiImp` | `com.feasycom.wifi.bean.BluetoothDeviceWrapper` |
| ESP Touch simple provisioning for modules such as BW246 | `ConfigNetworkBwApi` / `ConfigNetworkBwApiImp` | `StateBwResult` |
| TCP / BW256B high-speed transparent transmission | `TCPClientManager` / `FscClientBwBwApiImp` | No scan results |
| Cloud firmware, agreements, and ad images | `DeviceNetwork` | No scan results |

The two APIs most often confused are:

- `com.feasycom.ble` is the **general-purpose BLE central** API. It supports scanning, connecting, reading and writing GATT characteristics, and upgrading BLE modules.
- `com.feasycom.wifi.ble` is the **dedicated BLE provisioning channel for WiFi/BW modules**. It uses the fixed UUIDs `fff0/fff1/fff2` and controls the module through AT commands. It is not a generic BLE scanner.

Do not mix the scan results, callback interfaces, or connection objects from these two APIs.

---

## 1. Package Contents

The GitHub repository has the following structure:

```text
FeasyBlue-Android/
├── apk/
│   └── FeasyBlue3.5.7.5.apk       # Demo APK
├── docs/
│   ├── FeasyBlue使用文档.md       # Chinese guide
│   └── FeasyBlue_Integration_Guide_EN.md   # This document
├── feasyblue/                     # Application-layer source
│   ├── build.gradle
│   ├── libs/
│   │   ├── feasyblue.jar
│   │   ├── arm64-v8a/
│   │   │   └── libFeasyBlueLibrary.so
│   │   └── armeabi-v7a/
│   │       └── libFeasyBlueLibrary.so
│   └── src/main/...
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
├── gradle/wrapper/
└── README.md
```

Purpose of each file:

| File | Purpose |
|---|---|
| `feasyblue/libs/feasyblue.jar` | Java/Kotlin classes for the SDK |
| `feasyblue/libs/<abi>/libFeasyBlueLibrary.so` | Native capabilities such as JNI encryption, DFU parsing, and FACP |
| `apk/FeasyBlue3.5.7.5.apk` | Demo APK that can be installed directly |
| `README.md` | Repository notes, APK size, and SHA-256 |
| `docs/FeasyBlue使用文档.md` | Chinese integration guide |
| `docs/FeasyBlue_Integration_Guide_EN.md` | This English integration guide |
| `feasyblue` | Application project already integrated with the JAR and SO files; can be opened directly in Android Studio |

Notes:

- **The JAR and SO files must be used together.** Using the JAR alone causes an `UnsatisfiedLinkError` at runtime.
- The JAR is a plain class JAR, **not a fat JAR**. Third-party dependencies are not bundled automatically; integrators must add the dependencies described in Section 3.1.
- Only `arm64-v8a` and `armeabi-v7a` are supported. x86/x86_64 emulators cannot load the `.so` files.
- Do not rename `libFeasyBlueLibrary.so`. The SDK calls `System.loadLibrary("FeasyBlueLibrary")` internally; native methods will not be found if the file is renamed.
- `feasyblue/libs/` contains the SDK binary used by the application module. External integrators can copy these three files into their own `app/libs/`.
- The repository no longer keeps a duplicate `sdk/` directory; the SDK binary is maintained only under `feasyblue/libs/`.
- This delivery package does not include the `FeasyBlueLibrary` SDK source. To regenerate the JAR, use the project source repository.

---

## 2. Environment Requirements

| Item | Requirement |
|---|---|
| `minSdk` | 26 or higher (Android 8.0) |
| `compileSdk` | 35 (matches the current SDK source) |
| `targetSdk` | 35 |
| Java/Kotlin | Java 17 and the Kotlin 2.0 toolchain |
| ABI | `arm64-v8a`, `armeabi-v7a` |
| Bluetooth | Standard BLE devices require BLE; SPP devices require Classic Bluetooth |
| Kotlin | The JAR contains Kotlin classes; Java-only projects must add `kotlin-stdlib` |
| UI | The SDK contains no UI or resources; the app must implement its own UI |

> The demo uses `minSdk` 29, while the SDK itself supports 26. Use your own `minSdk`, but do not go below 26.

---

## 3. Integration Steps

### 3.1 Add the JAR and SO Files

The following steps copy the SDK from the repository's `feasyblue/libs/` into the integrator's app. The repository's `feasyblue` module already contains a copy; external integrators should place the files in their own `app/libs/` as shown:

```text
app/
└── libs/
    ├── feasyblue.jar
    ├── arm64-v8a/
    │   └── libFeasyBlueLibrary.so
    └── armeabi-v7a/
        └── libFeasyBlueLibrary.so
```

`app/build.gradle`:

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

Notes:

- Use the Kotlin version already present in your project if possible. The `2.0.0` above matches the current SDK build.
- `androidx.room:room-runtime` is required because `DeviceInfo` in the SDK uses `@Entity` and `@PrimaryKey` annotations. Keep it even if you do not use that data class, to avoid class-loading or reflection compatibility issues.
- `appcompat` and `material` are **not required by the SDK itself**; they are mainly used by the demo UI. Add them only if your UI needs them.
- `androidx.room:room-compiler` / KAPT is only required if your app uses a Room database.
- If you distribute through a private Maven repository, provide a POM containing the dependency coordinates above; otherwise, use the `fileTree` approach.

### 3.2 AndroidManifest Configuration

The SDK manifest already declares:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
```

**The app must declare the OTA services itself.** Otherwise, BLE/SPP upgrades may show no progress or fail immediately:

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

These services are bound internally by the SDK. Integrators do **not** need to start them manually.

Recommended additional permissions (trim according to your features):

```xml
<!-- Android 11 and below: location permission is required for BLE scanning -->
<uses-permission
    android:name="android.permission.ACCESS_FINE_LOCATION"
    android:maxSdkVersion="30" />
<uses-permission
    android:name="android.permission.ACCESS_COARSE_LOCATION"
    android:maxSdkVersion="30" />

<!-- Android 12 and above: runtime permissions for scanning and connecting -->
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- WiFi/BW provisioning, TCP, and cloud APIs -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission
    android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="33" />
```

If you do not need a permission, use `tools:node="remove"` to remove the permission merged from the SDK. Do not remove Bluetooth or network permissions without understanding the impact.

### 3.3 Runtime Permissions

The SDK does **not** request permissions. When permissions are not granted, scanning usually produces no callbacks, which is often mistaken for an SDK failure.

Recommended logic:

- Android 12 (API 31) and above: request `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` at runtime.
- Android 11 (API 30) and below: request `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` for BLE scanning.
- WiFi provisioning: Android 13 (API 33) and above may require `NEARBY_WIFI_DEVICES`. Location permission may also be required when reading the SSID/BSSID.
- Check that Bluetooth is enabled before calling. SPP also requires the device to be bonded or bondable.

Example:

```kotlin
private val permissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            startScan()
        } else {
            // Prompt the user to enable permissions in Settings.
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

### 3.4 ProGuard Rules

The SDK's `consumer-rules.pro` is currently empty. If your app enables `minifyEnabled true`, add the following rules:

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

Without these rules, a release build may compile but fail at runtime with `NoSuchMethodError`, missing native methods, or OTA/callback failures.

### 3.5 Packaging Notes

- Do not package only `arm64-v8a` while setting `abiFilters` to `armeabi-v7a`. The configured ABIs must match the actual SO files.
- Do not exclude `libFeasyBlueLibrary.so` in `packagingOptions`.
- If you use an App Bundle, make sure `jniLibs` are not stripped. Before release, verify that the APK/AAB contains SO files for both ABIs.
- To debug native crashes, preserve debug symbols:

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

## 4. Initialization and Minimal Examples

### 4.1 Standard BLE

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
        // Connected. Data transmission can start.
    }

    override fun blePeripheralDisconnected(
        gatt: BluetoothGatt?,
        address: String,
        status: Int
    ) {
        // Disconnected.
    }

    override fun packetReceived(
        address: String,
        strValue: String,
        hexString: String,
        data: ByteArray
    ) {
        // Data received. Prefer data for binary payloads.
    }

    override fun otaProgressUpdate(address: String, percentage: Float, status: Int) {
        // OTA progress.
    }
}
```

The initialization order is fixed:

1. `getInstance(context)`
2. `initialize()`
3. `setCallbacks(...)`

Wrap `BleClient` in a global singleton or repository. Do not tie callbacks to a screen's lifecycle.

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
        // Bonded devices are reported first; rssi may be 0.
    }

    override fun sppPeripheralConnected(device: BluetoothDevice, type: ConnectType) {
        // Connected.
    }

    override fun sppPeripheralDisconnected(address: String) {
        // Disconnected.
    }

    override fun packetReceived(
        address: String,
        strValue: String,
        hexString: String,
        data: ByteArray
    ) {
        // Data received.
    }
}
```

Note: Calling the no-argument `FscSppCentralApiImp.getInstance()` before initializing with a `Context` throws `RuntimeException("Context is null")`. Call the overload that takes a `Context` first.

### 4.3 WiFi/BW Modules

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
        // Note: this is wifi.bean.BluetoothDeviceWrapper, not FscDevice.
    }

    override fun blePeripheralConnected(gatt: BluetoothGatt?, device: BluetoothDevice?) {
        // Connected.
    }

    override fun blePeripheralDisconnected(
        gatt: BluetoothGatt?,
        device: BluetoothDevice?,
        isPinError: Boolean
    ) {
        // Disconnected.
    }

    override fun packetReceived(
        device: BluetoothDevice?,
        strValue: String?,
        hexString: String?,
        rawValue: ByteArray?
    ) {
        // AT response.
    }

    override fun packetSend(
        device: BluetoothDevice?,
        strValue: String?,
        hexString: String?,
        rawValue: ByteArray?
    ) {
    }

    override fun ipInformation(ip: String?) {
        // Response to AT+LIP.
    }

    override fun versionInformation(version: String?) {
        // Response to AT+VER.
    }
}
```

Key characteristics of `FscBleCentralBwApiImp`:

- It maintains only one BLE connection at a time.
- It uses the fixed service `fff0`, notify characteristic `fff1`, and write characteristic `fff2`.
- After connecting, it performs AUTH authentication before sending AT commands.
- It is designed for WiFi/BW modules and is not suitable for generic BLE peripherals.

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
        // TCP connected.
    }

    override fun disconnected() {
        // TCP disconnected.
    }

    override fun connectFailed() {
        // Connection failed.
    }

    override fun packetReceived(strValue: String, rawValue: ByteArray) {
        // Data received.
    }
}
```

`TCPClientManager` is a global singleton facade. `initClient()` does not guard against concurrent repeated initialization; call it once during screen/repository initialization.

---

## 5. API Reference

### 5.1 Common Interface `FscApi<T>`

Both `FscBleCentralApi` and `FscSppCentralApi` extend `FscApi<T>`. The common capabilities are:

| Method | Description |
|---|---|
| `boolean initialize()` | Initializes the BluetoothManager/Adapter. Must be called first. |
| `boolean isEnabled()` | Whether Bluetooth is enabled. |
| `void isShowLog(boolean enable)` | Log switch. The BLE implementation also controls `MsgLogger`. |
| `void setCallbacks(T callback)` | Registers callbacks. |
| `void startScan()` / `void stopScan()` | Starts/stops scanning. |
| `void connect(String address)` | Standard data connection. |
| `void connectToModify(String address)` | AT/parameter-mode connection. |
| `void connectToOTAWithFactory(String address, byte[] dfuFile, boolean reset)` | Connects and enters OTA mode. |
| `void disconnect()` / `void disconnect(String address)` | Disconnects. |
| `boolean isConnected()` / `boolean isConnected(String address)` | Queries connection state. |
| `List<FscDevice> getBondDevices()` | Returns bonded devices. |
| `boolean clearDevice(String address)` | Removes the bonding record for the specified device. |
| `boolean send(String data)` / `boolean send(byte[] data)` | Sends a string/byte array. |
| `boolean send(String address, String data)` / `boolean send(String address, byte[] data)` | Sends to the specified address. |
| `boolean sendFile(...)` | Sends test data, bytes, or an input stream. |
| `void sendATCommand(Set<String> commands)` | Sends a set of AT commands. |
| `void stopSend()` / `void stopSend(String address)` | Stops sending. |
| `boolean pauseSend(String address)` / `boolean continueSend(String address)` | Pauses/resumes sending. |
| `void setSendInterval(String address, Long interval)` | Sets the send interval. |

> `connectToVerifyOTAWithFactory` is also part of this interface, but the **BLE implementation is currently empty**. Use `connectToOTAWithFactory` for BLE. The SPP implementation is functional.

### 5.2 `FscBleCentralApi` / `FscBleCentralApiImp`

Package: `com.feasycom.ble.controler`

Singleton:

```java
static FscBleCentralApi getInstance(Context context)
static FscBleCentralApi getInstance()
```

In addition to the common interface, BLE provides:

| Method | Description |
|---|---|
| `void startScan(ScannerFilter filter)` | Scan with a filter parameter. The current implementation does not actually apply the filter; filter results in the callback. |
| `void connect(String address, Boolean facpSwitchOpen)` | Specifies whether FACP is enabled. |
| `int getMtu()` / `int getMtu(String address)` | Returns the MTU. |
| `int getMaximumPacketByte()` / `int getMaximumPacketByte(String address)` | Returns the maximum payload size per packet. |
| `BluetoothDevice getConnectedBleDevice()` | Returns the currently connected device. |
| `boolean setCharacteristic(ch, properties)` | Configures notify/read/write characteristics. |
| `boolean read(String address, BluetoothGattCharacteristic ch)` | Reads a characteristic. |
| `void requestMtu(int mtu)` / `void requestMtu(String address, int mtu)` | Requests an MTU. |
| `List<BluetoothGattService> getBluetoothGattServices(String address)` | Returns GATT services. |
| `BluetoothGattCharacteristic getWriteCharacteristic(String address)` | Returns the write characteristic. |
| `List<BluetoothGattCharacteristic> getNotifyCharacteristicList(String address)` | Returns the notify characteristic list. |
| `void createBond()` | Initiates bonding. |
| `void startOTA(String address, byte[] dfuFile, boolean reset)` | **Currently an empty implementation; do not use.** |

BLE usage notes:

- Up to 6 simultaneous connections (`MAX_CONNECT_NUMBER = 6`).
- `startScan()` stops automatically after 10 seconds by default.
- Callbacks may arrive on GATT/Binder threads. Switch to the main thread before updating UI.
- Use `connect`, `connectToModify`, and `connectToOTAWithFactory` for standard, parameter, and OTA connections respectively.

### 5.3 `FscSppCentralApi` / `FscSppCentralApiImp`

Package: `com.feasycom.spp.controler`

In addition to the common `FscApi`, SPP provides:

| Method | Description |
|---|---|
| `boolean isEnabledSDP()` | Whether the SDP service is enabled. |
| `void openSdpService(String customSppUuid)` | Starts the SDP service and waits for the module to connect back. Pass a complete UUID string. |
| `void closeSdpService()` | Stops the SDP service. |
| `void connect(String address)` | Connects to an SPP device. |

SPP usage notes:

- `startScan()` first reports bonded devices through `sppPeripheralFound`, then performs Classic Bluetooth discovery.
- Bonded devices may report `rssi` as 0.
- Both `connectToOTAWithFactory` and `connectToVerifyOTAWithFactory` are available.
- Before switching from a standard connection to OTA, call `disconnect()` to avoid connection-state conflicts.

### 5.4 `FscBleCentralBwApi` / `FscBleCentralBwApiImp`

Package: `com.feasycom.wifi.ble`

This is a dedicated interface for WiFi/BW modules. It **does not extend `FscApi`**.

Singleton:

```java
static FscBleCentralBwApiImp getInstance()
static FscBleCentralBwApiImp getInstance(Context context)
```

Main methods:

| Method | Description |
|---|---|
| `void initialize()` | Initializes the BluetoothManager and Adapter. |
| `boolean isBtEnabled()` | Whether Bluetooth is enabled. |
| `boolean enabled()` | Requests that Bluetooth be enabled. |
| `void setCallbacks(FscBleCentralBwCallbacks callback)` | Registers BLE callbacks. |
| `boolean connect(String address)` | Standard connection. |
| `boolean connect(String address, String pin)` | Connects with a PIN. The return value does not indicate final success; rely on callbacks. |
| `void disconnect()` | Disconnects. |
| `void startScan(Long time)` | Starts scanning. `time` is in milliseconds; 0 uses the default 15 seconds. |
| `void stopScan()` | Stops scanning. |
| `boolean isConnect()` | Whether the device is connected. |
| `boolean send(byte[] data)` | Sends data with automatic segmentation. |
| `void setNetwork(String ssid, String password, FscNetworkCentralBwCallbacks callback)` | BLE provisioning. |
| `void getIp()` | Queries the module IP (`AT+LIP`). |
| `void getVersion()` | Queries the version (`AT+VER`). |
| `void reset()` | Restores factory defaults (`AT+RESTORE`). |
| `void setBroker(String broker)` | Sets the MQTT broker. |
| `void setPort(String port)` | Sets the MQTT port. |
| `void setTopic(String topic)` | Sets the MQTT topic. |
| `void setCert(String cert)` | Sets the certificate. |
| `void setKey(String key)` | Sets the key. |
| `void getMac()` | Queries the MAC address. |
| `void getVer()` | Queries the version through another AT command. |
| `void startOTA(String otaName, FscOtaCentralBwCallbacks callback)` | Starts WiFi/BW OTA. |
| `void setReboot()` | Reboots the module. |
| `void getDhcp()` / `void setDhcp(boolean)` | Queries/sets DHCP. |
| `void setIp(String ip)` / `void setGw(String ip)` / `void setMask(String ip)` / `void setDns(String ip)` | Sets network parameters. |

Notes:

- It uses the fixed `fff0/fff1/fff2` UUIDs and only supports WiFi/BW modules that use this service.
- It uses a single-connection model. Do not call `connect` for multiple devices concurrently.
- After `setNetwork`, the SDK polls for the IP address and times out after about 20 seconds.

### 5.5 `ConfigNetworkBwApi` / `ConfigNetworkBwApiImp`

Package: `com.feasycom.wifi.simple`

Used for ESP Touch simple provisioning:

```java
void startConfig(int model, StateBwResult result, String password)
void stopConfig()
void setCallback(ConfigNetworkBwCallback callback)
StateBwResult getStateResult()
```

`ConfigNetworkBwCallback`:

```java
void success(String ip)
void failure(int status)
```

In the current implementation:

- `BwCommont.BW_246` uses ESP Touch.
- The methods for `BwCommont.BW_226` and `BwCommont.BW_236` are empty and cannot be used directly.

Main fields of `StateBwResult`:

```java
String ssid;
byte[] ssidBytes;
String bssid;
boolean wifiConnected;
boolean is5G;
InetAddress address;
```

### 5.6 TCP / BW256B

Package: `com.feasycom.wifi.tcp`

`TCPClientManager` is the most commonly used facade:

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

Main methods of `FscClientBwBwApiImp`:

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

Meaning of `isWifiMode`:

- `false`: standard TCP transparent transmission with smaller chunks and longer intervals.
- `true`: WiFi/BW256B high-speed transparent transmission with larger chunks and shorter intervals.

Choose the mode that matches the module and the demo project.

### 5.7 `FirmwareHttpSdk`

Package: `com.feasycom.wifi.tcp`

Provides a local firmware HTTP service for modules such as BW256B:

```kotlin
FirmwareHttpSdk.init(context)
FirmwareHttpSdk.setFirmwareUri(uri)
FirmwareHttpSdk.startServer(fileName, type): String?
FirmwareHttpSdk.stopServer()
```

`startServer` returns the phone's local firmware URL. `type` selects the local IP address; refer to the demo for specific values.

### 5.8 `DeviceNetwork`

Package: `com.feasycom.network`

This is the cloud API facade. Most methods are `suspend` functions:

| Method | Description |
|---|---|
| `downloadDFU(name)` | Downloads `https://dfu.feasycom.com/{name}.dfu`. |
| `downloadBIN(name)` | Downloads `https://bin.feasycom.com/{name}.bin`. |
| `downImg(path, fileName)` | Downloads an image. |
| `getLanch(parameter)` | Retrieves the splash screen configuration. |
| `getProtocol(params, local)` | Retrieves agreement/privacy text. |
| `getAllDevice(deviceInfo)` | Retrieves the device list. |
| `getAdData(adParams)` | Retrieves ad data. |

Notes:

- The name passed to `downloadDFU("ABC")` **must not include the `.dfu` suffix**.
- The same applies to `downloadBIN("ABC")`; do not include `.bin`.
- The methods return `ResponseBody`; write it to a file yourself.
- Network requests must run in a coroutine or background thread.

### 5.9 Callback Interfaces

#### `FscCentralCallbacks` (common to BLE/SPP)

| Method | Trigger |
|---|---|
| `startScan()` / `stopScan()` | Scan started/finished. |
| `packetReceived(address, strValue, dataHexString, data)` | Data received. |
| `packetSend(address, strValue, data)` | Data sent/send callback. |
| `sendPacketProgress(address, percentage, data)` | File-send progress. |
| `otaProgressUpdate(address, percentage, status)` | OTA progress/status. |
| `atCommandCallBack(command, parameter, type, status)` | AT command result. |
| `startATCommand()` / `endATCommand()` | AT command sequence started/finished. |

Common `type`/`status` values for `atCommandCallBack`:

- `type`: `COMMAND_STATE_QUERY = 5`, `COMMAND_STATE_SET = 6`
- `status`: `COMMAND_SUCCESSFUL = 2`, `COMMAND_FAILED = 3`, `COMMAND_TIME_OUT = 4`

#### `FscBleCentralCallbacks`

| Method | Trigger |
|---|---|
| `blePeripheralFound(FscDevice device, int rssi, byte[] record)` | BLE device discovered. |
| `blePeripheralConnected(BluetoothGatt gatt, String address, ConnectType type)` | BLE connection established. |
| `blePeripheralDisconnected(BluetoothGatt gatt, String address, int status)` | BLE disconnected. |
| `servicesFound(...)` | Services discovered. |
| `characteristicForService(...)` | Characteristic discovered. |
| `readResponse(...)` | Characteristic read result. |
| `bleMtuChanged(int mtu, int status)` | MTU changed. |
| `readModuleModel(String model)` | Module model read. |
| `readModuleVersion(String version)` | Module version read. |
| `onReadRemoteRssi(int rssi)` | Remote RSSI read. |

#### `FscSppCentralCallbacks`

| Method | Trigger |
|---|---|
| `sppPeripheralFound(FscDevice device, int rssi)` | SPP device discovered. |
| `sppPeripheralConnected(BluetoothDevice device, ConnectType type)` | SPP connection established. |
| `sppPeripheralDisconnected(String address)` | SPP disconnected. |
| `connectToModifyFail()` | Parameter-mode connection failed. |
| `bondIng()` | Bonding in progress. |
| `connectSuccess()` / `connectFailed()` | Connection result. |
| `packetSend(address, strValue, hexString, data)` | Send callback. |

#### `FscBleCentralBwCallbacks`

| Method | Trigger |
|---|---|
| `blePeripheralFound(wifi.bean.BluetoothDeviceWrapper, rssi, record)` | BW module discovered. |
| `blePeripheralConnected(BluetoothGatt, BluetoothDevice)` | BW connection established. |
| `blePeripheralDisconnected(BluetoothGatt, BluetoothDevice, boolean isPinError)` | BW disconnected. |
| `packetReceived(BluetoothDevice, strValue, hexString, rawValue)` | AT response received. |
| `packetSend(BluetoothDevice, strValue, hexString, rawValue)` | Send callback. |
| `ipInformation(String ip)` | `AT+LIP` result. |
| `versionInformation(String version)` | `AT+VER` result. |

#### `FscNetworkCentralBwCallbacks` / `FscOtaCentralBwCallbacks`

```java
// Provisioning
void success(String ip);
void failure();

// WiFi/BW OTA
void onOtaSuccess();
void onOtaFailure();
void onOtaProgress(String progress);
void onOtaNetworkIsNotConfigured();
void onOtaStartConfig();
```

#### `FscBwCallback` (TCP)

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

> Callbacks are not guaranteed to run on the main thread. GATT, Binder, Netty, and coroutine I/O threads may all trigger callbacks. Switch to the main thread before updating UI.

---

## 6. Typical Workflows

### 6.1 BLE Scanning, Connection, and Data Transfer

```kotlin
if (!api.isEnabled) {
    // Prompt the user to enable Bluetooth.
    return
}

api.setCallbacks(callbacks)
api.startScan()

// Filter by name, RSSI, or advertisement data in the scan callback.
override fun blePeripheralFound(device: FscDevice, rssi: Int, record: ByteArray?) {
    if (rssi < -80) return
    // addToList(device)
}

api.stopScan()
api.connect(device.address)
```

Connection success is reported by `blePeripheralConnected`; `connect()` itself does not return success or failure. After the connection succeeds, call `send()`. For binary data, prefer `send(ByteArray)` instead of converting the payload to a string.

### 6.2 BLE Parameter Mode / AT Commands

```kotlin
api.connectToModify(device.address)

// Send after the connection succeeds.
api.sendATCommand(setOf(
    "AT+NAME=FeasyBlue",
    "AT+ADVIN=200"
))
```

The SDK appends `\r\n` to AT commands. Pass the complete `AT+...` string. Results are delivered through `atCommandCallBack(command, parameter, type, status)`.

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

Important:

- Use `connectToOTAWithFactory` for BLE.
- `startOTA()` and `connectToVerifyOTAWithFactory()` are empty in the BLE implementation; do not call them.
- Disconnect the standard connection before upgrading.
- The app must declare `com.feasycom.ble.service.OTAService`.
- `reset = true` means restore factory settings after the upgrade. Pass the value required by your product.

### 6.4 SPP Scanning, Connection, and Data Transfer

```kotlin
if (!api.isEnabled) return
api.startScan()

override fun sppPeripheralFound(device: FscDevice, rssi: Int) {
    // Bonded devices are reported first, followed by discovered devices.
}

api.connect(device.address)
api.send(byteArrayOf(0x01, 0x02))
```

SPP usually requires bonding first. The bonding process may trigger `bondIng()`. Rely on `sppPeripheralConnected` / `sppPeripheralDisconnected` for connection results.

### 6.5 SPP OTA / SDP

```kotlin
// SDP: wait for the module to connect back.
api.openSdpService("00001101-0000-1000-8000-00805F9B34FB")

// Standard OTA
api.disconnect()
api.connectToOTAWithFactory(device.address, dfuBytes, false)

// Verify the module version when required.
api.connectToVerifyOTAWithFactory(device.address, dfuBytes, false, true)
```

SPP OTA also uses `com.feasycom.spp.service.OTAService`, which must be declared in the app manifest. NAK retransmission and the EOT special case for certain modules are handled internally by the SDK.

### 6.6 WiFi/BW Provisioning

```kotlin
// Connect to the BW module over its BLE channel first.
api.connect(device.address)

// Provision after the connection succeeds.
api.setNetwork(ssid, password, object : FscNetworkCentralBwCallbacks {
    override fun success(ip: String) {
        // Provisioning succeeded; the module IP is available.
    }

    override fun failure() {
        // Provisioning failed or timed out after about 20 seconds.
    }
})
```

Recommendations:

- Connect the phone to a 2.4 GHz WiFi network. ESP Touch/BW modules generally do not support 5 GHz.
- Confirm that `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, and location/nearby WiFi permissions are granted.
- After success, call `api.getVersion()`; the result is delivered through `versionInformation`.

### 6.7 WiFi/BW OTA

```kotlin
api.startOTA(otaUrl, object : FscOtaCentralBwCallbacks {
    override fun onOtaStartConfig() {
        // Start configuration / prepare for the upgrade.
    }

    override fun onOtaProgress(progress: String?) {
        // progress may be a string such as "10".
    }

    override fun onOtaSuccess() {
    }

    override fun onOtaFailure() {
    }

    override fun onOtaNetworkIsNotConfigured() {
        // Provision the module first.
    }
})
```

For solutions such as BW256B that use a local HTTP server on the phone and let the module download the firmware, also use:

```kotlin
FirmwareHttpSdk.init(context)
FirmwareHttpSdk.setFirmwareUri(uri)
val url = FirmwareHttpSdk.startServer(fileName, type)
// Pass the URL or AT command to the module.
FirmwareHttpSdk.stopServer()
```

Whether `otaName` is a URL or an AT command depends on the module protocol. The demo `WFOtaActivity` passes a URL.

### 6.8 TCP / BW256B Transparent Transmission

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

TCP is a separate path from BLE/SPP. Do not mix it with `FscBleCentralApi`. Call `disconnect()` when the screen is destroyed to avoid lingering Netty threads and connections.

### 6.9 Downloading Firmware from the Cloud

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

Do not include file extensions in the arguments to `downloadDFU` / `downloadBIN`. After downloading, pass `file.readBytes()` to the OTA API.

---

## 7. Data Models

### `FscDevice` (BLE/SPP Scan Result)

`com.feasycom.common.bean.FscDevice`

| Field / method | Description |
|---|---|
| `name` | Device name. |
| `address` | MAC address. |
| `device` | Original `BluetoothDevice`. |
| `rssi` | Signal strength. |
| `mode` | `BLE` or `SPP`. |
| `scanRecord` | Raw advertisement data. |
| `flag` / `serviceData` / `manufacturerSpecificData` | Parsed advertisement fields. |
| `completeLocalName` | Complete local name. |
| `txPowerLevel` | Advertised TX power. |
| `incompleteServiceUUIDs_16bit/128bit` | Service UUIDs in the advertisement. |

### `wifi.bean.BluetoothDeviceWrapper` (BW Scan Result)

`com.feasycom.wifi.bean.BluetoothDeviceWrapper`

| Field | Description |
|---|---|
| `device` | Original `BluetoothDevice`. |
| `name` | Device name. |
| `address` | Device address. |
| `rssi` | Signal strength. |
| `locationName` | Location name. |
| `beacon` | `FeasyBeaconBw`, containing `sensorBw`, `gateway`, and `battery`. |

### `DfuFileInfo`

`com.feasycom.ota.bean.DfuFileInfo`

```java
public int bootloader;
public int versionStart;       // Note: the field name is versionStart.
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

### Other Models

- `ConnectType`: `CONNECT`, `MODIFY`, and `OTA`, used to distinguish connection modes.
- `DeviceInfo`: an `androidx.room` entity that can be used to store device information. The SDK does not create a database automatically.
- `ScannerFilter`: contains `rssi` and `deviceName`. The current BLE implementation does not actually apply it; filter results in the callback.

---

## 8. Utility Classes

| Class | Main capabilities |
|---|---|
| `com.feasycom.ble.utils.FeasycomUtil` | `parseAdvData(FscDevice, byte[])` parses BLE advertisements. |
| `com.feasycom.ota.utils.OtaUtils` | `checkDfuFile`, `getDfuFileInformation`, `getModelName`, `needsReconnect`. |
| `com.feasycom.ota.utils.XmodemUtils` | Xmodem 1K packetization and OTA status constants. |
| `com.feasycom.ota.utils.FileUtil` | Byte, hexadecimal, and file utilities; `stringToInt`. |
| `com.feasycom.wifi.utils.BwUtils` | `bytesToHex`, `getBeacon`, `getLocationName`, and hexadecimal conversion. |
| `com.feasycom.wifi.utils.BwNetUtils` | WiFi utilities for SSID, IP, BSSID, broadcast address, and related values. |
| `com.feasycom.common.utils.FileUtils` | File utilities such as `createTestData`. |
| `com.feasycom.common.utils.MsgLogger` | SDK logging; controlled by `FscBleCentralApiImp.isShowLog`. |
| `com.feasycom.ota.utils.CRC16` | CRC16 checksum. |
| `com.feasycom.ota.utils.BT1038Utils` | BT1038-series protocol helpers. |
| `com.feasycom.spp.utils.ConstantUtil` | SPP module-model constants and `isDeviceModel`. |
| `com.feasycom.wifi.tcp.FirmwareHttpSdk` | Facade for the local firmware HTTP service. |

---

## 9. Constants Quick Reference

### 9.1 `Constant`

Package: `com.feasycom.common.utils.Constant`

| Constant | Value / meaning |
|---|---|
| `PACKAGE_SEND_FINISH` | `100`, packet transmission complete. |
| `FIFO_SEND_FINISH` | `101`, FIFO transmission complete. |
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

### 9.2 OTA Status `XmodemUtils`

| Constant | Value | Meaning |
|---|---|---|
| `OTA_STATUS_BEGIN` | `110` | Started. |
| `OTA_STATUS_PROCESSING` | `121` | In progress. |
| `OTA_STATUS_FINISH` | `10086` | Completed. |
| `OTA_STATUS_FAILED` | `120` | Failed. |
| `OTA_STATUS_VERIFY_FAILED` | `122` | Verification failed. |
| `OTA_STATUS_VERIFY_MODEL_FAILED` | `124` | Model mismatch. |
| `OTA_STATUS_VERIFY_APP_VERSION_FAILED` | `125` | App version mismatch. |
| `OTA_STATUS_AT_FAILED` | `123` | AT query failed. |

### 9.3 `BwCommont`

Package: `com.feasycom.wifi.BwCommont`

| Constant | Value |
|---|---|
| `BW_226` | `0` |
| `BW_236` | `1` |
| `BW_246` | `2` |
| `PORT_OCCUPATION` | `4` |
| `FAILED_TO_CONFIGURE_NETWORK` | `5` |

### 9.4 SPP Models

`com.feasycom.spp.utils.ConstantUtil` provides model constants such as `BT401`, `BT826`, `BT1036`, and `BT1116FC`, as well as `isDeviceModel(int)`. Refer to that class and the device protocol for the exact model values.

---

## 10. Notes and Troubleshooting

1. **No scan callbacks**
   - On Android 12+, are `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` granted at runtime?
   - On Android 11 and below, is location permission granted?
   - Is Bluetooth enabled?
   - Have `initialize()` and `setCallbacks()` been called?
   - Are you using `FscBleCentralBwApiImp` as a generic BLE API? It only supports BW modules with fixed UUIDs.

2. **BLE OTA shows no progress or fails immediately**
   - Does the app manifest declare `com.feasycom.ble.service.OTAService`?
   - Did you call `startOTA()` by mistake? It is currently empty in the BLE implementation.
   - Did you call `disconnect()` before upgrading?
   - Is the DFU file intact, and do the model and version match?

3. **SPP OTA shows no progress or fails immediately**
   - Does the app manifest declare `com.feasycom.spp.service.OTAService`?
   - Did you call `disconnect()` before switching from a standard connection to OTA?
   - Is the device already bonded?
   - Are you using `connectToOTAWithFactory` / `connectToVerifyOTAWithFactory`?

4. **`UnsatisfiedLinkError`**
   - Did you copy only the JAR and omit `libFeasyBlueLibrary.so`?
   - Is the SO file in the correct ABI directory?
   - Did you rename the SO file?
   - Do `abiFilters` match the SO ABIs?
   - Did the packaging tool strip the SO file?

5. **Release build callbacks do not fire / native methods are missing**
   - Did you enable minification without adding the keep rules in Section 3.4?
   - Did you preserve `native <methods>`?

6. **SPP `getInstance()` throws `Context is null`**
   - Call `FscSppCentralApiImp.getInstance(context)` first, then use the no-argument `getInstance()`.

7. **BLE connection produces no result**
   - `connect()` does not return a result. Wait for `blePeripheralConnected` / `blePeripheralDisconnected`.
   - Make sure `setCallbacks()` is called before `connect()`.
   - Verify that the device is in range and not occupied by another app.

8. **BW `connect(address, pin)` returns false**
   - Do not use the return value as the final connection result. Rely on `blePeripheralConnected` / `blePeripheralDisconnected`.

9. **WiFi provisioning fails**
   - Is the phone connected to a 2.4 GHz WiFi network?
   - Are WiFi, location, and nearby WiFi permissions granted?
   - Are the SSID and BSSID correct?
   - In `ConfigNetworkBwApiImp`, only the `BW_246` ESP Touch path is currently available.

10. **TCP cannot connect**
    - Did you call `TCPClientManager.initClient(callback)` first?
    - Are the IP address and port correct?
    - Are the phone and device on the same network?
    - Did the page call `disconnect()` and close the connection prematurely?

11. **How to interpret OTA status values**
    - BLE/SPP use `XmodemUtils.OTA_STATUS_*`.
    - WiFi/BW uses `onOtaProgress` / `onOtaSuccess` / `onOtaFailure` from `FscOtaCentralBwCallbacks`.
    - If the status is `120/122/124/125`, check the firmware model, version, checksum, and connection stability first.

12. **Multiple device connections**
    - Standard BLE supports up to 6 connections.
    - WiFi/BW supports a single connection.
    - TCP supports a single connection.
    - Do not use one `FscBleCentralBwApiImp` instance to manage multiple BW devices simultaneously.

13. **Callback threads**
    - SDK callbacks may come from GATT, Binder, Netty, or coroutine I/O threads.
    - Before updating UI, use `runOnUiThread`, `Handler(Looper.getMainLooper())`, or `lifecycleScope.launch(Dispatchers.Main)`.

14. **Lifecycle**
    - Stop scanning, disconnect, and stop TCP when the screen is destroyed.
    - Do not retain an Activity `Context` for long periods. The SDK contains static singletons and static `Context` fields; pass `applicationContext` consistently.
    - Call `closeSdpService()` when the SPP SDP service is no longer needed.

15. **Logging and security**
    - For release builds, call `FscBleCentralApiImp.isShowLog(false)` to avoid excessive SDK logging.
    - The SDK's network layer contains legacy compatibility settings. Review them against your company's security and compliance requirements before a production release.
    - Do not treat legacy statistics classes such as `Acquisition` as dependencies for new features.

16. **Scan filters**
    - The filter parameter of `startScan(ScannerFilter)` is not actually applied in the current BLE implementation.
    - Filter by RSSI and name in the `blePeripheralFound` callback.
    - Standard BLE scanning stops automatically after 10 seconds by default; BW scanning stops after 15 seconds by default.

---

## 11. Version History

| Library version | Description |
|---|---|
| `3.5.7.5` | Current delivery package/demo app version; SDK BuildConfig is `3.5.6.2`. Includes BLE/SPP OTA, WiFi/BW provisioning, TCP/BW256B, and cloud firmware download. |

> For historical changes, refer to the `README.md` in the project source repository. This delivery package does not include the SDK source.

---

## Appendix: Minimal Integration Checklist

- [ ] `feasyblue.jar` is in `app/libs`.
- [ ] `libFeasyBlueLibrary.so` is in `app/libs/<abi>/` for both ABIs.
- [ ] `jniLibs.srcDirs = ['libs']`.
- [ ] `abiFilters` contains only `arm64-v8a` and `armeabi-v7a`.
- [ ] `com.feasycom.ble.service.OTAService` is declared.
- [ ] `com.feasycom.spp.service.OTAService` is declared.
- [ ] Android 12+ requests `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
- [ ] Android 11 and below requests location permission.
- [ ] ProGuard keep rules are added.
- [ ] Initialization order is `getInstance(context)` → `initialize()` → `setCallbacks()`.
- [ ] UI updates are dispatched to the main thread.
- [ ] Scanning is stopped, connections are closed, and TCP is disconnected when the screen is destroyed.
