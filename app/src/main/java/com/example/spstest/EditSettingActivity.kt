package com.example.spstest

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var isErrorNr by remember { mutableStateOf(false) }
    var isErrorDbnr by remember { mutableStateOf(false) }
    var isErrorFactor by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

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
        settingsArray.put(setting)
    }
    var name by remember { mutableStateOf(setting.getString("name")) }
    var type by remember { mutableStateOf(setting.getString("type")) }
    var nr by remember { mutableStateOf(setting.getString("nr")) }
    var dbnr by remember { mutableStateOf(setting.getString("dbnr")) }
    var unit by remember { mutableStateOf(setting.getString("unit")) }
    var factor by remember { mutableStateOf(setting.getString("factor")) }
    val options = listOf("Double Integer", "Integer", "Real", "Byte", "Bit")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = { Text("Name des Wertes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dropdownExpanded = true }
                    .border(
                        1.dp,
                        MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = type, color = MaterialTheme.colors.onSurface)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown Arrow")
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                type = option
                                dropdownExpanded = false
                            }
                        ) {
                            Text(option)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nr.toString(),
                onValueChange = {
                    isErrorNr = try {
                        nr = it
                        it.toInt()
                        false
                    } catch (e: NumberFormatException) {
                        true
                    }
                },
                isError = isErrorNr,
                label = { Text("Nr") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (isErrorNr) Color.Red else MaterialTheme.colors.primary,
                    unfocusedBorderColor = if (isErrorNr) Color.Red else MaterialTheme.colors.onSurface.copy(
                        alpha = ContentAlpha.disabled
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dbnr.toString(),
                onValueChange = {
                    isErrorDbnr = try {
                        dbnr = it
                        it.toInt()
                        false
                    } catch (e: NumberFormatException) {
                        true
                    }
                },
                isError = isErrorDbnr,
                label = { Text("DBNr") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (isErrorDbnr) Color.Red else MaterialTheme.colors.primary,
                    unfocusedBorderColor = if (isErrorDbnr) Color.Red else MaterialTheme.colors.onSurface.copy(
                        alpha = ContentAlpha.disabled
                    )
                )
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
                onValueChange = {
                    isErrorFactor = try {
                        factor = it
                        // Feld wird missbraucht und kann im Bit Fall auch für die Bitnummer verwendet werden
                        if (type == "Bit")
                            factor.toInt()
                        else
                            it.toDouble()
                        false
                    } catch (e: NumberFormatException) {
                        true
                    }

                },
                isError = isErrorFactor,
                label = { Text(if (type == "Bit") "Bit Nummer" else "Faktor") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (isErrorFactor) Color.Red else MaterialTheme.colors.primary,
                    unfocusedBorderColor = if (isErrorFactor) Color.Red else MaterialTheme.colors.onSurface.copy(
                        alpha = ContentAlpha.disabled
                    )
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Log.d("SaveButton", "Button clicked")
                    saveSetting(
                        name,
                        type,
                        nr.toInt(),
                        dbnr.toInt(),
                        unit,
                        factor.toDouble(),
                        context,
                        settings,
                        setting
                    )
                    Toast.makeText(context, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
                    context.finish()
                },
                enabled = !(isErrorDbnr || isErrorFactor || isErrorNr),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Text("Speichern")
            }
            Button(
                onClick = {
                    Log.d("DeleteButton", "Button clicked")
                    if (index >= 0) {
                        settingsArray.remove(index)
                        FileHelper.saveJsonToFile(context, settings)
                    }
                    Toast.makeText(context, "Einstellung gelöscht", Toast.LENGTH_SHORT).show()
                    context.finish()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Text("Löschen")
            }
        }
    }
}

fun saveSetting(
    name: String?,
    type: String?,
    nr: Int,
    dbnr: Int,
    unit: String?,
    factor: Double,
    context: ComponentActivity,
    settings: JSONObject,
    setting: JSONObject
) {
    setting.put("name", name)
    setting.put("type", type)
    setting.put("nr", nr)
    setting.put("dbnr", dbnr)
    setting.put("unit", unit)
    setting.put("factor", factor)
    FileHelper.saveJsonToFile(context, settings)
}