package dji.sampleV5.moduledrone.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.models.RTKCenterVM
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkmobilestation.RTKLocation
import dji.sdk.keyvalue.value.rtkmobilestation.RTKSatelliteInfo
import dji.v5.manager.aircraft.rtk.RTKLocationInfo
import dji.v5.manager.aircraft.rtk.RTKSystemState
import dji.v5.utils.common.LogUtils
import dji.v5.ux.core.util.GpsUtils

import kotlinx.android.synthetic.main.frag_rtk_center_page.*


/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/3/19
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKCenterFragment : DJIFragment() {
    private val TAG = LogUtils.getTag("RTKCenterFragment")
    private val rtkCenterVM: RTKCenterVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_rtk_center_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListener()

        //只要打开RTK开关才会显示相关功能界面
        rtk_open_state_radio_group.setOnCheckedChangeListener { _, checkedId ->
            val rtkOpen = rtkCenterVM.getAircraftRTKModuleEnabledLD.value
            if (checkedId == R.id.btn_enable_rtk) {
                LogUtils.d(TAG, "RTK已开启")
                rl_rtk_all.visible()
                //避免重复开启
                if (rtkOpen?.data ==false) {
                    rtkCenterVM.setAircraftRTKModuleEnabled(true)
                }

            } else {
                LogUtils.d(TAG, "RTK已关闭")
                rl_rtk_all.gone()
                rtkCenterVM.setAircraftRTKModuleEnabled(false)
            }

            //每次开启或者关闭RTK模块后，都需要再次获取其开启状态
            rtkCenterVM.getAircraftRTKModuleEnabled()
        }
        //选择RTK服务类型并展示相关界面，默认选择基站RTK
        rtk_source_radio_group.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btn_rtk_source_base_rtk -> {
                    LogUtils.d(TAG, "切换到基站RTK")
                    //这里只切换UI，真正的切源操作放到具体页面
                    updateRTKUI(RTKReferenceStationSource.BASE_STATION)
                }
                R.id.btn_rtk_source_network -> {
                    LogUtils.d(TAG, "切换到网络RTK")
                    updateRTKUI(RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE)
                }
                R.id.btn_rtk_source_qx -> {
                    LogUtils.d(TAG, "切换到千寻RTK")
                    updateRTKUI(RTKReferenceStationSource.QX_NETWORK_SERVICE)
                }
            }
        }

        //处理打开相关RTK逻辑
        bt_open_network_rtk.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_network_trk_pag)
        }
        bt_open_rtk_station.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_rtk_station_page)
        }

        //监听数据源
        rtkCenterVM.addRTKLocationInfoListener()
        rtkCenterVM.addRTKSystemStateListener()
        rtkCenterVM.getAircraftRTKModuleEnabled()
        rtkCenterVM.getRTKReferenceStationSource()

    }

    private fun initListener() {
        rtkCenterVM.setAircraftRTKModuleEnableLD.observe(viewLifecycleOwner, {
            it.isSuccess.processResult("Set successfully", "Set failed")
        })

        rtkCenterVM.getAircraftRTKModuleEnabledLD.observe(viewLifecycleOwner, {
            LogUtils.d(TAG,"RTK开启状态：${it.data}")
            if (it?.data==true) {
                btn_enable_rtk.isChecked = true
                tv_rtk_enable.text="RTK is on"
            } else {
                btn_disable_rtk.isChecked = true
                tv_rtk_enable.text="RTK is off"
            }
        })
        rtkCenterVM.setRTKReferenceStationSourceLD.observe(viewLifecycleOwner, {
            it.isSuccess.processResult("Switch RTK service type successfully", "Switch RTK service type failed")
        })
        rtkCenterVM.rtkLocationInfoLD.observe(viewLifecycleOwner, {
            showRTKInfo(it.data)
        })
        rtkCenterVM.rtkSystemStateLD.observe(viewLifecycleOwner, {
            showRTKSystemStateInfo(it.data)
        })
        rtkCenterVM.rtkSourceLD.observe(viewLifecycleOwner, {
            it?.run {
                updateRTKUI(data)
            }
        })
    }

    private fun updateRTKUI(rtkReferenceStationSource: RTKReferenceStationSource?) {
        when (rtkReferenceStationSource) {
            RTKReferenceStationSource.BASE_STATION -> {
                bt_open_rtk_station.visible()
                bt_open_network_rtk.gone()
                rl_rtk_info_show.visible()
                btn_rtk_source_base_rtk.isChecked = true
            }
            RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE -> {
                bt_open_rtk_station.gone()
                bt_open_network_rtk.visible()
                rl_rtk_info_show.visible()
                btn_rtk_source_network.isChecked = true
            }
            RTKReferenceStationSource.QX_NETWORK_SERVICE -> {
                //待实现
            }
            else -> {
                ToastUtils.showToast("Current rtk reference station source is:$rtkReferenceStationSource")
            }
        }

    }

    private fun showRTKSystemStateInfo(rtkSystemState: RTKSystemState?) {
        rtkSystemState?.run {
            rtkConnected?.let {
                tv_tv_rtk_connect_info.text = if (rtkConnected) {
                    "Connected"
                } else {
                    "Disconnected"
                }
            }
            rtkHealthy?.let {
                tv_rtk_healthy_info.text = if (rtkHealthy) {
                    "healthy"
                } else {
                    "unhealthy"
                }
            }
            tv_rtk_error_info.text = error?.toString()
            //展示卫星数
            showSatelliteInfo(satelliteInfo)
        }
    }

    private fun showRTKInfo(rtkLocationInfo: RTKLocationInfo?) {
        rtkLocationInfo?.run {
            tv_trk_location_strategy.text = rtkLocation?.positioningSolution?.name
            tv_rtk_station_position_info.text = rtkLocation?.baseStationLocation?.toString()
            tv_rtk_mobile_position_info.text = rtkLocation?.mobileStationLocation?.toString()
            tv_rtk_position_std_distance_info.text = getRTKLocationDistance(rtkLocation)?.toString()
            tv_rtk_std_position_info.text = "stdLongitude:${rtkLocation?.stdLongitude}" +
                    ",stdLatitude:${rtkLocation?.stdLatitude}" +
                    ",stdAltitude=${rtkLocation?.stdAltitude}"


            tv_rtk_head_info.text = rtkHeading?.toString()
            tv_rtk_real_head_info.text = realHeading?.toString()
            tv_rtk_real_location_info.text = real3DLocation?.toString()


        }

    }

    private fun showSatelliteInfo(rtkSatelliteInfo: RTKSatelliteInfo?) {
        rtkSatelliteInfo?.run {
            var baseStationReceiverInfo = ""
            var mobileStationReceiver2Info = ""
            var mobileStationReceiver1Info = ""
            for (receiver1 in rtkSatelliteInfo.mobileStationReceiver1Info) {
                mobileStationReceiver1Info += "${receiver1.type.name}:${receiver1.count};"
            }
            for (receiver2 in rtkSatelliteInfo.mobileStationReceiver2Info) {
                mobileStationReceiver2Info += "${receiver2.type.name}:${receiver2.count};"
            }
            for (receiver3 in rtkSatelliteInfo.baseStationReceiverInfo) {
                baseStationReceiverInfo += "${receiver3.type.name}:${receiver3.count};"
            }

            tv_rtk_antenna_1_info.text = mobileStationReceiver1Info
            tv_rtk_antenna_2_info.text = mobileStationReceiver2Info
            tv_rtk_station_info.text = baseStationReceiverInfo

        }
    }


    private fun View.visible() {
        this.visibility = View.VISIBLE
    }

    private fun View.gone() {
        this.visibility = View.GONE
    }


    private fun Boolean.processResult(
        positiveMsg: String,
        negativeMsg: String,
        textView: TextView? = null
    ) {
        textView?.run {
            text = if (this@processResult) {
                positiveMsg
            } else {
                negativeMsg
            }
            return@processResult
        }
        if (this) {
            ToastUtils.showToast(positiveMsg)
        } else {
            ToastUtils.showToast(negativeMsg)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rtkCenterVM.clearAllRTKLocationInfoListener()
        rtkCenterVM.clearAllRTKSystemStateListener()
    }

    private fun getRTKLocationDistance(rtklocation: RTKLocation?): Double? {
        rtklocation?.run {
            baseStationLocation?.let { baseStationLocation ->
                mobileStationLocation?.let { mobileStationLocation ->
                    return GpsUtils.distance3D(baseStationLocation, mobileStationLocation)
                }
            }
        }
        return null
    }

}