package com.example.posgeneralsdkdemo.fragments.emv

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.posgeneralsdkdemo.EmvActivity
import com.example.posgeneralsdkdemo.R
import com.example.posgeneralsdkdemo.utils.EmvUtil
import com.example.posgeneralsdkdemo.utils.EmvUtil.formatAmount12
import com.urovo.i9000s.api.emv.ContantPara
import com.urovo.i9000s.api.emv.EmvNfcKernelApi

private const val ALL_F_10_BYTES = "FFFFFFFFFF"
    private const val ALL_0_10_BYTES = "0000000000"
private const val CLASSIC_TAC_A = "DC4004F800"
private const val CLASSIC_TAC_B = "BC78BCA800"

const val KEY_FLOOR_LIMIT = "floor_limit"
const val KEY_CONTACT_TAC_DENIAL = "contact_tac_denial"
const val KEY_CONTACT_TAC_ONLINE = "contact_tac_online"
const val KEY_CONTACT_TAC_DEFAULT = "contact_tac_default"

const val DEFAULT_FLOOR_LIMIT = "000000000000"
const val DEFAULT_CONTACT_TAC_DENIAL = ALL_0_10_BYTES
const val DEFAULT_CONTACT_TAC_ONLINE = CLASSIC_TAC_A
const val DEFAULT_CONTACT_TAC_DEFAULT = CLASSIC_TAC_B

const val UNION_PAY = "UnionPay"
const val VISA = "Visa"
const val MASTER_CARD = "MasterCard"

class AppParamsFragment : Fragment(R.layout.fragment_app_params) {

    private val tvInfo get() = requireView().findViewById<TextView>(R.id.tvInfo)

    private val btnAddUnionPay get() = requireView().findViewById<Button>(R.id.btnAddUnionPay)
    private val btnAddVisa get() = requireView().findViewById<Button>(R.id.btnAddVisa)
    private val btnAddMasterCard get() = requireView().findViewById<Button>(R.id.btnAddMasterCard)
    private val btnClearAllAid get() = requireView().findViewById<Button>(R.id.btnClearAllAid)

    private val spFloorLimit get() = requireView().findViewById<Spinner>(R.id.spFloorLimit)
    private val spTacDenial get() = requireView().findViewById<Spinner>(R.id.spTacDenial)
    private val spTacOnline get() = requireView().findViewById<Spinner>(R.id.spTacOnline)
    private val spTacDefault get() = requireView().findViewById<Spinner>(R.id.spTacDefault)

    private val mEmvKernelManager: EmvNfcKernelApi
        get() = (requireActivity() as EmvActivity).mEmvKernelManager

    private lateinit var sharedPreferences: SharedPreferences

    private val sharedVm: SharedVm by activityViewModels()

    private val arrayOfFloorLimit = arrayOf("000000000000", "000000000100", "000000010000", "000001000000", "000100000000", "010000000000")
    private val arrayOfTac = arrayOf(ALL_0_10_BYTES, ALL_F_10_BYTES, CLASSIC_TAC_A, CLASSIC_TAC_B)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        btnAddUnionPay.setOnClickListener { onAddUnionPayButtonClicked() }
        btnAddVisa.setOnClickListener { onAddVisaButtonClicked() }
        btnAddMasterCard.setOnClickListener { onAddMasterCardButtonClicked() }
        btnClearAllAid.setOnClickListener { onClearAllAidButtonClicked() }

        spFloorLimit.adapter = ArrayAdapter(requireContext(), simple_spinner_item, arrayOfFloorLimit).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spFloorLimit.setSelection(arrayOfFloorLimit.indexOf(sharedPreferences.getString(KEY_FLOOR_LIMIT, DEFAULT_FLOOR_LIMIT)))

        spTacDenial.adapter = ArrayAdapter(requireContext(), simple_spinner_item, arrayOfTac).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spTacDenial.setSelection(arrayOfTac.indexOf(sharedPreferences.getString(KEY_CONTACT_TAC_DENIAL, DEFAULT_CONTACT_TAC_DENIAL)))

        spTacOnline.adapter = ArrayAdapter(requireContext(), simple_spinner_item, arrayOfTac).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spTacOnline.setSelection(arrayOfTac.indexOf(sharedPreferences.getString(KEY_CONTACT_TAC_ONLINE, DEFAULT_CONTACT_TAC_ONLINE)))

