package com.example.model

enum class CameraMode(val label: String) {
    PHOTO("PHOTO"),
    VIDEO("VIDEO"),
    PRO("PRO MANUAL")
}

enum class LensPreset(val ratio: Float, val label: String) {
    ULTRA_WIDE_05(0.5f, "0.5x"),
    WIDE_1(1.0f, "1x"),
    TELE_2(2.0f, "2x"),
    ZOOM_5(5.0f, "5x"),
    SUPER_10(10.0f, "10x")
}

enum class FlashMode(val iconName: String, val label: String) {
    OFF("flash_off", "Off"),
    AUTO("flash_auto", "Auto"),
    ON("flash_on", "On"),
    TORCH("highlight", "Torch")
}

enum class FocusMode(val label: String) {
    AF("AF (Auto)"),
    MF("MF (Manual)")
}

enum class WhiteBalance(val label: String, val kelvin: Int) {
    AUTO("Auto WB", 5000),
    DAYLIGHT("Daylight", 5500),
    CLOUDY("Cloudy", 6500),
    TUNGSTEN("Tungsten", 3200),
    FLUORESCENT("Fluorescent", 4000)
}

enum class GridMode(val label: String) {
    NONE("Grid Off"),
    RULE_OF_THIRDS("3x3 Grid"),
    GOLDEN_RATIO("Golden Ratio")
}

enum class TimerMode(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_5(5, "5s"),
    SEC_10(10, "10s")
}

enum class CameraFilter(
    val id: String,
    val displayName: String,
    val description: String,
    val previewHue: Long
) {
    NORMAL("normal", "Standard", "True to life colors", 0xFF607D8B),
    VIVID_WIDE("vivid_wide", "Vivid 0.5x", "Punchy sky & landscape greens", 0xFF00B0FF),
    GOLDEN_HOUR("golden_hour", "Golden Hour", "Warm sunset amber radiance", 0xFFFF9800),
    CINEMATIC("cinematic", "Teal & Orange", "Hollywood blockbuster grade", 0xFF009688),
    NOIR("noir", "Noir B&W", "High contrast dramatic monochrome", 0xFF424242),
    CYBERPUNK("cyberpunk", "Cyberpunk", "Neon magenta & electric cyan", 0xFFE040FB),
    VINTAGE("vintage", "Vintage 70s", "Warm faded analog film look", 0xFF8D6E63),
    MOODY_FOREST("moody_forest", "Moody Forest", "Deep shadows & emerald tones", 0xFF2E7D32)
}
