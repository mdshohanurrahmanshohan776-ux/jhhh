package com.example.ui.gallery

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.CameraSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.GalleryFilterTab
import com.example.viewmodel.GalleryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBackToCamera: () -> Unit,
    onOpenEditor: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (uiState.selectedItem != null) {
            viewModel.selectItem(null)
        } else {
            onBackToCamera()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMedia()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("gallery_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar
            GalleryTopBar(
                onBack = {
                    if (uiState.selectedItem != null) {
                        viewModel.selectItem(null)
                    } else {
                        onBackToCamera()
                    }
                },
                onRefresh = { viewModel.loadMedia() }
            )

            // 2. Folder Organizer Statistics Card
            FolderOrganizerHeader(stats = uiState.folderStats)

            // 3. Category Filter Tabs (All, Photos, Videos, RAW, Favorites)
            GalleryTabs(
                selectedTab = uiState.currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )

            // 4. Media Grid
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
            } else if (uiState.filteredItems.isEmpty()) {
                EmptyGalleryView(tab = uiState.currentTab)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gallery_media_grid")
                ) {
                    items(uiState.filteredItems, key = { it.id }) { item ->
                        MediaGridThumbnail(
                            item = item,
                            onClick = { viewModel.selectItem(item) }
                        )
                    }
                }
            }
        }

        // Fullscreen Media Viewer / Player Overlay
        uiState.selectedItem?.let { item ->
            FullscreenMediaViewer(
                item = item,
                onClose = { viewModel.selectItem(null) },
                onToggleFavorite = { viewModel.toggleFavorite(item) },
                onShare = { viewModel.shareItem(item) },
                onDelete = { showDeleteConfirm = true },
                onOpenEditor = { onOpenEditor(item) },
                onShowExif = { viewModel.toggleExifDialog(true) }
            )
        }

        // Exif Info Dialog
        if (uiState.isShowingExifDialog && uiState.selectedItem != null) {
            MediaDetailsDialog(
                item = uiState.selectedItem!!,
                onDismiss = { viewModel.toggleExifDialog(false) }
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirm && uiState.selectedItem != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Media?", color = TextPrimary) },
                text = { Text("Are you sure you want to delete ${uiState.selectedItem!!.fileName}? This action cannot be undone.", color = TextSecondary) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteItem(uiState.selectedItem!!)
                            showDeleteConfirm = false
                        }
                    ) {
                        Text("Delete", color = AccentRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = CameraSurface
            )
        }
    }
}

@Composable
private fun GalleryTopBar(onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("gallery_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "UltraWide Studio",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Folder Organizer & Media Hub",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        IconButton(onClick = onRefresh, modifier = Modifier.testTag("gallery_refresh_button")) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = AccentGold
            )
        }
    }
}

@Composable
private fun FolderOrganizerHeader(stats: com.example.model.FolderStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("folder_stats_card"),
        colors = CardDefaults.cardColors(containerColor = CameraSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Folder Organizer",
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUTOMATIC FOLDER ORGANIZER",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "${stats.totalSizeMb} MB Storage",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Photos Folder Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CameraSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Photo, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Photos", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${stats.photosCount} Photos (${stats.rawCount} RAW)", color = TextSecondary, fontSize = 11.sp)
                        Text(text = stats.photosPath, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Videos Folder Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CameraSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Videos", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${stats.videosCount} Video Clips", color = TextSecondary, fontSize = 11.sp)
                        Text(text = stats.videosPath, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTabs(
    selectedTab: GalleryFilterTab,
    onTabSelected: (GalleryFilterTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = CameraBackground,
        contentColor = AccentGold,
        edgePadding = 12.dp,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = AccentGold
            )
        }
    ) {
        GalleryFilterTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.label,
                        color = if (selectedTab == tab) AccentGold else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier.testTag("gallery_tab_${tab.name}")
            )
        }
    }
}

@Composable
private fun MediaGridThumbnail(
    item: MediaItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(CameraSurface)
            .clickable { onClick() }
            .testTag("media_thumbnail_${item.fileName}")
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Ultra-Wide 0.5x Badge
        if (item.isUltraWide) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "0.5x",
                    color = AccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // RAW Badge
        if (item.mediaType == MediaType.RAW) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(AccentCyan, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "RAW",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Video Duration Badge
        if (item.mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "VID",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Favorite Heart Icon
        if (item.isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = AccentRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FullscreenMediaViewer(
    item: MediaItem,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onOpenEditor: () -> Unit,
    onShowExif: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = item.mediaType == MediaType.VIDEO

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("fullscreen_viewer")
    ) {
        // Center Media Display (Photo or Video Player)
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(item.uri)
                        val mc = MediaController(ctx)
                        mc.setAnchorView(this)
                        setMediaController(mc)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = item.uri,
                contentDescription = item.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("viewer_close_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = TextPrimary)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.fileName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${item.folderType} • ${if (item.isUltraWide) "0.5x Ultra-Wide" else "Standard Lens"}",
                    color = AccentGold,
                    fontSize = 10.sp
                )
            }

            IconButton(onClick = onShowExif, modifier = Modifier.testTag("viewer_exif_button")) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = TextPrimary)
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit in Studio (Only for Photos/RAW)
            if (!isVideo) {
                IconButton(onClick = onOpenEditor, modifier = Modifier.testTag("viewer_edit_button")) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Edit in Studio", tint = AccentGold)
                        Text("Edit", color = AccentGold, fontSize = 10.sp)
                    }
                }
            }

            // Favorite
            IconButton(onClick = onToggleFavorite, modifier = Modifier.testTag("viewer_favorite_button")) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) AccentRed else TextPrimary
                    )
                    Text("Favorite", color = TextPrimary, fontSize = 10.sp)
                }
            }

            // Share
            IconButton(onClick = onShare, modifier = Modifier.testTag("viewer_share_button")) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                    Text("Share", color = TextPrimary, fontSize = 10.sp)
                }
            }

            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.testTag("viewer_delete_button")) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                    Text("Delete", color = AccentRed, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun MediaDetailsDialog(
    item: MediaItem,
    onDismiss: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(item.dateAdded))
    val sizeMb = String.format(Locale.US, "%.2f MB", item.sizeBytes / (1024f * 1024f))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Media Metadata (EXIF)", color = AccentGold, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailItem("File Name", item.fileName)
                DetailItem("Storage Folder", item.folderType)
                DetailItem("Lens Mode", if (item.isUltraWide) "0.5x Ultra-Wide Angle" else "${item.zoomRatio}x Zoom")
                DetailItem("File Size", sizeMb)
                DetailItem("Date Created", dateStr)
                DetailItem("File Path", item.filePath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentGold, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CameraSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyGalleryView(tab: GalleryFilterTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No ${tab.label} Found",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Photos & videos captured with the UltraWide camera will automatically be saved and organized here.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
