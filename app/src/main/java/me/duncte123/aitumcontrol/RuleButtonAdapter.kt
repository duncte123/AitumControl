package me.duncte123.aitumcontrol

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.duncte123.aitumcontrol.models.Rule
import me.duncte123.aitumcontrol.models.RuleClickHandler

class RuleButtonAdapter(
    private val selectedRules: List<Rule>,
    private val listener: RuleClickHandler
) : RecyclerView.Adapter<RuleButtonViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleButtonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.rule_button_fragment, parent, false)

        return RuleButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleButtonViewHolder, position: Int) {
        holder.bind(selectedRules[position], listener)
    }

    override fun getItemCount() = selectedRules.size
}