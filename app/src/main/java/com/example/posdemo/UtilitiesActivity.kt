package com.example.posdemo

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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.posdemo.databinding.ActivityUtilitiesBinding
import com.example.posdemo.fragments.utilities.BeeperFragment
import com.example.posdemo.fragments.utilities.CameraScanFragment
import com.example.posdemo.fragments.utilities.LedFragment
import com.example.posdemo.utils.PermissionUtil
import com.google.android.material.tabs.TabLayoutMediator
import com.urovo.sdk.beeper.BeeperImpl
import com.urovo.sdk.led.LEDDriverImpl
import com.urovo.sdk.scanner.InnerScannerImpl

class UtilitiesActivity : AppCompatActivity() {

    companion object {
        private const val LEFT_SCAN_KEYCODE = 521
        private const val RIGHT_SCAN_KEYCODE = 520
        private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val PERMISSION_REQ_SCAN = 1001
    }

    private lateinit var binding: ActivityUtilitiesBinding
    val mBeeperManager = BeeperImpl.getInstance()
    val mLedManager = LEDDriverImpl.getInstance()
    val mCameraManager by lazy { InnerScannerImpl.getInstance(this) }
    private val mScanManager = ScanManager()

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
                runOnUiThread {
                    Toast.makeText(this@UtilitiesActivity, text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUtilitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPagerUtilities.adapter = UtilitiesPagerAdapter(this)
        binding.viewPagerUtilities.offscreenPageLimit = 2
        TabLayoutMediator(binding.tabLayoutUtilities, binding.viewPagerUtilities) { tab, position ->
            tab.text = when (position) {
                0 -> "Beeper"
                1 -> "LED"
                2 -> "CameraScan"
                else -> ""
            }
        }.attach()

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

class UtilitiesPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BeeperFragment()
            1 -> LedFragment()
            2 -> CameraScanFragment()
            else -> BeeperFragment()
        }
    }


}