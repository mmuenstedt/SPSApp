package com.example.spstest

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.InetAddress

class CountersModel(context: ComponentActivity, var sps: Communication) {
    var context = context
    var values  = MutableList(0) { DataItem("", "") }

    var settings = loadSettings(context)

    fun loadSettings(context: ComponentActivity): JSONArray =
        FileHelper.loadJsonFromFile(context).getJSONArray("settings")

    suspend fun refreshVerbrauch() = withContext(Dispatchers.IO){
        settings = loadSettings(context)
        values = MutableList(0) { DataItem("", "") }
        if (sps.hasConnection()) {
            val dbPumpenFrequenz = sps.GetDBW(0, 10)
            val dbDruck = 0.0 // sps.GetDBW(11, 0)
            val dbGemittelterDruck2Sek = sps.GetDBW(22, 3)
            val dbLuftfeuchte = 0 //sps.GetDBW(13, 0)
            //val dbVerbrauch = sps.GetDBW(14, 0)

            for (i in 0 until settings.length()) {
                val setting = settings.getJSONObject(i)
                val name = setting.getString("name")
                val type = setting.getString("type")
                val nr = setting.getInt("nr")
                val dbnr = setting.getInt("dbnr")
                val unit = setting.getString("unit")
                val factor = setting.getDouble("factor")
                val value = when (type) {
                    "Integer" -> (sps.GetDBW(nr, dbnr) * factor).toString()
                    "Real" -> (sps.GetDBR(nr, dbnr) * factor).toString()
                    "Double Integer" -> (sps.GetDBD(nr, dbnr) * factor).toString()
                    "Byte" -> (sps.GetDBB(nr, dbnr) * factor).toString()
                    "Bit" -> sps.GetDBX(nr, dbnr, factor.toInt()).toString()
                    else -> "-1.0"
                }
                val newItem = DataItem(name, "" + value + " " + unit)
                if(values.size <= i ) {
                   values.add(newItem)
                }else{
                    values[i] = newItem
                }
            }
        } else {
            for (i in 0 until settings.length()) {
                if(values.size <= i ) {
                    values.add(DataItem(settings.getJSONObject(i).getString("name"), "Connection failed"))
                }else{
                    values[i] = DataItem(settings.getJSONObject(i).getString("name"), "Connection failed")
                }
            }

            val addr: InetAddress = InetAddress.getByName("172.22.15.119")
            try {
                sps.newConnection(addr, 2.toByte())
            } catch (ioexc: IOException) {
                Log.d("refreshVerbrauch", "Connection failed")
            }
        }
        Log.d("refreshVerbrauch", values.toString())
    }

    fun reset() {

    }
}