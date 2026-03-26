package com.example.examhanaparal.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.example.examhanaparal.models.StudyGroup
import com.example.examhanaparal.models.UserProfile
import com.example.examhanaparal.navigation.Routes
import com.example.examhanaparal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val db           = FirebaseFirestore.getInstance()
    val remoteConfig = FirebaseRemoteConfig.getInstance()
    val currentUid   = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var groups           by remember { mutableStateOf<List<StudyGroup>>(emptyList()) }
    var currentUser      by remember { mutableStateOf<UserProfile?>(null) }
    var announcement     by remember { mutableStateOf("Welcome to HanapAral!") }
    var canCreateGroups  by remember { mutableStateOf(true) }

    // ── Remote Config: comma-separated list of disabled group IDs
    // Example in Firebase console: "groupId1,groupId2,groupId3"
    var disabledGroupIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var isLoadingGroups  by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut     by remember { mutableStateOf(false) }

    val isAdmin = currentUser?.role == "admin"

    // ── Fetch current user profile
    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    currentUser = doc.toObject(UserProfile::class.java)
                }
        }
    }

    // ── Remote Config — fetch and activate immediately
    LaunchedEffect(Unit) {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // For demo; use 3600 in production
            .build()
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "enable_group_creation" to true,
                "announcement_header"   to "Welcome to HanapAral!",
                "max_members_per_group" to 10L,
                "disabled_groups"       to ""   // empty = none disabled
            )
        )
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                announcement    = remoteConfig.getString("announcement_header")
                canCreateGroups = remoteConfig.getBoolean("enable_group_creation")

                // Parse disabled_groups: "id1,id2,id3" → Set<String>
                val raw = remoteConfig.getString("disabled_groups")
                disabledGroupIds = if (raw.isBlank()) emptySet()
                else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            }
        }
    }

    // ── Firestore real-time listener for groups
    DisposableEffect(Unit) {
        val listener = db.collection("groups")
            .addSnapshotListener { snapshots, _ ->
                snapshots ?: return@addSnapshotListener
                groups = snapshots.documents.mapNotNull { doc ->
                    doc.toObject(StudyGroup::class.java)?.copy(id = doc.id)
                }
                isLoadingGroups = false
            }
        onDispose { listener.remove() }
    }

    if (isLoggingOut) LoadingDialog("Signing out…")

    if (showLogoutDialog) {
        ConfirmDialog(
            title        = "Sign Out",
            message      = "Are you sure you want to sign out of HanapAral?",
            confirmLabel = "Sign Out",
            confirmColor = DangerRed,
            onConfirm    = {
                showLogoutDialog = false
                isLoggingOut     = true
                FirebaseAuth.getInstance().signOut()
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.MAIN) { inclusive = true }
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "HanapAral",
                            fontWeight    = FontWeight.ExtraBold,
                            color         = TextPrimary,
                            fontSize      = 20.sp,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.width(10.dp))
                        currentUser?.let { RoleBadge(it.role) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { navController.navigate(Routes.ADMIN) }) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel",
                                tint = AdminGold
                            )
                        }
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = TextSecondary)
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isAdmin && canCreateGroups,
                enter   = scaleIn() + fadeIn(),
                exit    = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick        = { navController.navigate(Routes.CREATE_GROUP) },
                    containerColor = AccentBlue,
                    contentColor   = TextPrimary,
                    shape          = RoundedCornerShape(16.dp),
                    icon           = { Icon(Icons.Default.Add, contentDescription = null) },
                    text           = { Text("New Group", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            // ── Announcement Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(AccentBluePale, DarkCard)))
                    .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        tint     = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(announcement, fontSize = 13.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── Section header
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Study Groups",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f)
                )
                if (!isLoadingGroups) {
                    Text(
                        "${groups.size} group${if (groups.size != 1) "s" else ""}",
                        fontSize = 13.sp,
                        color    = TextHint
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Group list
            when {
                isLoadingGroups -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(5) { ShimmerGroupCard() }
                    }
                }
                groups.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GroupOff, contentDescription = null, tint = TextHint, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(14.dp))
                            Text("No study groups yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (isAdmin) "Tap + to create the first group."
                                else "Check back later — groups will appear here.",
                                color = TextHint, fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding      = PaddingValues(bottom = 88.dp)
                    ) {
                        items(groups, key = { it.id }) { group ->
                            val isDisabled = group.id in disabledGroupIds
                            GroupCard(
                                group      = group,
                                currentUid = currentUid,
                                isDisabled = isDisabled,
                                onClick    = {
                                    if (!isDisabled) {
                                        navController.navigate(Routes.groupDetail(group.id))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCard(
    group      : StudyGroup,
    currentUid : String,
    isDisabled : Boolean = false,
    onClick    : () -> Unit
) {
    val isMember = group.members.contains(currentUid)
    val isAdmin  = group.adminUid == currentUid

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDisabled) { onClick() },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled) DarkCard.copy(alpha = 0.5f) else DarkCard
        ),
        border = when {
            isDisabled -> CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(DangerRedDim, DangerRed.copy(alpha = 0.3f)))
            )
            isAdmin    -> CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(AdminGoldDim, AdminGold.copy(alpha = 0.4f)))
            )
            else -> null
        }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            isDisabled -> DangerRedDim
                            isAdmin    -> AdminGoldDim
                            else       -> AccentBluePale
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDisabled) Icons.Default.Lock else Icons.Default.Group,
                    contentDescription = null,
                    tint     = when {
                        isDisabled -> DangerRed
                        isAdmin    -> AdminGold
                        else       -> AccentBlue
                    },
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.name,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 15.sp,
                    color         = if (isDisabled) TextHint else TextPrimary,
                    letterSpacing = (-0.1).sp
                )
                Spacer(Modifier.height(3.dp))
                Text(group.subject, fontSize = 13.sp, color = TextSecondary)

                if (isDisabled) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DangerRedDim)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Disabled by Admin",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = DangerRed,
                            letterSpacing = 0.3.sp
                        )
                    }
                } else if (isMember || isAdmin) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAdmin) AdminGoldDim else AccentBluePale)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (isAdmin) "Your Group" else "Member",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (isAdmin) AdminGold else AccentBlue,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            if (!isDisabled) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${group.memberCount}/${group.maxMembers}",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = when {
                            group.isFull -> DangerRed
                            group.memberCount >= group.maxMembers * 0.8 -> AdminGold
                            else -> AccentGreen
                        }
                    )
                    Text("members", fontSize = 10.sp, color = TextHint)
                }
            }
        }
    }
}
