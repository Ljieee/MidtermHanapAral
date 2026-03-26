package com.example.examhanaparal.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.examhanaparal.models.StudyGroup
import com.example.examhanaparal.models.UserProfile
import com.example.examhanaparal.ui.theme.*
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(navController: NavController, groupId: String) {
    val context    = LocalContext.current
    val db         = FirebaseFirestore.getInstance()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var group            by remember { mutableStateOf<StudyGroup?>(null) }
    var memberProfiles   by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var currentProfile   by remember { mutableStateOf<UserProfile?>(null) }
    var isActionLoading  by remember { mutableStateOf(false) }
    var actionMessage    by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var kickTargetUid    by remember { mutableStateOf<String?>(null) }

    // ── Real-time group listener
    DisposableEffect(groupId) {
        val listener = db.collection("groups").document(groupId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    group = doc.toObject(StudyGroup::class.java)?.copy(id = doc.id)
                }
            }
        onDispose { listener.remove() }
    }

    // ── Fetch current user profile (needed to pass name to notification)
    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    currentProfile = doc.toObject(UserProfile::class.java)
                }
        }
    }

    // ── Fetch member profiles when the members list changes
    LaunchedEffect(group?.members) {
        val memberIds = group?.members ?: return@LaunchedEffect
        memberIds.forEach { uid ->
            if (!memberProfiles.containsKey(uid)) {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val profile = doc.toObject(UserProfile::class.java) ?: return@addOnSuccessListener
                        memberProfiles = memberProfiles + (uid to profile)
                    }
            }
        }
    }

    val isMember = group?.members?.contains(currentUid) == true
    val isAdmin  = group?.adminUid == currentUid

    // ── Dialogs
    if (isActionLoading) LoadingDialog(actionMessage)

    if (showDeleteDialog) {
        ConfirmDialog(
            title        = "Delete Group",
            message      = "Permanently delete \"${group?.name}\"? This cannot be undone.",
            confirmLabel = "Delete",
            confirmColor = DangerRed,
            onConfirm    = {
                showDeleteDialog = false
                actionMessage    = "Deleting group…"
                isActionLoading  = true
                db.collection("groups").document(groupId).delete()
                    .addOnSuccessListener {
                        isActionLoading = false
                        Toast.makeText(context, "Group deleted.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    .addOnFailureListener { e ->
                        isActionLoading = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    kickTargetUid?.let { uid ->
        val memberName = memberProfiles[uid]?.name ?: "this member"
        ConfirmDialog(
            title        = "Remove Member",
            message      = "Remove $memberName from this group?",
            confirmLabel = "Remove",
            confirmColor = DangerRed,
            onConfirm    = {
                kickTargetUid   = null
                actionMessage   = "Removing member…"
                isActionLoading = true
                db.collection("groups").document(groupId)
                    .update("members", FieldValue.arrayRemove(uid))
                    .addOnSuccessListener {
                        isActionLoading = false
                        Toast.makeText(context, "$memberName removed.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        isActionLoading = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            onDismiss = { kickTargetUid = null }
        )
    }

    // ── Join group + send Firestore notification to admin
    fun joinGroup() {
        val g = group ?: return
        if (g.isFull) {
            Toast.makeText(context, "This group is already full.", Toast.LENGTH_SHORT).show()
            return
        }
        actionMessage   = "Joining group…"
        isActionLoading = true

        db.collection("groups").document(groupId)
            .update("members", FieldValue.arrayUnion(currentUid))
            .addOnSuccessListener {
                isActionLoading = false
                Toast.makeText(context, "You joined the group!", Toast.LENGTH_SHORT).show()

                // ── Write notification to admin's queue in Firestore
                val joinerName = currentProfile?.name ?: "A student"
                val notifData  = hashMapOf(
                    "type"       to "member_joined",
                    "memberName" to joinerName,
                    "memberUid"  to currentUid,
                    "groupId"    to groupId,
                    "groupName"  to g.name,
                    "timestamp"  to Date()
                )
                db.collection("notifications")
                    .document(g.adminUid)
                    .collection("items")
                    .add(notifData)
                // We don't block on this — fire and forget
            }
            .addOnFailureListener { e ->
                isActionLoading = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun leaveGroup() {
        actionMessage   = "Leaving group…"
        isActionLoading = true
        db.collection("groups").document(groupId)
            .update("members", FieldValue.arrayRemove(currentUid))
            .addOnSuccessListener {
                isActionLoading = false
                Toast.makeText(context, "You left the group.", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                isActionLoading = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        group?.name ?: "Group Detail",
                        color         = TextPrimary,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 18.sp,
                        letterSpacing = (-0.3).sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Group",
                                tint = DangerRed
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (group == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        val g = group!!

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Hero card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = DarkCard),
                    border   = if (isAdmin) CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(AdminGoldDim, AdminGold.copy(alpha = 0.5f)))
                    ) else null
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isAdmin) AdminGoldDim else AccentBluePale),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    tint     = if (isAdmin) AdminGold else AccentBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    g.name,
                                    fontSize      = 20.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    color         = TextPrimary,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(g.subject, fontSize = 14.sp, color = TextSecondary)
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatChip("Members",  "${g.memberCount}/${g.maxMembers}")
                            StatChip(
                                "Status",
                                if (g.isFull) "Full" else "Open",
                                if (g.isFull) DangerRed else AccentGreen
                            )
                            StatChip(
                                "Your Role",
                                when { isAdmin -> "Admin"; isMember -> "Member"; else -> "Guest" },
                                when { isAdmin -> AdminGold; isMember -> AccentBlue; else -> TextSecondary }
                            )
                        }
                    }
                }
            }

            // ── Members header
            item {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("MEMBERS", modifier = Modifier.weight(1f))
                    Text("${g.memberCount} / ${g.maxMembers}", fontSize = 12.sp, color = TextHint)
                }
            }

            // ── Member rows
            items(g.members) { uid ->
                val profile      = memberProfiles[uid]
                val isGroupAdmin = uid == g.adminUid

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (isGroupAdmin) AdminGoldPale else DarkCard
                    ),
                    border = if (isGroupAdmin) CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(AdminGoldDim, AdminGold.copy(alpha = 0.3f)))
                    ) else null
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LetterAvatar(
                            name       = profile?.name ?: "?",
                            size       = 42,
                            fontSize   = 15,
                            background = if (isGroupAdmin) AdminGoldDim else AccentBluePale,
                            textColor  = if (isGroupAdmin) AdminGold else AccentBlue
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    profile?.name ?: "Loading…",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = TextPrimary
                                )
                                if (isGroupAdmin) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AdminGoldDim)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "ADMIN",
                                            fontSize      = 9.sp,
                                            fontWeight    = FontWeight.ExtraBold,
                                            color         = AdminGold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                            if (profile != null) {
                                Text(
                                    "${profile.course} · ${profile.program}",
                                    fontSize = 12.sp,
                                    color    = TextHint
                                )
                            }
                        }
                        if (isAdmin && uid != currentUid && !isGroupAdmin) {
                            IconButton(
                                onClick  = { kickTargetUid = uid },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = "Remove",
                                    tint     = DangerRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Action buttons
            item {
                Spacer(Modifier.height(8.dp))
                when {
                    isAdmin -> {
                        OutlinedButton(
                            onClick  = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            border   = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Group", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    isMember -> {
                        OutlinedButton(
                            onClick  = { leaveGroup() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border   = BorderStroke(1.dp, DividerColor)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Leave Group", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> {
                        Button(
                            onClick  = { joinGroup() },
                            enabled  = !isActionLoading && !g.isFull,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = if (g.isFull) DangerRedDim else AccentBlue,
                                contentColor           = TextPrimary,
                                disabledContainerColor = DarkCard,
                                disabledContentColor   = TextHint
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (g.isFull) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Group is Full", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Join Group", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StatChip(
    label      : String,
    value      : String,
    valueColor : androidx.compose.ui.graphics.Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = TextHint, letterSpacing = 0.3.sp)
    }
}