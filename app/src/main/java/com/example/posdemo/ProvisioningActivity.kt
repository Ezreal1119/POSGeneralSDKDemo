package com.example.posdemo

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle

class ProvisioningActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val group = intent
            .getBundleExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
            ?.getString("group")
            ?: "default"

        getSharedPreferences("mdm", MODE_PRIVATE)
            .edit()
            .putString("group", group)
            .apply()

        when (intent.action) {
            DevicePolicyManager.ACTION_GET_PROVISIONING_MODE -> {
                val result = Intent().apply {
                    putExtra(
                        DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                        DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
                    )
                }
                setResult(RESULT_OK, result)
                finish()
            }

            DevicePolicyManager.ACTION_ADMIN_POLICY_COMPLIANCE -> {
                setResult(RESULT_OK)
                finish()
            }

            else -> {
                finish()
            }
        }
    }
}