package me.duncte123.aitumcontrol

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.duncte123.aitumcontrol.MainActivity.Companion.INTENT_RULES_KEY
import me.duncte123.aitumcontrol.models.Rule

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // TODO get extra named "rules"

        val strArray = intent.getStringExtra(INTENT_RULES_KEY)

        println(strArray)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()

    }
}