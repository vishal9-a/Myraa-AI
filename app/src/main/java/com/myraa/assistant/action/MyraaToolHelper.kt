package com.myraa.assistant.action

import org.json.JSONArray
import org.json.JSONObject

object MyraaToolHelper {
    fun functionDecl(
        name: String,
        description: String,
        properties: Map<String, String>,
        required: List<String> = emptyList()
    ): JSONObject {
        val decl = JSONObject()
        decl.put("name", name)
        decl.put("description", description)

        val parameters = JSONObject()
        parameters.put("type", "OBJECT")

        val props = JSONObject()
        for ((propName, propDesc) in properties) {
            val propObj = JSONObject()
            propObj.put("type", "STRING")
            propObj.put("description", propDesc)
            props.put(propName, propObj)
        }
        parameters.put("properties", props)

        if (required.isNotEmpty()) {
            val reqArray = JSONArray()
            for (req in required) {
                reqArray.put(req)
            }
            parameters.put("required", reqArray)
        }

        decl.put("parameters", parameters)
        return decl
    }
}
