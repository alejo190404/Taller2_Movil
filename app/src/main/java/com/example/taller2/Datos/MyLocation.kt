package com.example.taller2.Datos

import org.json.JSONException
import org.json.JSONObject
import java.util.Date

class MyLocation(var fecha: Date, var latitud: Double, var
longitud: Double) {
    fun toJSON(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put("latitud", latitud)
            obj.put("longitud", longitud)
            obj.put("date", fecha)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return obj
    }
}
