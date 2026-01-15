package com.example.posgeneralsdkdemo

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.device.DeviceManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.posgeneralsdkdemo.utils.ImageUtil
import com.google.zxing.BarcodeFormat
import com.urovo.sdk.print.PrinterProviderImpl


private const val CONTENT =
        "       WALMART SUPERCENTER     \n" +
        " 1234 MAIN STREET, ANYTOWN, USA\n" +
        "       TEL: (555) 123-4567     \n" +
        "-------------------------------\n" +
        "QTY  ITEM         PRICE   TOTAL\n" +
        "-------------------------------\n" +
        " 1   MILK 1 GAL   $3.49   $3.49\n" +
        " 2   BREAD LOAF   $1.99   $3.98\n" +
        " 1   EGGS DOZEN   $2.79   $2.79\n" +
        " 3   APPLES       $0.99   $2.97\n" +
        "-------------------------------\n" +
        "SUBTOTAL                 $13.23\n" +
        "TAX (8.25%)               $1.09\n" +
        "-------------------------------\n" +
        "TOTAL                    $14.32\n" +
        "-------------------------------\n" +
        "CASH                     $20.00\n" +
        "CHANGE                    $5.68\n" +
        "-------------------------------\n" +
        "    THANK YOU FOR SHOPPING     \n" +
        "       PLEASE VISIT AGAIN      \n"

class PrinterActivity : AppCompatActivity() {

    private val mPrinterManager = PrinterProviderImpl.getInstance(this)

    private val tvPrinterTitle by lazy { findViewById<TextView>(R.id.tvPrinterTitle) }
    private val pbWaiting by lazy { findViewById<ProgressBar>(R.id.pbWaiting) }
    private val btnPrintText by lazy { findViewById<Button>(R.id.btnPrintText) }
    private val btnPrintTextLeftRight by lazy { findViewById<Button>(R.id.btnPrintTextLeftRight) }
    private val btnPrintTextLeftMiddleRight by lazy { findViewById<Button>(R.id.btnPrintTextLeftMiddleRight) }
    private val btnPrintBarcode by lazy { findViewById<Button>(R.id.btnPrintBarcode) }
    private val btnPrintQrcode by lazy { findViewById<Button>(R.id.btnPrintQrcode) }
    private val btnPrintImage by lazy { findViewById<Button>(R.id.btnPrintImage) }
    private val btnPrintImageFromPhoto by lazy { findViewById<Button>(R.id.btnPrintImageFromPhoto) }
    private val btnLineFeed by lazy { findViewById<Button>(R.id.btnLineFeed) }
    private val spGray by lazy { findViewById<Spinner>(R.id.spGray) }

