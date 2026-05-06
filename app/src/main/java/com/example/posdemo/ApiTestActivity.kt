package com.example.posdemo

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.device.DeviceManager
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.example.posdemo.databinding.ActivityApiTestBinding
import com.example.posdemo.receivers.MyDeviceAdminReceiver
import com.example.posdemo.utils.ImageUtil
import com.example.posdemo.utils.PermissionUtil
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.urovo.sdk.insertcard.InsertCardHandlerImpl
import com.urovo.sdk.print.PrintFormat
import com.urovo.sdk.print.PrinterProviderImpl
import com.urovo.utils.BytesUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("DEPRECATION")
class ApiTestActivity : AppCompatActivity() {

    private var server: SimpleServer? = null

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
                    Log.e(TAG, "onReceive: $packageName")
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

        binding.btnTest1.setOnClickListener { onTest1ButtonClicked() }
        binding.btnTest2.setOnClickListener { onTest2ButtonClicked() }
        binding.btnTest3.setOnClickListener { onTest3ButtonClicked() }
        binding.btnTest4.setOnClickListener { onTest4ButtonClicked() }
        binding.btnTest5.setOnClickListener { onTest5ButtonClicked() }

//
//        server = SimpleServer(8080) {
//            // 👇 收到请求就弹 Toast
//            runOnUiThread {
//                Toast.makeText(this, "received order", Toast.LENGTH_SHORT).show()
//            }
//        }
//        server?.start()

    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.e(TAG, "dispatchKeyEvent: ${event.keyCode}")
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            btnTest1.visibility = VISIBLE
            btnTest2.visibility = VISIBLE
            btnTest3.visibility = GONE
            btnTest4.visibility = VISIBLE
            btnTest5.visibility = VISIBLE
//            btnTest2.visibility = VISIBLE
//            btnTest3.visibility = VISIBLE
//            btnTest4.visibility = VISIBLE
//            btnTest5.visibility = VISIBLE
        }
    }


    private fun onTest1ButtonClicked() {

        Log.e(TAG, "onTest1ButtonClicked")
        val mPrinterManager = PrinterProviderImpl.getInstance(this)
        mPrinterManager.initPrint()
        Thread {
            runCatching {
                // 1.
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintImageButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.setGray(1)
                val textBitmap = ImageUtil.textToBitmap(
                    lines = listOf("                     ₦  50.0                     "), // Testing special symbol
                    textSizePx = 30, //
                    paddingPx = 0, // Offset from x = 0;
                    lineGapPx = 2
                )
                // 2. Add Logo
                var format = Bundle().apply {
                    putInt(PrintFormat.ALIGN, PrintFormat.ALIGN_CENTER)
                    putInt(PrintFormat.OFFSET, 0)
                    putInt(PrintFormat.WIDTH, 196)
                    putInt(PrintFormat.HEIGHT, 58)
                }
                mPrinterManager.addImage(format, ImageUtil.bitmapToBytes(ImageUtil.pngToBitmap(resources, R.drawable.unipay)))
                mPrinterManager.feedLine(1)

                // 3. Add texts

                var textFormat = Bundle().apply {
                    putInt(ContentFormat.FONT.value, 1)
                    putBoolean(ContentFormat.FONT_BOLD.value, false)
                    putInt(ContentFormat.ALIGN.value, 1)
                    putInt(ContentFormat.LINE_HEIGHT.value, 0)
                }

                mPrinterManager.addText(textFormat, "IKECHUKWU MARTINS IROKA")
                textFormat.putInt(PrintFormat.FONT, PrintFormat.FONT_SMALL)
                mPrinterManager.addText(textFormat, "17 CBN Estate 2 Satellite Town Lagc")
                textFormat.putInt(PrintFormat.FONT, PrintFormat.FONT_NORMAL)
                mPrinterManager.addText(textFormat, "- - - - - - - - - - - - - - - -")
                mPrinterManager.addText(textFormat, "WALLET TRANSFER")
                mPrinterManager.addText(textFormat, "- - - - - - - - - - - - - - - -")


                mPrinterManager.addTextLeft_Right(textFormat, "Terminal ID", "2CRF7441")
                mPrinterManager.addTextLeft_Right(textFormat, "Date/Time", "24-06-2024 08:49:35")
                mPrinterManager.addTextLeft_Right(textFormat, "Trade Ref", "6ef5ab2-810f-f5b6dc9fe744")
                mPrinterManager.addTextLeft_Right(textFormat, "Sender Name", "ADEWALE/ADENIKE/MAMUDU")
                mPrinterManager.addTextLeft_Right(textFormat, "Narration", "N/A")
                mPrinterManager.addText(textFormat, "- - - - - - - - - - - - - - - -")
                mPrinterManager.addText(textFormat, "AMOUNT")
                mPrinterManager.addText(textFormat, "- - - - - - - - - - - - - - - -")

                val scaledTextBitmap = ImageUtil.scaleBitmap(textBitmap)
                mPrinterManager.addBitmap(scaledTextBitmap, 0)
                val qrBitmap = ImageUtil.stringToQrBitmap("I have a dream that one day I can play basketball without considering the need of eating anti-sharpie planet, but still having the same honor of joining the esteemed League for caring sloth.", 350)
                mPrinterManager.addBitmap(qrBitmap, 10)
                val scaledLastTextBitmap = ImageUtil.scaleBitmap(ImageUtil.textToBitmap(listOf("           THANK YOU FOR SHOPPING     ", "                  PLEASE VISIT AGAIN      "), 20, 0, 2))
                mPrinterManager.addBitmap(scaledLastTextBitmap, 0)
                mPrinterManager.feedLine(1) // If you pass -1, then negative line will be fed
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
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
                    Log.e(TAG, "onLocationResult: ")
                    val loc = result.lastLocation
                    if (loc != null) {
                        Log.e(TAG, "onLocationResult: Lat=${loc.latitude}, Lng=${loc.longitude}")
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

        error("")

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
        Log.e(TAG, "onTest3ButtonClicked: ${encryptUBrowserPassword("111111")}")
    }

    private fun onTest4ButtonClicked() {
        Log.e(TAG, "onTest4ButtonClicked")

        try {

            val reader = InsertCardHandlerImpl.getInstance()

            // ⚠️ 这里是关键：expected response length
            val response = reader.exchangeApdu(1.toByte(), BytesUtil.hexString2Bytes("881304000000420000019D72213100000000000000000000000031313134313032323100000000000000000000000000000000000000000000000000000003E80101000000000003E80000"), 900)

            if (response == null) {
                throw IllegalStateException("response is NULL")
            }

            if (response.isEmpty()) {
                throw IllegalStateException("response is empty")
            }

            binding.tvResult.text = buildString {
                appendLine("SIGN RESP SIZE: ${response.size}")
                appendLine("SIGN RESP HEX = ${BytesUtil.bytes2HexString(response)}")
            }

        } catch (e: Exception) {
            binding.tvResult.text = e.message

        }
    }

    private fun onTest5ButtonClicked() {
        Log.e(TAG, "onTest5ButtonClicked")
//        DeviceManager().setSettingProperty("persist-persist.sys.urv.all.settings.password", "")

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

//        "Download KMS_CA_Cert & PED_Cert:\n" +
//                " - Generate KeyPair (RSA / ECC)\n" +
//                " - Generate CSR(SN + PublicKey + Self-Signature)\n" +
//                " - Retrieve KMS_CA_Cert & PED_Cert(instance issued) from KMS server\n" +
//                "\n" +
//                "Download KEY(KMS_IP + KMS_Port):\n" +
//                "(1). mTLS:\n" +
//                " - ClientHello(randonNumber1) - ServerHello(randonNumber2)\n" +
//                " - ClientUploadCert(PED_Cert) - ServerUploadCert(KDH_Cert recovered using KMS_CA_Cert)\n" +
//                " - ClientKeyExchange(signed_transcript) - ServerKeyExchange(signed_transcript) [Mutual Prove]\n" +
//                " - FIN [Make sure same sharedKey - KBPK]\n" +
//                "(2). Key Download:\n" +
//                " - Client checks PED_Cert & KMS_CA_Cert exist or not.\n" +
//                " - Server uses the KBPK to encapsulate the KEY into TR31[Header+Encrypted_KEY+MAC].\n" +
//                " - Server signs the TR31 using it's own KDH private key\n" +
//                " - Transmit in HTTP\n" +
//                " - Client verify the signature using KDH's Cert. Make sure it's from KDH and TR31 not modified\n" +
//                " - Recover the Actual Key using the KBPK"
//
//        Log.e(TAG, "onTest5ButtonClicked: ${BytesUtil.bytes2HexString(DeviceManager().readKMSCA())}", )
//        Log.e(TAG, "onTest5ButtonClicked: ${BytesUtil.bytes2HexString(DeviceManager().kdhCrt)}", )
//        Log.e(TAG, "onTest5ButtonClicked: ${BytesUtil.bytes2HexString(DeviceManager().pedCrt)}", )

//        val clazz = Class.forName("android.device.UFSManager")
//        val method = clazz.getMethod("setWallpaper", Bitmap::class.java, Int::class.java)
//        method.invoke(clazz.newInstance(), ImageUtil.pngToBitmap(resources, R.drawable.wallpaper), 2)
//

//        try {
//            val icc = IccManager()
//
//            val ret = icc.open(2.toByte(), 1.toByte(), 3.toByte())
//            Log.d("PSAM", "open=" + ret)
//
//            // 2️⃣ activate（拿ATR）
//            val atr = ByteArray(64)
//            val atrLen = icc.activate(atr)
//            Log.d("PSAM", "ATR LEN=" + atrLen)
//            Log.d("PSAM", "ATR=" + bytesToHex(atr, atrLen))
//
//            // 3️⃣ 发APDU（最经典测试指令）
//            val cmd = hex("881304000000420000019D72213100000000000000000000000031313134313032323100000000000000000000000000000000000000000000000000000003E80101000000000003E80000") // GET RANDOM
//
//            val resp = ByteArray(256)
//            val sw = ByteArray(2)
//
//            val respLen = icc.apduTransmit(cmd, cmd.size, resp, sw)
//
//            Log.d("PSAM", "RESP LEN=" + respLen)
//            Log.d("PSAM", "RESP=" + bytesToHex(resp, respLen))
//            Log.d("PSAM", "SW=" + bytesToHex(sw, 2))
//
//            // 4️⃣ close
//            icc.close()
//        } catch (e: java.lang.Exception) {
//            Log.e("PSAM", "ERROR", e)
//        }

        val componentStr = packageName + "/" + MyDeviceAdminReceiver::class.java.name
        val componentName = ComponentName.unflattenFromString(componentStr)
        DeviceManager().setDeviceOwner(componentName)



    }

    fun getProp(key: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val result = process.inputStream.bufferedReader().readText().trim()
            "$key = $result"
        } catch (e: Exception) {
            "$key = ERROR"
        }
    }


    // 十六进制字符串 转 字节数组
    private fun hex(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    // 字节数组 转 十六进制大写字符串
    private fun bytesToHex(b: ByteArray, len: Int): String {
        val sb = StringBuilder()
        for (i in 0 until len) {
            sb.append(String.format("%02X", b[i]))
        }
        return sb.toString()
    }


}



// ---

class SimpleServer(
    private val port: Int,
    private val onRequest: () -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        job = scope.launch {
            serverSocket = ServerSocket(port)

            while (isActive) {
                val client = serverSocket?.accept() ?: break
                launch { handle(client) }
            }
        }
    }

    fun stop() {
        job?.cancel()
        serverSocket?.close()
        scope.cancel()
    }

    private fun handle(socket: Socket) {
        socket.use {
            val reader = BufferedReader(InputStreamReader(it.getInputStream()))
            val writer = OutputStreamWriter(it.getOutputStream())

            val requestLine = reader.readLine() ?: return

            // 👇 把 header 读掉（必须）
            while (true) {
                val line = reader.readLine() ?: return
                if (line.isBlank()) break
            }

            // 👇 核心：触发 Toast
            onRequest()

            val body = "received order"

            val response = """
                HTTP/1.1 200 OK
                Content-Type: text/plain
                Content-Length: ${body.toByteArray().size}
                Connection: close
                
                $body
            """.trimIndent().replace("\n", "\r\n")

            writer.write(response)
            writer.flush()
        }
    }
}