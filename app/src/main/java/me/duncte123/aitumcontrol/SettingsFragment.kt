package me.duncte123.aitumcontrol

import android.os.Bundle
import android.view.View
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat
import me.duncte123.aitumcontrol.models.Rule
import org.json.JSONArray

private const val ARG_ALLRULES = "allRules"

class SettingsFragment : PreferenceFragmentCompat() {
    private var allRules = mutableListOf<Rule>()

    // Methods are in order of execution
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val rulesJson = it.getString(ARG_ALLRULES) ?: return
            val jsonArray = JSONArray(rulesJson)

            for (i in 0 until jsonArray.length()) {
                allRules.add(
                    Rule.fromJson(jsonArray.getJSONObject(i))
                )
            }
        }

        println("========================================")
        println("All rules added")
        println("========================================")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hiddenRules = findPreference<MultiSelectListPreference>("hidden_rules")

        val ruleNames = allRules.map(Rule::name).toTypedArray()
        val ruleIds = allRules.map(Rule::id).toTypedArray()

        hiddenRules?.apply {
            entries = ruleNames
            entryValues = ruleIds
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(allRules: String?) = SettingsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ALLRULES, allRules)
            }
        }
    }
}