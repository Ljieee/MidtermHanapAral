package com.example.examhanaparal.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.examhanaparal.R
import com.example.examhanaparal.navigation.Routes
import com.example.examhanaparal.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(navController: NavController) {
    val context   = LocalContext.current
    val auth      = FirebaseAuth.getInstance()
    val db        = FirebaseFirestore.getInstance()
    var isLoading by remember { mutableStateOf(false) }

    // Staggered entrance animation states
    var showLogo   by remember { mutableStateOf(false) }
    var showTitle  by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(80);  showLogo  = true
        delay(180); showTitle = true
        delay(160); showButton = true
    }

    // Subtle pulse on logo
    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glow alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // ── Google Sign-In result handler
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account    = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isLoading = true
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: run { isLoading = false; return@addOnCompleteListener }
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            isLoading = false
                            if (doc.exists()) {
                                navController.navigate(Routes.MAIN) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Routes.PROFILE_SETUP) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    isLoading = false
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            isLoading = false
            Toast.makeText(context, "Sign-in error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchGoogleSignIn() {
        val gso    = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        launcher.launch(client.signInIntent)
    }

    if (isLoading) LoadingDialog("Signing you in…")

    // ── Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepNavy, DarkBackground, DarkBackground)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative radial glow behind the logo area
        Box(
            modifier = Modifier
                .size(380.dp)
                .offset(y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = glowAlpha * 0.18f),
                            DarkBackground.copy(alpha = 0f)
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
        ) {

            // ── Logo
            AnimatedVisibility(
                visible = showLogo,
                enter   = scaleIn(
                    initialScale  = 0.3f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(300))
            ) {
                Box(modifier = Modifier.scale(pulseScale)) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(30.dp))
                            .background(AccentBlue.copy(alpha = glowAlpha * 0.3f))
                    )
                    // Icon box
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AccentBlue, AccentBlueDim)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.School,
                            contentDescription = "HanapAral Logo",
                            tint               = TextPrimary,
                            modifier           = Modifier.size(52.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Title & subtitle
            AnimatedVisibility(
                visible = showTitle,
                enter   = slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec  = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(350))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text          = "HanapAral",
                        fontSize      = 38.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = TextPrimary,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text          = "Cloud-Based Study Group Finder",
                        fontSize      = 14.sp,
                        color         = TextSecondary,
                        textAlign     = TextAlign.Center,
                        letterSpacing = 0.3.sp,
                        lineHeight    = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(72.dp))

            // ── Sign-in button
            AnimatedVisibility(
                visible = showButton,
                enter   = slideInVertically(
                    initialOffsetY = { 70 },
                    animationSpec  = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(400))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick  = { launchGoogleSignIn() },
                        enabled  = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor   = TextPrimary,
                            disabledContainerColor = AccentBlueDim
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.School,
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text          = "Continue with Google",
                                fontSize      = 16.sp,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    HorizontalDivider(
                        color     = DividerColor,
                        modifier  = Modifier.fillMaxWidth(0.6f)
                    )

                    Spacer(Modifier.height(22.dp))

                    Text(
                        text      = "Only verified school accounts are\nallowed to access this app.",
                        fontSize  = 12.sp,
                        color     = TextHint,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
