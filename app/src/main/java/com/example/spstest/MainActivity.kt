package com.example.spstest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sps = Communication()
        SPSManager.sps = sps
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Pumpeninfos( this)
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        Log.d("onStart", "Start Main Activity")
    }
}

const val parameter_index = "INDEX_PARAM"
const val parameter_value = "VALUE_PARAM"

@Composable
fun Pumpeninfos(
    context: ComponentActivity = MainActivity()
) {
    val scope = rememberCoroutineScope()
    val model by remember { mutableStateOf(Model(context)) }
    var values by remember { mutableStateOf(model.values) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isAutoRefreshing by remember { mutableStateOf(true) }

    // Nur ausführen wenn App im Vordergrund
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val job = scope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    if (isAutoRefreshing) {
                        model.refreshSPSData()
                        values = model.values
                    }
                    delay(2000)
                }
            }
        }
        onDispose {
            job.cancel()
        }
    }

    val swipeRefreshState = remember { SwipeRefreshState(isRefreshing) }
    Scaffold(
        topBar = {
            TopBar(
                context,
                isAutoRefreshing = isAutoRefreshing,
                onToggleAutoRefresh = { isAutoRefreshing = it })
        }
    ) { paddingValues ->
        // Apply the padding values to the content
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colors.background
        ) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        model.refreshSPSData()
                        values = model.values
                        isRefreshing = false
                    }
                },
            ) {
                ItemList(values, onNameClick = { index ->
                    val intent = Intent(context, EditSettingActivity::class.java).apply {
                        putExtra(parameter_index, index)
                    }
                    context.startActivity(intent)
                },
                    onValueClick = { index ->
                        val intent = Intent(context, SetValueActivity::class.java).apply {
                            putExtra(parameter_index, index)
                            putExtra(parameter_value, values[index].value)
                        }
                        context.startActivity(intent)
                    })
            }
        }
    }
}


@Composable
fun ItemList(values: List<DataItem>, onNameClick: (Int) -> Unit, onValueClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(values) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TableCell(item.name, Modifier.weight(1f), index, onNameClick)
                TableCell(item.value, Modifier.weight(1f), index, onValueClick)
            }

        }
    }
}

@Composable
fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
    index: Int,
    onItemClick: (Int) -> Unit
) {
    val textStyle = MaterialTheme.typography.body1
    Surface(
        modifier = Modifier
            .clickable { onItemClick(index) }
            .padding(8.dp)
    ) {
        Text(
            text = text,
            modifier = modifier
                .padding(16.dp)
                .wrapContentWidth(Alignment.Start) // Align text to the start
            , style = textStyle
        )
    }
}

@Composable
fun TopBar(
    context: ComponentActivity = MainActivity(),
    isAutoRefreshing: Boolean,
    onToggleAutoRefresh: (Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2000, easing = LinearEasing),
            RepeatMode.Restart
        ), label = "RotationAnimation"
    )
    val currentRotation = if (isAutoRefreshing) rotationAngle else 0f

    TopAppBar(
        title = { Text(text = "SPS Control") },
        actions = {
            IconButton(onClick = { onToggleAutoRefresh(!isAutoRefreshing) }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = if (isAutoRefreshing) "Stop Refresh" else "Start Refresh",
                    modifier = Modifier.rotate(currentRotation)
                )
            }
            IconButton(onClick = {
                val intent = Intent(context, EditSettingActivity::class.java).apply {
                    putExtra(parameter_index, -1)
                }
                context.startActivity(intent)
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
            IconButton(onClick = { /* Handle menu click */ }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = Color.White
    )
}
