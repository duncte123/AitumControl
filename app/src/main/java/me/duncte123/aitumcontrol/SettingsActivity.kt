package me.duncte123.aitumcontrol

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import me.duncte123.aitumcontrol.MainActivity.Companion.INTENT_RULES_KEY

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)

        val strArray = intent.getStringExtra(INTENT_RULES_KEY)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment.newInstance(strArray))
            .commit()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (android.R.id.home == item.itemId) {
            onBackPressedDispatcher.onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }
}