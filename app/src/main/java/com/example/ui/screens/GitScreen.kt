package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.GitRepoItem
import com.example.data.model.GitStatusSummary
import com.example.data.model.ProjectEntity

@Composable
fun GitScreen(
    activeProject: ProjectEntity?,
    githubToken: String?,
    githubUsername: String?,
    githubRepos: List<GitRepoItem>,
    gitStatus: GitStatusSummary?,
    isLoading: Boolean,
    onConnectGitHub: (String) -> Unit,
    onDisconnectGitHub: () -> Unit,
    onSearchPublicRepos: (String) -> Unit,
    onDownloadRepo: (GitRepoItem, branch: String) -> Unit,
    onPushCommit: (owner: String, repo: String, branch: String, message: String, files: List<Pair<String, String>>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var tokenInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showConnectDialog by remember { mutableStateOf(false) }

    // Commit & Push Confirmation Dialog State
    var showPushApprovalDialog by remember { mutableStateOf(false) }
    var commitMessage by remember { mutableStateOf("Update via Code0 Studio") }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        // GitHub Account Header
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (githubUsername != null) {
                        Text(
                            text = "@$githubUsername",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Connected to GitHub",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "GitHub Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Connect with Personal Access Token (PAT)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (githubUsername != null) {
                    OutlinedButton(onClick = onDisconnectGitHub) {
                        Text("Disconnect", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = { showConnectDialog = true },
                        modifier = Modifier.testTag("connect_github_btn")
                    ) {
                        Text("Connect", fontSize = 12.sp)
                    }
                }
            }
        }

        // Tabs: Local Git Status vs GitHub Repositories
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Local Git Status", fontSize = 12.sp) },
                modifier = Modifier.testTag("tab_git_status")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Browse & Download", fontSize = 12.sp) },
                modifier = Modifier.testTag("tab_git_browse")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        when (selectedTab) {
            0 -> {
                // Local Git Status & Commit/Push
                if (activeProject == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Select an active project to view local git status", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Branch: ${gitStatus?.branch ?: "main"}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${activeProject.name} • ${gitStatus?.untrackedFiles?.size ?: 0} file(s) tracked",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "MODIFIED / UNCOMMITTED FILES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val files = gitStatus?.untrackedFiles ?: emptyList()
                        if (files.isEmpty()) {
                            item {
                                Text("Working tree clean. No local uncommitted modifications.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            items(files) { relPath ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "M",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFFB74D),
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Text(
                                        text = relPath,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Commit & Push Changes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = commitMessage,
                                        onValueChange = { commitMessage = it },
                                        placeholder = { Text("Commit message...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showPushApprovalDialog = true },
                                        modifier = Modifier.fillMaxWidth().testTag("git_push_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Push to GitHub (Requires Approval)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Browse & Download Repositories
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search GitHub repositories...") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(onClick = { onSearchPublicRepos(searchQuery) }) {
                            Text("Search")
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(githubRepos) { repo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (repo.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = repo.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (repo.description != null) {
                                            Text(
                                                text = repo.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${repo.stars} stars", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Branch: ${repo.defaultBranch}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Button(
                                        onClick = { onDownloadRepo(repo, repo.defaultBranch) },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Connect Token Dialog
    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = { Text("Connect GitHub Account") },
            text = {
                Column {
                    Text(
                        text = "Enter a GitHub Personal Access Token (classic or fine-grained) with 'repo' scope to browse private repos, download archives, and push commits.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("GitHub Token (ghp_...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("github_token_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tokenInput.isNotBlank()) {
                            onConnectGitHub(tokenInput.trim())
                            showConnectDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_connect_github_btn")
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Explicit Push Approval Dialog (Section 7 in prompt!)
    if (showPushApprovalDialog) {
        AlertDialog(
            onDismissRequest = { showPushApprovalDialog = false },
            title = { Text("Confirm GitHub Push") },
            text = {
                Column {
                    Text(
                        text = "You are about to push changes to GitHub. Please verify the target and files before proceeding:",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Target Branch: ${gitStatus?.branch ?: "main"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Commit Message: \"$commitMessage\"",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "Files to push: ${gitStatus?.untrackedFiles?.size ?: 0} file(s)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPushApprovalDialog = false
                        val owner = githubUsername ?: "user"
                        val repoName = activeProject?.name ?: "repo"
                        onPushCommit(owner, repoName, gitStatus?.branch ?: "main", commitMessage, emptyList())
                    },
                    modifier = Modifier.testTag("confirm_push_approval_btn")
                ) {
                    Text("Approve & Push")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPushApprovalDialog = false }) { Text("Cancel") }
            }
        )
    }
}
