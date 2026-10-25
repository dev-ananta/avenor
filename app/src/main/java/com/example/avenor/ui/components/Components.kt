package com.example.avenor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avenor.ui.theme.Av
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Av.RadiusL))
            .background(Av.Surface)
            .border(1.dp, Av.Border, RoundedCornerShape(Av.RadiusL))
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Av.Text)
        if (subtitle != null) Text(subtitle, color = Av.Muted, fontSize = 15.sp)
    }
}

@Composable
fun SectionHeading(title: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Av.Muted, fontSize = 14.sp)
    }
}

@Composable
fun Chip(text: String, color: Color = Av.Muted, icon: ImageVector? = null) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Fitts's Law: full-width, 56 dp tall, placed in the thumb zone wherever it's the main action. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = Av.Accent,
    contentColor: Color = Color.Black,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = Av.PrimaryHeight),
        shape = RoundedCornerShape(Av.RadiusM),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = contentColor),
    ) {
        if (icon != null) { Icon(icon, contentDescription = null); Box(Modifier.width(8.dp)) }
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(Av.RadiusM),
        border = BorderStroke(1.dp, Av.Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Av.Elevated, contentColor = Av.Text),
    ) {
        if (icon != null) { Icon(icon, contentDescription = null); Box(Modifier.width(8.dp)) }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, stroke: Dp = 10.dp, tint: Color = Av.Accent) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "ring")
    Canvas(modifier) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = Size(size.width - w, size.height - w)
        drawArc(Av.Elevated, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        drawArc(tint, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

@Composable
fun RowScope.StatTile(value: String, label: String, color: Color = Av.Text) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(Av.RadiusM))
            .background(Av.Elevated)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Av.Muted)
    }
}

@Composable
fun EmptyHint(icon: ImageVector, title: String, message: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = Av.Accent)
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(message, color = Av.Muted, fontSize = 14.sp)
    }
}

@Composable
fun MasteryDots(box: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.semantics { contentDescription = "Mastery $box of 5" },
    ) {
        for (level in 1..5) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (level <= box) color else Av.Elevated))
        }
    }
}

/** Fitts's Law in reverse: ending early takes a 1.5-second hold, so impulsive quits don't happen. */
@Composable
fun HoldToEndButton(onEnd: () -> Unit) {
    var holding by remember { mutableStateOf(false) }
    val currentOnEnd by rememberUpdatedState(onEnd)
    val fill by animateFloatAsState(if (holding) 1f else 0f, tween(if (holding) 1500 else 200), label = "hold")
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Av.MinTap)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                onClick(label = "End session early") { currentOnEnd(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    holding = true
                    val released = withTimeoutOrNull(1500) { tryAwaitRelease() }
                    holding = false
                    if (released == null) currentOnEnd()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize()) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Av.Red.copy(alpha = 0.25f)))
        }
        Text(if (holding) "Keep holding to end…" else "Hold to end early", color = Av.Muted, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(12.dp))
    }
}

fun clock(millis: Long): String {
    val total = ((millis + 999) / 1000).toInt()
    return "%02d:%02d".format(total / 60, total % 60)
}
