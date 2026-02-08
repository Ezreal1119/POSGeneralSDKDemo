package com.example.posdemo.fragments.emv

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedVm : ViewModel() {
    private val _termParamsRefresh = MutableLiveData<Int>()
    val termParamsRefresh: LiveData<Int> = _termParamsRefresh

    private val _appParamsLoadedRefresh = MutableLiveData<String>()
    val appParamsLoadedRefresh: LiveData<String> = _appParamsLoadedRefresh

    private val _appParamsClearRefresh = MutableLiveData<Int>()
    val appParamsClearRefresh: LiveData<Int> = _appParamsClearRefresh


    fun triggerTermParamsRefresh() {
        _termParamsRefresh.value = (_termParamsRefresh.value ?: 0) + 1 // Counter from 0 to ...
    }
    fun triggerAppParamsLoadedRefresh(aid: String) {
        _appParamsLoadedRefresh.value = aid
    }
    fun triggerAppParamsClearRefresh() {
        _appParamsClearRefresh.value = (_appParamsClearRefresh.value ?: 0) + 1 // Counter from 0 to ...
    }
}