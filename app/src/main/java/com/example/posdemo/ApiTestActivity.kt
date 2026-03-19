package com.example.posdemo

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothClass
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.device.DeviceManager
import android.device.IccManager
import android.device.SEManager
import android.device.UFSManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.posdemo.databinding.ActivityApiTestBinding
import com.example.posdemo.others.SimCard
import com.example.posdemo.utils.ImageUtil
import com.example.posdemo.utils.PermissionUtil
import com.example.posdemo.utils.UStageCrypto
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.urovo.utils.BytesUtil
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("DEPRECATION")
class ApiTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ApiTestActivity_TAG"
    }

    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            // 1. 获取包名（核心！）
            val packageName = intent.data?.schemeSpecificPart ?: return

            when (intent.action) {
                // ✅ 安装失败（你要的核心监听）
                Intent.ACTION_INSTALL_FAILURE -> {
                    // 失败原因（可选）
                    println("安装失败 → 包名：$packageName")
                    Log.e(TAG, "onReceive: $packageName", )
                    // 这里可以做你的业务逻辑：上报、弹窗、记录日志
                }

                // 安装成功
                Intent.ACTION_PACKAGE_ADDED -> {
                    println("安装成功 → 包名：$packageName")
                }

                // 卸载成功
                Intent.ACTION_PACKAGE_REMOVED -> {
                    println("卸载成功 → 包名：$packageName")
                }
            }
        }
    }

    private fun registerInstallReceiver() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_INSTALL_FAILURE) // 安装失败
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)          // 安装成功
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)        // 卸载成功
        filter.addDataScheme("package") // 必须加！否则收不到包相关广播
        registerReceiver(installReceiver, filter)
    }

    private lateinit var binding: ActivityApiTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTest1.setOnClickListener @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) { onTest1ButtonClicked() }
        binding.btnTest2.setOnClickListener { onTest2ButtonClicked() }
        binding.btnTest3.setOnClickListener { onTest3ButtonClicked() }
        binding.btnTest4.setOnClickListener { onTest4ButtonClicked() }
        binding.btnTest5.setOnClickListener { onTest5ButtonClicked() }

        binding.etTest1.doOnTextChanged { text, start, before, count ->

            if (text!!.endsWith("\n")) {
                Log.d("SCAN", "LF detected")
            }

            if (text.endsWith("\r")) {
                Log.d("SCAN", "CR detected")
            }

            if (text.endsWith("\t")) {
                Log.d("SCAN", "TAB detected")
            }
        }


        binding.etTest1.setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_ENTER -> {
                            Log.d("INPUT", "Enter key pressed (KeyEvent)")
                            true
                        }
                        KeyEvent.KEYCODE_DEL -> {
                            Log.d("INPUT", "Delete key pressed")
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }

        binding.etTest1.setOnEditorActionListener { v, actionId, event ->

            Log.e(TAG, "onCreate: 123", )
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    Log.d("INPUT", "IME Done pressed")
                    true
                }
                EditorInfo.IME_ACTION_SEARCH -> {
                    Log.d("INPUT", "IME Search pressed")
                    true
                }
                EditorInfo.IME_ACTION_NEXT -> {
                    Log.d("INPUT", "IME Next pressed")
                    true
                }
                else -> false
            }
        }

    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.e(TAG, "dispatchKeyEvent: ${event.keyCode}", )
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            btnTest1.visibility = VISIBLE
            btnTest2.visibility = VISIBLE
            btnTest3.visibility = VISIBLE
            btnTest4.visibility = VISIBLE
            btnTest5.visibility = VISIBLE
        }
    }


    private fun onTest1ButtonClicked() {
        Log.e(TAG, "onTest1ButtonClicked")
        fun printWithCPCL(printerIp: String = "192.168.1.100", printerPort: Int = 9100) {
            // 1. 配置打印参数
            val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val russianText = "Станова про накладання адміністративного стягнення" // 俄语文本
            val charset = Charset.forName("Windows-1251") // 匹配WPC1251编码

            // 2. 构建CPCL指令（核心：指定编码+字体编号）
            val cpclCommand = """
SIZE 50 mm, 30 mm
GAP 0 mm, 0 mm
CLS
TEXT 50, 120, "1", 0, 1, 1, "123 中文测试" 
PRINT 1
        
    """.trimIndent()

            // 3. 发送指令到打印机
            var socket: Socket? = null
            var os: OutputStream? = null
            try {
                socket = Socket(printerIp, printerPort)
                os = socket.getOutputStream()

                // 关键：将CPCL指令转为WPC1251编码的字节流（匹配打印机解码方式）
                val cpclBytes = cpclCommand.toByteArray(Charsets.UTF_8)
                os.write(cpclBytes)
                os.flush()

                println("CPCL指令发送成功！")
            } catch (e: Exception) {
                println("打印失败：${e.message}")
                e.printStackTrace()
            } finally {
                // 4. 关闭资源
                os?.close()
                socket?.close()
            }
        }

        Thread {
            printWithCPCL(printerIp = "10.10.10.235")
        }.start()

    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getLocationOnce() {
        if (!PermissionUtil.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION), 1001)) {
            Toast.makeText(this, "Please grant permission first", Toast.LENGTH_SHORT).show()
            return
        }

        val availability =
            com.google.android.gms.common.GoogleApiAvailability
                .getInstance()
                .isGooglePlayServicesAvailable(this)

        Log.e(TAG, "GMS availability = $availability")
        val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager

        Log.e(TAG, "GPS enabled = ${lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)}")
        Log.e(TAG, "Network enabled = ${lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)}")
        Log.e(TAG, "Passive enabled = ${lm.isProviderEnabled(android.location.LocationManager.PASSIVE_PROVIDER)}")


        val client = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 500L           // 0.5 秒
            fastestInterval = 200L    // 200 ms
            numUpdates = 10
        }

        client.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    Log.e(TAG, "onLocationResult: ", )
                    val loc = result.lastLocation
                    if (loc != null) {
                        Log.e(TAG, "onLocationResult: Lat=${loc.latitude}, Lng=${loc.longitude}", )
                        runOnUiThread {
                            binding.tvResult.text = "Lat=${loc.latitude}, Lng=${loc.longitude}"
                        }
                    }
                    client.removeLocationUpdates(this)
                }
            },
            mainLooper
        )
    }

    private fun onTest2ButtonClicked() {
        Log.e(TAG, "onTest2ButtonClicked")

        // 辅助函数：Hex字符串转字节数组（核心工具）
        fun hexStringToByteArray(hexStr: String): ByteArray {
            require(hexStr.length % 2 == 0) { "Hex字符串长度必须是偶数" }
            val byteArray = ByteArray(hexStr.length / 2)
            for (i in byteArray.indices) {
                val start = i * 2
                val hexByte = hexStr.substring(start, start + 2)
                byteArray[i] = hexByte.toInt(16).toByte()
            }
            return byteArray
        }


        fun main() {
            // 你的原始Hex字符串
            val hexStr =
                "D09FD0BED181D182D0B0D0BDD0BED0B2D0B020D0BFD180D0BE20D0BDD0B0D0BAD0BBD0B0D0B4D0B0D0BDD0BDD18F20D0B0D0B4D0BCD196D0BDD196D181D182D180D0B0D182D0B8D0B2D0BDD0BED0B3D0BE20D181D182D18FD0B3D0BDD0B5D0BDD0BDD18F20D0BFD0BE20D181D0BFD180D0B0D0B2D19620D0BFD180D0BE20D0B0D0B4D0BCD196D0BDD196D181D182D180D0B0D182D0B8D0B2D0BDD0B520D0BFD180D0B0D0B2D0BED0BFD0BED180D183D188D0B5D0BDD0BDD18F20D18320D181D184D0B5D180D19620D0B7D0B0D0B1D0B5D0B7D0BFD0B5D187D0B5D0BDD0BDD18F20D0B1D0B5D0B7D0BFD0B5D0BAD0B820D0B4D0BED180D0BED0B6D0BDD18CD0BED0B3D0BE20D180D183D185D18320D0B7D0B0D184D196D0BAD181D0BED0B2D0B0D0BDD0B520D0BDD0B520D0B220D0B0D0B2D182D0BED0BCD0B0D182D0B8D187D0BDD0BED0BCD18320D180D0B5D0B6D0B8D0BCD196200AE4B8ADE69687E6B58BE8AF95202020202020"

            // 1. Hex字符串转字节数组
            val bytes = hexStringToByteArray(hexStr)

            // 2. 用GB18030解码字节数组
            val result = String(bytes, Charset.forName("GB18030"))

            // 3. 输出解析结果
            println("GB18030解析结果：\n$result")
        }

        main()

    }

    private fun onTest3ButtonClicked() {
        Log.e(TAG, "onTest3ButtonClicked")


        fun encryptUBrowserPassword(plain: String): String {
            val keyInHex = "6836686e6269646e736d7a7776776d77"
            val iv = ByteArray(16)
            val key = SecretKeySpec(BytesUtil.hexString2Bytes(keyInHex), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(ct, Base64.NO_WRAP).replace("=", "")
        }

        fun decryptUBrowserPassword(b64NoPad: String): String {
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
        Log.e(TAG, "onTest3ButtonClicked: ${encryptUBrowserPassword("111111")}", )
    }

    private fun onTest4ButtonClicked() {
        Log.e(TAG, "onTest4ButtonClicked")




    }

    private fun onTest5ButtonClicked() {
        Log.e(TAG, "onTest5ButtonClicked")
        DeviceManager().setSettingProperty("persist-persist.sys.urv.all.settings.password", "")

//        runCatching {
//            val clazz = Class.forName("android.device.DeviceManager")
//            val method = clazz.getMethod("setPreferCall", Int::class.java)
//            method.invoke(clazz.newInstance(), SimCard.SIM_1.index) as Int
//        }.onSuccess {
//            Toast.makeText(this, "Switch Call to Sim 1 successfully", Toast.LENGTH_SHORT).show()
//        }.onFailure {
//            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
//            it.printStackTrace()
//        }

        //3082032230820284A00302010202073D7E3FFD138B8C300A06082A8648CE3D0403043032310C300A0603550406130343484E310E300C060355040A130555726F766F311230100603550403130943414B444820454343301E170D3234313130363037333830385A170D3239313130353037333830385A304E3110300E06035504030C07524B49204B4448311D301B060355040B0C1447656E6572616C2055726F766F20726F6F744341310E300C060355040A0C0555726F766F310B300906035504061302434E30819B301006072A8648CE3D020106052B81040023038186000400459CB10A8507FB155228A81DC745D35D5ED3E78A3CB69EB441557F87E12D2617CF241343E9D4C42E460F207E53B62573C30427D9C54E9AFA7DFFD9DF64E0B3D98300979B4EF77B9FD6FF735D02CA1279BF0B586AAA713BB96D57D47E9DD0E2E71C0C1D3452C6865B5B117019D88F915D981656F4653A8A9440579A1C0EC9138AE32A49A38201243082012030600603551D2304593057801448461AB836718EE2FBC3FA509B81099EE4FFA733A136A4343032310C300A0603550406130343484E310E300C060355040A130555726F766F3112301006035504031309524B4943412045434382073D7E3F965EAFDA301D0603551D0E0416041421B6F5D9A2A6612B669D25BF04CE7320699FDAC230120603551D130101FF040830060101FF020100300E0603551D0F0101FF0404030205A030160603551D250101FF040C300A06082B06010505070301302E0603551D1F042730253023A021A01F861D68747470733A2F2F63726C2E746573742E636F6D2F746573742E63726C303106082B0601050507010104253023302106082B06010505073001861568747470733A2F2F6F6373702E746573742E636F6D300A06082A8648CE3D04030403818B0030818702413CFF22D951C07F40A1A2EB3E298ECE1E632AEB30BEDCA6A05FC1B9808059CA2EE1D848D15D4351CB535FD6C81BBF56114433F715F17E7E2D585452203AD4D3B0DA024201725FC1D02DED8BD62C7FBF8A89C48158A5CF5040B63F4F1546B0290A0E0B787C5BFF13697B28404D6EA20D77D48AEF4FDE96719AD9E76B5D0824AD1853227B630E

        "Download KMS_CA_Cert & PED_Cert:\n" +
                " - Generate KeyPair (RSA / ECC)\n" +
                " - Generate CSR(SN + PublicKey + Self-Signature)\n" +
                " - Retrieve KMS_CA_Cert & PED_Cert(instance issued) from KMS server\n" +
                "\n" +
                "Download KEY(KMS_IP + KMS_Port):\n" +
                "(1). mTLS:\n" +
                " - ClientHello(randonNumber1) - ServerHello(randonNumber2)\n" +
                " - ClientUploadCert(PED_Cert) - ServerUploadCert(KDH_Cert recovered using KMS_CA_Cert)\n" +
                " - ClientKeyExchange(signed_transcript) - ServerKeyExchange(signed_transcript) [Mutual Prove]\n" +
                " - FIN [Make sure same sharedKey - KBPK]\n" +
                "(2). Key Download:\n" +
                " - Client checks PED_Cert & KMS_CA_Cert exist or not.\n" +
                " - Server uses the KBPK to encapsulate the KEY into TR31[Header+Encrypted_KEY+MAC].\n" +
                " - Server signs the TR31 using it's own KDH private key\n" +
                " - Transmit in HTTP\n" +
                " - Client verify the signature using KDH's Cert. Make sure it's from KDH and TR31 not modified\n" +
                " - Recover the Actual Key using the KBPK"

        Log.e(TAG, "onTest5ButtonClicked: ${BytesUtil.bytes2HexString(DeviceManager().readKMSCA())}", )
        Log.e(TAG, "onTest5ButtonClicked: ${BytesUtil.bytes2HexString(DeviceManager().kdhCrt)}", )
        Log.e(TAG, "onTest5ButtonClicked: ${BytesUtil.bytes2HexString(DeviceManager().pedCrt)}", )

//        val clazz = Class.forName("android.device.UFSManager")
//        val method = clazz.getMethod("setWallpaper", Bitmap::class.java, Int::class.java)
//        method.invoke(clazz.newInstance(), ImageUtil.pngToBitmap(resources, R.drawable.wallpaper), 2)
//

    }


}