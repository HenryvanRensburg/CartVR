package com.cartvr.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.cartvr.app.ui.CartVrApp
import com.cartvr.app.ui.CartVrViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CartVrViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CartVRApplication
        viewModel = ViewModelProvider(this, CartVrViewModel.factory(app))[CartVrViewModel::class.java]
        handleDeepLink(intent)

        setContent {
            CartVrApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        val code = data.getQueryParameter("code")
            ?: data.lastPathSegment?.takeIf {
                !it.equals("join", ignoreCase = true) && !it.equals("invite", ignoreCase = true)
            }
        if (code.isNullOrBlank()) return

        val isInvite = data.host.equals("invite", ignoreCase = true) ||
            (data.path?.contains("/invite", ignoreCase = true) == true)

        if (isInvite) {
            viewModel.handleIncomingFamilyInviteCode(code)
        } else {
            viewModel.handleIncomingShareCode(code)
        }
    }
}
