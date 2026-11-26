package com.cartvr.app.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ListIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object ListVisuals {
    val iconOptions = listOf(
        ListIconOption("cart", "Groceries", Icons.Rounded.ShoppingCart),
        ListIconOption("pharmacy", "Pharmacy", Icons.Rounded.LocalPharmacy),
        ListIconOption("hardware", "Hardware", Icons.Rounded.Build),
        ListIconOption("bag", "Shopping", Icons.Rounded.ShoppingBag),
        ListIconOption("home", "Home", Icons.Rounded.Home),
        ListIconOption("gift", "Gifts", Icons.Rounded.CardGiftcard),
        ListIconOption("pet", "Pets", Icons.Rounded.Pets),
        ListIconOption("party", "Events", Icons.Rounded.Celebration)
    )

    val colorHexOptions = listOf(
        "#0D9488", // Teal
        "#2563EB", // Blue
        "#D97706", // Amber
        "#EF4444", // Coral Red
        "#8B5CF6", // Violet
        "#10B981", // Emerald
        "#EC4899", // Pink
        "#6366F1"  // Indigo
    )

    fun iconFor(key: String): ImageVector =
        iconOptions.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.ShoppingCart

    fun parseColor(hex: String): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }
            .getOrDefault(Color(0xFF0D9488))

    fun buildShareMessage(listName: String, shareCode: String): String =
        "Join my CartVR shopping list \"$listName\":\n" +
            "https://henryvanrensburg.github.io/CartVR/join/?code=$shareCode\n\n" +
            "Or open CartVR and enter invite code: $shareCode"

    fun launchShareSheet(context: Context, listName: String, shareCode: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, buildShareMessage(listName, shareCode))
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Share \"$listName\"")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
