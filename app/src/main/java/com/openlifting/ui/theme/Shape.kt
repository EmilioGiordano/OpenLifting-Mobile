package com.openlifting.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val OpenLiftingShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // chips, small pills
    small      = RoundedCornerShape(8.dp),   // input fields, segmented controls
    medium     = RoundedCornerShape(12.dp),  // cards (default)
    large      = RoundedCornerShape(16.dp),  // hero cards, sheets
    extraLarge = RoundedCornerShape(24.dp)   // modals
)
