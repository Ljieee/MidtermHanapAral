package com.example.examhanaparal.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.examhanaparal.models.UserProfile
import com.example.examhanaparal.navigation.Routes
import com.example.examhanaparal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavController) {
    val context      = LocalContext.current
    val auth         = FirebaseAuth.getInstance()
    val db           = FirebaseFirestore.getInstance()
    val currentUid   = auth.currentUser?.uid ?: ""

    // ── State
    var profile          by remember { mutableStateOf<UserProfile?>(null) }
    var groupCount       by remember { mutableStateOf(0) }
    var isLoading        by remember { mutableStateOf(true) }
    var isSaving         by remember { mutableStateOf(false) }
    var isEditMode       by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoggingOut     by remember { mutableStateOf(false) }

    // ── Editable fields
    var editName    by remember { mutableStateOf("") }
    var editCourse  by remember { mutableStateOf("") }
    var editProgram by remember { mutableStateOf("") }

    // ── Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // ── Load profile
    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    val p = doc.toObject(UserProfile::class.java)
                    profile     = p
                    editName    = p?.name    ?: ""
                    editCourse  = p?.course  ?: ""
                    editProgram = p?.program ?: ""
                    isLoading   = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // ── Load group memberships count
    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            db.collection("groups")
                .whereArrayContains("members", currentUid)
                .get()
                .addOnSuccessListener { snap -> groupCount = snap.size() }
        }
    }

    // ── Save edited profile
    fun saveProfile() {
        if (editName.isBlank() || editCourse.isBlank() || editProgram.isBlank()) {
            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }
        isSaving = true
        db.collection("users").document(currentUid)
            .update(
                mapOf(
                    "name"    to editName.trim(),
                    "course"  to editCourse.trim(),
                    "program" to editProgram.trim()
                )
            )
            .addOnSuccessListener {
                profile = profile?.copy(
                    name    = editName.trim(),
                    course  = editCourse.trim(),
                    program = editProgram.trim()
                )
                isSaving    = false
                isEditMode  = false
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Dialogs & overlay loaders
    if (isSaving)    LoadingDialog("Saving changes…")
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
                auth.signOut()
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.MAIN) { inclusive = true }
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title        = "Delete Account",
            message      = "This will permanently remove your account and all associated data. This action cannot be undone.",
            confirmLabel = "Delete Account",
            confirmColor = DangerRed,
            onConfirm    = {
                showDeleteDialog = false
                isSaving = true
                db.collection("users").document(currentUid).delete()
                    .addOnSuccessListener {
                        auth.currentUser?.delete()
                        isSaving = false
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                    .addOnFailureListener { e ->
                        isSaving = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text          = "My Profile",
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
                    AnimatedContent(
                        targetState  = isEditMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label        = "editAction"
                    ) { editing ->
                        if (editing) {
                            // Cancel button
                            IconButton(onClick = {
                                // Revert edits
                                editName    = profile?.name    ?: ""
                                editCourse  = profile?.course  ?: ""
                                editProgram = profile?.program ?: ""
                                isEditMode  = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = DangerRed)
                            }
                        } else {
                            // Edit button
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = AccentBlue)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(animationSpec = tween(400)) +
                    slideInVertically(initialOffsetY = { 60 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {

                if (isLoading) {
                    // ── Skeleton loading state
                    repeat(3) {
                        ShimmerGroupCard()
                        Spacer(Modifier.height(10.dp))
                    }
                } else {
                    profile?.let { p ->

                        // ──────────────────────────────────────────
                        // SECTION 1 — Avatar + identity hero card
                        // ──────────────────────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(20.dp),
                            colors   = CardDefaults.cardColors(containerColor = DarkCard),
                            border   = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(
                                    listOf(DividerColor, AccentBluePale, DividerColor)
                                )
                            )
                        ) {
                            Column(
                                modifier            = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Avatar with ring
                                Box(contentAlignment = Alignment.Center) {
                                    // Outer glow ring
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (p.role == "admin")
                                                    AdminGoldDim.copy(alpha = 0.6f)
                                                else
                                                    AccentBluePale.copy(alpha = 0.7f)
                                            )
                                    )
                                    LetterAvatar(
                                        name       = p.name,
                                        size       = 76,
                                        fontSize   = 28,
                                        background = if (p.role == "admin") AdminGoldDim else AccentBluePale,
                                        textColor  = if (p.role == "admin") AdminGold else AccentBlue
                                    )
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text          = p.name,
                                    fontSize      = 22.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    color         = TextPrimary,
                                    letterSpacing = (-0.4).sp
                                )

                                Spacer(Modifier.height(6.dp))

                                RoleBadge(p.role)

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text      = p.email,
                                    fontSize  = 13.sp,
                                    color     = TextHint,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(20.dp))
                                HorizontalDivider(color = DividerColor)
                                Spacer(Modifier.height(20.dp))

                                // ── Stat row
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ProfileStat(
                                        icon  = Icons.Default.Group,
                                        value = groupCount.toString(),
                                        label = "Groups\nJoined"
                                    )
                                    VerticalDivider(
                                        modifier = Modifier.height(44.dp),
                                        color    = DividerColor
                                    )
                                    ProfileStat(
                                        icon  = Icons.Default.School,
                                        value = p.course,
                                        label = "Course"
                                    )
                                    VerticalDivider(
                                        modifier = Modifier.height(44.dp),
                                        color    = DividerColor
                                    )
                                    ProfileStat(
                                        icon  = Icons.Default.Badge,
                                        value = p.program.split(" ").firstOrNull() ?: p.program,
                                        label = "Year / Level"
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // ──────────────────────────────────────────
                        // SECTION 2 — Edit / View profile fields
                        // ──────────────────────────────────────────
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader("PROFILE INFORMATION", modifier = Modifier.weight(1f))
                            AnimatedVisibility(visible = isEditMode) {
                                Text(
                                    text     = "Editing",
                                    fontSize = 11.sp,
                                    color    = AccentBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        AnimatedContent(
                            targetState  = isEditMode,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(250)) + expandVertically()) togetherWith
                                        (fadeOut(animationSpec = tween(200)) + shrinkVertically())
                            },
                            label = "profileFields"
                        ) { editing ->
                            if (editing) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    HanapAralTextField(
                                        label         = "Full Name",
                                        value         = editName,
                                        onValueChange = { editName = it }
                                    )
                                    HanapAralTextField(
                                        label         = "Course (e.g. BSCS, BSIT)",
                                        value         = editCourse,
                                        onValueChange = { editCourse = it }
                                    )
                                    HanapAralTextField(
                                        label         = "Program / Year (e.g. 2nd Year)",
                                        value         = editProgram,
                                        onValueChange = { editProgram = it }
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ProfileInfoRow(
                                        icon  = Icons.Default.Person,
                                        label = "Full Name",
                                        value = p.name
                                    )
                                    ProfileInfoRow(
                                        icon  = Icons.Default.School,
                                        label = "Course",
                                        value = p.course
                                    )
                                    ProfileInfoRow(
                                        icon  = Icons.Default.Badge,
                                        label = "Program / Year",
                                        value = p.program
                                    )
                                    ProfileInfoRow(
                                        icon  = Icons.Default.Email,
                                        label = "Email",
                                        value = p.email
                                    )
                                }
                            }
                        }

                        // ── Save button — only visible in edit mode
                        AnimatedVisibility(
                            visible = isEditMode,
                            enter   = fadeIn() + expandVertically(),
                            exit    = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick  = { saveProfile() },
                                    enabled  = !isSaving,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape    = RoundedCornerShape(16.dp),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor         = AccentBlue,
                                        contentColor           = TextPrimary,
                                        disabledContainerColor = DarkCard
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier           = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text          = "Save Changes",
                                        fontSize      = 15.sp,
                                        fontWeight    = FontWeight.SemiBold,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // ──────────────────────────────────────────
                        // SECTION 3 — Account actions
                        // ──────────────────────────────────────────
                        SectionHeader("ACCOUNT")
                        Spacer(Modifier.height(12.dp))

                        AccountActionRow(
                            icon       = Icons.Default.Logout,
                            label      = "Sign Out",
                            tint       = DangerRed,
                            background = DangerRedDim,
                            onClick    = { showLogoutDialog = true }
                        )

                        Spacer(Modifier.height(10.dp))

                        AccountActionRow(
                            icon       = Icons.Default.DeleteForever,
                            label      = "Delete Account",
                            tint       = DangerRed.copy(alpha = 0.7f),
                            background = DangerRedDim.copy(alpha = 0.5f),
                            sublabel   = "Permanently remove all your data",
                            onClick    = { showDeleteDialog = true }
                        )

                        Spacer(Modifier.height(32.dp))

                        // App version footer
                        Text(
                            text      = "HanapAral · v1.0",
                            fontSize  = 11.sp,
                            color     = TextHint,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                    } ?: run {
                        // Profile failed to load
                        Box(
                            modifier         = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint     = TextHint,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(14.dp))
                                Text("Could not load profile", color = TextSecondary, fontSize = 15.sp)
                                Spacer(Modifier.height(6.dp))
                                Text("Pull down to retry", color = TextHint, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Stat column (icon + value + label)
@Composable
private fun ProfileStat(
    icon  : ImageVector,
    value : String,
    label : String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.widthIn(min = 72.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = AccentBlue,
            modifier           = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text          = value,
            fontSize      = 15.sp,
            fontWeight    = FontWeight.Bold,
            color         = TextPrimary,
            textAlign     = TextAlign.Center,
            letterSpacing = (-0.2).sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text      = label,
            fontSize  = 10.sp,
            color     = TextHint,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

// ── Read-only profile info row
@Composable
private fun ProfileInfoRow(
    icon  : ImageVector,
    label : String,
    value : String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = AccentBlue,
                    modifier           = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, fontSize = 11.sp, color = TextHint, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(2.dp))
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
        }
    }
}

// ── Tappable account action row
@Composable
private fun AccountActionRow(
    icon       : ImageVector,
    label      : String,
    tint       : Color,
    background : Color,
    sublabel   : String? = null,
    onClick    : () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = tint,
                    modifier           = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = tint)
                if (sublabel != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(sublabel, fontSize = 11.sp, color = TextHint)
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint     = TextHint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}