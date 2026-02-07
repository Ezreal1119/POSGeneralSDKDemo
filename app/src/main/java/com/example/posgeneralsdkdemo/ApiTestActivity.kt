package com.example.posgeneralsdkdemo

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.device.DeviceManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.databinding.ActivityApiTestBinding
import com.example.posgeneralsdkdemo.others.PACKAGE_COMPONENT_INFO
import com.example.posgeneralsdkdemo.others.PACKAGE_COMPONENT_MAIN
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import java.io.File
import java.io.FileOutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.security.Permission
import java.util.Locale

private const val TAG = "Patrick"
class ApiTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiTestBinding
    lateinit var tts: TextToSpeech


    @SuppressLint("MissingPermission", "ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.e(TAG, "onCreate: $packageName", )

        binding.btnTest1.setOnClickListener {
            Log.e(TAG, "onCreate: btnTest1")


            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts.language = Locale.ITALIAN
                    tts.setSpeechRate(1F)
                    tts.setPitch(1F)
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onDone(utteranceId: String?) {
                            Log.e(TAG, "onDone: ", )
                        }

                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "onError: ", )
                        }

                        override fun onStart(utteranceId: String?) {
                            Log.e(TAG, "onStart: ", )
                        }
                    })

                    tts.speak(
                        "Ciao, sono Patrick. Ciao Ciao?",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "speechId1"
                    )
                }
            }


        }
        binding.btnTest2.setOnClickListener {
            Log.e(TAG, "btnTest2")
            Log.e(TAG, "onCreate: ${DeviceManager().getSettingProperty("Global-ntp_server")}", )
            Log.e(TAG, "onCreate: ${DeviceManager().getSettingProperty("persist-persist.sys.timezone")}", )
            Log.e(TAG, "onCreate: ${DeviceManager().getSettingProperty("persist-persist.sys.settimezone")}", )

        }
        binding.btnTest3.setOnClickListener {
            Log.e(TAG, "btnTest3")
            DeviceManager().setAllowInstallApps("com.example.abd", 0, 2)
        }
        binding.btnTest4.setOnClickListener {
        }

        DeviceManager().setDeviceOwner(ComponentName.unflattenFromString("${packageName}/${MainActivity::class.java.name}"))
        runCatching {
            Log.e(TAG, "onCreate: ${DeviceManager().deviceOwner}", )

        }.onFailure {
            it.printStackTrace()
        }
        DeviceManager().setDeviceOwner(ComponentName.unflattenFromString(PACKAGE_COMPONENT_MAIN))

    }

    fun printText(ip: String, port: Int = 9100, text: String) {
        Thread {

            var socket: Socket? = null
            try {
                socket = Socket(ip, port)
                socket.soTimeout = 5000

                val out = socket.getOutputStream()


                out.write(byteArrayOf(0x1B, 0x40)) // Initialize
                out.write(text.toByteArray(Charsets.UTF_8))
                out.write(byteArrayOf(0x0A)) // feed line


                out.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }.start()
    }




    fun initTtsAndSpeak(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {

                // 设置西班牙语（西班牙）
                val result = tts.setLanguage(Locale("es", "ES"))

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.e("TTS", "Spanish language not supported or missing data")
                    return@TextToSpeech
                }

                // 可选：语速 & 音调
                tts.setSpeechRate(1.0f)   // 1.0 = 正常
                tts.setPitch(1.0f)        // 1.0 = 正常

                // 监听播报状态（很重要，POS 常用）
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        Log.d("TTS", "Speech started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String) {
                        Log.d("TTS", "Speech done: $utteranceId")
                    }

                    override fun onError(utteranceId: String) {
                        Log.e("TTS", "Speech error: $utteranceId")
                    }
                })

                // 西班牙语内容（一定要写西语）
                val spanishText = "Hola, este es Patrick hablando. ¿Cómo estás?"

                tts.speak(
                    spanishText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "spanish_speech_001"
                )
            } else {
                Log.e("TTS", "TTS init failed")
            }
        }
    }
}