        spTacDefault.adapter = ArrayAdapter(requireContext(), simple_spinner_item, arrayOfTac).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        spTacDefault.setSelection(arrayOfTac.indexOf(sharedPreferences.getString(KEY_CONTACT_TAC_DEFAULT, DEFAULT_CONTACT_TAC_DEFAULT)))

    }

    private fun onAddUnionPayButtonClicked() {
        sharedPreferences.edit {
            putString(KEY_FLOOR_LIMIT, spFloorLimit.selectedItem as String)
            putString(KEY_CONTACT_TAC_DENIAL, spTacDenial.selectedItem as String)
            putString(KEY_CONTACT_TAC_ONLINE, spTacOnline.selectedItem as String)
            putString(KEY_CONTACT_TAC_DEFAULT, spTacDefault.selectedItem as String)
        }
        runCatching {
            EmvUtil.addAidUpiIcc(mEmvKernelManager, spFloorLimit.selectedItem as String, spTacDenial.selectedItem as String, spTacOnline.selectedItem as String, spTacDefault.selectedItem as String)
        }.onSuccess {
            Toast.makeText(requireContext(), "Add UnionPay Params successfully", Toast.LENGTH_SHORT).show()
            tvInfo.text = buildString {
                append("<======APP Params added/updated======>\n\n")
                append(" - Card Type: IcCard\n")
                append(" - AID: A000000333010101 - UnionPay\n")
                append(" - App Version: 0030\n")
                append(" - Floor Limit: ${formatAmount12(spFloorLimit.selectedItem as String, 2)}\n")
                append(" - Contact TAC DENIAL: ${spTacDenial.selectedItem as String}\n")
                append(" - Contact TAC ONLINE: ${spTacOnline.selectedItem as String}\n")
                append(" - Contact TAC DEFAULT: ${spTacDefault.selectedItem as String}\n")
                append(" - Defaul DDOL: 9F3704")
            }
            sharedVm.triggerAppParamsLoadedRefresh(UNION_PAY)
        }.onFailure {
            tvInfo.text = it.message
            it.printStackTrace()
        }
    }

    private fun onAddVisaButtonClicked() {
        sharedPreferences.edit {
            putString(KEY_FLOOR_LIMIT, spFloorLimit.selectedItem as String)
            putString(KEY_CONTACT_TAC_DENIAL, spTacDenial.selectedItem as String)
            putString(KEY_CONTACT_TAC_ONLINE, spTacOnline.selectedItem as String)
            putString(KEY_CONTACT_TAC_DEFAULT, spTacDefault.selectedItem as String)
        }
        runCatching {
            EmvUtil.addAidVisaIcc(mEmvKernelManager, spFloorLimit.selectedItem as String, spTacDenial.selectedItem as String, spTacOnline.selectedItem as String, spTacDefault.selectedItem as String)
            tvInfo.text = buildString {
                append("<======APP Params added/updated======>\n\n")
                append(" - Card Type: IcCard\n")
                append(" - AID: A0000000031010 - Visa\n")
                append(" - App Version: 0002\n")
                append(" - Floor Limit: ${formatAmount12(spFloorLimit.selectedItem as String, 2)}\n")
                append(" - Contact TAC DENIAL: ${spTacDenial.selectedItem as String}\n")
                append(" - Contact TAC ONLINE: ${spTacOnline.selectedItem as String}\n")
                append(" - Contact TAC DEFAULT: ${spTacDefault.selectedItem as String}\n")
                append(" - Defaul DDOL: 9F3704")
            }
            sharedVm.triggerAppParamsLoadedRefresh(VISA)
        }.onSuccess {
            Toast.makeText(requireContext(), "Add Visa Params successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            tvInfo.text = it.message
            it.printStackTrace()
        }
    }

    private fun onAddMasterCardButtonClicked() {
        sharedPreferences.edit {
            putString(KEY_FLOOR_LIMIT, spFloorLimit.selectedItem as String)
            putString(KEY_CONTACT_TAC_DENIAL, spTacDenial.selectedItem as String)
            putString(KEY_CONTACT_TAC_ONLINE, spTacOnline.selectedItem as String)
            putString(KEY_CONTACT_TAC_DEFAULT, spTacDefault.selectedItem as String)
        }
        runCatching {
            EmvUtil.addAidMasterCardIcc(mEmvKernelManager, spFloorLimit.selectedItem as String, spTacDenial.selectedItem as String, spTacOnline.selectedItem as String, spTacDefault.selectedItem as String)
        }.onSuccess {
            Toast.makeText(requireContext(), "Add MasterCard Params successfully", Toast.LENGTH_SHORT).show()
            tvInfo.text = buildString {
                append("<======APP Params added/updated======>\n\n")
                append(" - Card Type: IcCard\n")
                append(" - AID: A0000000041010 - MasterCard\n")
                append(" - App Version: 0002\n")
                append(" - Floor Limit: ${formatAmount12(spFloorLimit.selectedItem as String, 2)}\n")
                append(" - Contact TAC DENIAL: ${spTacDenial.selectedItem as String}\n")
                append(" - Contact TAC ONLINE: ${spTacOnline.selectedItem as String}\n")
                append(" - Contact TAC DEFAULT: ${spTacDefault.selectedItem as String}\n")
                append(" - Defaul DDOL: 9F3704")
            }
            sharedVm.triggerAppParamsLoadedRefresh(MASTER_CARD)
        }.onFailure {
            tvInfo.text = it.message
            it.printStackTrace()
        }
    }

    private fun onClearAllAidButtonClicked() {
        runCatching {
            mEmvKernelManager.updateAID(ContantPara.Operation.CLEAR, null)
        }.onSuccess {
            Toast.makeText(requireContext(), "Clear All AIDs successfully", Toast.LENGTH_SHORT).show()
            tvInfo.text = ""
            sharedVm.triggerAppParamsClearRefresh()
        }.onFailure {
            tvInfo.text = it.message
            it.printStackTrace()
        }
    }
}
