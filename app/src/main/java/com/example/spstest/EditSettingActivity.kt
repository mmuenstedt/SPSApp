package com.example.spstest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.json.JSONObject

class EditSettingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val index = intent.getIntExtra(parameter_index, -1)
        setContent {
            SettingMask(index, this)
        }
    }
}

@Composable
fun SettingMask(index: Int, context: ComponentActivity = MainActivity()) {
    var settings = FileHelper.loadJsonFromFile(context)
    var settingsArray = settings.getJSONArray("settings")
    var setting: JSONObject
    if (index >= 0) {
        setting = settingsArray.getJSONObject(index)
    } else {
        setting = JSONObject()
        setting.put("name", "")
        setting.put("type", "W")
        setting.put("nr", 0)
        setting.put("dbnr", 0)
        setting.put("unit", "")
        setting.put("factor", 1.0)
    }
    var name by remember { mutableStateOf(setting.getString("name")) }
    var type by remember { mutableStateOf(setting.getString("type")) }
    var nr by remember { mutableStateOf(setting.getInt("nr")) }
    var dbnr by remember { mutableStateOf(setting.getInt("dbnr")) }
    var unit by remember { mutableStateOf(setting.getString("unit")) }
    var factor by remember { mutableStateOf(setting.getDouble("factor")) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = { Text("Name des Wertes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text("Typ des Wertes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nr.toString(),
                onValueChange = { nr = it.toInt() },
                label = { Text("Nr") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dbnr.toString(),
                onValueChange = { dbnr = it.toInt() },
                label = { Text("DBNr") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = unit,
                onValueChange = {
                    unit = it
                },
                label = { Text("Einheit des Wertes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = factor.toString(),
                onValueChange = { factor = it.toDouble() },
                label = { Text("Faktor") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Log.d("SaveButton", "Button clicked")
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
            ) {
                Text("Speichern")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SettingMask(-1)
}