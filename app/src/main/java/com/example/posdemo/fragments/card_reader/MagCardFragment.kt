package com.example.posdemo.fragments.card_reader

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.posdemo.CardReaderActivity

import com.example.posdemo.databinding.FragmentMagCardBinding
import com.example.posdemo.enums.MagCardTag
import com.urovo.sdk.magcard.listener.MagCardListener

class MagCardFragment : Fragment() {
    companion object {
        private const val TIMEOUT = 30
    }

    private var _binding: FragmentMagCardBinding? = null
    private val binding
        get() = _binding!!

    private val mMagCardReaderManager
        get() = (requireActivity() as CardReaderActivity).mMagCardReaderManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMagCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStartSearch.setOnClickListener { onStartSearchButtonClicked() }
        binding.btnStopSearch.setOnClickListener { onStopSearchButtonClicked() }
        binding.btnStartSearch.isEnabled = true
        binding.btnStopSearch.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mMagCardReaderManager.stopSearch()
    }

    private fun onStartSearchButtonClicked() {
        runCatching {
            mMagCardReaderManager.searchCard(TIMEOUT, mMagCardListener)
        }.onSuccess {
            binding.btnStartSearch.isEnabled = false
            binding.btnStopSearch.isEnabled = true
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onStopSearchButtonClicked() {
        runCatching {
            mMagCardReaderManager.stopSearch()
        }.onSuccess {
            Toast.makeText(requireContext(), "Stop Searching successfully", Toast.LENGTH_SHORT).show()
            binding.tvResult.text = ""
            binding.btnStartSearch.isEnabled = true
            binding.btnStopSearch.isEnabled = false
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }

    private val mMagCardListener = object : MagCardListener {
        override fun onSuccess(track: Bundle?) {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Mag Card read successfully",
                    Toast.LENGTH_SHORT
                ).show()
                binding.tvResult.text = buildString {
                    append("Track 1: ${track?.getString(MagCardTag.TRACK1.value)}\n\n")
                    append("Track 2: ${track?.getString(MagCardTag.TRACK2.value)}\n\n")
                    append("Track 3: ${track?.getString(MagCardTag.TRACK3.value)}\n\n")
                    append("PAN: ${track?.getString(MagCardTag.PAN.value)}\n\n")
                    append("Service code: ${track?.getString(MagCardTag.SERVICE_CODE.value)}\n\n")
                    append("Expiry date: ${track?.getString(MagCardTag.EXPIRED_DATE.value)}\n\n")
                }
            }
            onStartSearchButtonClicked()
        }

        override fun onError(error: Int, message: String?) {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Mag Card read failed",
                    Toast.LENGTH_SHORT
                ).show()
                binding.btnStartSearch.isEnabled = true
                binding.btnStopSearch.isEnabled = false
                binding.tvResult.text = "onError: error=$error, message=$message"
            }
        }

        override fun onTimeout() {
            requireActivity().runOnUiThread {
                binding.btnStartSearch.isEnabled = true
                binding.btnStopSearch.isEnabled = false
                Toast.makeText(requireContext(), "onTimeout", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }
}