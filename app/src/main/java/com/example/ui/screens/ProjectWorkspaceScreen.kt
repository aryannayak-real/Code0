package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.AIProviderEntity
import com.example.data.model.ChatMessage
import com.example.data.model.FileExplorerLayout
import com.example.data.model.FileSearchResult
import com.example.data.model.GitRepoItem
import com.example.data.model.GitStatusSummary
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFile
import com.example.data.model.ProposalEntity
import com.example.ui.components.CodeEditorView
import com.example.ui.components.FileExplorerView
import com.example.ui.components.FindInFilesSheet
import com.example.ui.components.LivePreviewSheet
import com.example.ui.components.ProposalReadyBanner
import com.example.ui.components.WorkspaceTopBar
import com.example.viewmodel.OpenTab
import com.example.viewmodel.WorkspaceView
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    project: ProjectEntity,
    projectFiles: List<ProjectFile>,
    openTabs: List<OpenTab>,
    activeTabIndex: Int,
    workspaceView: WorkspaceView,
    explorerLayout: FileExplorerLayout,
    bannerProposal: ProposalEntity?,
    // AI props
    activeProvider: AIProviderEntity?,
    chatMessages: List<ChatMessage>,
    isAILoading: Boolean,
    isAgentMode: Boolean,
    // Git props
    githubToken: String?,
    githubUsername: String?,
    githubRepos: List<GitRepoItem>,
    gitStatus: GitStatusSummary?,
    isGitHubLoading: Boolean,
    // Search props
    searchQuery: String = "",
    searchResults: List<FileSearchResult> = emptyList(),
    isSearching: Boolean = false,
    isSearchCaseSensitive: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearchCaseSensitive: (Boolean) -> Unit = {},
    onSelectSearchResult: (FileSearchResult) -> Unit = {},
    // Actions
    onSetWorkspaceView: (WorkspaceView) -> Unit,
    onBackToLauncher: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveFile: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenFile: (ProjectFile) -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRunDiagnostics: () -> Unit,
    onExportZip: () -> Unit,
    onCloseProject: () -> Unit,
    // AI Actions
    onToggleAgentMode: (Boolean) -> Unit,
    onSendAIPrompt: (String, List<ProjectFile>) -> Unit,
    onOpenProposalReview: (ProposalEntity) -> Unit,
    onApplyProposal: () -> Unit,
    onRejectProposal: () -> Unit,
    onDismissBanner: () -> Unit,
    // Git Actions
    onConnectGitHub: (String) -> Unit,
    onDisconnectGitHub: () -> Unit,
    onSearchPublicRepos: (String) -> Unit,
    onDownloadRepo: (GitRepoItem, String) -> Unit,
    onPushCommit: (owner: String, repo: String, branch: String, message: String, files: List<Pair<String, String>>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExplorerSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    var showUnsavedDialogOnBack by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentTab = if (openTabs.isNotEmpty() && activeTabIndex in openTabs.indices) {
        openTabs[activeTabIndex]
    } else null

    val isFileDirty = currentTab?.isDirty == true
    val activeFileName = currentTab?.file?.name
    val activeExtension = currentTab?.file?.extension?.lowercase().orEmpty()
    val isPreviewAvailable = activeExtension in setOf("html", "htm", "md", "markdown", "svg")

    val handleBackPress = {
        if (workspaceView != WorkspaceView.EDITOR) {
            onSetWorkspaceView(WorkspaceView.EDITOR)
        } else if (isFileDirty) {
            showUnsavedDialogOnBack = true
        } else {
            onBackToLauncher()
        }
    }

    BackHandler(onBack = handleBackPress)

    Scaffold(
        topBar = {
            WorkspaceTopBar(
                project = project,
                activeFileName = activeFileName,
                isFileDirty = isFileDirty,
                isAIPanelOpen = workspaceView == WorkspaceView.AI_ASSISTANT,
                isGitPanelOpen = workspaceView == WorkspaceView.GIT,
                isPreviewAvailable = isPreviewAvailable,
                onBackToLauncher = handleBackPress,
                onToggleExplorer = { showExplorerSheet = true },
                onOpenSearch = { showSearchSheet = true },
                onOpenPreview = { showPreviewSheet = true },
                onSaveFile = onSaveFile,
                onToggleAI = {
                    onSetWorkspaceView(
                        if (workspaceView == WorkspaceView.AI_ASSISTANT) WorkspaceView.EDITOR else WorkspaceView.AI_ASSISTANT
                    )
                },
                onToggleGit = {
                    onSetWorkspaceView(
                        if (workspaceView == WorkspaceView.GIT) WorkspaceView.EDITOR else WorkspaceView.GIT
                    )
                },
                onRunDiagnostics = onRunDiagnostics,
                onExportZip = onExportZip,
                onCloseProject = onCloseProject
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (workspaceView) {
                WorkspaceView.EDITOR -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Open File Tabs Bar
                        if (openTabs.isNotEmpty()) {
                            val tabScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .horizontalScroll(tabScrollState)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                openTabs.forEachIndexed { index, tab ->
                                    val isSelected = index == activeTabIndex
                                    Surface(
                                        onClick = { onSelectTab(index) },
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent,
                                        modifier = Modifier.padding(end = 4.dp).testTag("workspace_tab_$index")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${tab.file.name}${if (tab.isDirty) " *" else ""}",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { onCloseTab(index) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Close tab",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Editor View or Empty State
                        if (currentTab != null) {
                            CodeEditorView(
                                content = currentTab.content,
                                onContentChange = onContentChange,
                                onUndo = onUndo,
                                onRedo = onRedo,
                                fileExtension = currentTab.file.extension,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No File Open",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap the folder icon on the top toolbar to browse and open source files in ${project.name}.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                    )
                                    Button(onClick = { showExplorerSheet = true }) {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Browse Project Files")
                                    }
                                }
                            }
                        }
                    }
                }

                WorkspaceView.AI_ASSISTANT -> {
                    // Integrated AI Assistant inside workspace
                    AIAssistantScreen(
                        activeProject = project,
                        activeProvider = activeProvider,
                        chatMessages = chatMessages,
                        isAILoading = isAILoading,
                        isAgentMode = isAgentMode,
                        projectFiles = projectFiles,
                        onToggleAgentMode = onToggleAgentMode,
                        onSendPrompt = onSendAIPrompt,
                        onOpenProposalReview = { proposalId ->
                            if (bannerProposal != null && bannerProposal.id == proposalId) {
                                onOpenProposalReview(bannerProposal)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                WorkspaceView.GIT -> {
                    // Integrated Git & GitHub inside workspace
                    GitScreen(
                        activeProject = project,
                        githubToken = githubToken,
                        githubUsername = githubUsername,
                        githubRepos = githubRepos,
                        gitStatus = gitStatus,
                        isLoading = isGitHubLoading,
                        onConnectGitHub = onConnectGitHub,
                        onDisconnectGitHub = onDisconnectGitHub,
                        onSearchPublicRepos = onSearchPublicRepos,
                        onDownloadRepo = onDownloadRepo,
                        onPushCommit = onPushCommit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Stage B Floating Completion Banner
            if (bannerProposal != null) {
                ProposalReadyBanner(
                    proposal = bannerProposal,
                    onReview = { onOpenProposalReview(bannerProposal) },
                    onApply = onApplyProposal,
                    onReject = onRejectProposal,
                    onDismiss = onDismissBanner,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        }
    }

    // Modal File Explorer Sheet (Opens on demand)
    if (showExplorerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExplorerSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "${project.name} — Files",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FileExplorerView(
                    files = projectFiles,
                    layout = explorerLayout,
                    onFileClick = { file ->
                        onOpenFile(file)
                        showExplorerSheet = false
                    },
                    onCreateFile = onCreateFile,
                    onCreateFolder = onCreateFolder,
                    onDeleteFile = onDeleteFile,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Find in Files Modal Sheet
    if (showSearchSheet) {
        FindInFilesSheet(
            query = searchQuery,
            results = searchResults,
            isSearching = isSearching,
            isCaseSensitive = isSearchCaseSensitive,
            onQueryChange = onSearchQueryChange,
            onToggleCaseSensitive = onToggleSearchCaseSensitive,
            onSelectResult = { result ->
                onSelectSearchResult(result)
                showSearchSheet = false
            },
            onDismiss = { showSearchSheet = false }
        )
    }

    // Live Web & Markdown Preview Modal Sheet
    if (showPreviewSheet && currentTab != null) {
        LivePreviewSheet(
            fileName = currentTab.file.name,
            fileExtension = currentTab.file.extension,
            content = currentTab.content,
            projectDir = File(project.directoryPath),
            onDismiss = { showPreviewSheet = false }
        )
    }

    // Unsaved Changes Confirmation Dialog on Back
    if (showUnsavedDialogOnBack) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialogOnBack = false },
            title = { Text("Unsaved Changes") },
            text = {
                Text("You have unsaved edits in '${currentTab?.file?.name}'. Do you want to save your changes before leaving?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveFile()
                        showUnsavedDialogOnBack = false
                        onBackToLauncher()
                    }
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showUnsavedDialogOnBack = false
                            onBackToLauncher()
                        }
                    ) {
                        Text("Discard Edits", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showUnsavedDialogOnBack = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
