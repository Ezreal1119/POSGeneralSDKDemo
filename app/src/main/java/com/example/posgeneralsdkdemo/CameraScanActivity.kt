package com.example.posgeneralsdkdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.device.ScanManager
import android.device.ScanManager.ACTION_DECODE
import android.device.ScanManager.BARCODE_LENGTH_TAG
import android.device.ScanManager.BARCODE_STRING_TAG
import android.device.ScanManager.BARCODE_TYPE_TAG
import android.device.ScanManager.DECODE_DATA_TAG
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.example.posgeneralsdkdemo.utils.DebugUtil
import com.example.posgeneralsdkdemo.utils.PermissionUtil
import com.urovo.sdk.scanner.InnerScannerImpl
import com.urovo.sdk.scanner.listener.ScannerListener
import com.urovo.sdk.scanner.utils.Constant
import com.urovo.sdk.utils.BytesUtil

// <uses-permission android:name="android.permission.CAMERA" />

private const val TAG = "Patrick_ScannerActivity"
private const val LEFT_SCAN_KEYCODE = 521
private const val RIGHT_SCAN_KEYCODE = 520
private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
private const val PERMISSION_REQ_SCAN = 1001

class CameraScanActivity : AppCompatActivity() {

    private val tvResult by lazy { findViewById<TextView>(R.id.tvResult) }
    private val btnFrontScan by lazy { findViewById<Button>(R.id.btnFrontScan) }
    private val btnBackScan by lazy { findViewById<Button>(R.id.btnBackScan) }

    private val mCameraManager by lazy { InnerScannerImpl.getInstance(this) }
    private val mScanManager by lazy { ScanManager() }


    private val cameraParams = Bundle().apply {
        putString(Constant.Scankey.title, "Patrick's Title")
        putString(Constant.Scankey.upPromptString, "This is a top Prompt")
        putString(Constant.Scankey.downPromptString, "This is a bottom Prompt")
    }

    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DECODE) {
                val barcodeBytes = intent.getByteArrayExtra(DECODE_DATA_TAG)
                val barcodeString = intent.getStringExtra(BARCODE_STRING_TAG)
                val barcodeLen = intent.getIntExtra(BARCODE_LENGTH_TAG, 0)
                val barcodeType = intent.getByteExtra(BARCODE_TYPE_TAG, 0)
                val text = buildString {
                    append("Intent Received:\n\n")
                    append("barcodeString: \n$barcodeString\n\n")
                    append("barcodeLen: $barcodeLen\n\n")
                    append("barcodeType: $barcodeType")
                }
                tvResult.text = text
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_scan)

        btnFrontScan.setOnClickListener { onFrontScanButtonClicked() }
        btnBackScan.setOnClickListener { onBackScanButtonClicked() }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ACTION_DECODE)
        }
        registerReceiver(receiver, filter)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_SCAN) {
            if (PermissionUtil.checkPermissions(this, PERMISSIONS)) {
                Toast.makeText(this, "Camera permission granted. Please tap again to scan.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onFrontScanButtonClicked() {
        if (!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_SCAN)) {
            return
        }
        try {
            mCameraManager.startScan(this, cameraParams, Constant.CameraID.FRONT, 30, object: ScannerListener {
                override fun onSuccess(data: String?, byteData: ByteArray?) {
                    runOnUiThread {
                        tvResult.text = buildString {
                            append("data onSuccess: \n$data")
                            append("\ndata in Bytes: \n${BytesUtil.bytes2HexString(byteData)}")
                        }
                    }
                }
                override fun onError(error: Int, message: String?) {
                    runOnUiThread {
                        tvResult.text = buildString {
                            append("Error onSuccess: \n$error")
                            append("\nmessage: \n$message")
                        }
                    }
                }
                override fun onTimeout() {
                    runOnUiThread { DebugUtil.logAndToast(this@CameraScanActivity, TAG, "onTimeout") }
                }
                override fun onCancel() {
                    runOnUiThread { DebugUtil.logAndToast(this@CameraScanActivity, TAG, "onCancel") }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    private fun onBackScanButtonClicked() {
        if (!PermissionUtil.requestPermissions(this, PERMISSIONS, PERMISSION_REQ_SCAN)) {
            return
        }
        try {
            mCameraManager.startScan(this, cameraParams, Constant.CameraID.BACK, 30, object: ScannerListener {
                override fun onSuccess(data: String?, byteData: ByteArray?) {
                    runOnUiThread {
                        tvResult.text = buildString {
                            append("data onSuccess: \n$data")
                            append("\ndata in Bytes: \n${BytesUtil.bytes2HexString(byteData)}")
                        }
                    }
                }
                override fun onError(error: Int, message: String?) {
                    runOnUiThread {
                        tvResult.text = buildString {
                            append("Error onSuccess: \n$error")
                            append("\nmessage: \n$message")
                        }
                    }
                }
                override fun onTimeout() {
                    runOnUiThread { DebugUtil.logAndToast(this@CameraScanActivity, TAG, "onTimeout") }
                }
                override fun onCancel() {
                    runOnUiThread { DebugUtil.logAndToast(this@CameraScanActivity, TAG, "onCancel") }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != LEFT_SCAN_KEYCODE && keyCode != RIGHT_SCAN_KEYCODE) {
            return super.onKeyDown(keyCode, event)
        }
        if (event != null && event.repeatCount > 0) return true

        if (!mScanManager.scannerState) {
            Toast.makeText(this, "Please turn on the scanner first", Toast.LENGTH_SHORT).show()
            return super.onKeyUp(keyCode, event)
        }
        if (mScanManager.triggerLockState) {
            Toast.makeText(this, "Please unlock the scanner first", Toast.LENGTH_SHORT).show()
            return super.onKeyUp(keyCode, event)
        }
        mScanManager.switchOutputMode(0)
        mScanManager.startDecode()
        return true
    }
}