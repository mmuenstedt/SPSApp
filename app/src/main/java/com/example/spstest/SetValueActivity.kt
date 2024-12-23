package com.example.spstest

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SetValueActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val index = intent.getIntExtra(parameter_index, -1)
        var currentValue = intent.getStringExtra(parameter_value)
        if (currentValue == null) {
            currentValue = ""
        }
        setContent {
            SetValueMask(index, currentValue, this)
        }
    }
}

@Composable
fun SetValueMask(index: Int, currentValue: String, context: ComponentActivity = MainActivity()) {
    var settings = FileHelper.loadJsonFromFile(context)
    var settingsArray = settings.getJSONArray("settings")
    var setting: JSONObject
    var isErrorValue by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(currentValue) }
    val sps = Communication()

    setting = settingsArray.getJSONObject(index)

    var name = setting.getString("name")
    var type = setting.getString("type")
    var nr = setting.getString("nr")
    var dbnr = setting.getString("dbnr")
    var unit = setting.getString("unit")
    var factor = setting.getString("factor")
    val scope = rememberCoroutineScope()
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
                value = value,
                onValueChange = {
                    value = it
                    isErrorValue = try {
                        when (type) {
                            "Integer" -> it.toInt()
                            "Double Integer" -> it.toInt()
                            "Byte" -> it.toInt()
                            "Real" -> it.toFloat()
                        }
                        if (type == "Bit") {
                            if (it == "0" || it == "1" || it == "true" || it == "false") {
                                //gültiger Boolean
                                false
                            } else {
                                //Ungültiger Boolean in Eingabefeld
                                true
                            }
                        } else {
                            //Gültiger Wert in Eingabefeld
                            false
                        }
                    } catch (e: NumberFormatException) {
                        //Ungültige Zahl in Eingabefeld
                        true
                    }
                },
                isError = isErrorValue,
                label = { Text(name + "(NR: " + nr + ", DBNR: " + dbnr + ")") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (isErrorValue) Color.Red else MaterialTheme.colors.primary,
                    unfocusedBorderColor = if (isErrorValue) Color.Red else MaterialTheme.colors.onSurface.copy(
                        alpha = ContentAlpha.disabled
                    )
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Log.d("SetValueButton", "Button clicked")
                    scope.launch {
                        fetchData(sps, context, type, nr, dbnr, factor, value)
                    }
                    context.finish()
                },
                enabled = !(isErrorValue) && value != "Connection failed",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Text("Wert setzen")
            }
            Button(
                onClick = {
                    Log.d("CancelButton", "Button clicked")
                    context.finish()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Text("Abbrechen")
            }
        }
    }
}

suspend fun fetchData(
    sps: Communication,
    context: ComponentActivity,
    type: String,
    nr: String,
    dbnr: String,
    bitnummer: String,
    value: String
) {
    try{
        withContext(Dispatchers.IO) {
            if (!sps.hasConnection()) {
                sps.connect()
            }
            if (sps.setValue(
                    type,
                    nr.toInt(),
                    dbnr.toInt(),
                    if (type == "Bit") bitnummer.toInt() else 0,
                    value
                )
            ) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Wert erfolgreich gesetzt", Toast.LENGTH_SHORT).show()
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Wert konnte nicht gesetzt werden",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Fehler beim Setzen des Wertes", Toast.LENGTH_SHORT).show()
    }
}