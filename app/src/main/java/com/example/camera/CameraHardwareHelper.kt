package com.example.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log

data class CameraHardwareInfo(
    val hasPhysicalUltraWide: Boolean,
    val ultraWideCameraId: String?,
    val defaultBackCameraId: String?,
    val minZoomRatio: Float,
    val maxZoomRatio: Float,
    val minExposureIndex: Int,
    val maxExposureIndex: Int,
    val exposureStep: Float,
    val supportsRawCapture: Boolean,
    val physicalCameraIds: List<String>
)

object CameraHardwareHelper {
    private const val TAG = "CameraHardwareHelper"

    fun queryHardware(context: Context): CameraHardwareInfo {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return fallbackInfo()

        var hasUltraWide = false
        var ultraWideId: String? = null
        var defaultBackId: String? = null
        var minZoom = 1.0f
        var maxZoom = 10.0f
        var minEv = -4
        var maxEv = 4
        var evStep = 0.5f
        var supportsRaw = false
        val physicalIds = mutableListOf<String>()

        var lowestFocalLength = 999f
        var lowestFocalId: String? = null
        var defaultBackFocalLength = 4.5f

        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val minFocal = focalLengths?.minOrNull() ?: 4.0f

                    if (defaultBackId == null) {
                        defaultBackId = id
                        defaultBackFocalLength = minFocal
                    }

                    if (minFocal < lowestFocalLength) {
                        lowestFocalLength = minFocal
                        lowestFocalId = id
                    }

                    // Check zoom ratio range on Android 11+ (API 30+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val zoomRange = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                        if (zoomRange != null) {
                            if (zoomRange.lower < minZoom) {
                                minZoom = zoomRange.lower
                            }
                            if (zoomRange.upper > maxZoom) {
                                maxZoom = zoomRange.upper
                            }
                            if (zoomRange.lower <= 0.75f) {
                                hasUltraWide = true
                                ultraWideId = id
                            }
                        }
                    }

                    // Ultra-wide lenses typically have focal lengths <= 3.2mm
                    if (minFocal <= 3.2f) {
                        hasUltraWide = true
                        ultraWideId = id
                    }

                    // Check physical cameras if multi-camera
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val physicalCamIds = chars.physicalCameraIds
                        if (physicalCamIds.isNotEmpty()) {
                            physicalIds.addAll(physicalCamIds)
                            if (physicalCamIds.size > 1) {
                                hasUltraWide = true
                            }
                        }
                    }

                    // Exposure compensation
                    val evRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                    if (evRange != null) {
                        minEv = evRange.lower
                        maxEv = evRange.upper
                    }
                    val stepRational = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                    if (stepRational != null && stepRational.denominator != 0) {
                        evStep = stepRational.toFloat()
                    }

                    // RAW support
                    val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    if (capabilities != null) {
                        if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                            supportsRaw = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query camera hardware characteristics", e)
        }

        if (ultraWideId == null && lowestFocalId != null && lowestFocalId != defaultBackId && lowestFocalLength < defaultBackFocalLength) {
            ultraWideId = lowestFocalId
            hasUltraWide = true
        }

        // Even if hardware reports minZoom = 1.0f, our app provides 0.5x Ultra-Wide framing simulation
        return CameraHardwareInfo(
            hasPhysicalUltraWide = hasUltraWide,
            ultraWideCameraId = ultraWideId,
            defaultBackCameraId = defaultBackId,
            minZoomRatio = if (minZoom <= 0.65f) minZoom else 0.5f,
            maxZoomRatio = if (maxZoom >= 10.0f) maxZoom else 10.0f,
            minExposureIndex = minEv,
            maxExposureIndex = maxEv,
            exposureStep = evStep,
            supportsRawCapture = supportsRaw,
            physicalCameraIds = physicalIds
        )
    }

    private fun fallbackInfo() = CameraHardwareInfo(
        hasPhysicalUltraWide = false,
        ultraWideCameraId = null,
        defaultBackCameraId = null,
        minZoomRatio = 0.5f,
        maxZoomRatio = 10.0f,
        minExposureIndex = -4,
        maxExposureIndex = 4,
        exposureStep = 0.5f,
        supportsRawCapture = true,
        physicalCameraIds = emptyList()
    )
}
