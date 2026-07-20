package com.tankermanager.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tankermanager.app.ui.theme.Coral
import com.tankermanager.app.ui.theme.Foam
import com.tankermanager.app.ui.theme.HeroBrush
import com.tankermanager.app.ui.theme.InkMuted
import com.tankermanager.app.ui.theme.Lagoon
import com.tankermanager.app.ui.theme.LagoonDeep
import com.tankermanager.app.ui.theme.Mist
import com.tankermanager.app.ui.theme.Success
import com.tankermanager.app.ui.theme.Sun
import com.tankermanager.app.ui.theme.Warning

@Composable
fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Lagoon,
            unfocusedBorderColor = Color(0xFFC9E0DD),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LagoonDeep),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CoralButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 1.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun StatChip(
    title: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusPill(status: String?) {
    val (bg, fg) = statusColors(status)
    Text(
        text = friendlyStatus(status),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = fg,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

fun friendlyStatus(status: String?): String = when (status) {
    "ASSIGNED" -> "Assigned"
    "GOING_FOR_LOADING" -> "Going to bore"
    "LOADING" -> "Loading"
    "LOADING_COMPLETED" -> "Loaded"
    "EN_ROUTE" -> "On the way"
    "ARRIVED" -> "Arrived"
    "UNLOADING" -> "Unloading"
    "COMPLETED" -> "Completed"
    "CANCELLED" -> "Cancelled"
    else -> status ?: "—"
}

fun statusColors(status: String?): Pair<Color, Color> = when (status) {
    "COMPLETED" -> Success.copy(alpha = 0.15f) to Success
    "EN_ROUTE", "LOADING_COMPLETED" -> Lagoon.copy(alpha = 0.15f) to LagoonDeep
    "CANCELLED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    "ASSIGNED", "GOING_FOR_LOADING" -> Sun.copy(alpha = 0.2f) to Color(0xFF8A6A00)
    "LOADING", "UNLOADING", "ARRIVED" -> Warning.copy(alpha = 0.18f) to Color(0xFF8A6A00)
    else -> Mist to LagoonDeep
}

fun nextStatus(current: String?): String? = when (current) {
    "ASSIGNED" -> "GOING_FOR_LOADING"
    "GOING_FOR_LOADING" -> "LOADING"
    "LOADING" -> "LOADING_COMPLETED"
    "LOADING_COMPLETED" -> "EN_ROUTE"
    "EN_ROUTE" -> "ARRIVED"
    "ARRIVED" -> "UNLOADING"
    "UNLOADING" -> "COMPLETED"
    else -> null
}

@Composable
fun PulsingTruck(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier = modifier
            .size(88.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White, Mist))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.LocalShipping,
            contentDescription = null,
            tint = LagoonDeep,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun WaveBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HeroBrush)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Foam.copy(alpha = 0.25f), Foam)
                    )
                )
        )
        content()
    }
}

@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    padding: PaddingValues = PaddingValues(20.dp),
    topBarExtra: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                if (subtitle != null) {
                    Text(subtitle, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            topBarExtra?.invoke()
        }
        Spacer(modifier = Modifier.height(18.dp))
        content()
    }
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PulsingTruck()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = InkMuted)
    }
}

@Composable
fun ErrorBanner(message: String?) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut()
    ) {
        Text(
            text = message.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFEBEE))
                .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(14.dp))
                .padding(12.dp)
        )
    }
}
