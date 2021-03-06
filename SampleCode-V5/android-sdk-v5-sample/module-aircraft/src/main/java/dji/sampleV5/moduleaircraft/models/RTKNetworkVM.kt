package dji.sampleV5.moduleaircraft.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import dji.rtk.CoordinateSystem
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.key.RtkBaseStationKey
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState
import dji.sdk.keyvalue.value.rtkmobilestation.RTKLocation
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.manager.aircraft.rtk.network.INetworkServiceInfoListener
import dji.v5.utils.common.DjiSharedPreferencesManager
import dji.v5.utils.common.LogUtils

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/7/23
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class RTKNetworkVM : DJIViewModel() {
    companion object {
        const val CUSTOM_RTK_SETTING_CACHE = "customRTKSettingChache"
    }
    val currentRTKState = MutableLiveData(RTKServiceState.UNKNOWN)
    val currentRTKErrorMsg = MutableLiveData("")
    val currentCustomNetworkRTKSettings = MutableLiveData<RTKCustomNetworkSetting>()
    val currentQxNetworkCoordinateSystem = MutableLiveData<CoordinateSystem>()


    private val networkServiceInfoListener: INetworkServiceInfoListener = object :
        INetworkServiceInfoListener {
        override fun onServiceStateUpdate(state: RTKServiceState?) {
            state?.let {
                currentRTKState.value = state
            }
        }

        override fun onErrorCodeUpdate(code: IDJIError?) {
            code?.let {
                currentRTKErrorMsg.value = code.toString()
            }
        }
    }

    fun getCurrentCustomNetworkRTKSettingCache(context: Context?): String {
        val defaultSettings = RTKCustomNetworkSetting(
            "",
            0,
            "",
            "",
            ""
        )
        context?.let {
            return DjiSharedPreferencesManager.getString(context, CUSTOM_RTK_SETTING_CACHE, defaultSettings.toString());
        }
        return defaultSettings.toString()
    }

    override fun onCleared() {
        RTKCenter.getInstance().customRTKManager.removeNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        RTKCenter.getInstance().qxrtkManager.removeNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
    }

    fun addNetworkRTKServiceInfoCallback() {
        RTKCenter.getInstance().customRTKManager.addNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        RTKCenter.getInstance().qxrtkManager.addNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
    }


    // custom network
    fun startCustomNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().customRTKManager.startNetworkRTKService(callback)
    }

    fun stopCustomNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().customRTKManager.stopNetworkRTKService(callback)
    }

    fun setCustomNetworkRTKSettings(context: Context?, settings: RTKCustomNetworkSetting) {
        RTKCenter.getInstance().customRTKManager.customNetworkRTKSettings = settings
        currentCustomNetworkRTKSettings.value = RTKCenter.getInstance().customRTKManager.customNetworkRTKSettings
        context?.let {
            DjiSharedPreferencesManager.putString(it, CUSTOM_RTK_SETTING_CACHE, settings.toString())
        }
    }

    fun getCustomNetworkRTKSettings() {
        currentCustomNetworkRTKSettings.value = RTKCenter.getInstance().customRTKManager.customNetworkRTKSettings
    }

    //qx network
    fun startQXNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().qxrtkManager.startNetworkRTKService(callback)
    }

    fun stopQXNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().qxrtkManager.stopNetworkRTKService(callback)
    }

    fun setQXNetworkRTKCoordinateSystem(
        coordinateSystem: CoordinateSystem,
        callback: CommonCallbacks.CompletionCallback
    ) {
        RTKCenter.getInstance().qxrtkManager.setNetworkRTKCoordinateSystem(
            coordinateSystem,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    callback.onSuccess()
                    currentQxNetworkCoordinateSystem.value = coordinateSystem
                }

                override fun onFailure(error: IDJIError) {
                    callback.onFailure(error)
                }
            })
    }

    fun getQXNetworkRTKCoordinateSystem() {
        RTKCenter.getInstance().qxrtkManager.getNetworkRTKCoordinateSystem(object :
            CommonCallbacks.CompletionCallbackWithParam<CoordinateSystem> {
            override fun onSuccess(coordinateSystem: CoordinateSystem?) {
                coordinateSystem?.let {
                    currentQxNetworkCoordinateSystem.value = it
                }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.i(logTag, "onFailure $error")
            }
        })
    }
}