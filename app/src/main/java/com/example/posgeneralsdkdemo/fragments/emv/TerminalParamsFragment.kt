package com.example.posgeneralsdkdemo.fragments.emv

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.posgeneralsdkdemo.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import com.example.posgeneralsdkdemo.EmvActivity
import com.example.posgeneralsdkdemo.enums.TerminalTag
import com.example.posgeneralsdkdemo.utils.EmvUtil
import com.example.posgeneralsdkdemo.utils.EmvUtil.hexToBinaryBytes
import com.urovo.i9000s.api.emv.ContantPara
import com.urovo.i9000s.api.emv.EmvNfcKernelApi
import androidx.core.graphics.toColorInt

const val KEY_TERMINAL_TYPE = "terminal_type"
const val KEY_TERMINAL_COUNTRY_CODE = "terminal_country_code"
const val KEY_TERMINAL_CAPABILITIES = "terminal_capabilities"
const val KEY_THRESHOLD_RANDOM_SWITCH = "threshold_random_switch"
const val DEFAULT_TERMINAL_TYPE = "22"
const val DEFAULT_TERMINAL_COUNTRY_CODE = "0156"
const val DEFAULT_TERMINAL_CAPABILITIES = "E0F8C8"
const val DEFAULT_THRESHOLD_RANDOM_SWITCH = "00"

private val terminalTypeCodeMap = mapOf(
    "22" to "Online",
    "14" to "Offline",
)

private val countryCodeMap = mapOf(
    "0048" to "Bahrain (BH)",
    "0050" to "Bangladesh (BD)",
    "0056" to "Belgium (BE)",
    "0100" to "Bulgaria (BG)",
    "0124" to "Canada (CA)",
    "0152" to "Chile (CL)",
    "0156" to "China (CN)",
    "0170" to "Colombia (CO)",
    "0203" to "Czechia (CZ)",
    "0208" to "Denmark (DK)",
    "0231" to "Ethiopia (ET)",
    "0246" to "Finland (FI)",
    "0250" to "France (FR)",
    "0276" to "Germany (DE)",
    "0288" to "Ghana (GH)",
    "0300" to "Greece (GR)",
    "0344" to "Hong Kong (HK)", // Part of China
    "0348" to "Hungary (HU)",
    "0352" to "Iceland (IS)",
    "0356" to "India (IN)",
    "0360" to "Indonesia (ID)",
    "0372" to "Ireland (IE)",
    "0380" to "Italy (IT)",
    "0392" to "Japan (JP)",
    "0400" to "Jordan (JO)",
    "0404" to "Kenya (KE)",
    "0410" to "Korea (KR)",
    "0414" to "Kuwait (KW)",
    "0434" to "Libya (LY)",
    "0446" to "Macau (MO)", // Part of China
    "0458" to "Malaysia (MY)",
    "0484" to "Mexico (MX)",
    "0504" to "Morocco (MA)",
    "0512" to "Oman (OM)",
    "0524" to "Nepal (NP)",
    "0528" to "Netherlands (NL)",
    "0566" to "Nigeria (NG)",
    "0578" to "Norway (NO)",
    "0586" to "Pakistan (PK)",
    "0604" to "Peru (PE)",
    "0608" to "Philippines (PH)",
    "0616" to "Poland (PL)",
    "0620" to "Portugal (PT)",
    "0634" to "Qatar (QA)",
    "0642" to "Romania (RO)",
    "0643" to "Russia (RU)",
    "0682" to "Saudi Arabia (SA)",
    "0702" to "Singapore (SG)",
    "0704" to "Vietnam (VN)",
    "0710" to "South Africa (ZA)",
    "0724" to "Spain (ES)",
    "0729" to "Sudan (SD)",
    "0752" to "Sweden (SE)",
    "0756" to "Switzerland (CH)",
    "0764" to "Thailand (TH)",
    "0784" to "United Arab Emirates (AE)",
    "0158" to "Taiwan (TW)", // Part of China
    "0788" to "Tunisia (TN)",
    "0792" to "Turkey (TR)",
    "0800" to "Uganda (UG)",
    "0804" to "Ukraine (UA)",
    "0818" to "Egypt (EG)",
    "0826" to "United Kingdom (GB)",
    "0834" to "Tanzania (TZ)",
    "0840" to "United States (US)"
)
val thresholdRandomSwitchMap = mapOf(
    "00" to "Disabled",
    "01" to "Enabled",
)

class TerminalParamsFragment : Fragment(R.layout.fragment_terminal_params) {

