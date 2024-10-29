package com.example.spstest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val sps = Communication()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Pumpeninfos(sps, this)
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

@Composable
fun Pumpeninfos(
    sps: Communication = Communication(),
    context: ComponentActivity = MainActivity()
) {
    val scope = rememberCoroutineScope()
    val model by remember { mutableStateOf(CountersModel(context, sps)) }
    var values by remember { mutableStateOf(model.values) }
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        model.refreshVerbrauch()
        values = model.values
    }
    val swipeRefreshState = remember { SwipeRefreshState(isRefreshing) }
        Scaffold(
            topBar = { TopBar(context) }
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
                        // Setze isRefreshing auf true
                        isRefreshing = true
                        scope.launch {
                            model.refreshVerbrauch()
                            values = model.values
                            isRefreshing = false
                        }
                    },
                ) {
                    ItemList(values, onItemClick = { index ->
                        val intent = Intent(context, EditSettingActivity::class.java).apply {
                            putExtra(parameter_index, index)
                        }
                        context.startActivity(intent)
                    })
                }
            }
        }
    }

    @Composable
    fun ItemList(values: List<DataItem>, onItemClick: (Int) -> Unit) {
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
                    TableCell(item.name, Modifier.weight(1f), index, onItemClick)
                    TableCell(item.value, Modifier.weight(1f), index, onItemClick)
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
    fun TopBar(context: ComponentActivity = MainActivity()) {
        TopAppBar(
            title = { Text(text = "Pumpeninfos") },
            actions = {
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