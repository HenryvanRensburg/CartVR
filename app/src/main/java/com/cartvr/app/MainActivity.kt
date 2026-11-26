package com.cartvr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cartvr.app.ui.CartVrApp
import com.cartvr.app.ui.CartVrViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CartVRApplication
        setContent {
            val vm: CartVrViewModel = viewModel(factory = CartVrViewModel.factory(app))
            CartVrApp(viewModel = vm)
        }
    }
}
