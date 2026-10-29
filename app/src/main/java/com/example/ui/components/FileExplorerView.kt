package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileExplorerLayout
import com.example.data.model.ProjectFile

@Composable
fun FileExplorerView(
    files: List<ProjectFile>,
    layout: FileExplorerLayout,
    onFileClick: (ProjectFile) -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<ProjectFile?>(null) }

    // State for Expandable Tree
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }

    // State for Traditional Breadcrumb Navigation
    var currentNavPath by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        // Search & Creation Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search files...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f).height(46.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = { showNewFileDialog = true },
                modifier = Modifier.testTag("explorer_new_file_btn")
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = { showNewFolderDialog = true },
                modifier = Modifier.testTag("explorer_new_folder_btn")
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Layout Rendering
        when (layout) {
            FileExplorerLayout.EXPANDABLE_TREE -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    val filtered = filterProjectFiles(files, searchQuery)
                    items(filtered) { file ->
                        TreeFileItem(
                            file = file,
                            depth = 0,
                            expandedMap = expandedFolders,
                            onFileClick = onFileClick,
                            onDeleteClick = { fileToDelete = it }
                        )
                    }
                }
            }
            FileExplorerLayout.TRADITIONAL_NAV -> {
                // Breadcrumb trail
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentNavPath.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val parent = currentNavPath.substringBeforeLast('/', "")
                                currentNavPath = parent
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up directory", modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = if (currentNavPath.isEmpty()) "Root /" else "Root / $currentNavPath",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                val currentFolderFiles = getFilesAtSubpath(files, currentNavPath, searchQuery)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(currentFolderFiles) { file ->
                        FileRowItem(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    currentNavPath = if (currentNavPath.isEmpty()) file.name else "$currentNavPath/${file.name}"
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onDelete = { fileToDelete = file }
                        )
                    }
                }
            }
            FileExplorerLayout.COMBINED -> {
                // Combined layout: Split folders and flat files
                LazyColumn(modifier = Modifier.weight(1f)) {
                    val filtered = filterProjectFiles(files, searchQuery)
                    item {
                        Text(
                            "DIRECTORIES & FILES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filtered) { file ->
                        TreeFileItem(
                            file = file,
                            depth = 0,
                            expandedMap = expandedFolders,
                            onFileClick = onFileClick,
                            onDeleteClick = { fileToDelete = it }
                        )
                    }
                }
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create New File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File path (e.g. src/app.kt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_file_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            onCreateFile(newFileName.trim())
                            showNewFileDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_file")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Directory") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name (e.g. components)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_folder_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            showNewFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_folder")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete ${if (target.isDirectory) "Directory" else "File"}?") },
            text = {
                Text("Are you sure you want to delete '${target.name}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(target.path)
                        fileToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TreeFileItem(
    file: ProjectFile,
    depth: Int,
    expandedMap: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    onFileClick: (ProjectFile) -> Unit,
    onDeleteClick: (ProjectFile) -> Unit
) {
    val isExpanded = expandedMap[file.path] ?: false

    Column {
        FileRowItem(
            file = file,
            indentDp = (depth * 16).dp,
            isExpanded = isExpanded,
            onClick = {
                if (file.isDirectory) {
                    expandedMap[file.path] = !isExpanded
                } else {
                    onFileClick(file)
                }
            },
            onDelete = { onDeleteClick(file) }
        )

        if (file.isDirectory && isExpanded) {
            file.children.forEach { child ->
                TreeFileItem(
                    file = child,
                    depth = depth + 1,
                    expandedMap = expandedMap,
                    onFileClick = onFileClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
fun FileRowItem(
    file: ProjectFile,
    indentDp: androidx.compose.ui.unit.Dp = 0.dp,
    isExpanded: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = indentDp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when {
                file.isDirectory && isExpanded -> Icons.Default.FolderOpen
                file.isDirectory -> Icons.Default.Folder
                else -> Icons.Default.Description
            }
            val iconTint = when {
                file.isDirectory -> MaterialTheme.colorScheme.primary
                file.extension in listOf("kt", "java") -> Color(0xFF81C784)
                file.extension in listOf("py") -> Color(0xFFFFD54F)
                file.extension in listOf("js", "ts") -> Color(0xFF64B5F6)
                file.extension in listOf("html", "xml") -> Color(0xFFFF8A65)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = file.name,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            if (!file.isDirectory) {
                Text(
                    text = formatFileSize(file.size),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
            }
        }
    }
}

fun filterProjectFiles(files: List<ProjectFile>, query: String): List<ProjectFile> {
    if (query.isBlank()) return files
    return files.filter { it.name.contains(query, ignoreCase = true) || it.children.isNotEmpty() }
        .map {
            if (it.isDirectory) it.copy(children = filterProjectFiles(it.children, query)) else it
        }
}

fun getFilesAtSubpath(files: List<ProjectFile>, subpath: String, query: String): List<ProjectFile> {
    if (subpath.isBlank()) {
        return files.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }
    val parts = subpath.split('/')
    var currentList = files
    for (part in parts) {
        val folder = currentList.firstOrNull { it.isDirectory && it.name == part }
        currentList = folder?.children ?: emptyList()
    }
    return currentList.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
