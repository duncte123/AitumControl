package me.duncte123.aitumcontrol

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import me.duncte123.aitumcontrol.models.Rule
import org.json.JSONArray
import java.util.concurrent.Executors
import kotlin.Exception

// https://developer.android.com/guide/components/activities/activity-lifecycle
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val hiddenRuleIds = mutableListOf<String>()
    private val backgroundThread = Executors.newSingleThreadExecutor()

    private lateinit var recyclerView: RecyclerView
    private lateinit var statusText: TextView
    private lateinit var aitumNSD: AitumNSD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.rule_list)
        statusText = findViewById(R.id.status_text)
    }

    override fun onStart() {
        super.onStart()

        // Anything to make the app start faster :)
        backgroundThread.submit {
            try {
                setStatusText("$RED Waiting for Aitum....")

                val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager

                aitumNSD = AitumNSD(nsdManager, this) {
                    // When the rules are updated
                    resetRuleAdapter()
                }

                // Force own rules to be displayed
                if (BuildConfig.USE_FAKE_RULES) {
                    Log.d("Main", "Loading fake rules!!!")
                    aitumNSD.loadFakeRules()
                }

                val layoutManager = FlexboxLayoutManager(this)
                layoutManager.flexDirection = FlexDirection.ROW
                layoutManager.justifyContent = JustifyContent.SPACE_AROUND
                layoutManager.alignItems = AlignItems.CENTER

                runOnUiThread {
                    recyclerView.layoutManager = layoutManager
                }

                val preferences = PreferenceManager.getDefaultSharedPreferences(this)

                // calls resetRuleAdapter, needed before layout init
                onSharedPreferenceChanged(preferences, null)
            } catch (e: Exception) {
                Log.e("AitumControl", "Error setting up", e)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun resetRuleAdapter() {
        val filteredRules = aitumNSD.allRules.filter { !hiddenRuleIds.contains(it.id) }

        runOnUiThread {
            recyclerView.adapter = RuleButtonAdapter(filteredRules) {
                aitumNSD.executeRule(it.id)
            }
        }
    }

    // Don't unregister the settings listener here, as we pause during settings screen
    override fun onPause() {
        backgroundThread.submit {
            aitumNSD.stopDiscovery()
        }
        super.onPause()
    }

    override fun onResume() {
        backgroundThread.submit {
            aitumNSD.startDiscovery()
        }
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        backgroundThread.submit {
            aitumNSD.stopDiscovery()
        }
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    fun setStatusText(newText: String) {
        Log.d("Status_Text", newText)
        runOnUiThread {
            statusText.text = newText
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toolbar_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)

                val rulesJSON = JSONArray(aitumNSD.allRules.map(Rule::toJson))

                intent.putExtra(INTENT_RULES_KEY, rulesJSON.toString())
                startActivity(intent)

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        Log.d("Preferences", "DATASET IS CHANGED")

        val shouldKeepScreenOn = prefs.getBoolean("keep_device_on", false)

        runOnUiThread {
            if (shouldKeepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        val hiddenRulesPref = prefs.getStringSet("hidden_rules", setOf()) ?: return
        val hiddenRulesPrefList = hiddenRulesPref.toList()

        if (hiddenRulesPrefList == hiddenRuleIds) {
            Log.d("Preferences", "IS SAME")
            resetRuleAdapter()
            return
        }

        Log.d("Preferences", "====================================================")
        Log.d("Preferences", hiddenRulesPrefList.toString())
        Log.d("Preferences", "====================================================")

        hiddenRuleIds.clear()
        hiddenRuleIds.addAll(hiddenRulesPrefList)

        resetRuleAdapter()
    }

    companion object {
        const val GREEN = "\uD83D\uDFE2"
        const val RED = "\uD83D\uDD34"
        const val PEBBLE = "_pebble._tcp."

        const val INTENT_RULES_KEY = "rules_json"
    }
}