    private val tvTerminalCap get() = requireView().findViewById<TextView>(R.id.tvTerminalCap)
    private val tvInfo get() = requireView().findViewById<TextView>(R.id.tvInfo)
    private val tvTerminalType get() = requireView().findViewById<TextView>(R.id.tvTerminalType)
    private val tvTerminalCountry get() = requireView().findViewById<TextView>(R.id.tvTerminalCountry)
    private val tvThresholdRandomSwitch get() = requireView().findViewById<TextView>(R.id.tvThresholdRandomSwitch)
    private val llRow1 get() = requireView().findViewById<LinearLayout>(R.id.llRow1)
    private val llRow2 get() = requireView().findViewById<LinearLayout>(R.id.llRow2)
    private val llRow3 get() = requireView().findViewById<LinearLayout>(R.id.llRow3)

    private val btnResetTermCap get() = requireView().findViewById<Button>(R.id.btnResetTermCap)
    private val btnUpdateTerminalParameters get() = requireView().findViewById<Button>(R.id.btnUpdateTerminalParameters)

    private val mEmvKernelManager: EmvNfcKernelApi
        get() = (requireActivity() as EmvActivity).mEmvKernelManager

    private lateinit var sharedPreferences: SharedPreferences
    private val sharedVm: SharedVm by activityViewModels()
    private lateinit var termCapBytes: ByteArray

    private val row1Spec = listOf(
        BitSpec("Manual", 0), // B1b8
        BitSpec("Mag", 1), // B1b7
        BitSpec("ICC", 2) // B1b6
    )

    private val row2Spec = listOf(
        BitSpec("P_Off PIN", 8), // B2b8
        BitSpec("Online PIN", 9), // B2b7
        BitSpec("Sign", 10), // B2b6
        BitSpec("E_Off PIN", 11), // B2b5
        BitSpec("No CVM", 12) // B2b4
    )

    private val row3Spec = listOf(
        BitSpec("SDA", 16), // B3b8
        BitSpec("DDA", 17), // B3b7
        BitSpec("CDA", 20) // B3b4
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val savedTerminalTypeCode = sharedPreferences.getString(KEY_TERMINAL_TYPE, DEFAULT_TERMINAL_TYPE)
        val savedTerminalTypeName = terminalTypeCodeMap[savedTerminalTypeCode] ?: "Unknown"
        tvTerminalType.text = "$savedTerminalTypeName - $savedTerminalTypeCode"

        val savedCountryCode = sharedPreferences.getString(KEY_TERMINAL_COUNTRY_CODE, DEFAULT_TERMINAL_COUNTRY_CODE)
        val savedCountryName = countryCodeMap[savedCountryCode] ?: "Unknown"
        tvTerminalCountry.text = "$savedCountryName - $savedCountryCode"

        val savedThresholdRandomSwitchCode = sharedPreferences.getString(KEY_THRESHOLD_RANDOM_SWITCH, DEFAULT_THRESHOLD_RANDOM_SWITCH) ?: DEFAULT_THRESHOLD_RANDOM_SWITCH
        val savedThresholdRandomSwitchName = thresholdRandomSwitchMap[savedThresholdRandomSwitchCode] ?: "Unknown"
        tvThresholdRandomSwitch.text = "$savedThresholdRandomSwitchName - $savedThresholdRandomSwitchCode"

        termCapBytes = EmvUtil.hex6To3Bytes(sharedPreferences.getString(KEY_TERMINAL_CAPABILITIES, DEFAULT_TERMINAL_CAPABILITIES) ?: DEFAULT_TERMINAL_CAPABILITIES)
        inflateRow(llRow1, row1Spec)
        inflateRow(llRow2, row2Spec)
        inflateRow(llRow3, row3Spec)

        tvTerminalType.setOnClickListener {
            showWheelDialog(
                context = requireContext(),
                list = terminalTypeCodeMap.map { (code, name) -> "$name - $code" },
                current = tvTerminalType.text?.toString()
            ) { selected ->
                tvTerminalType.text = selected
                uiRefreshOnSaveAndUpdate()
            }
        }

        tvTerminalCountry.setOnClickListener {
            showWheelDialog(
                context = requireContext(),
                list = countryCodeMap.map { (code, name) -> "$name - $code" },
                current = tvTerminalCountry.text?.toString()
            ) { selected ->
                tvTerminalCountry.text = selected
                uiRefreshOnSaveAndUpdate()
            }
        }

        tvThresholdRandomSwitch.setOnClickListener {
            showWheelDialog(
                context = requireContext(),
                list = thresholdRandomSwitchMap.map { (code, name) -> "$name - $code" },
                current = tvThresholdRandomSwitch.text?.toString()
            ) { selected ->
                tvThresholdRandomSwitch.text = selected
                uiRefreshOnSaveAndUpdate()
            }
        }

        btnResetTermCap.setOnClickListener { onResetTermCapButtonClicked() }
        btnUpdateTerminalParameters.setOnClickListener { onUpdateTerminalParametersButtonClicked() }

        uiRefreshOnSaveAndUpdate()
    }

