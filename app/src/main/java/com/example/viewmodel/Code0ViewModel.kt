package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.Code0Database
import com.example.data.local.PreferencesManager
import com.example.data.model.AIProviderEntity
import com.example.data.model.BuildDiagnosticEntity
import com.example.data.model.ChatMessage
import com.example.data.model.FileExplorerLayout
import com.example.data.model.FileSearchResult
import com.example.data.model.GitRepoItem
import com.example.data.model.GitStatusSummary
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFile
import com.example.data.model.ProposalEntity
import com.example.data.model.ProposedFileChange
import com.example.data.model.RecoverySnapshotEntity
import com.example.data.model.ThemeMode
import com.example.data.model.ThemePreset
import com.example.data.repository.AIService
import com.example.data.repository.BuildDiagnosticsRunner
import com.example.data.repository.GitHubRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.ProposalRepository
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class Code0Screen {
    PROJECT_LAUNCHER,
    WORKSPACE,
    SETTINGS_OVERVIEW,
    SETTINGS_AI_PROVIDERS,
    SETTINGS_APPEARANCE,
    SETTINGS_FILE_EXPLORER,
    SETTINGS_GITHUB,
    SETTINGS_STORAGE,
    SETTINGS_BUILD_DIAGNOSTICS,
    SETTINGS_SECURITY,
    SETTINGS_ABOUT,
    DIFF_REVIEW
}

enum class WorkspaceView {
    EDITOR,
    AI_ASSISTANT,
    GIT
}

data class OpenTab(
    val file: ProjectFile,
    var content: String,
    var originalDiskContent: String,
    var isDirty: Boolean = false
)

