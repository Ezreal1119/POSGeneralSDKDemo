package com.example.posdemo.maps

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.example.posdemo.R
import com.example.posdemo.databinding.ActivityLocationBinding

class LocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationBinding
    private var mLocationClient: LocationClient? = null
    private var mLocationClientOption: LocationClientOption? = null

    private val mLocationListener = object : BDAbstractLocationListener() {
        override fun onReceiveLocation(location: BDLocation?) {
            runOnUiThread {
                binding.tvResult.text = buildString {
                    append("Country: ${location?.country}\n")
                    append("Province: ${location?.province}\n")
                    append("City: ${location?.city}\n")
                    append("District: ${location?.district}\n")
                    append("Street: ${location?.street}\n")
                    append("Address: ${location?.addrStr}\n")
                    append("Description: ${location?.locationDescribe}\n\n")

                    append("Latitude: ${location?.latitude}\n")
                    append("Longitude: ${location?.longitude}\n")
                    append("Preciseness: ${location?.radius}\n\n")

                    if (location?.floor != null) {
                        append("BuildingID: ${location.buildingID}\n")
                        append("BuildingName: ${location.buildingName}\n")
                        append("Floor: ${location.floor}\n")

                    }
                }
                binding.webViewLocation.post {
                    binding.webViewLocation.evaluateJavascript(
                        "showPoint(${location?.latitude}, ${location?.longitude});",
                        null
                    )
                }

            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.webViewLocation.settings.javaScriptEnabled = true
        binding.webViewLocation.settings.domStorageEnabled = true
        binding.webViewLocation.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        binding.webViewLocation.loadUrl("file:///android_asset/map.html")



        LocationClient.setAgreePrivacy(true)
        mLocationClient = LocationClient(applicationContext)
        mLocationClientOption = LocationClientOption()

        setOptions()
        mLocationClient?.registerLocationListener(mLocationListener)
        mLocationClient?.start()
    }

    override fun onStart() {
        super.onStart()
        binding.tvResult.text = "Searching..."
    }

    override fun onStop() {
        super.onStop()
        if (mLocationClient?.isStarted ?: false) {
            mLocationClient?.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationClient = null
        mLocationClientOption = null

    }


    private fun setOptions() {
        mLocationClientOption?.locationMode = LocationClientOption.LocationMode.Hight_Accuracy;
        //可选，设置定位模式，默认高精度
        //LocationMode.Hight_Accuracy：高精度；
        //LocationMode. Battery_Saving：低功耗；
        //LocationMode. Device_Sensors：仅使用设备；
        //LocationMode.Fuzzy_Locating, 模糊定位模式；v9.2.8版本开始支持，可以降低API的调用频率，但同时也会降低定位精度；

        mLocationClientOption?.setCoorType("bd09ll");
        //可选，设置返回经纬度坐标类型，默认gcj02
        //gcj02：国测局坐标；
        //bd09ll：百度经纬度坐标；
        //bd09：百度墨卡托坐标；
        //海外地区定位，无需设置坐标类型，统一返回wgs84类型坐标

        mLocationClientOption?.setFirstLocType(LocationClientOption.FirstLocType.SPEED_IN_FIRST_LOC)
        //可选，首次定位时可以选择定位的返回是准确性优先还是速度优先，默认为速度优先
        //可以搭配setOnceLocation(Boolean isOnceLocation)单次定位接口使用，当设置为单次定位时，setFirstLocType接口中设置的类型即为单次定位使用的类型
        //FirstLocType.SPEED_IN_FIRST_LOC:速度优先，首次定位时会降低定位准确性，提升定位速度；
        //FirstLocType.ACCUARACY_IN_FIRST_LOC:准确性优先，首次定位时会降低速度，提升定位准确性；

        mLocationClientOption?.setScanSpan(1000);
        //可选，设置发起定位请求的间隔，int类型，单位ms
        //如果设置为0，则代表单次定位，即仅定位一次，默认为0
        //如果设置非0，需设置1000ms以上才有效

        mLocationClientOption?.isOpenGnss = true;
        //可选，设置是否使用卫星定位，默认false
        //使用高精度和仅用设备两种定位模式的，参数必须设置为true

        mLocationClientOption?.isLocationNotify = true;
        //可选，设置是否当卫星定位有效时按照1S/1次频率输出卫星定位结果，默认false

        mLocationClientOption?.setIgnoreKillProcess(false);
        //可选，定位SDK内部是一个service，并放到了独立进程。
        //设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)

        mLocationClientOption?.SetIgnoreCacheException(false);
        //可选，设置是否收集Crash信息，默认收集，即参数为false

        mLocationClientOption?.setWifiCacheTimeOut(5*60*1000);
        //可选，V7.2版本新增能力
        //如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位

        mLocationClientOption?.setEnableSimulateGnss(false);
        //可选，设置是否需要过滤卫星定位仿真结果，默认需要，即参数为false

        mLocationClientOption?.setNeedNewVersionRgc(true);
        //可选，设置是否需要最新版本的地址信息。默认需要，即参数为true

        mLocationClientOption?.setIsNeedAddress(true);
        //可选，是否需要地址信息，默认为不需要，即参数为false
        //如果开发者需要获得当前点的地址信息，此处必须为true\\

        mLocationClientOption?.setIsNeedLocationDescribe(true);
        //可选，是否需要位置描述信息，默认为不需要，即参数为false
        //如果开发者需要获得当前点的位置信息，此处必须为true

        mLocationClient?.locOption = mLocationClientOption;
        //mLocationClient为第二步初始化过的LocationClient对象
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        //更多LocationClientOption的配置，请参照类参考中LocationClientOption类的详细说明
    }
}