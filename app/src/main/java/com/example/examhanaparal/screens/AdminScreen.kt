package com.example.examhanaparal.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.example.examhanaparal.models.StudyGroup
import com.example.examhanaparal.navigation.Routes
import com.example.examhanaparal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController) {
    val context = LocalContext.current
    val db      = FirebaseFirestore.getInstance()

    var groups          by remember { mutableStateOf<List<StudyGroup>>(emptyList()) }
    var totalUsers      by remember { mutableStateOf(0) }
    var isLoading       by remember { mutableStateOf(true) }
    var isActionLoading by remember { mutableStateOf(false) }
    var deleteTarget    by remember { mutableStateOf<StudyGroup?>(null) }

    // Load groups in real-time
    DisposableEffect(Unit) {
        val listener = db.collection("groups")
            .addSnapshotListener { snapshots, _ ->
                snapshots ?: return@addSnapshotListener
                groups    = snapshots.documents.mapNotNull { doc ->
                    doc.toObject(StudyGroup::class.java)?.copy(id = doc.id)
                }
                isLoading = false
            }
        onDispose { listener.remove() }
    }

    // Load user count
    LaunchedEffect(Unit) {
        db.collection("users").get()
            .addOnSuccessListener { snap -> totalUsers = snap.size() }
    }

    val totalMembers = groups.sumOf { it.memberCount }
    val fullGroups   = groups.count { it.isFull }
    val openGroups   = groups.count { !it.isFull }

    if (isActionLoading) LoadingDialog("Deleting group…")

    deleteTarget?.let { target ->
        ConfirmDialog(
            title        = "Delete Group",
            message      = "Permanently delete \"${target.name}\"? All member data for this group will be lost.",
            confirmLabel = "Delete",
            confirmColor = DangerRed,
            onConfirm    = {
                val id   = target.id
                deleteTarget    = null
                isActionLoading = true
                db.collection("groups").document(id).delete()
                    .addOnSuccessListener {
                        isActionLoading = false
                        Toast.makeText(context, "Group deleted.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        isActionLoading = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            onDismiss = { deleteTarget = null }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint     = AdminGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Admin Dashboard",
                            color         = TextPrimary,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 18.sp,
                            letterSpacing = (-0.3).sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Welcome banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(listOf(AdminGoldDim, AdminGoldPale))
                        )
                        .border(1.dp, AdminGold.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            "Admin Control Panel",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = AdminGold,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Manage all study groups and monitor activity.",
                            fontSize = 13.sp,
                            color    = AdminGold.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // ── Stats row
            item {
                SectionHeader("OVERVIEW")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Default.Group,
                        value    = "${groups.size}",
                        label    = "Groups",
                        accent   = AccentBlue,
                        dim      = AccentBluePale
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Default.People,
                        value    = "$totalUsers",
                        label    = "Users",
                        accent   = AccentGreen,
                        dim      = AccentGreenDim
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Default.Lock,
                        value    = "$fullGroups",
                        label    = "Full",
                        accent   = DangerRed,
                        dim      = DangerRedDim
                    )
                }
            }

            // ── Groups section header
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("ALL GROUPS", modifier = Modifier.weight(1f))
                    Button(
                        onClick = { navController.navigate(Routes.CREATE_GROUP) },
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor   = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier       = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // ── Loading shimmer
            if (isLoading) {
                items(4) { ShimmerGroupCard() }
            }

            // ── Empty state
            if (!isLoading && groups.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.GroupOff,
                                contentDescription = null,
                                tint     = TextHint,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No groups found.", color = TextSecondary, fontSize = 15.sp)
                        }
                    }
                }
            }

            // ── Group list
            items(groups, key = { it.id }) { group ->
                AdminGroupCard(
                    group    = group,
                    onView   = { navController.navigate(Routes.groupDetail(group.id)) },
                    onDelete = { deleteTarget = group }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    modifier : Modifier,
    icon     : ImageVector,
    value    : String,
    label    : String,
    accent   : Color,
    dim      : Color
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(dim.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Text(
                label,
                fontSize      = 11.sp,
                color         = TextHint,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun AdminGroupCard(
    group    : StudyGroup,
    onView   : () -> Unit,
    onDelete : () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint     = AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.name,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Text(
                    group.subject,
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Member count chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentBluePale)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${group.memberCount}/${group.maxMembers} members",
                            fontSize = 10.sp,
                            color    = AccentBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Full/Open chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (group.isFull) DangerRedDim else AccentGreenDim.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (group.isFull) "Full" else "Open",
                            fontSize   = 10.sp,
                            color      = if (group.isFull) DangerRed else AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Delete button
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint     = DangerRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}