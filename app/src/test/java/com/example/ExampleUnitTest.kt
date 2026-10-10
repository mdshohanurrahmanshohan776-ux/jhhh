package com.example

import com.example.model.CameraFilter
import com.example.model.CameraMode
import com.example.model.LensPreset
import com.example.model.MediaType
import com.example.editor.EditAdjustments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLensPresets() {
        assertEquals(0.5f, LensPreset.ULTRA_WIDE_05.ratio, 0.01f)
        assertEquals("0.5x", LensPreset.ULTRA_WIDE_05.label)
        assertEquals(1.0f, LensPreset.WIDE_1.ratio, 0.01f)
        assertEquals(2.0f, LensPreset.TELE_2.ratio, 0.01f)
        assertEquals(5.0f, LensPreset.ZOOM_5.ratio, 0.01f)
        assertEquals(10.0f, LensPreset.SUPER_10.ratio, 0.01f)
    }

    @Test
    fun testCameraModes() {
        assertEquals("PHOTO", CameraMode.PHOTO.label)
        assertEquals("VIDEO", CameraMode.VIDEO.label)
        assertEquals("PRO MANUAL", CameraMode.PRO.label)
    }

    @Test
    fun testCameraFilters() {
        assertTrue(CameraFilter.entries.any { it.id == "vivid_wide" })
        assertTrue(CameraFilter.entries.any { it.id == "golden_hour" })
        assertTrue(CameraFilter.entries.any { it.id == "cinematic" })
        assertTrue(CameraFilter.entries.any { it.id == "noir" })
    }

    @Test
    fun testEditAdjustmentsDefaults() {
        val adjustments = EditAdjustments()
        assertEquals(0f, adjustments.exposure, 0.01f)
        assertEquals(0f, adjustments.contrast, 0.01f)
        assertEquals(0f, adjustments.saturation, 0.01f)
        assertEquals(0, adjustments.rotationDegrees)
        assertFalse(adjustments.flipHorizontal)
        assertEquals(CameraFilter.NORMAL, adjustments.selectedFilter)
    }

    @Test
    fun testMediaTypeCategorization() {
        val photoType = MediaType.PHOTO
        val videoType = MediaType.VIDEO
        val rawType = MediaType.RAW

        assertEquals("PHOTO", photoType.name)
        assertEquals("VIDEO", videoType.name)
        assertEquals("RAW", rawType.name)
    }
}
