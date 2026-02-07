package com.example.posgeneralsdkdemo.fragments.emv

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.posgeneralsdkdemo.databinding.FragmentAppParamsBinding
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

const val DEFAULT_CONTACT_TAC_DENIAL = ALL_0_10_BYTES
const val DEFAULT_CONTACT_TAC_ONLINE = CLASSIC_TAC_A
const val DEFAULT_CONTACT_TAC_DEFAULT = CLASSIC_TAC_B

const val UNION_PAY_ICC = "UnionPay_ICC"
const val UNION_PAY_PICC = "UnionPay_PICC"
const val VISA_ICC = "Visa_ICC"
const val VISA_PICC = "Visa_PICC"
const val MASTER_CARD_ICC = "MasterCard_ICC"
const val MASTER_CARD_PICC = "MasterCard_PICC"
private val arrayOfTac = arrayOf(ALL_0_10_BYTES, ALL_F_10_BYTES, CLASSIC_TAC_A, CLASSIC_TAC_B)

class AppParamsFragment : Fragment(R.layout.fragment_app_params) {

    private var _binding: FragmentAppParamsBinding? = null
    private val binding get() = _binding!!

    private val mEmvKernelManager: EmvNfcKernelApi
        get() = (requireActivity() as EmvActivity).mEmvKernelManager
    private val sharedPreferences: SharedPreferences
        get() = (requireActivity() as EmvActivity).sharedPreferences

