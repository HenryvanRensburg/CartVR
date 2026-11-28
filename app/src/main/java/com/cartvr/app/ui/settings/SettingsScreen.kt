package com.cartvr.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cartvr.app.BuildConfig
import com.cartvr.app.data.notifications.NotificationHelper
import com.cartvr.app.data.preferences.ThemeMode
import com.cartvr.app.ui.UpdateUiState
import com.cartvr.app.ui.calendar.CalendarVisuals
import com.cartvr.app.ui.components.ListVisuals
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    displayName: String,
    userEmail: String?,
    householdName: String,
    isAdmin: Boolean,
    themeMode: ThemeMode,
    myAccentColor: String,
    lastSyncError: String? = null,
    updateState: UpdateUiState,
    notificationsEnabled: Boolean,
    notifyOnChecked: Boolean,
    liveModeEnabled: Boolean,
    liveModeDurationHours: Int,
    liveModeStopAtMs: Long,
    morningSummaryEnabled: Boolean,
    morningSummaryHour: Int,
    morningSummaryMinute: Int,
    defaultEventReminderMinutes: Int,
    notifyTaskAssignments: Boolean,
    notifySharedCalendar: Boolean,
    onThemeSelected: (ThemeMode) -> Unit,
    onAccentColorSelected: (String) -> Unit,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onNotifyOnCheckedChanged: (Boolean) -> Unit,
    onLiveModeEnabledChanged: (Boolean) -> Unit,
    onLiveModeDurationChanged: (Int) -> Unit,
    onPreviewChime: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onMorningSummaryEnabledChanged: (Boolean) -> Unit,
    onMorningSummaryTimeChanged: (Int, Int) -> Unit,
    onDefaultEventReminderChanged: (Int) -> Unit,
    onNotifyTaskAssignmentsChanged: (Boolean) -> Unit,
    onNotifySharedCalendarChanged: (Boolean) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onManageFamily: () -> Unit,
    onSyncNow: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionDeniedNote by remember { mutableStateOf(false) }
    var pendingLiveAfterPermission by remember { mutableStateOf(false) }
    var pendingSummaryAfterPermission by remember { mutableStateOf(false) }
    var showMorningTimePicker by remember { mutableStateOf(false) }

    var exactAlarmsOk by remember { mutableStateOf(NotificationHelper.canScheduleExactAlarms(context)) }
    var batteryUnrestrictedOk by remember { mutableStateOf(NotificationHelper.isIgnoringBatteryOptimizations(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsOk = NotificationHelper.canScheduleExactAlarms(context)
                batteryUnrestrictedOk = NotificationHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            when {
                pendingSummaryAfterPermission -> {
                    pendingSummaryAfterPermission = false
                    onMorningSummaryEnabledChanged(true)
                }
                pendingLiveAfterPermission -> {
                    pendingLiveAfterPermission = false
                    onLiveModeEnabledChanged(true)
                }
                else -> onNotificationsEnabledChanged(true)
            }
        } else {
            pendingSummaryAfterPermission = false
            pendingLiveAfterPermission = false
            permissionDeniedNote = true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Appearance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Saved locally on this phone so everyone can pick their own theme.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeSelected(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "My Calendar Color",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Used across the Family Calendar & Tasks so everyone knows what's yours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CalendarVisuals.memberColorOptions.forEach { hex ->
                            val color = ListVisuals.parseColor(hex)
                            val selected = hex.equals(myAccentColor, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onAccentColorSelected(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Notifications Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shared List Alerts",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Chime when someone else updates a shared list. Checks every 15 min in the background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { wantEnable ->
                                if (!wantEnable) {
                                    permissionDeniedNote = false
                                    onNotificationsEnabledChanged(false)
                                } else if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    pendingLiveAfterPermission = false
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    permissionDeniedNote = false
                                    onNotificationsEnabledChanged(true)
                                }
                            }
                        )
                    }

                    if (permissionDeniedNote) {
                        Text(
                            text = "Notification permission is blocked on this phone. Allow notifications in Android settings to hear alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = onOpenSoundSettings,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null)
                            Text("  Open Notification Settings")
                        }
                    }

                    if (notificationsEnabled) {
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Shopping Mode",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Instant alerts while shopping. Shows a quiet status bar notice with a Stop button.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (liveModeEnabled && liveModeStopAtMs > System.currentTimeMillis()) {
                                    val untilText = remember(liveModeStopAtMs) {
                                        Instant.ofEpochMilli(liveModeStopAtMs)
                                            .atZone(ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Active until $untilText",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Switch(
                                checked = liveModeEnabled,
                                onCheckedChange = { wantLive ->
                                    if (!wantLive) {
                                        onLiveModeEnabledChanged(false)
                                    } else if (
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !NotificationHelper.hasNotificationPermission(context)
                                    ) {
                                        pendingLiveAfterPermission = true
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onLiveModeEnabledChanged(true)
                                    }
                                }
                            )
                        }

                        Text(
                            text = "Auto-off timer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1 to "1 hr", 2 to "2 hrs", 4 to "4 hrs").forEach { (hours, label) ->
                                FilterChip(
                                    selected = liveModeDurationHours == hours,
                                    onClick = { onLiveModeDurationChanged(hours) },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notify on ticked-off items",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Off by default so you only hear chimes for new items.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notifyOnChecked,
                                onCheckedChange = onNotifyOnCheckedChanged
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPreviewChime,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("  Preview Chime")
                            }
                            OutlinedButton(
                                onClick = onOpenSoundSettings,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("  Chime Sound")
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Task assignment alerts",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Chime when another family member assigns a shared task to you.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notifyTaskAssignments,
                                onCheckedChange = onNotifyTaskAssignmentsChanged
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Shared calendar alerts",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Chime when someone adds or updates a shared family event.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notifySharedCalendar,
                                onCheckedChange = onNotifySharedCalendarChanged
                            )
                        }
                    }
                }
            }
        }

        // Reminders & Daily Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Reminders & Daily Summary",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Morning Summary",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Morning overview of today's tasks & calendar events on this phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = morningSummaryEnabled,
                            onCheckedChange = { wantSummary ->
                                if (!wantSummary) {
                                    onMorningSummaryEnabledChanged(false)
                                } else if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    pendingSummaryAfterPermission = true
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onMorningSummaryEnabledChanged(true)
                                }
                            }
                        )
                    }

                    if (morningSummaryEnabled) {
                        val formattedTime = remember(morningSummaryHour, morningSummaryMinute) {
                            String.format(Locale.getDefault(), "%02d:%02d", morningSummaryHour, morningSummaryMinute)
                        }
                        OutlinedButton(
                            onClick = { showMorningTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Morning Summary Time: $formattedTime")
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Default Event Reminder",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Used for calendar events set to \"Phone default\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            15 to "15m",
                            30 to "30m",
                            60 to "1 hr",
                            1440 to "1 day"
                        ).forEach { (minutes, label) ->
                            FilterChip(
                                selected = defaultEventReminderMinutes == minutes,
                                onClick = { onDefaultEventReminderChanged(minutes) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Alarm & Battery Reliability",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (exactAlarmsOk && batteryUnrestrictedOk) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Exact alarms and battery settings look great on this phone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BatteryAlert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Android battery saving or alarm limits may delay your Morning Summary or timed reminders.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!exactAlarmsOk) {
                                        OutlinedButton(
                                            onClick = onOpenExactAlarmSettings,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Allow Exact Alarms")
                                        }
                                    }
                                    if (!batteryUnrestrictedOk) {
                                        OutlinedButton(
                                            onClick = onOpenBatteryOptimizationSettings,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Unrestrict Battery")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Household & Sync Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Household & Sync",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Household: $householdName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Signed in as $displayName (${userEmail ?: "—"})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!lastSyncError.isNullOrBlank()) {
                        Text(
                            text = "Last sync error: $lastSyncError",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onSyncNow,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Text("  Sync Now")
                    }
                }
            }
        }

        // Family Admin Card (visible only to Henry / Admins)
        if (isAdmin) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Family Admin",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Invite your son, daughter, or family members directly from CartVR.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onManageFamily,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                            Text("  Manage Family")
                        }
                    }
                }
            }
        }

        // App Updates Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "App Updates",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Installed version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!updateState.noUpdateMessage.isNullOrBlank()) {
                        Text(
                            text = updateState.noUpdateMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!updateState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = updateState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !updateState.isChecking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (updateState.isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text("  Checking GitHub...")
                        } else {
                            Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                            Text("  Check for Updates")
                        }
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Text("  Sign Out")
            }
        }
    }

    if (showMorningTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = morningSummaryHour,
            initialMinute = morningSummaryMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showMorningTimePicker = false },
            title = { Text("Morning Summary Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMorningSummaryTimeChanged(timeState.hour, timeState.minute)
                        showMorningTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showMorningTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
