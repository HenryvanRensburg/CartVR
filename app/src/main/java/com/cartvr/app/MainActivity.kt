package com.cartvr.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.cartvr.app.data.notifications.NotificationHelper
import com.cartvr.app.ui.CartVrApp
import com.cartvr.app.ui.CartVrViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CartVrViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CartVRApplication
        viewModel = ViewModelProvider(this, CartVrViewModel.factory(app))[CartVrViewModel::class.java]
        handleIntent(intent)

        setContent {
            CartVrApp(viewModel = viewModel)
        }
    }

    override fun onStart() {
        super.onStart()
        (application as CartVRApplication).repository.setAppForeground(true)
    }

    override fun onStop() {
        (application as CartVRApplication).repository.setAppForeground(false)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val notificationListId = intent?.getStringExtra(NotificationHelper.EXTRA_LIST_ID)
        if (!notificationListId.isNullOrBlank()) {
            viewModel.openListFromNotification(notificationListId)
            return
        }

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
