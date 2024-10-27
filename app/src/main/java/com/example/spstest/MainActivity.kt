package com.example.spstest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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
                    Verbrauch(refresherWrapper, sps, this)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("onStart", "Start Main Activity")

    }

    override fun onStop() {
        super.onStop()
        refresherWrapper.refresher?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        refresherWrapper.refresher?.stop()
    }
}

@Preview
@Composable
fun Verbrauch(
    refresherWrapper: RefresherWrapper = RefresherWrapper(),
    sps: Communication = Communication(),
    context: ComponentActivity = MainActivity()
) {
    val scope = rememberCoroutineScope()
    val model = remember { CountersModel(context, scope, sps) }
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
    val values = model.values
    ItemList(values, onItemClick = { index ->
        Log.d("ItemClick", "Item $index clicked")
        val intent = Intent(context, EditSettingActivity::class.java).apply {
            putExtra("PARAM_KEY", ""+ index)
        }
        context.startActivity(intent)})
}

@Composable
fun ItemList(values: List<DataItem>, onItemClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(values) { index, item ->
            Row(Modifier.fillMaxWidth()) {
                TableCell(item.name, Modifier.weight(1f), index, onItemClick)
                TableCell(item.value, Modifier.weight(1f), index, onItemClick)
            }

        }
    }
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier, index: Int, onItemClick: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(index) }
            .padding(8.dp)
    ) {
        Text(
            text = text,
            modifier = modifier
                .padding(16.dp)
                .wrapContentWidth(Alignment.Start) // Align text to the start
        )
    }
}