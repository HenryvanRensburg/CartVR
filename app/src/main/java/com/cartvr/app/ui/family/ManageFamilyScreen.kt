package com.cartvr.app.ui.family

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cartvr.app.data.local.entity.ShoppingListEntity
import com.cartvr.app.data.remote.dto.FamilyInviteDto
import com.cartvr.app.data.remote.dto.FamilyMemberDto
import com.cartvr.app.ui.FamilyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFamilyScreen(
    currentUserId: String?,
    familyState: FamilyUiState,
    sharedLists: List<ShoppingListEntity>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreateInvite: (name: String, role: String, autoListIds: List<String>) -> Unit,
    onCreateMemberDirectly: (name: String, email: String, password: String, role: String, autoListIds: List<String>) -> Unit,
    onShareInvite: (FamilyInviteDto) -> Unit,
    onRevokeInvite: (FamilyInviteDto) -> Unit,
    onUpdateRole: (FamilyMemberDto, newRole: String) -> Unit,
    onRemoveMember: (FamilyMemberDto) -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var confirmRemoveMember by remember { mutableStateOf<FamilyMemberDto?>(null) }
    var confirmRevokeInvite by remember { mutableStateOf<FamilyInviteDto?>(null) }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Family", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !familyState.isLoading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                text = { Text("Add Member") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (familyState.isLoading || familyState.isActionInProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!familyState.errorMessage.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            )
                        ) {
                            Text(
                                text = familyState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Family Members (${familyState.members.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(familyState.members, key = { it.userId }) { member ->
                    val isSelf = member.userId == currentUserId
                    FamilyMemberCard(
                        member = member,
                        isSelf = isSelf,
                        onToggleRole = {
                            val nextRole = if (member.role == "admin") "member" else "admin"
                            onUpdateRole(member, nextRole)
                        },
                        onRequestRemove = { confirmRemoveMember = member }
                    )
                }

                if (familyState.invites.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pending Invites (${familyState.invites.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(familyState.invites, key = { it.id }) { invite ->
                        PendingInviteCard(
                            invite = invite,
                            onShare = { onShareInvite(invite) },
                            onRequestRevoke = { confirmRevokeInvite = invite }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddFamilyMemberSheet(
            sharedLists = sharedLists,
            isSubmitting = familyState.isActionInProgress,
            onDismiss = { showAddSheet = false },
            onCreateInvite = { name, role, autoListIds ->
                onCreateInvite(name, role, autoListIds)
                showAddSheet = false
            },
            onCreateDirectly = { name, email, password, role, autoListIds ->
                onCreateMemberDirectly(name, email, password, role, autoListIds)
                showAddSheet = false
            }
        )
    }

    confirmRemoveMember?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmRemoveMember = null },
            title = { Text("Remove ${target.displayName}?") },
            text = {
                Text("This will remove ${target.displayName} from your family, revoke access to all shared lists, and delete their CartVR login.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMember(target)
                        confirmRemoveMember = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveMember = null }) { Text("Cancel") }
            }
        )
    }

    confirmRevokeInvite?.let { invite ->
        AlertDialog(
            onDismissRequest = { confirmRevokeInvite = null },
            title = { Text("Revoke invite for ${invite.invitedName}?") },
            text = { Text("Code ${invite.code} will stop working immediately.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRevokeInvite(invite)
                        confirmRevokeInvite = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Revoke")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevokeInvite = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FamilyMemberCard(
    member: FamilyMemberDto,
    isSelf: Boolean,
    onToggleRole: () -> Unit,
    onRequestRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isAdmin = member.role == "admin"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = if (isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Person,
                    contentDescription = null,
                    tint = if (isAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(9.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSelf) "${member.displayName} (You)" else member.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (member.email.isNotBlank()) {
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isAdmin) "Admin" else "Member",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (!isSelf) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Member actions")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isAdmin) "Change to Member" else "Make Admin") },
                            leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleRole()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Member", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRequestRemove()
                            }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun PendingInviteCard(
    invite: FamilyInviteDto,
    onShare: () -> Unit,
    onRequestRevoke: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invite.invitedName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Code: ${invite.code} • ${invite.role.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share invite",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRequestRevoke) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Revoke invite",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFamilyMemberSheet(
    sharedLists: List<ShoppingListEntity>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onCreateInvite: (name: String, role: String, autoListIds: List<String>) -> Unit,
    onCreateDirectly: (name: String, email: String, password: String, role: String, autoListIds: List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var useDirectMode by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("member") }

    // Pre-check "Groceries" by default
    val selectedListIds = remember(sharedLists) {
        mutableStateListOf<String>().apply {
            sharedLists
                .filter { it.name.equals("Groceries", ignoreCase = true) }
                .forEach { add(it.id) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Add Family Member",
                style = MaterialTheme.typography.titleLarge
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !useDirectMode,
                    onClick = { useDirectMode = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("Invite Link")
                }
                SegmentedButton(
                    selected = useDirectMode,
                    onClick = { useDirectMode = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("Add Directly")
                }
            }

            Text(
                text = if (!useDirectMode) {
                    "Send a WhatsApp or SMS invite link so they can set up their own email and password."
                } else {
                    "Create their CartVR login right here—handy if you're setting up their phone for them."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("First name (e.g. Liam)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (useDirectMode) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 6 characters)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Role",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = role == "member",
                        onClick = { role = "member" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Member")
                    }
                    SegmentedButton(
                        selected = role == "admin",
                        onClick = { role = "admin" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Admin")
                    }
                }
            }

            if (sharedLists.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Starting Shared Lists (Private lists stay hidden)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    sharedLists.forEach { list ->
                        val checked = selectedListIds.contains(list.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedListIds.remove(list.id) else selectedListIds.add(list.id)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (!selectedListIds.contains(list.id)) selectedListIds.add(list.id)
                                    } else {
                                        selectedListIds.remove(list.id)
                                    }
                                }
                            )
                            Text(text = list.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            val canSubmit = if (!useDirectMode) {
                name.isNotBlank() && !isSubmitting
            } else {
                name.isNotBlank() && email.contains("@") && password.length >= 6 && !isSubmitting
            }

            Button(
                onClick = {
                    if (!useDirectMode) {
                        onCreateInvite(name.trim(), role, selectedListIds.toList())
                    } else {
                        onCreateDirectly(name.trim(), email.trim(), password, role, selectedListIds.toList())
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (!useDirectMode) "Create & Share Invite" else "Create Account")
                }
            }
        }
    }
}
