package me.duncte123.aitumcontrol

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.duncte123.aitumcontrol.models.Rule
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress

// TODO: Implement settings to hide hide rules
class MainActivity : AppCompatActivity() {
    private val allRules = mutableListOf<Rule>()
    private var workerIP = ""
    private val aitumBase: String
        get() = "http://$workerIP:7777"

    private val httpClient = OkHttpClient()
    private var aitumConnected = false

    var aitumName = ""

    private lateinit var nsdManager: NsdManager

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d("NSD", "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d("NSD", "Service discovery success $service")

            if (service.serviceType == PEBBLE) {
                setStatusText("$RED Connecting....")
                aitumName = service.serviceName
                nsdManager.resolveService(service, resolveListener)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.d("NSD", "service lost: $service")
            setStatusText("$RED Disconnected.")
            aitumConnected = false
        }

        override fun onDiscoveryStopped(serviceType: String) {
//            setStatusText("$RED Disconnected.")
            Log.d("NSD", "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("NSD", "Discovery failed: Error code:$errorCode")
            setStatusText("$RED Failed to connect")
            stopDiscovery()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("NSD", "Discovery failed: Error code:$errorCode")
            setStatusText("$RED Failed to connect")
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
                // stopDiscovery()
            }
        }
    }

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        aitumConnected = false
        setStatusText("$RED Waiting for Aitum....")

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

        recyclerView = findViewById(R.id.rule_list)

        recyclerView.layoutManager = GridLayoutManager(this, 2) // TODO: based on screen width

        recyclerView.adapter = RuleButtonAdapter(allRules) {
            // Toast.makeText(this, "I clicked ${it.name}", Toast.LENGTH_SHORT).show()
            executeRule(it.id)
        }
    }

    private fun stopDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener)
        httpClient.connectionPool.evictAll()
    }

    override fun onPause() {
        stopDiscovery()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        nsdManager.discoverServices(PEBBLE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun onDestroy() {
        stopDiscovery()
        super.onDestroy()
    }

    private fun setStatusText(newText: String) {
       runOnUiThread {
           Log.d("Status_Text", newText)
           findViewById<TextView>(R.id.status_text).text = newText
       }
    }

    private fun executeRule(ruleId: String) {
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
                setStatusText("$RED Failed to connect: ${e.message}")
            }

            @SuppressLint("NotifyDataSetChanged")
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

                        setStatusText("$GREEN Connected to Aitum")

                        runOnUiThread {
                            recyclerView.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
        })
    }

    companion object {
        const val GREEN = "\uD83D\uDFE2"
        const val RED = "\uD83D\uDD34"
        const val PEBBLE = "_pebble._tcp."
    }
}