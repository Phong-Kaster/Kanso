package com.photo.editor.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkManager {
    private lateinit var connectivityManager: ConnectivityManager

    /** Set of valid networks which are connected to internet */
    private val validNetworks by lazy { mutableSetOf<Network>() }
    private val _networkConnectedFlow by lazy { MutableStateFlow(true) }
    val networkConnectedFlow by lazy { _networkConnectedFlow.asStateFlow() }
    val isNetworkConnected: Boolean
        get() = networkConnectedFlow.value

    /**
     * Initialize this singleton object,
     * should be called in application class with injected SettingsRepository */
    fun init(context: Context){
        _networkConnectedFlow.value = initialNetworkConnected(context)
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                    validNetworks.add(network)
                }
                checkValidNetworks()
            }

            override fun onLost(network: Network) {
                validNetworks.remove(network)
                checkValidNetworks()
            }
        })
    }

    /** Check if device is connected to internet
     * This is called once when init function is called to get initial value */
    private fun initialNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun checkValidNetworks() {
        _networkConnectedFlow.value = validNetworks.size > 0
    }
}