package com.example.zlata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.zlata.ui.screens.EnergoApp
import com.example.zlata.ui.theme.ZlataTheme
import com.example.zlata.viewmodel.EnergoViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: EnergoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZlataTheme {
                EnergoApp(viewModel)
            }
        }
    }
}
