package me.duncte123.aitumcontrol.models

data class Rule(val name: String, val id: String)

fun interface RuleClickHandler {
    fun invoke(rule: Rule)
}