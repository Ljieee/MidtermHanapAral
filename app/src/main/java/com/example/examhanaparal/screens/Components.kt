package com.example.examhanaparal.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mij.hanaparal.models.StudyGroup
import com.mij.hanaparal.ui.theme.*

// ── Reusable styled text field
@Composable
fun HanapAralTextField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    modifier     : Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
) {
    OutlinedTextField(
        value              = value,
        onValueChange      = onValueChange,
        label              = { Text(label, fontSize = 13.sp) },
        singleLine         = true,
        modifier           = modifier.fillMaxWidth(),
        keyboardOptions    = keyboardOptions,
        visualTransformation = visualTransformation,
        colors             = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = AccentBlue,
            unfocusedBorderColor    = DividerColor,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary,
            cursorColor             = AccentBlue,
            focusedLabelColor       = AccentBlue,
            unfocusedLabelColor     = TextHint,
            unfocusedContainerColor = DarkCard,
            focusedContainerColor   = DarkCardAlt
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

// ── Full-screen loading dialog
@Composable
fun LoadingDialog(message: String = "Please wait...") {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    colors = listOf(DividerColor, AccentBluePale, DividerColor)
                )
            )
        ) {
            Column(
                modifier            = Modifier.padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spinning ring
                CircularProgressIndicator(
                    color       = AccentBlue,
                    strokeWidth = 3.dp,
                    modifier    = Modifier.size(52.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text       = message,
                    color      = TextPrimary,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = "This won't take long",
                    color    = TextHint,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── Study group card item
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
            .clickable(enabled = !isDisabled) { onClick() }
            .alpha(if (isDisabled) 0.6f else 1f),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = if (isAdmin) CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(AdminGoldDim, AdminGold.copy(alpha = 0.4f)))
        ) else null
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAdmin) AdminGoldDim else AccentBluePale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint     = if (isAdmin) AdminGold else AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = group.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Text(
                    text     = group.subject,
                    fontSize = 12.sp,
                    color    = TextSecondary
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Member count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentBluePale)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${group.memberCount}/${group.maxMembers}",
                            fontSize   = 10.sp,
                            color      = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isMember) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentGreenDim)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Joined",
                                fontSize   = 10.sp,
                                color      = AccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isDisabled) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Disabled",
                    tint     = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint     = TextHint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
