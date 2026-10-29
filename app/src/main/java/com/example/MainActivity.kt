package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.screens.DiffReviewScreen
import com.example.ui.screens.FirstLaunchSetupDialog
import com.example.ui.screens.ProjectLauncherScreen
import com.example.ui.screens.ProjectWorkspaceScreen
import com.example.ui.screens.settings.SettingsAIProvidersScreen
import com.example.ui.screens.settings.SettingsAboutScreen
import com.example.ui.screens.settings.SettingsAppearanceScreen
import com.example.ui.screens.settings.SettingsDiagnosticsScreen
import com.example.ui.screens.settings.SettingsFileExplorerScreen
import com.example.ui.screens.settings.SettingsGitHubScreen
import com.example.ui.screens.settings.SettingsOverviewScreen
import com.example.ui.screens.settings.SettingsSecurityScreen
import com.example.ui.screens.settings.SettingsStorageScreen
import com.example.ui.theme.Code0Theme
import com.example.viewmodel.Code0Screen
import com.example.viewmodel.Code0ViewModel
import com.example.viewmodel.WorkspaceView

class MainActivity : ComponentActivity() {
    private val viewModel: Code0ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safety guard against silent crashes on background threads
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("Code0CrashGuard", "Uncaught exception intercepted on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()
        setContent {
            Code0App(viewModel)
        }
    }
}

