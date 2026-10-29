package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity

/**
 * Clean main launcher header:
 * - Code0 branding on the left
 * - Settings icon in the top-right corner
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherTopBar(
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Code0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("launcher_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Compact top toolbar for the active coding workspace:
 * - Back button to return to launcher
 * - Project name & active file name
 * - File-explorer toggle
 * - Save action (with dirty status)
 * - AI Assistant action
 * - Git / Source control action
 * - Contextual overflow menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTopBar(
    project: ProjectEntity,
    activeFileName: String?,
    isFileDirty: Boolean,
    isAIPanelOpen: Boolean,
    isGitPanelOpen: Boolean,
    isPreviewAvailable: Boolean = false,
    onBackToLauncher: () -> Unit,
    onToggleExplorer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPreview: () -> Unit,
    onSaveFile: () -> Unit,
    onToggleAI: () -> Unit,
    onToggleGit: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onExportZip: () -> Unit,
    onCloseProject: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onBackToLauncher,
                modifier = Modifier.testTag("workspace_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Projects",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        title = {
            Column {
                Text(
                    text = project.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activeFileName != null) {
                    Text(
                        text = if (isFileDirty) "$activeFileName • Unsaved" else activeFileName,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isFileDirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            // File Explorer Toggle
            IconButton(
                onClick = onToggleExplorer,
                modifier = Modifier.testTag("workspace_explorer_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Project Files",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Find in Files Action
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.testTag("workspace_search_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Find in Files",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Live Preview Action (shows if file is previewable)
            if (isPreviewAvailable) {
                IconButton(
                    onClick = onOpenPreview,
                    modifier = Modifier.testTag("workspace_preview_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Live Preview",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Save Action (with dirty indicator)
            IconButton(
                onClick = onSaveFile,
                modifier = Modifier.testTag("workspace_save_btn")
            ) {
                BadgedBox(
                    badge = {
                        if (isFileDirty) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save File",
                        tint = if (isFileDirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // AI Assistant Action
            IconButton(
                onClick = onToggleAI,
                modifier = Modifier.testTag("workspace_ai_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = if (isAIPanelOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Git / Source Control Action
            IconButton(
                onClick = onToggleGit,
                modifier = Modifier.testTag("workspace_git_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Source,
                    contentDescription = "Source Control",
                    tint = if (isGitPanelOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Contextual Overflow Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("workspace_overflow_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Operations",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Find in Files") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpenSearch()
                        }
                    )
                    if (isPreviewAvailable) {
                        DropdownMenuItem(
                            text = { Text("Live Preview") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenPreview()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Run Diagnostics") },
                        leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRunDiagnostics()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Project ZIP") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onExportZip()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Close Project") },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onCloseProject()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Reusable detail screen top bar with clear back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("detail_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
