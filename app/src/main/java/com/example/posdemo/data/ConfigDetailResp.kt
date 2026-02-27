package com.example.posdemo.data



data class ConfigDetailResp(
    val code: Int,
    val data: ConfigEnvelope?
)

data class ConfigEnvelope(
    val configType: String,
    val configDetail: com.google.gson.JsonElement
)

data class DeviceConfigDetail(
    val id: String?,
    val appInstall: Int?,
    val usbDebug: Int?,
    val otherConfig: String?,
    val subAccount: String?,
    val companyCode: String?,
    val createTime: String?,
    val updateTime: String?,
    val mqttPwd: String?,
    val version: Int?
)

data class DeviceFenceConfigDetail(
    val id: String?,
    val deviceGroupId: String?,

    val fenceCenterLng: String?,
    val fenceCenterLat: String?,
    val fenceRadius: String?,

    val fenceSwitch: Int?,
    val fenceLimitMobileData: Int?,
    val fenceLimitScreenOpen: Int?,
    val fenceLimitBlueTooth: Int?,
    val fenceLimitRestoreFactoryData: Int?,
    val fenceLimitDeviceBell: Int?,

    val handleStatus: Int?,

    val updateTime: String?,
    val createTime: String?,

    val createUser: String?,
    val updateUser: String?,

    val useMap: String?,
    val email: String?,

    val frequency: String?,
    val restoreDataLimitMinute: Int?,
    val restoreDataLimitHour: Int?,

    val version: Int?
)

data class FunctionConfigDetail(
    val id: String?,

    val functionConfigName: String?,
    val functionConfigCode: String?,

    val blueTooth: String?,
    val wifi: String?,
    val allowUninstall: String?,
    val uninstallWhitelist: String?,
    val mobleTraffic: String?,
    val thirdApp: String?,
    val camera: String?,
    val phone: String?,
    val message: String?,
    val gps: String?,
    val adb: String?,
    val usb: String?,
    val home: String?,
    val dropDown: String?,
    val input: String?,
    val restoreFactory: String?,
    val sd: String?,

    val createTime: String?,
    val updateTime: String?,
    val pushTime: String?,

    val effectiveStatus: Int?,
    val version: Int?
)

data class WifiWhitelistConfigDetail(
    val id: String?,

    val wifiWhitelistName: String?,
    val sendTime: String?,

    val wifiWhitelist: String?,
    val wifiWhitelistCode: String?,

    val dataSource: Int?,
    val version: Int?
)

data class SilentAppConfigDetail(
    val configCode: String?,
    val appPackage: String?,
    val version: Int?
)

data class OccupyScreenAppConfigDetail(
    val configCode: String?,
    val appPackage: String?,
    val unLockPwd: String?,
    val version: Int?
)

data class DesktopConfigDetail(
    val id: String?,

    val cusDeskCode: String?,
    val cusDeskName: String?,
    val cusDeskPackageName: String?,

    val deskApkUrl: String?,
    val appVersion: String?,

    val unlockPwd: String?,

    val hideStatusBar: Int?,
    val hideNavigationBar: Int?,
    val timedUnlock: Int?,
    val lockTime: Int?,
    val template: Int?,
    val layoutRow: Int?,
    val layoutColumn: Int?,

    val deskPicUrl: String?,

    val createTime: String?,
    val effectiveStatus: Int?,

    val cusDeskType: String?,

    val configFileDownloadUrl: String?,

    val version: Int?
)

data class WifiConfigDetail(
    val id: String?,

    val wifiName: String?,
    val ssid: String?,

    val onlyssid: String?,
    val hideNetwork: String?,
    val autoJoin: String?,

    val safeType: String?,
    val password: String?,

    val sendTime: String?,
    val wifiCode: String?,

    val version: Int?
)

data class ApnConfigDetail(
    val id: String?,
    val configCode: String?,
    val dvcId: String?,

    val configName: String?,

    val apnName: String?,
    val apn: String?,

    val agent: String?,
    val port: String?,
    val userName: String?,
    val passWord: String?,
    val server: String?,
    val mmsc: String?,
    val mmsAgent: String?,
    val mmsPort: String?,

    val mcc: String?,
    val mnc: String?,

    val authenticationType: String?,
    val apnType: String?,
    val apnProtocol: String?,

    val createTime: String?,
    val updateTime: String?,

    val version: Int?
)

data class SendScriptConfigDetail(
    val id: String?,
    val sendScriptCode: String?,
    val sendScriptName: String?,

    val scriptType: String?,

    val sendScriptContentVo: SendScriptContentVo?,

    val companyCode: String?,
    val version: Int?
)

data class SendScriptContentVo(
    val action: String?,
    val packageName: String?,
    val className: String?,

    val startMode: Int?,

    val appModule: AppModule?,

    val content: String?
)

data class AppModule(
    val packageName: String?,
    val className: String?
)

data class AppWhitelistConfigDetail(
    val id: String?,

    val appWhitelistName: String?,
    val sendTime: String?,

    val appWhitelistContent: List<AppWhitelistItem>?,

    val appWhitelistCode: String?,

    val createTime: String?,
    val updateTime: String?,

    val version: Int?
)

data class AppWhitelistItem(
    val appName: String?,
    val appVersionName: String?,
    val appPackage: String?
)

data class AppDeployConfigDetail(
    val id: String?,

    val deployName: String?,
    val configCode: String?,
    val dvcId: String?,

    val deployMode: Int?,
    val createTime: String?,
    val updateTime: String?,
    val deployTime: String?,

    val deployInterval: Int?,

    val appDeployApps: List<AppDeployApp>?,

    val version: Int?,

    val autoStart: String?
)

data class AppDeployApp(
    val appVersionCode: String?,
    val appName: String?,

    val sign: String?,
    val signRemark: String?,

    val appPackage: String?,
    val appUrl: String?,
    val appLogo: String?,

    val appVersionName: String?,
    val appCode: String?
)

data class BootAnimationConfigDetail(
    val configCode: String?,
    val zipPath: String?,
    val status: Int?,
    val version: Int?
)

data class StrategyConfigDetail(
    val configName: String?,
    val configFileUrl: String?,
    val configCode: String?,

    val configType: String?,
    val version: String?
)