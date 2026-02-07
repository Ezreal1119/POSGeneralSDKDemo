package com.example.posgeneralsdkdemo

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.posgeneralsdkdemo.databinding.ActivityEmvBinding
import com.example.posgeneralsdkdemo.fragments.emv.AppParamsFragment
import com.example.posgeneralsdkdemo.fragments.emv.HomeFragment
import com.example.posgeneralsdkdemo.fragments.emv.TerminalParamsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.urovo.i9000s.api.emv.EmvNfcKernelApi

// <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>

const val PREFS_NAME = "emv_prefs"

class EmvActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmvBinding

    val mEmvKernelManager: EmvNfcKernelApi by lazy { EmvNfcKernelApi.getInstance(this) }

    val sharedPreferences: SharedPreferences by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmvBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = MainPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "EMV"
                1 -> "TermParams"
                2 -> "AppParams"
                else -> "EMV"
            }
        }.attach()
    }
}
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> TerminalParamsFragment()
            2 -> AppParamsFragment()
            else -> HomeFragment()
        }
    }
}



