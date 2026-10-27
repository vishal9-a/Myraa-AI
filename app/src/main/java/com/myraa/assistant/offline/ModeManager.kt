package com.myraa.assistant.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.myraa.assistant.data.NovaPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ModeManager {
    private const val KEY_ALWAYS_OFFLINE = "always_offline_mode"
    private const val KEY_HANDS_FREE = "hands_free_wake_word_enabled"
    private const val KEY_WAKE_SENSITIVITY = "wake_word_sensitivity"

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isAlwaysOffline = MutableStateFlow(false)
    val isAlwaysOffline: StateFlow<Boolean> = _isAlwaysOffline.asStateFlow()

    private val _isHandsFreeEnabled = MutableStateFlow(false)
    val isHandsFreeEnabled: StateFlow<Boolean> = _isHandsFreeEnabled.asStateFlow()

    private val _sensitivity = MutableStateFlow(WakeSensitivity.MEDIUM)
    val sensitivity: StateFlow<WakeSensitivity> = _sensitivity.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        _isAlwaysOffline.value = NovaPreferences.getBoolean(context, KEY_ALWAYS_OFFLINE, false)
        _isHandsFreeEnabled.value = NovaPreferences.getBoolean(context, KEY_HANDS_FREE, false)
        val sensStr = NovaPreferences.getString(context, KEY_WAKE_SENSITIVITY, WakeSensitivity.MEDIUM.name)
        _sensitivity.value = try { WakeSensitivity.valueOf(sensStr) } catch (_: Exception) { WakeSensitivity.MEDIUM }

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val currentNetwork = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(currentNetwork)
                _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                    }
                })
            }
        } catch (_: Exception) {
            _isOnline.value = true
        }
    }

    fun setAlwaysOffline(context: Context, enabled: Boolean) {
        _isAlwaysOffline.value = enabled
        NovaPreferences.putBoolean(context, KEY_ALWAYS_OFFLINE, enabled)
    }

    fun setHandsFreeEnabled(context: Context, enabled: Boolean) {
        _isHandsFreeEnabled.value = enabled
        NovaPreferences.putBoolean(context, KEY_HANDS_FREE, enabled)
        if (enabled) {
            MyraaListenService.start(context)
        } else {
            MyraaListenService.stop(context)
        }
    }

    fun setSensitivity(context: Context, s: WakeSensitivity) {
        _sensitivity.value = s
        NovaPreferences.putString(context, KEY_WAKE_SENSITIVITY, s.name)
    }

    fun shouldUseOfflineBrain(): Boolean {
        return _isAlwaysOffline.value || !_isOnline.value
    }
}
