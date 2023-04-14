package me.duncte123.aitumcontrol

import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import me.duncte123.aitumcontrol.models.Rule
import me.duncte123.aitumcontrol.models.RuleClickHandler

class RuleButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val btn: Button

    init {
        btn = view.findViewById(R.id.rule_button)
    }

    fun bind(rule: Rule, listener: RuleClickHandler) {
        btn.text = rule.name

        btn.setOnClickListener {
            listener.invoke(rule)
        }
    }
}