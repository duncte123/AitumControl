package me.duncte123.aitumcontrol

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val buttonToRuleId = mutableMapOf<Int, String>() // button_id TO aitum_rule_id
    // TODO: yell at david until he exposes pebble outside of the local network
    private val aitumIP = "http://192.168.1.182:7777"
    private val httpClient = OkHttpClient()
    private var aitumConnected = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        aitumConnected = false
        setStatusText("$RED Waiting for connection....")

        connectToAitum()
    }

    private fun setStatusText(newText: String) {
        findViewById<TextView>(R.id.status_text).text = newText
    }

    fun onRuleButtonPressed(view: View) {
        println(view.id)
        println("Rule ID to trigger: ${buttonToRuleId[view.id]}")

        if (!aitumConnected) {
            return
        }

        if (buttonToRuleId.containsKey(view.id)) {
            val ruleId = buttonToRuleId[view.id]!!
            executeRule(ruleId)
        }
    }

    private fun executeRule(ruleId: String) {
        val request = Request.Builder()
            .url("$aitumIP/aitum/rules/$ruleId")
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
            .url("$aitumIP/aitum/rules")
            .get()
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val ruleMap = mapOf(
                    // aitum_rule_name TO button_id
                    "Main cam" to R.id.cam_main,
                    "C920" to R.id.cam_c920,
                    "Room Cam" to R.id.cam_room
                )

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
                            if (ruleMap.containsKey(key)) {
                                buttonToRuleId[ruleMap[key]!!] = data.getString(key)
                            }
                        }

                        aitumConnected = true

                        runOnUiThread {
                            setStatusText("$GREEN Connected to Aitum")
                        }
                    }
                }
            }
        })
    }

    companion object {
        const val GREEN = "\uD83D\uDFE2"
        const val RED = "\uD83D\uDD34"
    }
}