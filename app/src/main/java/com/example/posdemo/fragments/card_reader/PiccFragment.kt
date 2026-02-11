package com.example.posdemo.fragments.card_reader

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.example.posdemo.CardReaderActivity
import com.example.posdemo.R
import com.example.posdemo.databinding.FragmentPiccBinding
import com.example.posdemo.utils.CardUtil
import com.urovo.sdk.rfcard.listener.RFSearchListener
import com.urovo.sdk.rfcard.utils.CardInfo
import com.urovo.utils.BytesUtil


class PiccFragment : Fragment() {
    companion object {
        private const val SELECT_PPSE = "00A404000E325041592E5359532E444446303100"
    }

    private var _binding: FragmentPiccBinding? = null
    private val binding
        get() = _binding!!

    private val mRfCardReaderManager
        get() = (requireActivity() as CardReaderActivity).mRfCardReaderManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPiccBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            btnStartSearchPicc.setOnClickListener { onStartSearchPiccButtonClicked() }
            btnStopSearchPicc.setOnClickListener { onStopSearchPiccButtonClicked() }
            btnSelectPpse.setOnClickListener { onSelectPpseButtonClicked() }
            btnSelectAid.setOnClickListener { onSelectAidButtonClicked() }

            btnStartSearchPicc.isEnabled = true
            btnStopSearchPicc.isEnabled = false
            btnSelectPpse.isEnabled = false
            btnSelectAid.isEnabled = false

            etPiccAid.doOnTextChanged { text, _, _, _ ->
                binding.btnSelectAid.isEnabled = etPiccAid.text.toString().isNotEmpty()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mRfCardReaderManager.stopSearch()
    }

    private fun onStartSearchPiccButtonClicked() {
        runCatching {
            mRfCardReaderManager.searchCard(object : RFSearchListener{
                override fun onCardPass(
                    cardType: Int,
                    uid: ByteArray?,
                    cardInfo: CardInfo?
                ) {
                    requireActivity().runOnUiThread {
                        binding.tvResult.text = buildString {
                            if(BytesUtil.bytes2HexString(cardInfo?.ATQA) != null) {
                                append("Card Type: \nType A - ${CardUtil.parseCardType(cardType)}\n\n")
                            } else {
                                append("Card Type: Type B - ${CardUtil.parseCardType(cardType)}\n\n")
                            }
                            append("UID(Max 4Bytes, might not be Complete):\n${BytesUtil.bytes2HexString(uid)}\n\n")
                            append("${CardUtil.parseSak(cardInfo?.SAK?.toInt() ?: 0)}")
                        }
                        binding.apply {
                            btnStartSearchPicc.isEnabled = false
                            btnStopSearchPicc.isEnabled = true
                            btnSelectPpse.isEnabled = true
                            btnSelectAid.isEnabled = true
                        }
                    }
                    startDetectCardStatusChanged()
                }
                override fun onFail(error: Int, message: String?) {
                    requireActivity().runOnUiThread {
                        binding.tvResult.text = buildString {
                            append("searchCard() onFail:\n")
                            append("error=$error\n")
                            append("message=$message")
                        }
                    }
                }
            })
        }.onSuccess {
            Toast.makeText(requireContext(), "Searching PICC...", Toast.LENGTH_SHORT).show()
            binding.apply {
                btnStartSearchPicc.isEnabled = false
                btnStopSearchPicc.isEnabled = true
                btnSelectPpse.isEnabled = false
                btnSelectAid.isEnabled = false
                tvResult.text = ""
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onStopSearchPiccButtonClicked() {
        runCatching {
            mRfCardReaderManager.stopSearch()
        }.onSuccess {
            Toast.makeText(requireContext(), "Stop searching successfully", Toast.LENGTH_SHORT).show()
            binding.apply {
                btnStartSearchPicc.isEnabled = true
                btnStopSearchPicc.isEnabled = false
                btnSelectPpse.isEnabled = false
                btnSelectAid.isEnabled = false
                etPiccAid.setText("")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onSelectPpseButtonClicked() {
        runCatching {
            return@runCatching mRfCardReaderManager.exchangeApdu(BytesUtil.hexString2Bytes(SELECT_PPSE)) ?: throw Exception("SELECT PPSE failed")
        }.onSuccess { rspData ->
            Toast.makeText(requireContext(), "SELECT PPSE successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("APDU received after SELECT_PPSE:\n\n")
                append("${BytesUtil.bytes2HexString(rspData)}")
            }
            val selectAidCommand = buildString {
                append("00A40400")
                append(CardUtil.extractTlvLenHex(com.urovo.sdk.utils.BytesUtil.bytes2HexString(rspData), "4F"))
                append(CardUtil.extractTlvValue(com.urovo.sdk.utils.BytesUtil.bytes2HexString(rspData), "4F"))
                append("00")
            }
            binding.etPiccAid.setText(selectAidCommand)
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun onSelectAidButtonClicked() {
        runCatching {
            return@runCatching mRfCardReaderManager.exchangeApdu(BytesUtil.hexString2Bytes(binding.etPiccAid.text.toString())) ?: throw Exception("SELECT AID failed")
        }.onSuccess { rspData ->
            Toast.makeText(requireContext(), "SELECT AID successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = buildString {
                append("APDU received after SELECT_AID:\n\n")
                append("${BytesUtil.bytes2HexString(rspData)}")
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private fun startDetectCardStatusChanged() {
        Thread {
            while (mRfCardReaderManager.isExist) {
                Thread.sleep(50)
            }
            requireActivity().runOnUiThread {
                onStopSearchPiccButtonClicked()
            }
        }.start()
    }

}

/*
    PICC(CL) APDU flow:
    1. SELECT PPSE(00A40400 / 3250) -> 6F(P)+4F/50/87
    2. SELECT AID(00A40400 / AID) -> 6F(P)+9F38
    3. GPO(80A8 / PDOL) -> 80(P) + AIP/AFL
    4. READ RECORD(00B201/02/03/...) -> 70(P) + TLVs
 */