    private fun onResetTermCapButtonClicked() {
//        btnSaveAndUpdate.isEnabled = true
        termCapBytes = EmvUtil.hex6To3Bytes(DEFAULT_TERMINAL_CAPABILITIES)
        inflateRow(llRow1, row1Spec)
        inflateRow(llRow2, row2Spec)
        inflateRow(llRow3, row3Spec)
        uiRefreshOnSaveAndUpdate()
    }

    private fun onUpdateTerminalParametersButtonClicked() {
        sharedPreferences.edit {
            putString(KEY_TERMINAL_TYPE, tvTerminalType.text.toString().takeLast(2))
            putString(KEY_TERMINAL_COUNTRY_CODE, tvTerminalCountry.text.toString().takeLast(4))
            putString(KEY_THRESHOLD_RANDOM_SWITCH, tvThresholdRandomSwitch.text.toString().takeLast(2))

            putString(KEY_TERMINAL_CAPABILITIES, tvTerminalCap.text.toString().trim().uppercase())
        }

        val updatedTerminalParameters = buildString {
            append(TerminalTag.TERMINAL_TYPE.tag) // 9F35
            append(TerminalTag.TERMINAL_TYPE.len) // 01
            append(sharedPreferences.getString(KEY_TERMINAL_TYPE, DEFAULT_TERMINAL_TYPE) ?: DEFAULT_TERMINAL_TYPE) // Just a label, no real effect. 0x22 means support Online transaction. 0x14 means Offline POS.

            append(TerminalTag.COUNTRY_CODE.tag) // 9F1A
            append(TerminalTag.COUNTRY_CODE.len) // 02
            append(sharedPreferences.getString(KEY_TERMINAL_COUNTRY_CODE, DEFAULT_TERMINAL_COUNTRY_CODE) ?: DEFAULT_TERMINAL_COUNTRY_CODE) // The country is Spain

            append(TerminalTag.RANDOM_TRANSACTION_SWITCH.tag) // 9F1A
            append(TerminalTag.RANDOM_TRANSACTION_SWITCH.len) // 02
            append(sharedPreferences.getString(KEY_THRESHOLD_RANDOM_SWITCH, DEFAULT_THRESHOLD_RANDOM_SWITCH) ?: DEFAULT_THRESHOLD_RANDOM_SWITCH) // The country is Spain

            append(TerminalTag.TERMINAL_CAPABILITIES.tag) // 9F33
            append(TerminalTag.TERMINAL_CAPABILITIES.len) // 03
            append(sharedPreferences.getString(KEY_TERMINAL_CAPABILITIES, DEFAULT_TERMINAL_CAPABILITIES) ?: DEFAULT_TERMINAL_CAPABILITIES) // "E0F8C8" means support all; "E0F0C8" means not support No_CVM
        }
        runCatching {
            mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.UNKNOWN, updatedTerminalParameters)
            mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.ICC, updatedTerminalParameters)
            mEmvKernelManager.updateTerminalParamters(ContantPara.CardSlot.PICC, updatedTerminalParameters)
        }.onSuccess {
            Toast.makeText(requireContext(), "Terminal Params updated successfully", Toast.LENGTH_SHORT).show()
            uiRefreshOnSaveAndUpdate()
            sharedVm.triggerTermParamsRefresh()
        }.onFailure {
            tvInfo.text = it.message
            it.printStackTrace()
        }
    }

    private fun showWheelDialog(
        context: Context,
        list: List<String>,
        current: String? = null,
        onSelected: (String) -> Unit
    ) {
        val picker = NumberPicker(context).apply {
            minValue = 0
            maxValue = list.size - 1
            displayedValues = list.toTypedArray()
            wrapSelectorWheel = false

            val idx = current?.let { list.indexOf(it) } ?: -1
            if (idx >= 0) value = idx
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Select Country")
            .setView(picker)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                onSelected(list[picker.value])
            }
            .show()
    }

    private fun inflateRow(parent: LinearLayout, specs: List<BitSpec>) {
        parent.removeAllViews()
        specs.forEach { spec ->
            val btn = Button(requireContext()).apply {
                text = spec.label
                isAllCaps = false
                setPadding(0, 0, 0, 0)
                textSize = 10F
                updateBitButtonStyle(this, isBitOn(termCapBytes, spec.index))
                setOnClickListener {
                    flipBit(termCapBytes, spec.index)
                    updateBitButtonStyle(this, isBitOn(termCapBytes, spec.index))
                    uiRefreshOnSaveAndUpdate()
                }
            }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 12
            }
            parent.addView(btn, lp)
        }
    }

    private fun updateBitButtonStyle(btn: Button, on: Boolean) {
        if (on) {
            btn.setBackgroundColor("#66BB6A".toColorInt())
            btn.setTextColor(Color.BLACK)
        } else {
            btn.setBackgroundColor("#DDDDDD".toColorInt())
            btn.setTextColor(Color.BLACK)
        }
    }

    private fun isBitOn(bytes: ByteArray, idx: Int): Boolean {
        val byteIndex = idx / 8 // 0,1,2
        val posInRow = idx % 8  // 0..7  (0 means b8)
        val mask = 1 shl (7 - posInRow) // 0->bit7(b8), 7->bit0(b1)
        return (bytes[byteIndex].toInt() and mask) != 0
    }

    private fun flipBit(bytes: ByteArray, idx: Int) {
        val byteIndex = idx / 8
        val posInRow = idx % 8
        val mask = 1 shl (7 - posInRow)
        bytes[byteIndex] = (bytes[byteIndex].toInt() xor mask).toByte()
    }

    private fun isNothingChanged(): Boolean {
        return tvTerminalCap.text == sharedPreferences.getString(KEY_TERMINAL_CAPABILITIES, DEFAULT_TERMINAL_CAPABILITIES)  &&
                tvTerminalType.text.toString().takeLast(2) == sharedPreferences.getString(KEY_TERMINAL_TYPE, DEFAULT_TERMINAL_TYPE) &&
                tvTerminalCountry.text.toString().takeLast(4) == sharedPreferences.getString(KEY_TERMINAL_COUNTRY_CODE, DEFAULT_TERMINAL_COUNTRY_CODE) &&
                tvThresholdRandomSwitch.text.toString().takeLast(2) == sharedPreferences.getString(KEY_THRESHOLD_RANDOM_SWITCH, DEFAULT_THRESHOLD_RANDOM_SWITCH)
    }

    // <-----------------UI helper methods-----------------> //

    private fun uiRefreshOnSaveAndUpdate() {
        tvTerminalCap.text = EmvUtil.bytesToHex6(termCapBytes)
        btnUpdateTerminalParameters.isEnabled = !isNothingChanged()
        if (tvTerminalCap.text.toString().trim().uppercase() == "E0F8C8") {
            tvTerminalCap.setBackgroundColor(Color.GREEN)
        } else {
            tvTerminalCap.setBackgroundColor(Color.RED)
        }
        tvInfo.text = buildString {
            append("${hexToBinaryBytes(tvTerminalCap.text.toString().trim().uppercase())} - Current\n")
            append("11100000 11111000 11001000 - E0F8C8\n\n")

            append("<===========Terminal Cap Info===========>\n\n")

            append("Terminal Capabilities (9F33):\n")
            append("Byte1 (Supported Card Type): 11100000\n")
            append(" - b8 Manual Key Entry supported\n")
            append(" - b7 Mag-Stripe Card supported\n")
            append(" - b6 ICC supported\n")
            append(" - b5~b1 RFU\n\n")

            append("Byte2 (Supported CVM Type): 11111000\n")
            append(" - b8 Offline PIN (plaintext) supported\n")
            append(" - b7 Online PIN (enciphered) supported\n")
            append(" - b6 Signature supported\n")
            append(" - b5 Offline PIN (enciphered) supported\n")
            append(" - b4 No CVM supported\n")
            append(" - b3~b1 RFU\n\n")

            append("Byte3 (Supported Security Methods): 11001000\n")
            append(" - b8 SDA supported\n")
            append(" - b7 DDA supported\n")
            append(" - b4 CDA supported\n")
            append(" - b6,b5,b3~b1 RFU")
        }
    }

}

data class BitSpec(val label: String, val index: Int)


