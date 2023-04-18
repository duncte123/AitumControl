package me.duncte123.aitumcontrol

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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

// https://developer.android.com/guide/components/activities/activity-lifecycle
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val hiddenRuleIds = mutableListOf<String>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var aitumNSD: AitumNSD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setStatusText("$RED Waiting for Aitum....")

        recyclerView = findViewById(R.id.rule_list)
    }

    override fun onStart() {
        super.onStart()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        onSharedPreferenceChanged(preferences, null)

        // TODO: remove this, clears all preferences
        /*preferences.edit {
            clear().commit()
        }*/

        val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        aitumNSD = AitumNSD(nsdManager, this) {
            // When the rules are updated
            runOnUiThread {
                resetRuleAdapter()
            }
        }

        val layoutManager = FlexboxLayoutManager(this)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.SPACE_AROUND
        layoutManager.alignItems = AlignItems.CENTER

        recyclerView.layoutManager = layoutManager

        resetRuleAdapter()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun resetRuleAdapter() {
        val filteredRules = aitumNSD.allRules.filter { !hiddenRuleIds.contains(it.id) }

        recyclerView.adapter = RuleButtonAdapter(filteredRules) {
            aitumNSD.executeRule(it.id)
        }
    }

    // Don't unregister the settings listener here, as we pause during settings screen
    override fun onPause() {
        aitumNSD.stopDiscovery()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        aitumNSD.startDiscovery()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        aitumNSD.stopDiscovery()
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    fun setStatusText(newText: String) {
       runOnUiThread {
           Log.d("Status_Text", newText)
           findViewById<TextView>(R.id.status_text).text = newText
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

        val hiddenRulesPref = prefs.getStringSet("hidden_rules", setOf()) ?: return
        val hiddenRulesPrefList = hiddenRulesPref.toList()

        if (hiddenRulesPrefList == hiddenRuleIds) {
            Log.d("Preferences", "IS SAME")
            return
        }

        Log.d("Preferences", "====================================================")
        Log.d("Preferences", hiddenRulesPrefList.toString())
        Log.d("Preferences", "====================================================")

        hiddenRuleIds.clear()
        hiddenRuleIds.addAll(hiddenRulesPrefList)

        if (this::recyclerView.isInitialized) {
            resetRuleAdapter()
        }
    }

    companion object {
        const val GREEN = "\uD83D\uDFE2"
        const val RED = "\uD83D\uDD34"
        const val PEBBLE = "_pebble._tcp."

        const val INTENT_RULES_KEY = "rules_json"
    }
}