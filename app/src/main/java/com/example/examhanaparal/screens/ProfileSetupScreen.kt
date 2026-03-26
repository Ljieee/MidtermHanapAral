package com.example.examhanaparal.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.examhanaparal.models.UserProfile
import com.example.examhanaparal.navigation.Routes
import com.example.examhanaparal.ui.theme.*

// ── Admin access key — change this to your desired key
private const val ADMIN_ACCESS_KEY = "HANAPARAL_ADMIN"

@Composable
fun ProfileSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val user    = FirebaseAuth.getInstance().currentUser
    val db      = FirebaseFirestore.getInstance()

    var name      by remember { mutableStateOf(user?.displayName ?: "") }
    var course    by remember { mutableStateOf("") }
    var program   by remember { mutableStateOf("") }
    var role      by remember { mutableStateOf("student") }
    var adminKey  by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun saveProfile() {
        if (name.isBlank() || course.isBlank() || program.isBlank()) {
            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }
        if (role == "admin" && adminKey.trim() != ADMIN_ACCESS_KEY) {
            Toast.makeText(context, "Invalid admin access key.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        val profile = UserProfile(
            uid     = user?.uid ?: "",
            name    = name.trim(),
            course  = course.trim(),
            program = program.trim(),
            email   = user?.email ?: "",
            role    = role
        )
        db.collection("users").document(user!!.uid)
            .set(profile)
            .addOnSuccessListener {
                isLoading = false
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    if (isLoading) LoadingDialog("Saving your profile…")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top gradient bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(listOf(AccentBlueDim, AccentBlue, AccentBlueDim))
                )
        )

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(animationSpec = tween(400)) +
                    slideInVertically(initialOffsetY = { 60 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(40.dp))

                // Header
                Text(
                    text       = "Set Up Your Profile",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = "This info will be visible to your group members.",
                    fontSize = 14.sp,
                    color    = TextSecondary
                )

                Spacer(Modifier.height(36.dp))

                // ── Role Selector
                SectionHeader("SELECT YOUR ROLE")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoleCard(
                        modifier    = Modifier.weight(1f),
                        icon        = Icons.Default.School,
                        label       = "Student",
                        description = "Join and participate in study groups",
                        isSelected  = role == "student",
                        accentColor = AccentBlue,
                        dimColor    = AccentBluePale,
                        onClick     = { role = "student" }
                    )
                    RoleCard(
                        modifier    = Modifier.weight(1f),
                        icon        = Icons.Default.AdminPanelSettings,
                        label       = "Admin",
                        description = "Create and manage study groups",
                        isSelected  = role == "admin",
                        accentColor = AdminGold,
                        dimColor    = AdminGoldDim,
                        onClick     = { role = "admin" }
                    )
                }

                // Admin key field — appears only when admin is selected
                AnimatedVisibility(
                    visible = role == "admin",
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value         = adminKey,
                            onValueChange = { adminKey = it },
                            label         = { Text("Admin Access Key", fontSize = 13.sp) },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = AdminGold,
                                unfocusedBorderColor    = DividerColor,
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary,
                                cursorColor             = AdminGold,
                                focusedLabelColor       = AdminGold,
                                unfocusedLabelColor     = TextHint,
                                unfocusedContainerColor = DarkCard,
                                focusedContainerColor   = DarkCardAlt
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text     = "Obtain the access key from your instructor.",
                            fontSize = 11.sp,
                            color    = TextHint,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Profile fields
                SectionHeader("PROFILE INFORMATION")
                Spacer(Modifier.height(12.dp))

                HanapAralTextField(
                    label         = "Full Name",
                    value         = name,
                    onValueChange = { name = it }
                )
                Spacer(Modifier.height(14.dp))
                HanapAralTextField(
                    label         = "Course (e.g. BSCS, BSIT)",
                    value         = course,
                    onValueChange = { course = it }
                )
                Spacer(Modifier.height(14.dp))
                HanapAralTextField(
                    label         = "Program / Year (e.g. 2nd Year)",
                    value         = program,
                    onValueChange = { program = it }
                )

                Spacer(Modifier.height(36.dp))

                // ── Save button
                Button(
                    onClick  = { saveProfile() },
                    enabled  = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (role == "admin") AdminGold else AccentBlue,
                        contentColor   = if (role == "admin") Color(0xFF1A1000) else TextPrimary,
                        disabledContainerColor = DarkCard
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text          = "Save & Continue",
                        fontSize      = 16.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RoleCard(
    modifier    : Modifier,
    icon        : ImageVector,
    label       : String,
    description : String,
    isSelected  : Boolean,
    accentColor : Color,
    dimColor    : Color,
    onClick     : () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) accentColor else DividerColor,
        animationSpec = tween(200),
        label         = "border"
    )
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) dimColor.copy(alpha = 0.5f) else DarkCard,
        animationSpec = tween(200),
        label         = "bg"
    )

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                width  = if (isSelected) 2.dp else 1.dp,
                color  = borderColor,
                shape  = RoundedCornerShape(16.dp)
            ),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = if (isSelected) 0.2f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = if (isSelected) accentColor else TextHint,
                    modifier           = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text       = label,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = if (isSelected) accentColor else TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = description,
                fontSize  = 11.sp,
                color     = TextHint,
                lineHeight = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}