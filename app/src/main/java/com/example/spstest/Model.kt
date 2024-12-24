package com.example.spstest

import android.util.Log
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.InetAddress
import kotlin.math.roundToInt

class Model(context: ComponentActivity) {
    var context = context
    var values = MutableList(0) { DataItem("", "") }

    var settings = loadSettings(context)

    fun loadSettings(context: ComponentActivity): JSONArray =
        FileHelper.loadJsonFromFile(context).getJSONArray("settings")

    suspend fun refreshSPSData() = withContext(Dispatchers.IO) {
        settings = loadSettings(context)
        values = MutableList(0) { DataItem("", "") }
        if (SPSManager.sps?.hasConnection() == true) {
            for (i in 0 until settings.length()) {
                val setting = settings.getJSONObject(i)
                val name = setting.getString("name")
                val type = setting.getString("type")
                val nr = setting.getInt("nr")
                val dbnr = setting.getInt("dbnr")
                val unit = setting.getString("unit")
                val factor = setting.getDouble("factor")
                val value = try {
                    when (type) {
                        "Integer" -> (SPSManager.sps!!.GetDBW(nr, dbnr) * factor).roundToInt().toString()
                        "Real" -> (SPSManager.sps!!.GetDBR(nr, dbnr) * factor).toString()
                        "Double Integer" -> (SPSManager.sps!!.GetDBD(nr, dbnr) * factor).roundToInt().toString()
                        "Byte" -> (SPSManager.sps!!.GetDBB(nr, dbnr) * factor).roundToInt().toString()
                        "Bit" -> SPSManager.sps!!.GetDBX(nr, dbnr, factor.toInt()).toString()
                        else -> "-1.0"
                    }
                } catch (ioexc: IOException) {
                    "IO Exception"
                }

                val newItem = DataItem(name, "" + value + " " + unit)
                if (values.size <= i) {
                    values.add(newItem)
                } else {
                    values[i] = newItem
                }
            }
        } else {
            for (i in 0 until settings.length()) {
                if (values.size <= i) {
                    values.add(
                        DataItem(
                            settings.getJSONObject(i).getString("name"),
                            "Connection failed"
                        )
                    )
                } else {
                    values[i] =
                        DataItem(settings.getJSONObject(i).getString("name"), "Connection failed")
                }
            }
            connectSPS()
        }
        Log.d("refreshVerbrauch", values.toString())
    }

    private fun connectSPS() {
        if (SPSManager.sps == null) {
            Log.d("connectSPS", "sps is null")
            SPSManager.sps = Communication()
        }
        SPSManager.sps!!.connect()
        Log.d("connectSPS", "HasConnection: " + SPSManager.sps!!.hasConnection().toString())
    }
}