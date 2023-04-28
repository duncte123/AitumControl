package me.duncte123.aitumcontrol

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import me.duncte123.aitumcontrol.models.Rule
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.net.InetAddress

class AitumNSD(
    private val nsdManager: NsdManager, private val main: MainActivity,
    private val rulesUpdated: () -> Unit
) {
    val allRules = mutableListOf<Rule>()
    private var workerIP = ""
    private val aitumBase: String
        get() = "http://$workerIP:7777"
    private val httpClient = OkHttpClient()
    private var aitumConnected = false
    private var aitumName = ""

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d("NSD", "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d("NSD", "Service discovery success $service")

            if (service.serviceType == MainActivity.PEBBLE) {
                main.setStatusText("${MainActivity.RED} Connecting....")
                aitumName = service.serviceName
                nsdManager.resolveService(service, resolveListener)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.d("NSD", "service lost: $service")
            main.setStatusText("${MainActivity.RED} Disconnected.")
            aitumConnected = false
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d("NSD", "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("NSD", "Discovery failed: Error code:$errorCode")
            main.setStatusText("${MainActivity.RED} Failed to connect")
            stopDiscovery()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("NSD", "Discovery failed: Error code:$errorCode")
            main.setStatusText("${MainActivity.RED} Failed to connect")
            stopDiscovery()
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e("NSD", "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d("NSD", "Resolve Succeeded. $serviceInfo")

            if (serviceInfo.serviceName == aitumName) {
                val port: Int = serviceInfo.port
                val host: InetAddress = serviceInfo.host

                workerIP = host.hostAddress!!

                connectToAitum()
                Log.d("NSD", "AITUM: $host:$port")
            }
        }
    }

    fun loadFakeRules() {
        this.allRules.addAll(listOf(
            Rule("SFX: Honk", "1"),
            Rule("SFX: Discord Ping", "2"),
            Rule("Scene: starting soon", "3"),
            Rule("Scene: gaming", "4"),
            Rule("Scene: just chatting", "5"),
            Rule("Scene: brb", "6"),
            Rule("Scene: ending stream", "7"),
            Rule("I needed one more", "8"),
        ))
    }

    fun startDiscovery() {
        nsdManager.discoverServices(MainActivity.PEBBLE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        try {
            // Will throw if the listener is nog registered
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (ignored: Exception) {}
        httpClient.connectionPool.evictAll()
    }

    fun executeRule(ruleId: String) {
        if (BuildConfig.USE_FAKE_RULES) {
            return
        }

        val request = Request.Builder()
            .url("$aitumBase/aitum/rules/$ruleId")
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    private fun connectToAitum() {
        val request = Request.Builder()
            .url("$aitumBase/aitum/rules")
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                main.setStatusText("${MainActivity.RED} ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                allRules.clear()

                response.use { resp ->
                    resp.body?.use {
                        val rw = it.string()
                        val json = JSONObject(rw)

                        if ("OK" != json.getString("status")) {
                            // Not connected
                            return
                        }

                        val data = json.getJSONObject("data")

                        data.keys().forEach { key ->
                            allRules.add(
                                Rule(key, data.getString(key))
                            )
                        }

                        aitumConnected = true

                        main.setStatusText("${MainActivity.GREEN} Connected to Aitum")
                        rulesUpdated()
                    }
                }
            }
        })
    }
}