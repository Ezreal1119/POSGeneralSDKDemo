package com.example.posdemo.tools

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import com.example.posdemo.R

class SoundTool private constructor(private val context: Context) { // Singleton design

    private var soundPool: SoundPool? = null
    private var successSoundId: Int = 0
    private var errorSoundId: Int = 0

    init {
        if (soundPool == null) {
            soundPool = SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0)
            successSoundId = soundPool?.load(context, R.raw.success, 1) ?: 0
            errorSoundId = soundPool?.load(context, R.raw.error, 1) ?: 0
        }
    }

    fun playSound(soundType: Int) {
        val pool = soundPool ?: return
        when (soundType) {
            SOUND_TYPE_SUCCESS -> pool.play(successSoundId, 1F, 1F, 1, 0, 1F)
            SOUND_TYPE_ERROR -> pool.play(errorSoundId, 1F, 1F, 1, 0, 1F)
        }
    }

    companion object { // Space to put all the static fields and methods; In kotlin, it's allowed to access outer class
        const val SOUND_TYPE_SUCCESS = 0
        const val SOUND_TYPE_ERROR = 1

        @SuppressLint("StaticFieldLeak")
        @Volatile // Make take effects instantly to avoid multi-newing
        private var mySound: SoundTool? = null

        fun getMySound(context: Context): SoundTool {
            return mySound ?: synchronized(this) { // If mySound is null, then lock the companion (No other places can use)
                mySound ?: SoundTool(context.applicationContext).also { // DCL(Double-Checked Locking), make sure mySound is null after waiting for the Lock
                    mySound = it
                }
            }
        }
    }

}