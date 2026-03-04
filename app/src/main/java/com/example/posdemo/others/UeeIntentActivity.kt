package com.example.posdemo.others

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.posdemo.R
import com.example.posdemo.databinding.ActivityUeeIntentBinding
import com.example.posdemo.utils.PackageUtil
import com.example.posdemo.utils.PermissionUtil
import com.urovo.utils.BytesUtil
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class UeeIntentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUeeIntentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUeeIntentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCheckKeyRemap.setOnClickListener { onCheckKeyRemapButtonClicked() }
        binding.btnImportKeyRemap.setOnClickListener { onImportKeyRemapButtonClicked() }
        binding.btnExportKeyRemap.setOnClickListener { onExportKeyRemapButtonClicked() }
        binding.btnEnableKeyRemap.setOnClickListener { onEnableKeyRemapButtonClicked() }
        binding.btnDisableKeyRemap.setOnClickListener { onDisableKeyRemapButtonClicked() }
        binding.btnResetKeyRemap.setOnClickListener { onResetKeyRemapButtonClicked() }

        binding.btnCheckUBrowserConfig.setOnClickListener { onCheckUBrowserConfigButtonClicked() }
        binding.btnImportUBrowserConfig.setOnClickListener { onImportUBrowserConfigButtonClicked() }
        binding.btnExportUBrowserConfig.setOnClickListener { onExportUBrowserConfigButtonClicked() }
        binding.btnResetUBrowserConfig.setOnClickListener { onResetUBrowserConfigButtonClicked() }

        binding.btnCheckEnterpriseLauncherConfig.setOnClickListener { onCheckEnterpriseLauncherConfigButtonClicked() }
        binding.btnImportEnterpriseLauncherConfig.setOnClickListener { onImportEnterpriseLauncherConfigButtonClicked() }
        binding.btnExportEnterpriseLauncherConfig.setOnClickListener { onExportEnterpriseLauncherConfigButtonClicked() }
        binding.btnResetEnterpriseLauncherConfig.setOnClickListener { onResetEnterpriseLauncherConfigButtonClicked() }

    }

    override fun onStart() {
        super.onStart()
        if (!PackageUtil.isPackageInstalled(this, "com.ubx.keyremap")) {
            binding.btnCheckKeyRemap.isEnabled = false
            binding.btnImportKeyRemap.isEnabled = false
            binding.btnExportKeyRemap.isEnabled = false
            binding.btnEnableKeyRemap.isEnabled = false
            binding.btnDisableKeyRemap.isEnabled = false
            binding.btnResetKeyRemap.isEnabled = false
        }
        if (!PackageUtil.isPackageInstalled(this, "com.urovo.browser.u")) {
            binding.btnCheckUBrowserConfig.isEnabled = false
            binding.btnImportUBrowserConfig.isEnabled = false
            binding.btnExportUBrowserConfig.isEnabled = false
            binding.btnResetUBrowserConfig.isEnabled = false
        }
        if (!PackageUtil.isPackageInstalled(this, "com.urovo.enterprisemodeldesktop")) {
            binding.btnCheckEnterpriseLauncherConfig.isEnabled = false
            binding.btnImportEnterpriseLauncherConfig.isEnabled = false
            binding.btnExportEnterpriseLauncherConfig.isEnabled = false
            binding.btnResetEnterpriseLauncherConfig.isEnabled = false
        }

    }

    private fun onCheckKeyRemapButtonClicked() {
        if (!PermissionUtil.ensureAllFilesAccess(this)) {
            return
        }
        runCatching {
            val file = File("/sdcard/keys_config.txt")
            if (!file.isFile) {
                Toast.makeText(this, "No File. Please export or upload to \"/sdcard/keys_config.txt\" first.", Toast.LENGTH_SHORT).show()
                return
            }
            return@runCatching file
        }.onSuccess { file ->
            binding.tvResult.text = buildString {
                append(file.readText())
                append("\nThis might not be the up-to-date one. Please export to sync.")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onImportKeyRemapButtonClicked() {
        runCatching {
            val intent = Intent().apply {
                action = "action.PROGRAMMABLE_IMPORT_KEY"
                `package` = "com.ubx.keyremap"
            }
            startActivity(intent)
        }.onSuccess {
            Toast.makeText(this, "Import Key Remap Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Imported from \"/sdcard/keys_config.txt\"\n")
                append("adb shell am start -a action.PROGRAMMABLE_IMPORT_KEY -p com.ubx.keyremap\n\n")
                append("Note:\n")
                append(" - You can just import the partial config. e.g. \n")
                append("<Key>\n" +
                        "    <KeyName>SCAN_2</KeyName>\n" +
                        "    <KeyCode>14</KeyCode>\n" +
                        "</Key>\n")
                append(" - If KeyName already exists, then will overwrite\n")
                append(" - If KeyName not exists, then will be appended, the rest remains")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onExportKeyRemapButtonClicked() {
        runCatching {
            val intent = Intent().apply {
                action = "action.PROGRAMMABLE_EXPORT_KEY"
                `package` = "com.ubx.keyremap"
            }
            startActivity(intent)
        }.onSuccess {
            Toast.makeText(this, "Export Key Remap Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Exported to \"/sdcard/keys_config.txt\"\n")
                append("\"adb shell am start -a action.PROGRAMMABLE_EXPORT_KEY -p com.ubx.keyremap\"")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onEnableKeyRemapButtonClicked() {
        if (!PermissionUtil.ensureAllFilesAccess(this)) {
            return
        }
        runCatching {
            val file = File("/sdcard/keys_config.txt")
            file.writeText("<KeyRemapEnabled>true</KeyRemapEnabled>")
            val intent = Intent().apply {
                action = "action.PROGRAMMABLE_IMPORT_KEY"
                `package` = "com.ubx.keyremap"
            }
            startActivity(intent)
        }.onSuccess {
            Toast.makeText(this, "Enable Key Remap Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Enabled Key Remap successfully\n\n")
                append("<KeyRemapEnabled>true</KeyRemapEnabled>\n")
                append("\"adb shell am start -a action.PROGRAMMABLE_IMPORT_KEY -p com.ubx.keyremap\"")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onDisableKeyRemapButtonClicked() {
        if (!PermissionUtil.ensureAllFilesAccess(this)) {
            return
        }
        runCatching {
            val file = File("/sdcard/keys_config.txt")
            file.writeText("<KeyRemapEnabled>false</KeyRemapEnabled>")
            val intent = Intent().apply {
                action = "action.PROGRAMMABLE_IMPORT_KEY"
                `package` = "com.ubx.keyremap"
            }
            startActivity(intent)
        }.onSuccess {
            Toast.makeText(this, "Disable Key Remap Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Disabled Key Remap successfully\n\n")
                append("<KeyRemapEnabled>false</KeyRemapEnabled>\n")
                append("\"adb shell am start -a action.PROGRAMMABLE_IMPORT_KEY -p com.ubx.keyremap\"")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onResetKeyRemapButtonClicked() {
        runCatching {
            val intent = Intent().apply {
                component = ComponentName("com.ubx.keyremap", "com.ubx.keyremap.component.ImportExportService")
                putExtra("programmable", 3)
            }
            startService(intent)
        }.onSuccess {
            Toast.makeText(this, "Reset Key Remap Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Reset Key Remap successfully\n\n")
                append("\"startService with putExtra(\"programmable\", 3)\"")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onCheckUBrowserConfigButtonClicked() {
        if (!PermissionUtil.ensureAllFilesAccess(this)) {
            return
        }
        runCatching {
            val file = File("/sdcard/Download/U_Browser.config")
            if (!file.isFile) {
                Toast.makeText(this, "No File. Please export or upload to \"/sdcard/Download/U_Browser.config\" first.", Toast.LENGTH_SHORT).show()
                return
            }
            return@runCatching file
        }.onSuccess { file ->
            binding.tvResult.text = buildString {
                append(file.readText())
                append("\nThis might not be the up-to-date one. Please export to sync.\n\n")
                append("Config Default Template:\n")
                append("IMPORTANT ONES: \n" +
                        " - homepage\n" +
                        " - startup: default_page / homepage\n" +
                        " - bookmarks\n" +
                        " - whitelist: Only can access these pages\n" +
                        " - password: password of Settings. This is encrypted locally\n" +
                        " - KIOSK MODE\n" +
                        " - OTHERS: TAB_NOT_CLOSED; NO_SCROLLING; HIDE_NAVIGATION; \n\n" +
                            "scale=100\n" +
                            "language=system\n" +
                            "whilelist=false\n" +
                            "zoom=false\n" +
                            "force_dark=false\n" +
                            "scroll_disable=false\n" +
                            "hide_nav_bar=false\n" +
                            "timeout=30\n" +
                            "dominate=false\n" +
                            "bookmark=[]\n" +
                            "tab_always=false\n" +
                            "javascript_enabled=false\n" +
                            "force_adapt=false\n" +
                            "password=\n" +
                            "startup=load_empty_page\n" +
                            "private_mode=false\n" +
                            "theme=system\n" +
                            "mode_desktop=false\n" +
                            "search_engine=Google\n" +
                            "homepage=ubrowser://newtab")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onImportUBrowserConfigButtonClicked() {
        if (!File("/sdcard/Download/U_Browser.config").isFile) {
            Toast.makeText(this, "No File. Please export or upload to \"/sdcard/Download/U_Browser.config\" first.", Toast.LENGTH_SHORT).show()
        }
        runCatching {
            val intent = Intent().apply {
                action = "com.urovo.browser.action.CONFIG"
                `package` = "com.urovo.browser.u"
                putExtra("FILE_PATH", "/sdcard/Download/U_Browser.config")
            }
            sendBroadcast(intent)
        }.onSuccess {
            Toast.makeText(this, "Import UBrowser Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Imported from \"/sdcard/Download/U_Browser.config\"\n")
                append("adb shell am broadcast -a com.urovo.browser.action.CONFIG -p com.urovo.browser.u --es FILE_PATH /sdcard/Download/U_Browser.config -f 0x01000000\n\n")
                append("Note:\n")
                append(" - Must import the whole Config. Default value will be set for the missing config(e.g. if empty file is imported, same as Reset \n")
                append(" - Password is encrypted in the config file \n\n")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onExportUBrowserConfigButtonClicked() {
        runCatching {
            val intent = Intent().apply {
                action = "com.urovo.browser.action.CONFIG_EXPORT"
                `package` = "com.urovo.browser.u"
            }
            sendBroadcast(intent)
        }.onSuccess {
            Toast.makeText(this, "Export UBrowser Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Exported to \"/sdcard/Download/U_Browser.config\"\n\n")
                append("\"adb shell am broadcast -a com.urovo.browser.action.CONFIG_EXPORT -p com.urovo.browser.u -f 0x01000000\"")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onResetUBrowserConfigButtonClicked() {
        if (!PermissionUtil.ensureAllFilesAccess(this)) {
            return
        }
        runCatching {
            val file = File("/sdcard/Download/U_Browser.config")
            file.writeText("scale=100\n" +
                    "language=system\n" +
                    "whilelist=false\n" +
                    "zoom=false\n" +
                    "force_dark=false\n" +
                    "scroll_disable=false\n" +
                    "hide_nav_bar=false\n" +
                    "timeout=30\n" +
                    "dominate=false\n" +
                    "bookmark=[]\n" +
                    "tab_always=false\n" +
                    "javascript_enabled=false\n" +
                    "force_adapt=false\n" +
                    "password=\n" +
                    "startup=load_empty_page\n" +
                    "private_mode=false\n" +
                    "theme=system\n" +
                    "mode_desktop=false\n" +
                    "search_engine=Google\n" +
                    "homepage=ubrowser://newtab")
            val intent = Intent().apply {
                action = "com.urovo.browser.action.CONFIG"
                `package` = "com.urovo.browser.u"
                putExtra("FILE_PATH", "/sdcard/Download/U_Browser.config")
            }
            sendBroadcast(intent)
        }.onSuccess {
            Toast.makeText(this, "Reset UBrowser Config successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onCheckEnterpriseLauncherConfigButtonClicked() {
        binding.tvResult.text = buildString {
            append("Nothing to check for now...\n\n")
            append(enterpriseLauncherNotes)
        }
    }


    private fun onImportEnterpriseLauncherConfigButtonClicked() {
        if (!File("/sdcard/Download/settings_property.json").isFile) {
            Toast.makeText(this, "No File. Please export or upload to \"/sdcard/Download/settings_property.json\" first.", Toast.LENGTH_SHORT).show()
        }
        runCatching {
            val intent = Intent().apply {
                action = "com.ubx.action.CONFIG_PATH"
                `package` = "com.urovo.enterprisemodeldesktop"
                putExtra("config_path", "/sdcard/Download/settings_property.json")
            }
            sendBroadcast(intent)
        }.onSuccess {
            Toast.makeText(this, "Import EnterpriseLauncher Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Imported from \"/sdcard/Download/settings_property.json\"\n")
                append("adb shell am broadcast -a com.ubx.action.CONFIG_PATH -p com.urovo.enterprisemodeldesktop --es config_path /sdcard/Download/settings_property.json -f 0x01000000\n\n")
                append("Note:\n")
                append(" - Must partially import the settingTableList. For the rest, they are complete import.\n")
                append(" - Must have all 5 Lists, otherwise app will crash\n\n")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onExportEnterpriseLauncherConfigButtonClicked() {
        Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show()
    }


    private fun onResetEnterpriseLauncherConfigButtonClicked() {
        val file = File("/sdcard/Download/settings_property.json")
        file.writeText(enterpriseDefaultConfig)
        runCatching {
            val intent = Intent().apply {
                action = "com.ubx.action.CONFIG_PATH"
                `package` = "com.urovo.enterprisemodeldesktop"
                putExtra("config_path", "/sdcard/Download/settings_property.json")
            }
            sendBroadcast(intent)
        }.onSuccess {
            Toast.makeText(this, "Reset EnterpriseLauncher Config successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("Reset Enterprise Launcher successfully\n\n")
                append("This is using broadcast to importing default config of EnterpriseLauncher")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    // <----------------- Helper methods ----------------->

    private fun encryptUBrowserPassword(plain: String): String {
        val keyInHex = "6836686e6269646e736d7a7776776d77"
        val iv = ByteArray(16)
        val key = SecretKeySpec(BytesUtil.hexString2Bytes(keyInHex), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(ct, Base64.NO_WRAP).replace("=", "")
    }

    private fun decryptUBrowserPassword(b64NoPad: String): String {
        val keyInHex = "6836686e6269646e736d7a7776776d77"
        val iv = ByteArray(16)
        val padded = buildString {
            append(b64NoPad)
            while (length % 4 != 0) append('=')
        }

        val key = SecretKeySpec(BytesUtil.hexString2Bytes(keyInHex), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        val ct = Base64.decode(padded, Base64.NO_WRAP)
        val pt = cipher.doFinal(ct)
        return String(pt, Charsets.UTF_8)
    }

    private val enterpriseDefaultConfig = "{\n" +
            "   \"keepAliveAppsTableList\":[\n" +
            "      \n" +
            "   ],\n" +
            "   \"settingTableList\":[\n" +
            "      {\n" +
            "         \"diescription\":\"登录密码\",\n" +
            "         \"paramName\":\"adminPassword\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"1\",\n" +
            "         \"baseObjId\":41\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"默启应用\",\n" +
            "         \"paramName\":\"defaultLaunchApp\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":42\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"默启应用开关\",\n" +
            "         \"paramName\":\"defaultLaunchAppisSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":43\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"app白名单是否全选开关\",\n" +
            "         \"paramName\":\"appWhiteIsAllSelected\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":44\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"Wifi白名单开关\",\n" +
            "         \"paramName\":\"wifiWhiteIsSwitch\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":45\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否为第一次启动\",\n" +
            "         \"paramName\":\"isFirstLaunch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":46\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否启动了透明页面\",\n" +
            "         \"paramName\":\"isStartTransparent\",\n" +
            "         \"paramVal\":\"1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":47\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具开关\",\n" +
            "         \"paramName\":\"quickToolSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":48\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具Wlan的开关\",\n" +
            "         \"paramName\":\"wlanSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":49\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具蓝牙的开关\",\n" +
            "         \"paramName\":\"bluetoothSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":50\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具移动网络的开关\",\n" +
            "         \"paramName\":\"mobileNetworkSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":51\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具显示设置的开关\",\n" +
            "         \"paramName\":\"displaySettingSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":52\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具声音设置的开关\",\n" +
            "         \"paramName\":\"soundSettingSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":53\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具显示位置设置的选择\",\n" +
            "         \"paramName\":\"entrySettingSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":54\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"应用白名单开关\",\n" +
            "         \"paramName\":\"appWhiteIsSwitch\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":55\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"log开关\",\n" +
            "         \"paramName\":\"logIsSwitch\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":56\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"默启应用地址\",\n" +
            "         \"paramName\":\"defaultLaunchAppActivity\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":57\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"图标大小\",\n" +
            "         \"paramName\":\"iconSize\",\n" +
            "         \"paramVal\":\"3\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":58\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"壁纸路径\",\n" +
            "         \"paramName\":\"paperPath\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":59\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"允许保存的最大日志大小\",\n" +
            "         \"paramName\":\"logCapacity\",\n" +
            "         \"paramVal\":\"10\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"10\",\n" +
            "         \"baseObjId\":60\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用导航栏\",\n" +
            "         \"paramName\":\"navigationBar\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":61\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否允许应用市场/ums安装\",\n" +
            "         \"paramName\":\"umsInstall\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":62\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用导航栏左键\",\n" +
            "         \"paramName\":\"backButton\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":63\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用导航栏中键\",\n" +
            "         \"paramName\":\"homeButton\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":64\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用导航栏右键\",\n" +
            "         \"paramName\":\"menuButton\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":65\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用状态栏\",\n" +
            "         \"paramName\":\"statusBar\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":66\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用移动网络\",\n" +
            "         \"paramName\":\"mobileData\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":67\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用WIFI\",\n" +
            "         \"paramName\":\"wifiData\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":68\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用蓝牙\",\n" +
            "         \"paramName\":\"bluetoothData\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":69\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用GPS\",\n" +
            "         \"paramName\":\"GPSStatus\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":70\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用USB\",\n" +
            "         \"paramName\":\"USBStatus\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":71\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用ADB\",\n" +
            "         \"paramName\":\"ADBStatus\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":72\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否自动电话录音\",\n" +
            "         \"paramName\":\"CallRecordStatus\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":73\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"录音文件最大容量\",\n" +
            "         \"paramName\":\"CallRecordFileCapacity\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":74\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"标题类型\",\n" +
            "         \"paramName\":\"titleType\",\n" +
            "         \"paramVal\":\"1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"1\",\n" +
            "         \"baseObjId\":75\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"自定义Launcher标题\",\n" +
            "         \"paramName\":\"customizeTitle\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"UROVO-企业模式\",\n" +
            "         \"baseObjId\":76\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"启用企业模式\",\n" +
            "         \"paramName\":\"isStart\",\n" +
            "         \"paramVal\":\"1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":77\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否启用向导\",\n" +
            "         \"paramName\":\"isGuideStart\",\n" +
            "         \"paramVal\":\"1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":78\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"访客桌面文件夹管理\",\n" +
            "         \"paramName\":\"folderManagement\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":79\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"访客桌面文件夹路径\",\n" +
            "         \"paramName\":\"folderPath1\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":80\n" +
            "      }\n" +
            "   ],\n" +
            "   \"webShortcutAppsTableList\":[\n" +
            "      \n" +
            "   ],\n" +
            "   \"whiteListAppsTableList\":[\n" +
            "      \n" +
            "   ],\n" +
            "   \"whiteListWifiTableList\":[\n" +
            "      \n" +
            "   ]\n" +
            "}"

    private val enterpriseLauncherNotes = "Important ones:\n" +
            " - whiteListAppsTableList: The APPs displayed to the visitors\n" +
            " - webShortCut\n" +
            " - Admin Password\n" +
            " - QuickToo for settings\n" +
            " - Kiosk related: disable three keys + disable status bar\n" +
            "\n" +
            "Default Config:\n" +
            "{\n" +
            "   \"keepAliveAppsTableList\":[\n" +
            "\n" +
            "   ],\n" +
            "   \"settingTableList\":[\n" +
            "      {\n" +
            "         \"diescription\":\"登录密码\",\n" +
            "         \"paramName\":\"adminPassword\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"1\",\n" +
            "         \"baseObjId\":41\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"默启应用\",\n" +
            "         \"paramName\":\"defaultLaunchApp\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":42\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"默启应用开关\",\n" +
            "         \"paramName\":\"defaultLaunchAppisSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":43\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"app白名单是否全选开关\",\n" +
            "         \"paramName\":\"appWhiteIsAllSelected\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":44\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"Wifi白名单开关\",\n" +
            "         \"paramName\":\"wifiWhiteIsSwitch\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":45\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否为第一次启动\",\n" +
            "         \"paramName\":\"isFirstLaunch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":46\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否启动了透明页面\",\n" +
            "         \"paramName\":\"isStartTransparent\",\n" +
            "         \"paramVal\":\"1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":47\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具开关\",\n" +
            "         \"paramName\":\"quickToolSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":48\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具Wlan的开关\",\n" +
            "         \"paramName\":\"wlanSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":49\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具蓝牙的开关\",\n" +
            "         \"paramName\":\"bluetoothSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":50\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具移动网络的开关\",\n" +
            "         \"paramName\":\"mobileNetworkSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":51\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具显示设置的开关\",\n" +
            "         \"paramName\":\"displaySettingSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":52\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具声音设置的开关\",\n" +
            "         \"paramName\":\"soundSettingSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":53\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"快捷设置工具显示位置设置的选择\",\n" +
            "         \"paramName\":\"entrySettingSwitch\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":54\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"应用白名单开关\",\n" +
            "         \"paramName\":\"appWhiteIsSwitch\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":55\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"log开关\",\n" +
            "         \"paramName\":\"logIsSwitch\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":56\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"默启应用地址\",\n" +
            "         \"paramName\":\"defaultLaunchAppActivity\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":57\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"图标大小\",\n" +
            "         \"paramName\":\"iconSize\",\n" +
            "         \"paramVal\":\"3\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":58\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"壁纸路径\",\n" +
            "         \"paramName\":\"paperPath\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":59\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"允许保存的最大日志大小\",\n" +
            "         \"paramName\":\"logCapacity\",\n" +
            "         \"paramVal\":\"10\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"10\",\n" +
            "         \"baseObjId\":60\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用导航栏\",\n" +
            "         \"paramName\":\"navigationBar\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":61\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否允许应用市场/ums安装\",\n" +
            "         \"paramName\":\"umsInstall\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":62\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用导航栏左键\",\n" +
            "         \"paramName\":\"backButton\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":63\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用导航栏中键\",\n" +
            "         \"paramName\":\"homeButton\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":64\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用导航栏右键\",\n" +
            "         \"paramName\":\"menuButton\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":65\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用状态栏\",\n" +
            "         \"paramName\":\"statusBar\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":66\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用移动网络\",\n" +
            "         \"paramName\":\"mobileData\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":67\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用WIFI\",\n" +
            "         \"paramName\":\"wifiData\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":68\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用蓝牙\",\n" +
            "         \"paramName\":\"bluetoothData\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":69\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用GPS\",\n" +
            "         \"paramName\":\"GPSStatus\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":70\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用USB\",\n" +
            "         \"paramName\":\"USBStatus\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":71\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否禁用ADB\",\n" +
            "         \"paramName\":\"ADBStatus\",\n" +
            "         \"paramVal\":\"-1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"-1\",\n" +
            "         \"baseObjId\":72\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否自动电话录音\",\n" +
            "         \"paramName\":\"CallRecordStatus\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":73\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"录音文件最大容量\",\n" +
            "         \"paramName\":\"CallRecordFileCapacity\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":74\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"标题类型\",\n" +
            "         \"paramName\":\"titleType\",\n" +
            "         \"paramVal\":\"1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"1\",\n" +
            "         \"baseObjId\":75\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"自定义Launcher标题\",\n" +
            "         \"paramName\":\"customizeTitle\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"UROVO-企业模式\",\n" +
            "         \"baseObjId\":76\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"启用企业模式\",\n" +
            "         \"paramName\":\"isStart\",\n" +
            "         \"paramVal\":\"1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":77\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"是否启用向导\",\n" +
            "         \"paramName\":\"isGuideStart\",\n" +
            "         \"paramVal\":\"1\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":78\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"访客桌面文件夹管理\",\n" +
            "         \"paramName\":\"folderManagement\",\n" +
            "         \"paramVal\":\"0\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"0\",\n" +
            "         \"baseObjId\":79\n" +
            "      },\n" +
            "      {\n" +
            "         \"diescription\":\"访客桌面文件夹路径\",\n" +
            "         \"paramName\":\"folderPath1\",\n" +
            "         \"paramVal\":\"\",\n" +
            "         \"paramValRegex\":\"\",\n" +
            "         \"testParamVal\":\"\",\n" +
            "         \"baseObjId\":80\n" +
            "      }\n" +
            "   ],\n" +
            "   \"webShortcutAppsTableList\":[\n" +
            "\n" +
            "   ],\n" +
            "   \"whiteListAppsTableList\":[\n" +
            "\n" +
            "   ],\n" +
            "   \"whiteListWifiTableList\":[\n" +
            "\n" +
            "   ]\n" +
            "}\n" +
            "\n" +
            "Notes:\n" +
            "\"diescription\":\"登录密码\", // The Admin Password. If \"\", then means no password\n" +
            "\"diescription\":\"默启应用\", // The App that will auto-start after bootup (Only One)\n" +
            "\"diescription\":\"默启应用开关\", // Decide if the App auto-start is enabled or not\n" +
            "\"diescription\":\"app白名单是否全选开关\", // \"0\" means not Select All Apps; \"1\" means All Apps are selected as WhiteList Apps\n" +
            "\"diescription\":\"Wifi白名单开关\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear the list set by other APPs)\n" +
            "\"diescription\":\"快捷设置工具开关\", // \"0\" means QuickTool Disabled; \"1\" means QuickTool Enabled.\n" +
            "\"diescription\":\"快捷设置工具Wlan的开关\", // \"0\" means Disabled; \"1\" means Enabled. Will be displayed as a Icon or a Tab\n" +
            "\"diescription\":\"快捷设置工具蓝牙的开关\", // \"0\" means Disabled; \"1\" means Enabled. Will be displayed as a Icon or a Tab\n" +
            "\"diescription\":\"快捷设置工具移动网络的开关\", // \"0\" means Disabled; \"1\" means Enabled. Will be displayed as a Icon or a Tab\n" +
            "\"diescription\":\"快捷设置工具显示设置的开关\", // \"0\" means Disabled; \"1\" means Enabled. Will be displayed as a Icon or a Tab\n" +
            "\"diescription\":\"快捷设置工具声音设置的开关\", // \"0\" means Disabled; \"1\" means Enabled. Will be displayed as a Icon or a Tab\n" +
            "\"diescription\":\"快捷设置工具显示位置设置的选择\", // \"0\" means QuickTool listed as Icons; \"1\" means QuickTool listed as Tabs of Settings\n" +
            "\"diescription\":\"应用白名单开关\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear the list set by other APPs)\n" +
            "\"diescription\":\"图标大小\", // From 2 - 5\n" +
            "\"diescription\":\"是否允许应用市场/ums安装\", // \"0\" means OFF; \"1\" means On. If on, App downloaded from UMS will be automatically displayed to visitor.\n" +
            "\"diescription\":\"是否禁用导航栏左键\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用导航栏中键\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用导航栏右键\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用状态栏\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用移动网络\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用WIFI\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用蓝牙\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用GPS\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用USB\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"是否禁用ADB\", // \"-1\" means None; \"0\" means OFF; \"1\" means On (OFF will clear Settings by other APPs)\n" +
            "\"diescription\":\"标题类型\", // \"1\": Customize; \"2\": Model; \"3\": Serial Number\n" +
            "\"diescription\":\"自定义Launcher标题\", // Custom Title. Will take effect only titleType is \"1\"\n" +
            "\"diescription\":\"访客桌面文件夹管理\", // To decide if fold display is enabled or not\n" +
            "\"diescription\":\"访客桌面文件夹路径\", // The folder that will be displayed to the Visitor. Only videos will be shown\n" +
            "\"webShortcutAppsTableList\":[\n" +
            "// For shortcut icon (based on Chrome)\n" +
            "],\n" +
            "\"whiteListAppsTableList\":[\n" +
            "// Visitor Launcher Apps: The Apps that will be displayed for the visitor. (This is the same list of AppWhiteList)\n" +
            "// Must note: if turn on AppWhitelist, all the Apps outside of whitelist will be uninstalled.\n" +
            "],\n" +
            "\"whiteListWifiTableList\":[\n" +
            "// The WiFi WhiteList\n" +
            "]"
}



/*

Important ones:
 - whiteListAppsTableList: The APPs displayed to the visitors
 - webShortCut
 - Admin Password
 - QuickToo for settings
 - Kiosk related: disable three keys + disable status bar

Default Config:
{
   "keepAliveAppsTableList":[

   ],
   "settingTableList":[
      {
         "diescription":"登录密码",
         "paramName":"adminPassword",
         "paramVal":"",
         "paramValRegex":"",
         "testParamVal":"1",
         "baseObjId":41
      },
      {
         "diescription":"默启应用",
         "paramName":"defaultLaunchApp",
         "paramVal":"",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":42
      },
      {
         "diescription":"默启应用开关",
         "paramName":"defaultLaunchAppisSwitch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":43
      },
      {
         "diescription":"app白名单是否全选开关",
         "paramName":"appWhiteIsAllSelected",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":44
      },
      {
         "diescription":"Wifi白名单开关",
         "paramName":"wifiWhiteIsSwitch",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":45
      },
      {
         "diescription":"是否为第一次启动",
         "paramName":"isFirstLaunch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":46
      },
      {
         "diescription":"是否启动了透明页面",
         "paramName":"isStartTransparent",
         "paramVal":"1",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":47
      },
      {
         "diescription":"快捷设置工具开关",
         "paramName":"quickToolSwitch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":48
      },
      {
         "diescription":"快捷设置工具Wlan的开关",
         "paramName":"wlanSwitch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":49
      },
      {
         "diescription":"快捷设置工具蓝牙的开关",
         "paramName":"bluetoothSwitch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":50
      },
      {
         "diescription":"快捷设置工具移动网络的开关",
         "paramName":"mobileNetworkSwitch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":51
      },
      {
         "diescription":"快捷设置工具显示设置的开关",
         "paramName":"displaySettingSwitch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":52
      },
      {
         "diescription":"快捷设置工具声音设置的开关",
         "paramName":"soundSettingSwitch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":53
      },
      {
         "diescription":"快捷设置工具显示位置设置的选择",
         "paramName":"entrySettingSwitch",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":54
      },
      {
         "diescription":"应用白名单开关",
         "paramName":"appWhiteIsSwitch",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":55
      },
      {
         "diescription":"log开关",
         "paramName":"logIsSwitch",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":56
      },
      {
         "diescription":"默启应用地址",
         "paramName":"defaultLaunchAppActivity",
         "paramVal":"",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":57
      },
      {
         "diescription":"图标大小",
         "paramName":"iconSize",
         "paramVal":"3",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":58
      },
      {
         "diescription":"壁纸路径",
         "paramName":"paperPath",
         "paramVal":"",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":59
      },
      {
         "diescription":"允许保存的最大日志大小",
         "paramName":"logCapacity",
         "paramVal":"10",
         "paramValRegex":"",
         "testParamVal":"10",
         "baseObjId":60
      },
      {
         "diescription":"是否禁用导航栏",
         "paramName":"navigationBar",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":61
      },
      {
         "diescription":"是否允许应用市场/ums安装",
         "paramName":"umsInstall",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"0",
         "baseObjId":62
      },
      {
         "diescription":"是否禁用导航栏左键",
         "paramName":"backButton",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":63
      },
      {
         "diescription":"是否禁用导航栏中键",
         "paramName":"homeButton",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":64
      },
      {
         "diescription":"是否禁用导航栏右键",
         "paramName":"menuButton",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":65
      },
      {
         "diescription":"是否禁用状态栏",
         "paramName":"statusBar",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":66
      },
      {
         "diescription":"是否禁用移动网络",
         "paramName":"mobileData",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":67
      },
      {
         "diescription":"是否禁用WIFI",
         "paramName":"wifiData",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":68
      },
      {
         "diescription":"是否禁用蓝牙",
         "paramName":"bluetoothData",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":69
      },
      {
         "diescription":"是否禁用GPS",
         "paramName":"GPSStatus",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":70
      },
      {
         "diescription":"是否禁用USB",
         "paramName":"USBStatus",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":71
      },
      {
         "diescription":"是否禁用ADB",
         "paramName":"ADBStatus",
         "paramVal":"-1",
         "paramValRegex":"",
         "testParamVal":"-1",
         "baseObjId":72
      },
      {
         "diescription":"是否自动电话录音",
         "paramName":"CallRecordStatus",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"0",
         "baseObjId":73
      },
      {
         "diescription":"录音文件最大容量",
         "paramName":"CallRecordFileCapacity",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"0",
         "baseObjId":74
      },
      {
         "diescription":"标题类型",
         "paramName":"titleType",
         "paramVal":"1",
         "paramValRegex":"",
         "testParamVal":"1",
         "baseObjId":75
      },
      {
         "diescription":"自定义Launcher标题",
         "paramName":"customizeTitle",
         "paramVal":"",
         "paramValRegex":"",
         "testParamVal":"UROVO-企业模式",
         "baseObjId":76
      },
      {
         "diescription":"启用企业模式",
         "paramName":"isStart",
         "paramVal":"1",
         "paramValRegex":"",
         "testParamVal":"0",
         "baseObjId":77
      },
      {
         "diescription":"是否启用向导",
         "paramName":"isGuideStart",
         "paramVal":"1",
         "paramValRegex":"",
         "testParamVal":"0",
         "baseObjId":78
      },
      {
         "diescription":"访客桌面文件夹管理",
         "paramName":"folderManagement",
         "paramVal":"0",
         "paramValRegex":"",
         "testParamVal":"0",
         "baseObjId":79
      },
      {
         "diescription":"访客桌面文件夹路径",
         "paramName":"folderPath1",
         "paramVal":"",
         "paramValRegex":"",
         "testParamVal":"",
         "baseObjId":80
      }
   ],
   "webShortcutAppsTableList":[

   ],
   "whiteListAppsTableList":[

   ],
   "whiteListWifiTableList":[

   ]
}

Notes:
"diescription":"登录密码", // The Admin Password. If "", then means no password
"diescription":"默启应用", // The App that will auto-start after bootup (Only One)
"diescription":"默启应用开关", // Decide if the App auto-start is enabled or not
"diescription":"app白名单是否全选开关", // "0" means not Select All Apps; "1" means All Apps are selected as WhiteList Apps
"diescription":"Wifi白名单开关", // "-1" means None; "0" means OFF; "1" means On (OFF will clear the list set by other APPs)
"diescription":"快捷设置工具开关", // "0" means QuickTool Disabled; "1" means QuickTool Enabled.
"diescription":"快捷设置工具Wlan的开关", // "0" means Disabled; "1" means Enabled. Will be displayed as a Icon or a Tab
"diescription":"快捷设置工具蓝牙的开关", // "0" means Disabled; "1" means Enabled. Will be displayed as a Icon or a Tab
"diescription":"快捷设置工具移动网络的开关", // "0" means Disabled; "1" means Enabled. Will be displayed as a Icon or a Tab
"diescription":"快捷设置工具显示设置的开关", // "0" means Disabled; "1" means Enabled. Will be displayed as a Icon or a Tab
"diescription":"快捷设置工具声音设置的开关", // "0" means Disabled; "1" means Enabled. Will be displayed as a Icon or a Tab
"diescription":"快捷设置工具显示位置设置的选择", // "0" means QuickTool listed as Icons; "1" means QuickTool listed as Tabs of Settings
"diescription":"应用白名单开关", // "-1" means None; "0" means OFF; "1" means On (OFF will clear the list set by other APPs)
"diescription":"图标大小", // From 2 - 5
"diescription":"是否允许应用市场/ums安装", // "0" means OFF; "1" means On. If on, App downloaded from UMS will be automatically displayed to visitor.
"diescription":"是否禁用导航栏左键", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用导航栏中键", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用导航栏右键", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用状态栏", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用移动网络", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用WIFI", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用蓝牙", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用GPS", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用USB", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"是否禁用ADB", // "-1" means None; "0" means OFF; "1" means On (OFF will clear Settings by other APPs)
"diescription":"标题类型", // "1": Customize; "2": Model; "3": Serial Number
"diescription":"自定义Launcher标题", // Custom Title. Will take effect only titleType is "1"
"diescription":"访客桌面文件夹管理", // To decide if fold display is enabled or not
"diescription":"访客桌面文件夹路径", // The folder that will be displayed to the Visitor. Only videos will be shown
"webShortcutAppsTableList":[
// For shortcut icon (based on Chrome)
],
"whiteListAppsTableList":[
// Visitor Launcher Apps: The Apps that will be displayed for the visitor. (This is the same list of AppWhiteList)
// Must note: if turn on AppWhitelist, all the Apps outside of whitelist will be uninstalled.
],
"whiteListWifiTableList":[
// The WiFi WhiteList
]
 */