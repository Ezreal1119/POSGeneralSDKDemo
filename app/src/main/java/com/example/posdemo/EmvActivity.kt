package com.example.posdemo

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.posdemo.fragments.emv.AppParamsFragment
import com.example.posdemo.fragments.emv.EmvHomeFragment
import com.example.posdemo.fragments.emv.TerminalParamsFragment
import com.example.posdemo.databinding.ActivityEmvBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.urovo.i9000s.api.emv.EmvNfcKernelApi
import com.urovo.sdk.pinpad.PinPadProviderImpl

// <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>

const val PREFS_NAME = "emv_prefs"

class EmvActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmvBinding

    val mEmvKernelManager: EmvNfcKernelApi by lazy { EmvNfcKernelApi.getInstance(this) }
    val mPinpadManager: PinPadProviderImpl by lazy { PinPadProviderImpl.getInstance() }
    val sharedPreferences: SharedPreferences by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPagerEmv.adapter = EmvPagerAdapter(this)
        binding.viewPagerEmv.offscreenPageLimit = 2
        TabLayoutMediator(binding.tabLayoutEmv, binding.viewPagerEmv) { tab, position ->
            tab.text = when (position) {
                0 -> "EMV"
                1 -> "TermParams"
                2 -> "AppParams"
                else -> "EMV"
            }
        }.attach()
    }
}
class EmvPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> EmvHomeFragment()
            1 -> TerminalParamsFragment()
            2 -> AppParamsFragment()
            else -> EmvHomeFragment()
        }
    }
}



