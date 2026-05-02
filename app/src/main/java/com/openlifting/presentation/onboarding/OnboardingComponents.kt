package com.openlifting.presentation.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.MuscleSide
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

// ── Step progress bar (segmented) ───────────────────────────────────────────

@Composable
fun StepProgressBar(currentIndex: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(total) { i ->
            val (color, _) = when {
                i <  currentIndex -> MaterialTheme.olExtras.ink3 to true
                i == currentIndex -> MaterialTheme.olExtras.emerald to true
                else              -> MaterialTheme.colorScheme.outlineVariant to false
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// ── Big countdown digit ─────────────────────────────────────────────────────

@Composable
fun CountdownDigit(value: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MonoText.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

// ── Big live activation bar (used during CONTRACT phase) ────────────────────

@Composable
fun BigActivationBar(
    pct: Float,
    label: String = "ACTIVACIÓN",
    color: Color = MaterialTheme.olExtras.emerald,
    modifier: Modifier = Modifier
) {
    val animatedPct by animateFloatAsState(targetValue = pct, label = "activation")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text  = label,
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3
            )
            Text(
                text  = "${animatedPct.toInt()}%",
                style = MonoText.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        LinearProgressIndicator(
            progress    = { (animatedPct / 100f).coerceIn(0f, 1f) },
            color       = color,
            trackColor  = MaterialTheme.colorScheme.surfaceVariant,
            modifier    = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            gapSize     = 0.dp,
            drawStopIndicator = {}
        )
    }
}

// ── 5×2 MVC checkpoints grid (used in MvcExplain + Done) ────────────────────

@Composable
fun MvcCheckGrid(
    captured: Map<Pair<Muscle, MuscleSide>, Float?> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val muscles = listOf(
        Muscle.VASTUS_LATERALIS, Muscle.VASTUS_MEDIALIS,
        Muscle.GLUTEUS_MAXIMUS,  Muscle.ERECTOR_SPINAE,  Muscle.BICEPS_FEMORIS
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header row
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(0.9f))
            Text(
                text  = "IZQUIERDA",
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3,
                modifier = Modifier.weight(1f)
            )
            Text(
                text  = "DERECHA",
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3,
                modifier = Modifier.weight(1f)
            )
        }
        // 5 muscle rows
        muscles.forEach { muscle ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = muscle.shortName,
                    style = MonoText.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.9f)
                )
                MvcCheckCell(
                    captured = captured[muscle to MuscleSide.LEFT],
                    modifier = Modifier.weight(1f)
                )
                MvcCheckCell(
                    captured = captured[muscle to MuscleSide.RIGHT],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MvcCheckCell(captured: Float?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(vertical = 2.dp), contentAlignment = Alignment.CenterStart) {
        if (captured != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.olExtras.emerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.olExtras.emeraldSoft,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text  = "${captured.toInt()}%",
                    style = MonoText.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.olExtras.ink3, CircleShape)
            )
        }
    }
}

// ── Muscle-side big pill (used as title block in MvcCapture) ────────────────

@Composable
fun MuscleSidePill(side: MuscleSide, modifier: Modifier = Modifier) {
    val label = when (side) {
        MuscleSide.LEFT  -> "IZQUIERDA"
        MuscleSide.RIGHT -> "DERECHA"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text  = label,
            style = MonoText.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Diagram placeholder (consistent with the Claude Design output) ──────────

@Composable
fun MuscleDiagramPlaceholder(
    sideLetter: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text  = "DIAGRAMA MUSCULAR",
                style = MonoText.labelSmall,
                color = MaterialTheme.olExtras.ink3
            )
            if (sideLetter != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.olExtras.emeraldSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = sideLetter,
                        style = MonoText.titleLarge,
                        color = MaterialTheme.olExtras.emerald
                    )
                }
            }
        }
    }
}