class Code0ViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Code0Database.getInstance(application)
    val prefs = PreferencesManager(application)

    val projectRepo = ProjectRepository(application, db.projectDao())
    val proposalRepo = ProposalRepository(db.proposalDao(), db.recoveryDao())
    val gitHubRepo = GitHubRepository()
    val aiService = AIService()
    val diagnosticsRunner = BuildDiagnosticsRunner(db.buildDiagnosticDao())

    /**
     * Global CoroutineExceptionHandler to prevent crashes/ANRs during heavy operations
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("Code0ViewModel", "Handled background error safely: ${throwable.message}", throwable)
        _statusMessage.value = "Operation notice: ${throwable.localizedMessage ?: "System limit reached, paused"}"
        _isAILoading.value = false
        _isGitHubLoading.value = false
        _isDiagnosticRunning.value = false
    }

    private fun launchSafe(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        viewModelScope.launch(coroutineExceptionHandler, block = block)
    }

    // Navigation & Screen state
    private val _currentScreen = MutableStateFlow(Code0Screen.PROJECT_LAUNCHER)
    val currentScreen: StateFlow<Code0Screen> = _currentScreen.asStateFlow()

    private val _workspaceView = MutableStateFlow(WorkspaceView.EDITOR)
    val workspaceView: StateFlow<WorkspaceView> = _workspaceView.asStateFlow()

    fun setWorkspaceView(view: WorkspaceView) {
        _workspaceView.value = view
    }

    // First launch state
    val isFirstLaunchCompleted = prefs.isFirstLaunchCompleted
    val themePreset = prefs.themePreset
    val themeMode = prefs.themeMode
    val explorerLayout = prefs.explorerLayout

    // Projects
    val projects: StateFlow<List<ProjectEntity>> = projectRepo.projectsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val projectFiles: StateFlow<List<ProjectFile>> = _projectFiles.asStateFlow()

    // Editor Tabs & State
    private val _openTabs = MutableStateFlow<List<OpenTab>>(emptyList())
    val openTabs: StateFlow<List<OpenTab>> = _openTabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // Editor Undo/Redo stack for current file
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    // Pending Proposals & Stage B Completion Banner
    val pendingProposals: StateFlow<List<ProposalEntity>> = proposalRepo.pendingProposalsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _bannerProposal = MutableStateFlow<ProposalEntity?>(null)
    val bannerProposal: StateFlow<ProposalEntity?> = _bannerProposal.asStateFlow()

    private val _reviewingProposal = MutableStateFlow<ProposalEntity?>(null)
    val reviewingProposal: StateFlow<ProposalEntity?> = _reviewingProposal.asStateFlow()

    private val _reviewChanges = MutableStateFlow<List<ProposedFileChange>>(emptyList())
    val reviewChanges: StateFlow<List<ProposedFileChange>> = _reviewChanges.asStateFlow()

    val recoverySnapshots: StateFlow<List<RecoverySnapshotEntity>> = proposalRepo.recoverySnapshotsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // AI Chat & Agent
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                sender = "assistant",
                text = "Welcome to Code0 Studio! I can explain your code, suggest improvements, or generate multi-file change proposals with full review and approval."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    private val _isAgentMode = MutableStateFlow(false)
    val isAgentMode: StateFlow<Boolean> = _isAgentMode.asStateFlow()

    // AI Providers
    val aiProviders: StateFlow<List<AIProviderEntity>> = db.aiProviderDao().getAllProvidersFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _activeProvider = MutableStateFlow<AIProviderEntity?>(null)
    val activeProvider: StateFlow<AIProviderEntity?> = _activeProvider.asStateFlow()

    // GitHub
    val githubToken = prefs.githubToken
    val githubUsername = prefs.githubUsername

    private val _githubRepos = MutableStateFlow<List<GitRepoItem>>(emptyList())
    val githubRepos: StateFlow<List<GitRepoItem>> = _githubRepos.asStateFlow()

    private val _isGitHubLoading = MutableStateFlow(false)
    val isGitHubLoading: StateFlow<Boolean> = _isGitHubLoading.asStateFlow()

    private val _gitStatus = MutableStateFlow<GitStatusSummary?>(null)
    val gitStatus: StateFlow<GitStatusSummary?> = _gitStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Global Project Search ("Find in Files")
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchCaseSensitive = MutableStateFlow(false)
    val isSearchCaseSensitive: StateFlow<Boolean> = _isSearchCaseSensitive.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileSearchResult>>(emptyList())
    val searchResults: StateFlow<List<FileSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Build Diagnostics
    val diagnostics: StateFlow<List<BuildDiagnosticEntity>> = diagnosticsRunner.allDiagnosticsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _isDiagnosticRunning = MutableStateFlow(false)
    val isDiagnosticRunning: StateFlow<Boolean> = _isDiagnosticRunning.asStateFlow()

    init {
        launchSafe {
            // Seed default Gemini provider if none exists
            val existing = db.aiProviderDao().getActiveProvider()
            if (existing == null) {
                val gemini = AIProviderEntity(
                    id = "gemini-default",
                    name = "Google Gemini",
                    type = "GEMINI",
                    apiKey = "",
                    modelName = "gemini-1.5-flash",
                    endpointUrl = "",
                    isActive = true
                )
                db.aiProviderDao().insertProvider(gemini)
                _activeProvider.value = gemini
            } else {
                _activeProvider.value = existing
            }

            // Restore last active project if available
            val lastId = prefs.lastActiveProjectId.value
            if (lastId != null) {
                val p = projectRepo.getProject(lastId)
                if (p != null) selectProject(p)
            }
        }
    }

    fun navigateTo(screen: Code0Screen) {
        _currentScreen.value = screen
    }

    fun completeFirstLaunch(preset: ThemePreset, layout: FileExplorerLayout) {
        prefs.setThemePreset(preset)
        prefs.setExplorerLayout(layout)
        prefs.setFirstLaunchCompleted(true)
    }

    fun selectProject(project: ProjectEntity) {
        _activeProject.value = project
        prefs.setLastActiveProjectId(project.id)
        refreshProjectFiles()
        refreshGitStatus()
    }

    fun createProject(name: String, templateType: String, description: String = "") {
        viewModelScope.launch {
            val project = projectRepo.createProject(name, templateType, description)
            selectProject(project)
            _workspaceView.value = WorkspaceView.EDITOR
            navigateTo(Code0Screen.WORKSPACE)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepo.deleteProject(project)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
                _openTabs.value = emptyList()
                _projectFiles.value = emptyList()
                prefs.setLastActiveProjectId(null)
                navigateTo(Code0Screen.PROJECT_LAUNCHER)
            }
        }
    }

    fun closeActiveProject() {
        _activeProject.value = null
        _openTabs.value = emptyList()
        _projectFiles.value = emptyList()
        prefs.setLastActiveProjectId(null)
        navigateTo(Code0Screen.PROJECT_LAUNCHER)
    }

    fun refreshProjectFiles() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val files = projectRepo.loadProjectFiles(File(proj.directoryPath))
            _projectFiles.value = files
            projectRepo.updateProjectStats(proj.id)
        }
    }

    fun refreshGitStatus() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val status = gitHubRepo.getLocalGitStatus(File(proj.directoryPath), proj.gitBranch)
            _gitStatus.value = status
        }
    }

    // Editor actions
    fun openFile(file: ProjectFile) {
        if (file.isDirectory) return
        val currentList = _openTabs.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.file.path == file.path }

        if (existingIndex != -1) {
            _activeTabIndex.value = existingIndex
        } else {
            viewModelScope.launch {
                val content = projectRepo.readFileContent(file.path)
                currentList.add(
                    OpenTab(
                        file = file,
                        content = content,
                        originalDiskContent = content,
                        isDirty = false
                    )
                )
                _openTabs.value = currentList
                _activeTabIndex.value = currentList.size - 1
                undoStack.clear()
                redoStack.clear()
            }
        }
        _workspaceView.value = WorkspaceView.EDITOR
        navigateTo(Code0Screen.WORKSPACE)
    }

    fun closeTab(index: Int) {
        val currentList = _openTabs.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _openTabs.value = currentList
            if (_activeTabIndex.value >= currentList.size) {
                _activeTabIndex.value = maxOf(0, currentList.size - 1)
            }
        }
    }

    fun selectTab(index: Int) {
        if (index in _openTabs.value.indices) {
            _activeTabIndex.value = index
        }
    }

    fun updateActiveFileContent(newContent: String) {
        val tabs = _openTabs.value
        val idx = _activeTabIndex.value
        if (idx in tabs.indices) {
            val tab = tabs[idx]
            if (tab.content != newContent) {
                undoStack.add(tab.content)
                redoStack.clear()
                val updatedTab = tab.copy(
                    content = newContent,
                    isDirty = newContent != tab.originalDiskContent
                )
                val newList = tabs.toMutableList().apply { set(idx, updatedTab) }
                _openTabs.value = newList
            }
        }
    }

    fun saveActiveFile() {
        val tabs = _openTabs.value
        val idx = _activeTabIndex.value
        if (idx in tabs.indices) {
            val tab = tabs[idx]
            viewModelScope.launch {
                val success = projectRepo.writeFileContent(tab.file.path, tab.content)
                if (success) {
                    val updatedTab = tab.copy(
                        originalDiskContent = tab.content,
                        isDirty = false
                    )
                    val newList = tabs.toMutableList().apply { set(idx, updatedTab) }
                    _openTabs.value = newList
                    refreshProjectFiles()
                    refreshGitStatus()
                }
            }
        }
    }

    fun undoEditor() {
        val tabs = _openTabs.value
        val idx = _activeTabIndex.value
        if (idx in tabs.indices && undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(tabs[idx].content)
            val updatedTab = tabs[idx].copy(
                content = prev,
                isDirty = prev != tabs[idx].originalDiskContent
            )
            _openTabs.value = tabs.toMutableList().apply { set(idx, updatedTab) }
        }
    }

    fun redoEditor() {
        val tabs = _openTabs.value
        val idx = _activeTabIndex.value
        if (idx in tabs.indices && redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(tabs[idx].content)
            val updatedTab = tabs[idx].copy(
                content = next,
                isDirty = next != tabs[idx].originalDiskContent
            )
            _openTabs.value = tabs.toMutableList().apply { set(idx, updatedTab) }
        }
    }

    fun createNewFileInProject(relativePath: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            projectRepo.createFile(File(proj.directoryPath), relativePath, "")
            refreshProjectFiles()
        }
    }

    fun createNewFolderInProject(relativePath: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            projectRepo.createDirectory(File(proj.directoryPath), relativePath)
            refreshProjectFiles()
        }
    }

    fun deleteFileInProject(filePath: String) {
        viewModelScope.launch {
            projectRepo.deleteFileOrFolder(filePath)
            // Close tab if open
            val tabs = _openTabs.value.toMutableList()
            val openIdx = tabs.indexOfFirst { it.file.path == filePath }
            if (openIdx != -1) {
                closeTab(openIdx)
            }
            refreshProjectFiles()
            refreshGitStatus()
        }
    }

    // AI & Proposals
    fun setAgentMode(isAgent: Boolean) {
        _isAgentMode.value = isAgent
    }

    fun setSearchCaseSensitive(caseSensitive: Boolean) {
        _isSearchCaseSensitive.value = caseSensitive
        val q = _searchQuery.value
        if (q.isNotBlank()) {
            searchInProject(q, caseSensitive)
        }
    }

    fun searchInProject(query: String, caseSensitive: Boolean = _isSearchCaseSensitive.value) {
        _searchQuery.value = query
        val proj = _activeProject.value
        if (query.isBlank() || proj == null) {
            _searchResults.value = emptyList()
            return
        }
        launchSafe {
            _isSearching.value = true
            val results = projectRepo.searchProjectFiles(
                projectDir = File(proj.directoryPath),
                query = query,
                caseSensitive = caseSensitive
            )
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun openSearchResult(result: FileSearchResult) {
        val proj = _activeProject.value ?: return
        val file = File(result.filePath)
        val projectFile = ProjectFile(
            name = file.name,
            path = file.absolutePath,
            relativePath = result.relativePath,
            isDirectory = false,
            size = file.length(),
            lastModified = file.lastModified(),
            extension = file.extension
        )
        openFile(projectFile)
    }

    fun sendAIPrompt(prompt: String, attachedFiles: List<ProjectFile> = emptyList()) {
        if (prompt.isBlank()) return
        val proj = _activeProject.value
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "user",
            text = prompt,
            attachedFiles = attachedFiles.map { it.name }
        )
        _chatMessages.value = _chatMessages.value + userMsg

        launchSafe {
            _isAILoading.value = true
            val provider = _activeProvider.value

            // Build attachments context if any
            val attachmentsContext = if (attachedFiles.isNotEmpty()) {
                val sb = StringBuilder("\n\nUser pinned the following files as context:\n")
                for (af in attachedFiles) {
                    val code = projectRepo.readFileContent(af.path).take(4000)
                    sb.append("--- File: ${af.relativePath} ---\n")
                    sb.append(code).append("\n\n")
                }
                sb.toString()
            } else ""

            val enhancedPrompt = prompt + attachmentsContext

            if (_isAgentMode.value && proj != null) {
                // Agent Mode: inspect real project, generate multi-file proposal
                val projectDir = File(proj.directoryPath)
                val proposalResult = aiService.generateAgentProposals(provider, projectDir, enhancedPrompt)

                proposalResult.onSuccess { changes ->
                    val proposal = proposalRepo.createProposal(
                        projectId = proj.id,
                        taskTitle = prompt.take(50),
                        description = "Proposed modifications for: $prompt",
                        changes = changes
                    )
                    // Trigger Stage B Completion Banner!
                    _bannerProposal.value = proposal

                    val assistantMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = "assistant",
                        text = "Prepared proposed changes for ${changes.size} file(s). You can review diffs, check conflicts, and choose whether to Apply or Reject.",
                        proposalId = proposal.id
                    )
                    _chatMessages.value = _chatMessages.value + assistantMsg
                }.onFailure { err ->
                    val assistantMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = "assistant",
                        text = "Failed to prepare proposals: ${err.message}"
                    )
                    _chatMessages.value = _chatMessages.value + assistantMsg
                }
            } else {
                // Chat Mode
                val contextSummary = if (proj != null) {
                    "Active Project: ${proj.name} (${proj.projectType}). File count: ${proj.fileCount}$attachmentsContext"
                } else {
                    "No active project selected$attachmentsContext"
                }

                val result = aiService.sendChatMessage(provider, prompt, contextSummary)
                result.onSuccess { reply ->
                    val assistantMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = "assistant",
                        text = reply
                    )
                    _chatMessages.value = _chatMessages.value + assistantMsg
                }.onFailure { err ->
                    val assistantMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = "assistant",
                        text = "Error: ${err.message}"
                    )
                    _chatMessages.value = _chatMessages.value + assistantMsg
                }
            }
            _isAILoading.value = false
        }
    }

    fun dismissBannerProposal() {
        _bannerProposal.value = null
    }

    fun openProposalReview(proposal: ProposalEntity) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            _reviewingProposal.value = proposal
            val changes = proposalRepo.verifyAndDetectConflicts(File(proj.directoryPath), proposal)
            _reviewChanges.value = changes
            navigateTo(Code0Screen.DIFF_REVIEW)
        }
    }

    fun toggleFileSelectionInProposal(relativePath: String) {
        val current = _reviewChanges.value.map {
            if (it.relativePath == relativePath) it.copy(isSelected = !it.isSelected) else it
        }
        _reviewChanges.value = current
    }

    fun applyCurrentProposal() {
        val proposal = _reviewingProposal.value ?: return
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val result = proposalRepo.applyProposal(File(proj.directoryPath), proposal.id)
            if (result.success) {
                _statusMessage.value = "Applied ${result.appliedCount} file(s) successfully!"
                _bannerProposal.value = null
                _reviewingProposal.value = null
                refreshProjectFiles()
                refreshGitStatus()
                // Reload tabs if open
                reloadOpenTabs()
                _workspaceView.value = WorkspaceView.EDITOR
                navigateTo(Code0Screen.WORKSPACE)
            } else {
                _statusMessage.value = "Apply failed: ${result.error}"
            }
        }
    }

    fun rejectCurrentProposal() {
        val proposal = _reviewingProposal.value ?: return
        viewModelScope.launch {
            proposalRepo.rejectProposal(proposal.id)
            _statusMessage.value = "Proposal rejected. Original files were kept untouched."
            _bannerProposal.value = null
            _reviewingProposal.value = null
            _workspaceView.value = WorkspaceView.EDITOR
            navigateTo(Code0Screen.WORKSPACE)
        }
    }

    fun revertSnapshot(snapshot: RecoverySnapshotEntity) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val success = proposalRepo.revertRecoverySnapshot(File(proj.directoryPath), snapshot.id)
            if (success) {
                _statusMessage.value = "Reverted changes from snapshot '${snapshot.taskTitle}'"
                refreshProjectFiles()
                refreshGitStatus()
                reloadOpenTabs()
            } else {
                _statusMessage.value = "Failed to revert snapshot"
            }
        }
    }

    private fun reloadOpenTabs() {
        viewModelScope.launch {
            val tabs = _openTabs.value.toMutableList()
            for (i in tabs.indices) {
                val f = tabs[i].file
                if (File(f.path).exists()) {
                    val content = projectRepo.readFileContent(f.path)
                    tabs[i] = tabs[i].copy(
                        content = content,
                        originalDiskContent = content,
                        isDirty = false
                    )
                }
            }
            _openTabs.value = tabs
        }
    }

    // GitHub
    fun connectGitHubAccount(token: String) {
        viewModelScope.launch {
            _isGitHubLoading.value = true
            val result = gitHubRepo.validateToken(token)
            result.onSuccess { username ->
                prefs.setGitHubCredentials(token, username)
                _statusMessage.value = "Connected as GitHub user @$username"
                loadUserGitHubRepos(token)
            }.onFailure { err ->
                _statusMessage.value = "Authentication error: ${err.message}"
            }
            _isGitHubLoading.value = false
        }
    }

    fun disconnectGitHub() {
        prefs.setGitHubCredentials(null, null)
        _githubRepos.value = emptyList()
        _statusMessage.value = "GitHub account disconnected."
    }

    fun loadUserGitHubRepos(token: String) {
        viewModelScope.launch {
            _isGitHubLoading.value = true
            val res = gitHubRepo.fetchUserRepos(token)
            res.onSuccess { list ->
                _githubRepos.value = list
            }.onFailure { err ->
                _statusMessage.value = err.message
            }
            _isGitHubLoading.value = false
        }
    }

    fun searchPublicGitHub(query: String) {
        viewModelScope.launch {
            _isGitHubLoading.value = true
            val res = gitHubRepo.searchPublicRepos(query, githubToken.value)
            res.onSuccess { list ->
                _githubRepos.value = list
            }.onFailure { err ->
                _statusMessage.value = err.message
            }
            _isGitHubLoading.value = false
        }
    }

    fun downloadRepositoryAsProject(repo: GitRepoItem, branch: String) {
        launchSafe {
            _isGitHubLoading.value = true
            _statusMessage.value = "Downloading ${repo.fullName} ($branch)..."

            val project = projectRepo.createProject(
                name = repo.name,
                templateType = "General",
                description = repo.description ?: "Downloaded from GitHub: ${repo.htmlUrl}"
            )

            val targetDir = File(project.directoryPath)
            val downloadRes = gitHubRepo.downloadAndExtractRepository(
                owner = repo.fullName.substringBefore('/'),
                repo = repo.name,
                branch = branch.ifBlank { repo.defaultBranch },
                token = githubToken.value,
                targetDir = targetDir
            ) { msg ->
                _statusMessage.value = msg
            }

            downloadRes.onSuccess { count ->
                _statusMessage.value = "Successfully imported $count files into project '${repo.name}'!"
                selectProject(project)
                _workspaceView.value = WorkspaceView.EDITOR
                navigateTo(Code0Screen.WORKSPACE)
            }.onFailure { err ->
                _statusMessage.value = "Download failed: ${err.message}"
            }
            _isGitHubLoading.value = false
        }
    }

    fun exportCurrentProjectZip() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val exportDir = File(getApplication<Application>().filesDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "${proj.name.replace(" ", "_")}.zip")
            val ok = projectRepo.exportProjectZip(proj, zipFile)
            if (ok) {
                _statusMessage.value = "Project exported: ${zipFile.name} (${zipFile.length() / 1024} KB)"
            } else {
                _statusMessage.value = "Failed to export ZIP"
            }
        }
    }

    fun pushCommitWithApproval(
        owner: String,
        repo: String,
        branch: String,
        commitMessage: String,
        filesToPush: List<Pair<String, String>>
    ) {
        val token = githubToken.value ?: return
        viewModelScope.launch {
            _isGitHubLoading.value = true
            val res = gitHubRepo.pushCommitToGitHub(owner, repo, branch, token, commitMessage, filesToPush)
            res.onSuccess { msg ->
                _statusMessage.value = msg
                refreshGitStatus()
            }.onFailure { err ->
                _statusMessage.value = "Push failed: ${err.message}"
            }
            _isGitHubLoading.value = false
        }
    }

    // Diagnostics & Build
    fun runDiagnostics() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            _isDiagnosticRunning.value = true
            val diag = diagnosticsRunner.runProjectDiagnostics(proj)
            _statusMessage.value = "Diagnostics finished: ${diag.status}"
            _isDiagnosticRunning.value = false
        }
    }

    // AI Providers management
    fun saveAIProvider(provider: AIProviderEntity) {
        viewModelScope.launch {
            db.aiProviderDao().insertProvider(provider)
            if (provider.isActive) {
                db.aiProviderDao().deactivateAll()
                db.aiProviderDao().setActiveProvider(provider.id)
                _activeProvider.value = provider
            }
            _statusMessage.value = "Provider '${provider.name}' saved"
        }
    }

    fun setActiveAIProvider(id: String) {
        viewModelScope.launch {
            db.aiProviderDao().deactivateAll()
            db.aiProviderDao().setActiveProvider(id)
            val all = db.aiProviderDao().getActiveProvider()
            _activeProvider.value = all
        }
    }

    fun testProvider(provider: AIProviderEntity, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = aiService.testProviderConnection(provider)
            res.onSuccess { msg -> onResult(msg) }
                .onFailure { err -> onResult("Error: ${err.message}") }
        }
    }

    fun deleteAIProvider(id: String) {
        viewModelScope.launch {
            db.aiProviderDao().deleteProviderById(id)
            _statusMessage.value = "AI Provider removed"
            val active = db.aiProviderDao().getActiveProvider()
            _activeProvider.value = active
        }
    }

    fun purgeRejectedProposals() {
        viewModelScope.launch {
            db.proposalDao().purgeRejectedProposals()
            _statusMessage.value = "Rejected proposal cache purged"
        }
    }

    fun clearRecoverySnapshots() {
        viewModelScope.launch {
            proposalRepo.clearOldRecoverySnapshots()
            _statusMessage.value = "Recovery history cleared"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
