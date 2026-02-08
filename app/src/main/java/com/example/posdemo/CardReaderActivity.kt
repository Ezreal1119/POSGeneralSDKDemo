package com.example.posdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.posdemo.fragments.card_reader.IcCardFragment
import com.example.posdemo.fragments.card_reader.MagCardFragment
import com.example.posdemo.databinding.ActivityCardReaderBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.urovo.sdk.insertcard.InsertCardHandlerImpl
import com.urovo.sdk.magcard.MagCardReaderImpl

class CardReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCardReaderBinding

    val mICCardReaderManager = InsertCardHandlerImpl.getInstance()
    val mMagCardReaderManager = MagCardReaderImpl.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPagerCardReader.adapter = CardReaderPagerAdapter(this)
        binding.viewPagerCardReader.offscreenPageLimit = 1
        TabLayoutMediator(binding.tabLayoutCardReader, binding.viewPagerCardReader) { tab, position ->
            tab.text = when (position) {
                0 -> "ICCard"
                1 -> "MagCard"
                else -> ""
            }
        }.attach()
    }
}

class CardReaderPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> IcCardFragment()
            1 -> MagCardFragment()
            else -> IcCardFragment()
        }
    }


}