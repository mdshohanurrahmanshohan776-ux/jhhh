package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.MediaItem
import com.example.ui.camera.CameraScreen
import com.example.ui.editor.PhotoEditorScreen
import com.example.ui.gallery.GalleryScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltraWideCameraTheme
import com.example.viewmodel.CameraViewModel
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.GalleryViewModel

sealed class AppScreen {
    object Camera : AppScreen()
    object Gallery : AppScreen()
    data class Editor(val item: MediaItem) : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()
    private val galleryViewModel: GalleryViewModel by viewModels()
    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UltraWideCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CameraBackground
                ) {
                    UltraWideApp(
                        cameraViewModel = cameraViewModel,
                        galleryViewModel = galleryViewModel,
                        editorViewModel = editorViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun UltraWideApp(
    cameraViewModel: CameraViewModel,
    galleryViewModel: GalleryViewModel,
    editorViewModel: EditorViewModel
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Camera) }

    if (!hasCameraPermission) {
        CameraPermissionPrompt(
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        )
    } else {
        when (val screen = currentScreen) {
            is AppScreen.Camera -> {
                CameraScreen(
                    viewModel = cameraViewModel,
                    onNavigateToGallery = {
                        galleryViewModel.loadMedia()
                        currentScreen = AppScreen.Gallery
                    }
                )
            }
            is AppScreen.Gallery -> {
                GalleryScreen(
                    viewModel = galleryViewModel,
                    onBackToCamera = {
                        cameraViewModel.refreshLastMedia()
                        currentScreen = AppScreen.Camera
                    },
                    onOpenEditor = { item ->
                        currentScreen = AppScreen.Editor(item)
                    }
                )
            }
            is AppScreen.Editor -> {
                PhotoEditorScreen(
                    mediaItem = screen.item,
                    viewModel = editorViewModel,
                    onBack = {
                        currentScreen = AppScreen.Gallery
                    },
                    onSaved = { savedItem ->
                        galleryViewModel.loadMedia()
                        cameraViewModel.refreshLastMedia()
                        currentScreen = AppScreen.Gallery
                    }
                )
            }
        }
    }
}

@Composable
fun CameraPermissionPrompt(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBackground)
            .padding(24.dp)
            .testTag("permission_prompt_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(CameraSurface, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFC837))
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Permission",
                    tint = AccentGold,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "UltraWide Pro Camera",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To capture ultra-wide 0.5x photos, record videos, and adjust manual exposure & focus, please grant Camera and Audio permissions.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_permission_button")
            ) {
                Text(
                    text = "Grant Camera Permissions",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
