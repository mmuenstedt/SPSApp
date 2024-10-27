package com.example.spstest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

class EditSettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val parameter = intent.getStringExtra("PARAM_KEY") ?: "No Parameter"
            Surface(color = MaterialTheme.colorScheme.background) {
                Text("This is the New Activity. Parameter: $parameter")
            }
        }
    }
}