    private val sharedVm: SharedVm by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAppParamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.apply {
            btnAddUnionPayIcc.setOnClickListener { onAddUnionPayIccButtonClicked() }
            btnAddVisaIcc.setOnClickListener { onAddVisaIccButtonClicked() }
            btnAddMasterCardIcc.setOnClickListener { onAddMasterCardIccButtonClicked() }
            btnAddUnionPayPicc.setOnClickListener { onAddUnionPayPiccButtonClicked() }
            btnAddVisaPicc.setOnClickListener { onAddVisaPiccButtonClicked() }
            btnAddMasterCardPicc.setOnClickListener { onAddMasterCardPiccButtonClicked() }
            btnClearAllAid.setOnClickListener { onClearAllAidButtonClicked() }

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onAddUnionPayIccButtonClicked() {
        sharedPreferences.edit {
            putString(KEY_CONTACT_TAC_DENIAL, binding.spTacDenial.selectedItem as String)
            putString(KEY_CONTACT_TAC_ONLINE, binding.spTacOnline.selectedItem as String)
            putString(KEY_CONTACT_TAC_DEFAULT, binding.spTacDefault.selectedItem as String)
        }
        runCatching {
            EmvUtil.addAidUpiIcc(mEmvKernelManager, "000000010000", "00020000", binding.spTacDenial.selectedItem as String, binding.spTacOnline.selectedItem as String, binding.spTacDefault.selectedItem as String)
        }.onSuccess {
            Toast.makeText(requireContext(), "Add UnionPay ICC Params successfully", Toast.LENGTH_SHORT).show()
            binding.tvInfo.text = buildString {
                append("<======APP_ICC Params added/updated======>\n\n")
                append(" - Card Type: IcCard\n")
                append(" - AID: A000000333010101 - UnionPay\n")
                append(" - App Version: 0030\n")
                append(" - Threshold(Fixed): 100.00\n")
                append(" - Floor Limit(Fixed): 200.00\n")
                append(" - Contact TAC DENIAL: ${binding.spTacDenial.selectedItem as String}\n")
                append(" - Contact TAC ONLINE: ${binding.spTacOnline.selectedItem as String}\n")
                append(" - Contact TAC DEFAULT: ${binding.spTacDefault.selectedItem as String}\n")
                append(" - Defaul DDOL: 9F3704(Fixed)")
            }
            sharedVm.triggerAppParamsLoadedRefresh(UNION_PAY_ICC)
        }.onFailure {
            binding.tvInfo.text = it.message
            it.printStackTrace()
        }
    }

    private fun onAddVisaIccButtonClicked() {
        sharedPreferences.edit {
            putString(KEY_CONTACT_TAC_DENIAL, binding.spTacDenial.selectedItem as String)
            putString(KEY_CONTACT_TAC_ONLINE, binding.spTacOnline.selectedItem as String)
            putString(KEY_CONTACT_TAC_DEFAULT, binding.spTacDefault.selectedItem as String)
        }
        runCatching {
            EmvUtil.addAidVisaIcc(mEmvKernelManager, "000000010000", "00020000", binding.spTacDenial.selectedItem as String, binding.spTacOnline.selectedItem as String, binding.spTacDefault.selectedItem as String)
        }.onSuccess {
            Toast.makeText(requireContext(), "Add Visa ICC Params successfully", Toast.LENGTH_SHORT).show()
            binding.tvInfo.text = buildString {
                append("<======APP_ICC Params added/updated======>\n\n")
                append(" - Card Type: IcCard\n")
                append(" - AID: A0000000031010 - Visa\n")
                append(" - App Version: 0002\n")
                append(" - Threshold(Fixed): 100.00\n")
                append(" - Floor Limit(Fixed): 200.00\n")
                append(" - Contact TAC DENIAL: ${binding.spTacDenial.selectedItem as String}\n")
                append(" - Contact TAC ONLINE: ${binding.spTacOnline.selectedItem as String}\n")
                append(" - Contact TAC DEFAULT: ${binding.spTacDefault.selectedItem as String}\n")
                append(" - Defaul DDOL: 9F3704(Fixed)")
            }
            sharedVm.triggerAppParamsLoadedRefresh(VISA_ICC)
        }.onFailure {
            binding.tvInfo.text = it.message
            it.printStackTrace()
        }
    }

    private fun onAddMasterCardIccButtonClicked() {
        sharedPreferences.edit {
            putString(KEY_CONTACT_TAC_DENIAL, binding.spTacDenial.selectedItem as String)
            putString(KEY_CONTACT_TAC_ONLINE, binding.spTacOnline.selectedItem as String)
            putString(KEY_CONTACT_TAC_DEFAULT, binding.spTacDefault.selectedItem as String)
        }
        runCatching {
            EmvUtil.addAidMasterCardIcc(mEmvKernelManager, "000000010000", "00020000", binding.spTacDenial.selectedItem as String, binding.spTacOnline.selectedItem as String, binding.spTacDefault.selectedItem as String)
        }.onSuccess {
            Toast.makeText(requireContext(), "Add MasterCard ICC Params successfully", Toast.LENGTH_SHORT).show()
            binding.tvInfo.text = buildString {
                append("<======APP_ICC Params added/updated======>\n\n")
                append(" - Card Type: IcCard\n")
                append(" - AID: A0000000041010 - MasterCard\n")
                append(" - App Version: 0002\n")
                append(" - Threshold(Fixed): 100.00\n")
                append(" - Floor Limit(Fixed): 200.00\n")
                append(" - Contact TAC DENIAL: ${binding.spTacDenial.selectedItem as String}\n")
                append(" - Contact TAC ONLINE: ${binding.spTacOnline.selectedItem as String}\n")
                append(" - Contact TAC DEFAULT: ${binding.spTacDefault.selectedItem as String}\n")
                append(" - Defaul DDOL: 9F3704(Fixed)")
            }
            sharedVm.triggerAppParamsLoadedRefresh(MASTER_CARD_ICC)
        }.onFailure {
            binding.tvInfo.text = it.message
            it.printStackTrace()
        }
    }

    private fun onAddUnionPayPiccButtonClicked() {
        runCatching {
            EmvUtil.addAidUpiPicc(mEmvKernelManager)
        }.onSuccess {
            Toast.makeText(requireContext(), "Add UnionPay PICC Params successfully", Toast.LENGTH_SHORT).show()
            binding.tvInfo.text = buildString {
                append("<======APP_PICC Params added/updated======>\n\n")
                append(" - Card Type: UpiCard\n")
                append(" - AID: A000000333010101 - UnionPay\n")
                append(" - Floor Limit(Fixed): 0.00\n")
                append(" - CVM Required Limit(Fixed): 300.00\n")
                append(" - Transaction Limit(Fixed): 50000.00\n")
                append(" - TTQ(Fixed): 36004000\n")
                append(" - Limit Switch(Fixed): FE00\n")
            }
            sharedVm.triggerAppParamsLoadedRefresh(UNION_PAY_PICC)
        }.onFailure {
            binding.tvInfo.text = it.message
            it.printStackTrace()
        }
    }

    private fun onAddVisaPiccButtonClicked() {
        sharedVm.triggerAppParamsLoadedRefresh(VISA_PICC)
    }

    private fun onAddMasterCardPiccButtonClicked() {
        sharedVm.triggerAppParamsLoadedRefresh(MASTER_CARD_PICC)

    }

    private fun onClearAllAidButtonClicked() {
        runCatching {
            mEmvKernelManager.updateAID(ContantPara.Operation.CLEAR, null)
        }.onSuccess {
            Toast.makeText(requireContext(), "Clear All AIDs successfully", Toast.LENGTH_SHORT).show()
            binding.tvInfo.text = ""
            sharedVm.triggerAppParamsClearRefresh()
        }.onFailure {
            binding.tvInfo.text = it.message
            it.printStackTrace()
        }
    }
}