@Composable
fun Code0App(viewModel: Code0ViewModel) {
    val themePreset by viewModel.themePreset.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val explorerLayout by viewModel.explorerLayout.collectAsState()
    val isFirstLaunchDone by viewModel.isFirstLaunchCompleted.collectAsState()

    val currentScreen by viewModel.currentScreen.collectAsState()
    val workspaceView by viewModel.workspaceView.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val projectFiles by viewModel.projectFiles.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabIndex by viewModel.activeTabIndex.collectAsState()

    val bannerProposal by viewModel.bannerProposal.collectAsState()
    val reviewingProposal by viewModel.reviewingProposal.collectAsState()
    val reviewChanges by viewModel.reviewChanges.collectAsState()
    val recoverySnapshots by viewModel.recoverySnapshots.collectAsState()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAILoading by viewModel.isAILoading.collectAsState()
    val isAgentMode by viewModel.isAgentMode.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val aiProviders by viewModel.aiProviders.collectAsState()

    val githubToken by viewModel.githubToken.collectAsState()
    val githubUsername by viewModel.githubUsername.collectAsState()
    val githubRepos by viewModel.githubRepos.collectAsState()
    val isGitHubLoading by viewModel.isGitHubLoading.collectAsState()
    val gitStatus by viewModel.gitStatus.collectAsState()

    val diagnostics by viewModel.diagnostics.collectAsState()
    val isDiagnosticRunning by viewModel.isDiagnosticRunning.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isSearchCaseSensitive by viewModel.isSearchCaseSensitive.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Code0Theme(preset = themePreset, themeMode = themeMode) {
        if (!isFirstLaunchDone) {
            FirstLaunchSetupDialog(
                initialPreset = themePreset,
                initialLayout = explorerLayout,
                onComplete = { preset, layout ->
                    viewModel.completeFirstLaunch(preset, layout)
                },
                onSkip = {
                    viewModel.completeFirstLaunch(themePreset, explorerLayout)
                }
            )
        } else {
            // Contextual Back Navigation for non-workspace screens
            BackHandler(
                enabled = currentScreen != Code0Screen.PROJECT_LAUNCHER && currentScreen != Code0Screen.WORKSPACE
            ) {
                when (currentScreen) {
                    Code0Screen.DIFF_REVIEW -> {
                        viewModel.navigateTo(Code0Screen.WORKSPACE)
                        viewModel.setWorkspaceView(WorkspaceView.AI_ASSISTANT)
                    }
                    Code0Screen.SETTINGS_AI_PROVIDERS,
                    Code0Screen.SETTINGS_APPEARANCE,
                    Code0Screen.SETTINGS_FILE_EXPLORER,
                    Code0Screen.SETTINGS_GITHUB,
                    Code0Screen.SETTINGS_STORAGE,
                    Code0Screen.SETTINGS_BUILD_DIAGNOSTICS,
                    Code0Screen.SETTINGS_SECURITY,
                    Code0Screen.SETTINGS_ABOUT -> {
                        viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW)
                    }
                    Code0Screen.SETTINGS_OVERVIEW -> {
                        viewModel.navigateTo(Code0Screen.PROJECT_LAUNCHER)
                    }
                    else -> {
                        viewModel.navigateTo(Code0Screen.PROJECT_LAUNCHER)
                    }
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    when (currentScreen) {
                        Code0Screen.PROJECT_LAUNCHER -> {
                            ProjectLauncherScreen(
                                projects = projects,
                                onSelectProject = { proj ->
                                    viewModel.selectProject(proj)
                                    viewModel.setWorkspaceView(WorkspaceView.EDITOR)
                                    viewModel.navigateTo(Code0Screen.WORKSPACE)
                                },
                                onCreateProject = { name, tmpl, desc ->
                                    viewModel.createProject(name, tmpl, desc)
                                },
                                onDeleteProject = { viewModel.deleteProject(it) },
                                onOpenSettings = {
                                    viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW)
                                },
                                onImportRepo = { repo, branch ->
                                    viewModel.downloadRepositoryAsProject(repo, branch)
                                },
                                githubRepos = githubRepos,
                                isGitHubLoading = isGitHubLoading,
                                onSearchPublicRepos = { viewModel.searchPublicGitHub(it) }
                            )
                        }

                        Code0Screen.WORKSPACE -> {
                            val project = activeProject
                            if (project != null) {
                                ProjectWorkspaceScreen(
                                    project = project,
                                    projectFiles = projectFiles,
                                    openTabs = openTabs,
                                    activeTabIndex = activeTabIndex,
                                    workspaceView = workspaceView,
                                    explorerLayout = explorerLayout,
                                    bannerProposal = bannerProposal,
                                    activeProvider = activeProvider,
                                    chatMessages = chatMessages,
                                    isAILoading = isAILoading,
                                    isAgentMode = isAgentMode,
                                    githubToken = githubToken,
                                    githubUsername = githubUsername,
                                    githubRepos = githubRepos,
                                    gitStatus = gitStatus,
                                    isGitHubLoading = isGitHubLoading,
                                    searchQuery = searchQuery,
                                    searchResults = searchResults,
                                    isSearching = isSearching,
                                    isSearchCaseSensitive = isSearchCaseSensitive,
                                    onSearchQueryChange = { viewModel.searchInProject(it) },
                                    onToggleSearchCaseSensitive = { viewModel.setSearchCaseSensitive(it) },
                                    onSelectSearchResult = { viewModel.openSearchResult(it) },
                                    onSetWorkspaceView = { viewModel.setWorkspaceView(it) },
                                    onBackToLauncher = {
                                        viewModel.closeActiveProject()
                                    },
                                    onSelectTab = { viewModel.selectTab(it) },
                                    onCloseTab = { viewModel.closeTab(it) },
                                    onContentChange = { viewModel.updateActiveFileContent(it) },
                                    onSaveFile = { viewModel.saveActiveFile() },
                                    onUndo = { viewModel.undoEditor() },
                                    onRedo = { viewModel.redoEditor() },
                                    onOpenFile = { viewModel.openFile(it) },
                                    onCreateFile = { viewModel.createNewFileInProject(it) },
                                    onCreateFolder = { viewModel.createNewFolderInProject(it) },
                                    onDeleteFile = { viewModel.deleteFileInProject(it) },
                                    onRunDiagnostics = { viewModel.runDiagnostics() },
                                    onExportZip = { viewModel.exportCurrentProjectZip() },
                                    onCloseProject = { viewModel.closeActiveProject() },
                                    onToggleAgentMode = { viewModel.setAgentMode(it) },
                                    onSendAIPrompt = { prompt, attached -> viewModel.sendAIPrompt(prompt, attached) },
                                    onOpenProposalReview = { viewModel.openProposalReview(it) },
                                    onApplyProposal = { viewModel.applyCurrentProposal() },
                                    onRejectProposal = { viewModel.rejectCurrentProposal() },
                                    onDismissBanner = { viewModel.dismissBannerProposal() },
                                    onConnectGitHub = { viewModel.connectGitHubAccount(it) },
                                    onDisconnectGitHub = { viewModel.disconnectGitHub() },
                                    onSearchPublicRepos = { viewModel.searchPublicGitHub(it) },
                                    onDownloadRepo = { repo, branch ->
                                        viewModel.downloadRepositoryAsProject(repo, branch)
                                    },
                                    onPushCommit = { owner, repo, branch, msg, files ->
                                        viewModel.pushCommitWithApproval(owner, repo, branch, msg, files)
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    viewModel.navigateTo(Code0Screen.PROJECT_LAUNCHER)
                                }
                            }
                        }

                        Code0Screen.SETTINGS_OVERVIEW -> {
                            SettingsOverviewScreen(
                                onNavigateToCategory = { viewModel.navigateTo(it) },
                                onBack = { viewModel.navigateTo(Code0Screen.PROJECT_LAUNCHER) }
                            )
                        }

                        Code0Screen.SETTINGS_AI_PROVIDERS -> {
                            SettingsAIProvidersScreen(
                                providers = aiProviders,
                                activeProvider = activeProvider,
                                onSetActiveProvider = { viewModel.setActiveAIProvider(it) },
                                onSaveProvider = { viewModel.saveAIProvider(it) },
                                onDeleteProvider = { viewModel.deleteAIProvider(it) },
                                onTestProvider = { provider, callback ->
                                    viewModel.testProvider(provider, callback)
                                },
                                onBack = { viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW) }
                            )
                        }

                        Code0Screen.SETTINGS_APPEARANCE -> {
                            SettingsAppearanceScreen(
                                currentTheme = themePreset,
                                currentThemeMode = themeMode,
                                onSetTheme = { viewModel.prefs.setThemePreset(it) },
                                onSetThemeMode = { viewModel.prefs.setThemeMode(it) },
                                onBack = { viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW) }
                            )
                        }

                        Code0Screen.SETTINGS_FILE_EXPLORER -> {
                            SettingsFileExplorerScreen(
                                currentLayout = explorerLayout,
                                onSetLayout = { viewModel.prefs.setExplorerLayout(it) },
                                onBack = { viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW) }
                            )
                        }

                        Code0Screen.SETTINGS_GITHUB -> {
                            SettingsGitHubScreen(
                                githubToken = githubToken,
                                githubUsername = githubUsername,
                                isLoading = isGitHubLoading,
                                onConnectToken = { viewModel.connectGitHubAccount(it) },
                                onDisconnect = { viewModel.disconnectGitHub() },
                                onBack = { viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW) }
                            )
                        }

                        Code0Screen.SETTINGS_STORAGE -> {
                            SettingsStorageScreen(
                                projects = projects,
                                recoverySnapshots = recoverySnapshots,
                                onClearSnapshots = { viewModel.clearRecoverySnapshots() },
                                onRevertSnapshot = { viewModel.revertSnapshot(it) },
                                onPurgeRejectedProposals = { viewModel.purgeRejectedProposals() },
                                onBack = { viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW) }
                            )
                        }

                        Code0Screen.SETTINGS_BUILD_DIAGNOSTICS -> {
                            SettingsDiagnosticsScreen(
                                activeProject = activeProject,
                                diagnostics = diagnostics,
                                isDiagnosticRunning = isDiagnosticRunning,
                                onRunDiagnostics = { viewModel.runDiagnostics() },
                                onBack = { viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW) }
                            )
                        }

                        Code0Screen.SETTINGS_SECURITY -> {
                            SettingsSecurityScreen(
                                onBack = { viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW) }
                            )
                        }

                        Code0Screen.SETTINGS_ABOUT -> {
                            SettingsAboutScreen(
                                onResetPreferences = { viewModel.prefs.resetToDefaults() },
                                onBack = { viewModel.navigateTo(Code0Screen.SETTINGS_OVERVIEW) }
                            )
                        }

                        Code0Screen.DIFF_REVIEW -> {
                            val proposal = reviewingProposal
                            if (proposal != null) {
                                DiffReviewScreen(
                                    proposal = proposal,
                                    changes = reviewChanges,
                                    proposalRepo = viewModel.proposalRepo,
                                    onToggleFileSelection = { viewModel.toggleFileSelectionInProposal(it) },
                                    onApply = { viewModel.applyCurrentProposal() },
                                    onReject = { viewModel.rejectCurrentProposal() },
                                    onBack = {
                                        viewModel.navigateTo(Code0Screen.WORKSPACE)
                                        viewModel.setWorkspaceView(WorkspaceView.AI_ASSISTANT)
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    viewModel.navigateTo(Code0Screen.WORKSPACE)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
