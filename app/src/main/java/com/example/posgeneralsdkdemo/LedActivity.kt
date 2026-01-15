package com.example.posgeneralsdkdemo

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.urovo.sdk.led.LEDDriverImpl

class LedActivity : AppCompatActivity() {

    private val mLedManager = LEDDriverImpl.getInstance()

    private val btnTurnOnLed by lazy { findViewById<Button>(R.id.btnTurnOnLed) }
    private val btnTurnOffLed by lazy { findViewById<Button>(R.id.btnTurnOffLed) }
    private val btnTurnOnAllLed by lazy { findViewById<Button>(R.id.btnTurnOnAllLed) }
    private val btnTurnOffAllLed by lazy { findViewById<Button>(R.id.btnTurnOffAllLed) }
    private val spLedColor by lazy { findViewById<Spinner>(R.id.spLedColor) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_led)

        btnTurnOnLed.setOnClickListener { onTurnOnLedButtonClicked() }
        btnTurnOffLed.setOnClickListener { onTurnOffLedButtonClicked() }
        btnTurnOnAllLed.setOnClickListener { onTurnOnAllLedButtonClicked() }
        btnTurnOffAllLed.setOnClickListener { onTurnOffAllLedButtonClicked() }

        spLedColor.adapter = ArrayAdapter(this, simple_spinner_item, LedColor.entries).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }

    private fun onTurnOnLedButtonClicked() {
        try {
            val color = spLedColor.selectedItem as LedColor
            mLedManager.turnOn(color.id)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error on onTurnOnLedButtonClicked()", Toast.LENGTH_SHORT).show()
        }
    }


    private fun onTurnOffLedButtonClicked() {
        try {
            val color = spLedColor.selectedItem as LedColor
            mLedManager.turnOff(color.id)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error on onTurnOffLedButtonClicked()", Toast.LENGTH_SHORT).show()
        }
    }


    private fun onTurnOnAllLedButtonClicked() {
        try {
            for (i in 1..4) {
                mLedManager.turnOn(i)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error on onTurnOnAllLedButtonClicked()", Toast.LENGTH_SHORT).show()
        }
    }


    private fun onTurnOffAllLedButtonClicked() {
        try {
            for (i in 1..4) {
                mLedManager.turnOff(i)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error on onTurnOffAllLedButtonClicked", Toast.LENGTH_SHORT).show()
        }
    }

}

enum class LedColor(val id: Int, val color: String) {
    BLUE(1, "Blue"),
    YELLOW(2, "Yellow"),
    GREEN(3, "Green"),
    RED(4, "Red");

    override fun toString(): String {
        return color
    }
}