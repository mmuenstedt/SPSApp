package com.example.spstest

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.IOException
import java.net.InetAddress

class CountersModel(context: ComponentActivity, scope: CoroutineScope, var sps: Communication) {
    var verbrauch by mutableStateOf(0.0)
        private set
    var pumpenfrequenz by mutableStateOf(0.0)
        private set
    var druck by mutableStateOf(0.0)
        private set
    var gemittelterDruck2Sek by mutableStateOf(0.0)
        private set
    var luftfeuchte by mutableStateOf(0)
        private set

    var values by mutableStateOf(listOf<DataItem>())
        private set

    val settings = FileHelper.loadJsonFromFile(context).getJSONArray("settings")

    init {
        snapshotFlow {}
            .onEach(::println)
            .launchIn(scope)
        Log.d("Settings", settings.toString(2))
    }
    fun updateItem(index: Int, newValue: DataItem) {
        values = values.toMutableList().apply {
            this[index] = newValue
        }
    }

    fun addItem(item: DataItem) {
        values = values.toMutableList().apply {
            add(item)
        }
    }

    fun refreshVerbrauch() {
        if (sps.hasConnection()) {
            val dbPumpenFrequenz = sps.GetDBW(0, 10)
            val dbDruck = 0.0 // sps.GetDBW(11, 0)
            val dbGemittelterDruck2Sek = sps.GetDBW(22, 3)
            val dbLuftfeuchte = 0 //sps.GetDBW(13, 0)
            //val dbVerbrauch = sps.GetDBW(14, 0)
            pumpenfrequenz = dbPumpenFrequenz / 1.04
            verbrauch = dbPumpenFrequenz * 7.6923

            druck = dbDruck
            gemittelterDruck2Sek = dbGemittelterDruck2Sek * 1.0
            luftfeuchte = dbLuftfeuchte

            for (i in 0 until settings.length()) {
                val setting = settings.getJSONObject(i)
                val name = setting.getString("name")
                val type = setting.getString("type")
                val nr = setting.getInt("nr")
                val dbnr = setting.getInt("dbnr")
                val unit = setting.getString("unit")
                val factor = setting.getDouble("factor")
                val value = when (type) {
                    "W" -> sps.GetDBW(nr, dbnr) * factor
                    "R" -> sps.GetDBR(nr, dbnr) * factor
                    "D" -> sps.GetDBD(nr, dbnr) * factor
                    else -> 0.0
                }
                val newItem = DataItem(name, "" + value + " " + unit)
                if(values.size <= i ) {
                    addItem(newItem)
                }else{
                    updateItem(i, newItem)
                }
            }

        } else {
            verbrauch = -1.0
            pumpenfrequenz = -1.0
            druck = -1.0
            gemittelterDruck2Sek = -1.0
            luftfeuchte = -1
            Log.d("refreshVerbrauch",""+ values.size)
            for (i in 0 until settings.length()) {
                if(values.size <= i ) {
                    addItem(DataItem(settings.getJSONObject(i).getString("name"), "Connection failed"))
                }else{
                    updateItem(i,  DataItem(values[i].name, "Connection failed"))
                }
            }

            var addr: InetAddress = InetAddress.getByName("172.22.15.119")
            try {
                sps.newConnection(addr, 2.toByte())
            } catch (ioexc: IOException) {
                Log.d("refreshVerbrauch", "Connection failed")
            }
        }
        Log.d("refreshVerbrauch", values.toString())
    }

    fun reset() {
        verbrauch = 0.0

    }
}