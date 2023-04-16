package me.duncte123.aitumcontrol.models

import org.json.JSONObject

data class Rule(val name: String, val id: String) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("name", name)
            .put("id", id)
    }

    companion object {
        @JvmStatic
        fun fromJson(json: JSONObject): Rule {
            return Rule(
                json.getString("name"),
                json.getString("id")
            )
        }
    }

}

fun interface RuleClickHandler {
    fun invoke(rule: Rule)
}