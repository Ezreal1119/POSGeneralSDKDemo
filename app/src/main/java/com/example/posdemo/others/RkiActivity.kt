package com.example.posdemo.others

import android.content.Intent
import android.device.DeviceManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.posdemo.R
import com.example.posdemo.databinding.ActivityRkiBinding
import com.urovo.sdk.utils.BytesUtil

class RkiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRkiBinding

    private val rkiLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val status = result.data?.getIntExtra("status", -1)
            val message = result.data?.getStringExtra("message")
            val keyIndex = result.data?.getIntExtra("status", -1)
            binding.tvResult.text = buildString {
                append("onActivityResult: \n")
                append(" - status: $status\n")
                append(" - message: $message\n")
                append(" - keyIndex: $keyIndex\n\n")
                append("Main Code:\n")
                append("val intent = Intent().apply {\n" +
                        "    setClassName(\"com.ubx.dukpt\", \"com.ubx.dupktdownload.ui.remote.ExternalRemoteActivity\")\n" +
                        "    putExtra(\"IP_RKI\", binding.etKmsIp.text.toString())\n" +
                        "    putExtra(\"PORT_RKI\", binding.etKmsPort.text.toString())\n" +
                        "}\n" +
                        "rkiLauncher.launch(intent)\n\n")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRkiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRkiDownloadKey.setOnClickListener { onRkiDownloadKeyButtonClicked() }
        binding.btnCheckCaCert.setOnClickListener { onCheckCaCertButtonClicked() }
        binding.btnCheckPedCert.setOnClickListener { onCheckPedCertButtonClicked() }
        binding.btnCheckKdhCert.setOnClickListener { onCheckKdhCertButtonClicked() }

        binding.tvResult.text = "Download KMS_CA_Cert & PED_Cert:\n" +
                " - Generate KeyPair (RSA / ECC)\n" +
                " - Generate CSR(SN + PublicKey + Self-Signature)\n" +
                " - Retrieve KMS_CA_Cert & PED_Cert(instance issued) from KMS server\n" +
                "\n" +
                "Download KEY(KMS_IP + KMS_Port):\n" +
                "(1). mTLS:\n" +
                " - ClientHello(randonNumber1) - ServerHello(randonNumber2)\n" +
                " - ClientUploadCert(PED_Cert) - ServerUploadCert(KDH_Cert recovered using KMS_CA_Cert)\n" +
                " - ClientKeyExchange(signed_transcript) - ServerKeyExchange(signed_transcript) [Mutual Prove]\n" +
                " - FIN [Make sure same sharedKey - KBPK]\n" +
                "(2). Key Download:\n" +
                " - Client checks PED_Cert & KMS_CA_Cert exist or not.\n" +
                " - Server uses the KBPK to encapsulate the KEY into TR31[Header+Encrypted_KEY+MAC].\n" +
                " - Server signs the TR31 using it's own KDH private key\n" +
                " - Transmit in HTTP\n" +
                " - Client verify the signature using KDH's Cert. Make sure it's from KDH and TR31 not modified\n" +
                " - Recover the Actual Key using the KBPK"
    }

    private fun onRkiDownloadKeyButtonClicked() {
        runCatching {
            val intent = Intent().apply {
                setClassName("com.ubx.dukpt", "com.ubx.dupktdownload.ui.remote.ExternalRemoteActivity")
                putExtra("IP_RKI", binding.etKmsIp.text.toString())
                putExtra("PORT_RKI", binding.etKmsPort.text.toString())
            }
            rkiLauncher.launch(intent)
        }.onSuccess {
            Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show()
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onCheckCaCertButtonClicked() {
        runCatching {
            val cert = DeviceManager().readKMSCA()
            if (cert.size == 0) error("KMS_CA_CERT not Found! Please download first!")
            return@runCatching cert
        }.onSuccess { cert ->
            binding.tvResult.text = buildString {
                append("KMS_CA_CERT:\n\n")
                append(CertUtil.parseCert(cert))
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onCheckPedCertButtonClicked() {
        runCatching {
            val cert = DeviceManager().pedCrt
            if (cert.size == 0) error("KMS_CA_CERT not Found! Please download first!")
            return@runCatching cert
        }.onSuccess { cert ->
            binding.tvResult.text = buildString {
                append("PED_CERT(PinEntryDevice):\n\n")
                append(CertUtil.parseCert(cert))
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


    private fun onCheckKdhCertButtonClicked() {
        runCatching {
            val cert = DeviceManager().kdhCrt
            if (cert.size == 0) error("KMS_CA_CERT not Found! Please download first!")
            return@runCatching cert
        }.onSuccess { cert ->
            binding.tvResult.text = buildString {
                append("KDH_CERT(KMS):\n\n")
                append(CertUtil.parseCert(cert))
            }
        }.onFailure {
            binding.tvResult.text = it.message
            it.printStackTrace()
        }
    }


}