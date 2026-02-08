package com.example.posdemo.fragments.utilities

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.posdemo.UtilitiesActivity
import com.example.posdemo.databinding.FragmentCameraScanBinding
import com.example.posdemo.utils.PermissionUtil
import com.urovo.sdk.scanner.listener.ScannerListener
import com.urovo.sdk.scanner.utils.Constant
import com.urovo.sdk.utils.BytesUtil

class CameraScanFragment : Fragment() {

    companion object {
        private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val PERMISSION_REQ_SCAN = 1001
        private val cameraParams = Bundle().apply {
            putString(Constant.Scankey.title, "Patrick's Title")
            putString(Constant.Scankey.upPromptString, "This is a top Prompt")
            putString(Constant.Scankey.downPromptString, "This is a bottom Prompt")
        }
    }

    private var _binding: FragmentCameraScanBinding? = null
    private val binding
        get() = _binding!!

    private val mCameraManager
        get() = (requireActivity() as UtilitiesActivity).mCameraManager


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnFrontScan.setOnClickListener { onFrontScanButtonClicked() }
        binding.btnBackScan.setOnClickListener { onBackScanButtonClicked() }
    }

    private fun onFrontScanButtonClicked() {
        if (!PermissionUtil.requestPermissions(requireActivity(), PERMISSIONS, PERMISSION_REQ_SCAN)) {
            Toast.makeText(requireContext(), "Please grant camera permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            mCameraManager.startScan(requireContext(), cameraParams, Constant.CameraID.FRONT, 30, object: ScannerListener {
                override fun onSuccess(data: String?, byteData: ByteArray?) {
                    requireActivity().runOnUiThread {
                        binding.tvResult.text = buildString {
                            append("data in String:\n\n")
                            append(" - $data\n\n")
                            append("data in Bytes:\n\n")
                            append(" - ${BytesUtil.bytes2HexString(byteData)}")
                        }
                    }
                }
                override fun onError(error: Int, message: String?) {
                    requireActivity().runOnUiThread {
                        binding.tvResult.text = buildString {
                            append("Error onSuccess: \n\n - $error\n\n")
                            append("message: \n\n$message")
                        }
                    }
                }
                override fun onTimeout() {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "onTimeOut", Toast.LENGTH_SHORT).show() }
                }
                override fun onCancel() {
                    requireActivity(). runOnUiThread {
                        Toast.makeText(requireContext(), "onCancel", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }.onFailure {
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }

    private fun onBackScanButtonClicked() {
        if (!PermissionUtil.requestPermissions(requireActivity(), PERMISSIONS, PERMISSION_REQ_SCAN)) {
            Toast.makeText(requireContext(), "Please grant camera permission first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            mCameraManager.startScan(requireContext(), cameraParams, Constant.CameraID.BACK, 30, object: ScannerListener {
                override fun onSuccess(data: String?, byteData: ByteArray?) {
                    requireActivity().runOnUiThread {
                        binding.tvResult.text = buildString {
                            append("data in String:\n\n")
                            append(" - $data\n\n")
                            append("data in Bytes:\n\n")
                            append(" - ${BytesUtil.bytes2HexString(byteData)}")
                        }
                    }
                }
                override fun onError(error: Int, message: String?) {
                    requireActivity().runOnUiThread {
                        binding.tvResult.text = buildString {
                            append("Error onSuccess: \n$error")
                            append("\nmessage: \n$message")
                        }
                    }
                }
                override fun onTimeout() {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "onTimeOut", Toast.LENGTH_SHORT).show() }
                }
                override fun onCancel() {
                    requireActivity(). runOnUiThread {
                        Toast.makeText(requireContext(), "onCancel", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }.onFailure {
            Toast.makeText(requireContext() , it.message, Toast.LENGTH_SHORT).show()
            it.printStackTrace()
        }
    }
}