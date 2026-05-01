package com.openlifting.domain.model

enum class Muscle(val displayName: String, val shortName: String) {
    VASTUS_LATERALIS("Vasto Lateral", "VL"),
    VASTUS_MEDIALIS("Vasto Medial", "VM"),
    GLUTEUS_MAXIMUS("Glúteo Mayor", "GMax"),
    ERECTOR_SPINAE("Erector Espinal", "ES"),
    BICEPS_FEMORIS("Bíceps Femoral", "BF")
}

enum class MuscleSide(val label: String) {
    LEFT("Izq"),
    RIGHT("Der")
}
