package com.example.spstest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.net.InetAddress


class MainActivity : ComponentActivity() {
    private val sps = Communication()
    private val refresherWrapper = RefresherWrapper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Verbrauch(refresherWrapper, sps)
                }
            }
            TwoColumnTable()
        }
    }
}

@Preview
@Composable
fun Verbrauch(
    refresherWrapper: RefresherWrapper = RefresherWrapper(),
    sps: Communication = Communication()
) {
    val scope = rememberCoroutineScope()
    val model = remember { CountersModel(scope, sps) }
    val myStyle = TextStyle(
        fontSize = 24.sp,
        shadow = Shadow(
            color = Color.Blue, blurRadius = 3f
        )
    )

    if (refresherWrapper.refresher == null) {
        refresherWrapper.refresher = Refresher(model)
        val thread = Thread(refresherWrapper.refresher)
        thread.start()
    }
    val values = listOf(
        DataItem("Pumpenfrequenz", model.pumpenfrequenz.toString() + " Hz"),
        DataItem("Verbrauch", model.verbrauch.toString() + " l/h"),
        DataItem("Druck", model.druck.toString() + " bar"),
        DataItem("Gemittelter Druck 2 Sek", model.gemittelterDruck2Sek.toString() + " bar"),
        DataItem("Luftfeuchte", model.luftfeuchte.toString() + "%")
    )
    LazyColumn(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        /*
        // Header row
        item {
            Row(Modifier.fillMaxWidth()) {
                TableCell("Name", Modifier.weight(1f))
                TableCell("Wert", Modifier.weight(1f))
            }
        }
        */


        // Data rows
        items(values) { item ->
            Row(Modifier.fillMaxWidth()) {
                TableCell(item.name, Modifier.weight(1f))
                TableCell(item.value, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                TableCell("Reset", Modifier.weight(1f))
                Button(onClick = model::reset) {
                    Text("Reset")
                }
            }
        }
    }


    /*
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "Verbrauch: ${model.verbrauch}",
            modifier = Modifier.padding(16.dp),
            style = myStyle
        )
        Text(
            "Test: ${model.druck}",
            modifier = Modifier.padding(16.dp),
            style = myStyle
        )

    }*/
}


class CountersModel(scope: CoroutineScope, var sps: Communication) {
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
    init {
        snapshotFlow {}
            .onEach(::println)
            .launchIn(scope)
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

        } else {
            verbrauch = -1.0
            pumpenfrequenz = -1.0
            druck = -1.0
            gemittelterDruck2Sek = -1.0
            luftfeuchte = -1
            var addr: InetAddress = InetAddress.getByName("172.22.15.119")
            sps.newConnection(addr, 2.toByte())
        }
    }

    fun reset() {
        verbrauch = 0.0

    }
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .padding(16.dp)
            .wrapContentWidth(Alignment.Start) // Align text to the start
    )
}

data class DataItem(val name: String, val value: String)

@Composable
fun TwoColumnTable() {

}

class RefresherWrapper() {
    var refresher: Refresher? = null
}

class Refresher(private val model: CountersModel) : Runnable {
    private var isRunning = false
    override fun run() {
        isRunning = true
        while (isRunning) {
            Thread.sleep(1000)
            model.refreshVerbrauch()
        }
    }

    fun stop() {
        isRunning = false
    }
}