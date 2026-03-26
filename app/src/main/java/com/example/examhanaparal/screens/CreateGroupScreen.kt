package com.example.examhanaparal.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.example.examhanaparal.ui.theme.*
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(navController: NavController) {
    val context      = LocalContext.current
    val db           = FirebaseFirestore.getInstance()
    val remoteConfig = FirebaseRemoteConfig.getInstance()

    var groupName  by remember { mutableStateOf("") }
    var subject    by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(false) }

    // Form is valid when both fields are filled
    val isFormValid = groupName.isNotBlank() && subject.isNotBlank()

    // Button press scale animation
    val buttonScale by animateFloatAsState(
        targetValue   = if (isLoading) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "btnScale"
    )

    fun createGroup() {
        if (!isFormValid) {
            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        val uid        = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            isLoading = false; return
        }
        val maxMembers = remoteConfig.getLong("max_members_per_group").takeIf { it > 0 } ?: 10L

        val data = hashMapOf(
            "name"       to groupName.trim(),
            "subject"    to subject.trim(),
            "adminUid"   to uid,
            "members"    to listOf(uid),
            "maxMembers" to maxMembers,
            "createdAt"  to Date()
        )

        db.collection("groups").add(data)
            .addOnSuccessListener {
                isLoading = false
                Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    if (isLoading) LoadingDialog("Creating your group…")

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Study Group",
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {

            // ── Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(18.dp),
                colors   = CardDefaults.cardColors(containerColor = DarkCard),
                border   = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(DividerColor, AccentBluePale, DividerColor))
                )
            ) {
                Row(
                    modifier          = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentBluePale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint     = AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "New Study Group",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                        Text(
                            "You will be the group administrator",
                            fontSize = 12.sp,
                            color    = TextHint
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionHeader("GROUP DETAILS")
            Spacer(Modifier.height(14.dp))

            HanapAralTextField(
                label         = "Group Name",
                value         = groupName,
                onValueChange = { groupName = it }
            )

            Spacer(Modifier.height(14.dp))

            HanapAralTextField(
                label         = "Subject / Topic",
                value         = subject,
                onValueChange = { subject = it }
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Maximum group size is set by your administrator.",
                fontSize = 12.sp,
                color    = TextHint,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(Modifier.height(36.dp))

            // ── Submit button
            Button(
                onClick  = { createGroup() },
                enabled  = !isLoading && isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = AccentBlue,
                    contentColor           = TextPrimary,
                    disabledContainerColor = DarkCard,
                    disabledContentColor   = TextHint
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation  = 4.dp,
                    pressedElevation  = 1.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Text(
                    text          = "Create Group",
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }

            // Hint text when form is incomplete
            AnimatedVisibility(
                visible = !isFormValid,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Fill in both fields to continue.",
                        fontSize = 12.sp,
                        color    = TextHint,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}