package com.example.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DetailScreenTopBar
import com.example.viewmodel.Code0Screen

data class SettingsCategory(
    val screen: Code0Screen,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun SettingsOverviewScreen(
    onNavigateToCategory: (Code0Screen) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        SettingsCategory(
            screen = Code0Screen.SETTINGS_AI_PROVIDERS,
            title = "AI Providers",
            description = "Configure Gemini, OpenAI, Claude, API keys, and models",
            icon = Icons.Default.AutoAwesome
        ),
        SettingsCategory(
            screen = Code0Screen.SETTINGS_APPEARANCE,
            title = "Appearance & Customization",
            description = "Color themes (Charcoal+Green default) and Dark/Light modes",
            icon = Icons.Default.Palette
        ),
        SettingsCategory(
            screen = Code0Screen.SETTINGS_FILE_EXPLORER,
            title = "File Explorer",
            description = "Choose tree hierarchy, breadcrumbs, or combined layout",
            icon = Icons.Default.Folder
        ),
        SettingsCategory(
            screen = Code0Screen.SETTINGS_GITHUB,
            title = "GitHub & Git",
            description = "GitHub account token, repositories, and source control",
            icon = Icons.Default.Source
        ),
        SettingsCategory(
            screen = Code0Screen.SETTINGS_STORAGE,
            title = "Storage & Recovery",
            description = "Disk usage, proposal cache, and undo recovery snapshots",
            icon = Icons.Default.History
        ),
        SettingsCategory(
            screen = Code0Screen.SETTINGS_BUILD_DIAGNOSTICS,
            title = "Build & Diagnostics",
            description = "Project structure integrity check, syntax tests, and logs",
            icon = Icons.Default.BugReport
        ),
        SettingsCategory(
            screen = Code0Screen.SETTINGS_SECURITY,
            title = "Security & Privacy",
            description = "Credential storage, permissions, and AI transmission policies",
            icon = Icons.Default.Security
        ),
        SettingsCategory(
            screen = Code0Screen.SETTINGS_ABOUT,
            title = "General & About",
            description = "Code0 v1.0.0, architecture details, and preferences reset",
            icon = Icons.Default.Info
        )
    )

    Scaffold(
        topBar = {
            DetailScreenTopBar(title = "Settings", onBack = onBack)
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "SETTINGS CATEGORIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(categories) { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigateToCategory(cat.screen) }
                        .testTag("settings_cat_${cat.screen.name}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = cat.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
