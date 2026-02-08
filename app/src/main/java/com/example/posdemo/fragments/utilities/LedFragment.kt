package com.example.posdemo.fragments.utilities

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.posdemo.UtilitiesActivity
import com.example.posdemo.databinding.FragmentLedBinding
import com.example.posdemo.enums.LedColor

class LedFragment : Fragment() {

    private var _binding: FragmentLedBinding? = null
    private val binding
        get() = _binding!!

    private val mLedManager
        get() = (requireActivity() as UtilitiesActivity).mLedManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnTurnOnLed.setOnClickListener { onTurnOnLedButtonClicked() }
        binding.btnTurnOffLed.setOnClickListener { onTurnOffLedButtonClicked() }
        binding.btnTurnOnAllLed.setOnClickListener { onTurnOnAllLedButtonClicked() }
        binding.btnTurnOffAllLed.setOnClickListener { onTurnOffAllLedButtonClicked() }

        binding.spLedColor.adapter = ArrayAdapter(requireContext(), simple_spinner_item, LedColor.entries).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onTurnOnLedButtonClicked() {
        runCatching {
            val color = binding.spLedColor.selectedItem as LedColor
            mLedManager.turnOn(color.id)
        }.onFailure {
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffLedButtonClicked() {
        runCatching {
            val color = binding.spLedColor.selectedItem as LedColor
            mLedManager.turnOff(color.id)
        }.onFailure {
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOnAllLedButtonClicked() {
        runCatching {
            for (i in 1..4) {
                mLedManager.turnOn(i)
            }
        }.onSuccess {
            Toast.makeText(requireContext(), "Turned on all LEDs successfully", Toast.LENGTH_SHORT).show()
        }.onFailure{
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }


    private fun onTurnOffAllLedButtonClicked() {
        runCatching {
            for (i in 1..4) {
                mLedManager.turnOff(i)
            }
        }.onSuccess {
            Toast.makeText(requireContext(), "Turned off all LEDs successfully", Toast.LENGTH_SHORT).show()
        }.onFailure{
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

}