    private val grayArray = arrayOf(-6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6)

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val bmp = contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input)
            }
            mPrinterManager.addImage(
                imageFormat,
                ImageUtil.image2Bytes(bmp)
            )
            mPrinterManager.startPrint()
            runOnUiThread {
                Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                uiRefreshOnStopPrinting()
            }
        }
    }

    private val textFormat = Bundle().apply {
        putInt(ContentFormat.FONT.value, 1)
        putBoolean(ContentFormat.FONT_BOLD.value, false)
        putInt(ContentFormat.ALIGN.value, 1)
        putInt(ContentFormat.LINE_HEIGHT.value, 0)
    }
    private val barcodeFormat = Bundle().apply {
        putInt(ContentFormat.ALIGN.value, 1)
        putInt(ContentFormat.WIDTH.value, 400)
        putInt(ContentFormat.HEIGHT.value, 100)
        putSerializable(ContentFormat.BARCODE_TYPE.value, BarcodeFormat.CODE_128)
    }
    private val qrcodeFormat = Bundle().apply {
        putInt(ContentFormat.ALIGN.value, 1)
        putInt(ContentFormat.EXPECTED_HEIGHT.value, 300)
    }
    private val imageFormat = Bundle().apply {
        putInt(ContentFormat.HEIGHT.value, 300)
        putInt(ContentFormat.WIDTH.value, 300)
        putInt(ContentFormat.OFFSET.value, 35)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer)

        btnPrintText.setOnClickListener { onPrintTextButtonClicked() }
        btnPrintTextLeftRight.setOnClickListener { onPrintTextLeftRightButtonClicked() }
        btnPrintTextLeftMiddleRight.setOnClickListener { onPrintTextLeftMiddleRightButtonClicked() }
        btnPrintBarcode.setOnClickListener { onPrintBarcodeButtonClicked() }
        btnPrintQrcode.setOnClickListener { onPrintQrcodeButtonClicked() }
        btnPrintImage.setOnClickListener { onPrintImageButtonClicked() }
        btnPrintImageFromPhoto.setOnClickListener { onPrintImageFromPhotoButtonClicked() }
        btnLineFeed.setOnClickListener { onLineFeedButtonClicked() }

        spGray.adapter = ArrayAdapter(this, simple_spinner_item, grayArray).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spGray.setSelection(grayArray.indexOf(0))
    }

    private fun onPrintTextButtonClicked() {
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                mPrinterManager.setGray(spGray.selectedItem as Int)
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintTextButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.addText(textFormat, CONTENT)
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess { ret ->
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }

    private fun onPrintTextLeftRightButtonClicked() {
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                mPrinterManager.setGray(spGray.selectedItem as Int)
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintTextLeftRightButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.addTextLeft_Right(textFormat, "Patrick", "Urovo")
                mPrinterManager.feedLine(-3)
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintTextLeftMiddleRightButtonClicked() {
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                mPrinterManager.setGray(spGray.selectedItem as Int)
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintTextLeftMiddleRightButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.addTextLeft_Center_Right(
                    textFormat,
                    "Patrick",
                    "works in",
                    "Urovo"
                )
                mPrinterManager.feedLine(-3)
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintBarcodeButtonClicked() {
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                mPrinterManager.setGray(spGray.selectedItem as Int)
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintBarcodeButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.addBarCode(barcodeFormat, "8618807737955Patrick")
                mPrinterManager.feedLine(-3)
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintQrcodeButtonClicked() {
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                mPrinterManager.setGray(spGray.selectedItem as Int)
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintQrcodeButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.addQrCode(qrcodeFormat, "https://www.urovo.com/")
                mPrinterManager.feedLine(-3)
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintImageButtonClicked() {
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                mPrinterManager.setGray(spGray.selectedItem as Int)
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onPrintImageButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.addImage(
                    imageFormat,
                    ImageUtil.image2Bytes(BitmapFactory.decodeResource(getResources(), R.drawable.pikachu))
                )
                mPrinterManager.feedLine(-3)
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Printing successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }


    private fun onPrintImageFromPhotoButtonClicked() {
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        runCatching {
            mPrinterManager.setGray(spGray.selectedItem as Int)
            if (mPrinterManager.status != 0x00) {
                throw Exception("onPrintImageFromPhotoButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
            }
            pickImageLauncher.launch(arrayOf("image/*"))
        }.onFailure {
            runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
            it.printStackTrace()
        }
    }



    private fun onLineFeedButtonClicked() {
        mPrinterManager.initPrint()
        uiRefreshOnStartPrinting()
        Thread {
            runCatching {
                if (mPrinterManager.status != 0x00) {
                    throw Exception("onLineFeedButtonClicked: Printer not ready - statusCode=${mPrinterManager.status}")
                }
                mPrinterManager.feedLine(1)
                val ret = mPrinterManager.startPrint()
                if (ret != 0x00) throw Exception("startPrint(): Printing failed")
            }.onSuccess {
                runOnUiThread {
                    Toast.makeText(this@PrinterActivity, "Line Feed successfully", Toast.LENGTH_SHORT).show()
                    uiRefreshOnStopPrinting()
                }
            }.onFailure {
                runOnUiThread { Toast.makeText(this, "onFailure: ${mPrinterManager.status}", Toast.LENGTH_SHORT).show() }
                it.printStackTrace()
            }
            mPrinterManager.close()
        }.start()
    }

    private fun uiRefreshOnStartPrinting() {
        btnPrintText.isEnabled = false
        btnPrintTextLeftRight.isEnabled = false
        btnPrintTextLeftMiddleRight.isEnabled = false
        btnPrintBarcode.isEnabled = false
        btnPrintQrcode.isEnabled = false
        btnPrintImage.isEnabled = false
        btnPrintImageFromPhoto.isEnabled = false
        btnLineFeed.isEnabled = false
        tvPrinterTitle.visibility = View.INVISIBLE
        pbWaiting.visibility = View.VISIBLE
    }

    private fun uiRefreshOnStopPrinting() {
        btnPrintText.isEnabled = true
        btnPrintTextLeftRight.isEnabled = true
        btnPrintTextLeftMiddleRight.isEnabled = true
        btnPrintBarcode.isEnabled = true
        btnPrintQrcode.isEnabled = true
        btnPrintImage.isEnabled = true
        btnPrintImageFromPhoto.isEnabled = true
        btnLineFeed.isEnabled = true
        tvPrinterTitle.visibility = View.VISIBLE
        pbWaiting.visibility = View.INVISIBLE
    }
}

enum class ContentFormat(val value: String) {
    FONT("font"),
    FONT_BOLD("fontBold"),
    ALIGN("align"),
    LINE_HEIGHT("lineHeight"),
    WIDTH("width"),
    HEIGHT("height"),
    BARCODE_TYPE("barcode_type"),
    EXPECTED_HEIGHT("expectedHeight"),
    OFFSET("offset")
}