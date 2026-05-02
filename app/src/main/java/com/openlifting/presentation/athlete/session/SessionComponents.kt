package com.openlifting.presentation.athlete.session

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.openlifting.domain.model.Muscle
import com.openlifting.domain.model.Recommendation
import com.openlifting.domain.model.RiskLevel
import com.openlifting.domain.model.SquatDepth
import com.openlifting.domain.model.SquatVariant
import com.openlifting.presentation.common.RiskBadge
import com.openlifting.presentation.common.toColor
import com.openlifting.presentation.common.toSoftColor
import com.openlifting.ui.theme.MonoText
import com.openlifting.ui.theme.olExtras

// ── SetHeader: shared across Metadata, Measuring, Analysis ──────────────────

@Composable
fun SetHeader(
    loadKg: Float,
    targetReps: Int,
    rpe: Float? = null,
    variant: SquatVariant? = null,
    depth: SquatDepth? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text  = "${loadKg.toInt()} kg",
                style = MonoText.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "× $targetReps reps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (rpe != null || variant != null || depth != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rpe?.let { ChipPill("RPE ${formatRpe(it)}") }
                variant?.let { ChipPill(it.displayName) }
                depth?.let { ChipPill(it.displayName) }
            }
        }
    }
}

@Composable
private fun ChipPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatRpe(rpe: Float): String =
    if (rpe == rpe.toInt().toFloat()) rpe.toInt().toString()
    else "%.1f".format(rpe)

// ── BilateralRow: muscle name + L/R bars + BSA chip ─────────────────────────

@Composable
fun BilateralRow(
    muscle: Muscle,
    leftPct: Float,
    rightPct: Float,
    bsaPct: Float,
    risk: RiskLevel
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Muscle short name in mono
        Text(
            text  = muscle.shortName,
            style = MonoText.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp)
        )

        // L/R bars stacked
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BilateralSideBar(label = "IZQ", pct = leftPct, risk = risk)
            BilateralSideBar(label = "DER", pct = rightPct, risk = risk)
        }

        // BSA chip on the right
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(risk.toSoftColor())
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .width(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "%.0f%%".format(bsaPct),
                style = MonoText.labelSmall,
                color = risk.toColor()
            )
        }
    }
}

@Composable
private fun BilateralSideBar(label: String, pct: Float, risk: RiskLevel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text  = label,
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3,
            modifier = Modifier.width(28.dp)
        )
        LinearProgressIndicator(
            progress    = { (pct / 100f).coerceIn(0f, 1f) },
            color       = risk.toColor(),
            trackColor  = MaterialTheme.colorScheme.surfaceVariant,
            modifier    = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            gapSize     = 0.dp,
            drawStopIndicator = {}
        )
        Text(
            text  = "${pct.toInt()}%",
            style = MonoText.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp)
        )
    }
}

// ── MetricCard (with risk badge, used in Set Analysis grid) ─────────────────

@Composable
fun MetricCard(
    label: String,
    value: String,
    risk: RiskLevel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text  = label.uppercase(),
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3
        )
        Text(
            text  = value,
            style = MonoText.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        RiskBadge(risk)
    }
}

// ── RecommendationCard ──────────────────────────────────────────────────────

@Composable
fun RecommendationCard(rec: Recommendation) {
    val severityColor = rec.severity.toColor()
    val softColor     = rec.severity.toSoftColor()
    val icon: ImageVector = when (rec.severity) {
        RiskLevel.RISK    -> Icons.Filled.WarningAmber
        RiskLevel.MONITOR -> Icons.Filled.WarningAmber
        RiskLevel.NORMAL  -> Icons.Filled.Info
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(softColor)
            .border(1.dp, severityColor.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = severityColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text  = rec.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Sets recap mini-table (Session Summary) ─────────────────────────────────

@Composable
fun SetsRecapTable(items: List<SetRecapItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        items.forEachIndexed { idx, item ->
            if (idx > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Set number circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = item.setNumber.toString(),
                        style = MonoText.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text  = "${item.loadKg.toInt()} kg × ${item.targetReps}",
                        style = MonoText.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = "BSA peor ${"%.1f".format(item.bsaWorstPct)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.olExtras.ink3
                    )
                }
                RiskBadge(item.overallRisk)
            }
        }
    }
}

// ── Inline live-bar (used in Measuring substate to suggest mid-flight) ──────

@Composable
fun InlineLiveBar(
    label: String,
    pct: Float,
    color: Color = MaterialTheme.olExtras.emerald,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MonoText.labelSmall,
            color = MaterialTheme.olExtras.ink3,
            modifier = Modifier.width(48.dp)
        )
        LinearProgressIndicator(
            progress = { (pct / 100f).coerceIn(0f, 1f) },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Spacer(Modifier.width(4.dp))
    }
}
