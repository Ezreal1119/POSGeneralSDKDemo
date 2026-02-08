package com.example.posdemo.iccard

import android.device.IccManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.posdemo.databinding.ActivitySle4442Binding
import com.example.posdemo.fragments.sle.Sle4428Fragment
import com.example.posdemo.fragments.sle.Sle4442Fragment
import com.google.android.material.tabs.TabLayoutMediator


const val ICC_SLOT = 0x00
const val SLE_CARD_TYPE = 0x02
const val VOLTAGE_5V = 0x02
const val INDEX_8 = 8
const val INDEX_100 = 100
const val INDEX_900 = 900
const val PASSWORD_ALL_Fs = "FFFFFF"

// [0, 31] is Protected; [32 - 255] is Non-Protected
// Must verifyPassword before Writing and Changing password
class SleCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySle4442Binding

    val mIccManager = IccManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySle4442Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = SlePagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 1
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "SLE4442(256B)"
                1 -> "SLE4428(1024B)"
                else -> ""
            }
        }.attach()
    }


}

class SlePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> Sle4442Fragment()
            1 -> Sle4428Fragment()
            else -> Sle4442Fragment()
        }
    }


}