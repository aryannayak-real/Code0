package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val directoryPath: String,
    val projectType: String,
    val description: String,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val gitBranch: String = "main",
    val remoteUrl: String? = null,
    val fileCount: Int = 0,
    val totalSize: Long = 0L
)

data class ProjectFile(
    val name: String,
    val path: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String,
    val children: List<ProjectFile> = emptyList()
)

enum class ChangeType {
    MODIFIED,
    CREATED,
    DELETED
}

@Entity(tableName = "proposals")
data class ProposalEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val taskTitle: String,
    val description: String,
    val status: String, // "PENDING", "APPLIED", "REJECTED"
    val createdAt: Long,
    val appliedAt: Long? = null,
    val validationStatus: String = "PASSED", // "PASSED", "WARNING", "SKIPPED"
    val changesJson: String // Serialized List<ProposedFileChange>
)

data class ProposedFileChange(
    val relativePath: String,
    val changeType: ChangeType,
    val originalContent: String,
    val proposedContent: String,
    val isSelected: Boolean = true,
    val hasConflict: Boolean = false,
    val conflictReason: String? = null
)

@Entity(tableName = "recovery_snapshots")
data class RecoverySnapshotEntity(
    @PrimaryKey val id: String,
    val proposalId: String,
    val projectId: String,
    val taskTitle: String,
    val timestamp: Long,
    val backupJson: String // Serialized map of relativePath -> originalContent
)

@Entity(tableName = "ai_providers")
data class AIProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "GEMINI", "OPENAI", "ANTHROPIC", "CUSTOM"
    val apiKey: String,
    val modelName: String,
    val endpointUrl: String,
    val isActive: Boolean = false
)

@Entity(tableName = "build_diagnostics")
data class BuildDiagnosticEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val taskName: String,
    val timestamp: Long,
    val status: String, // "SUCCESS", "FAILED", "RUNNING"
    val logs: String,
    val durationMs: Long,
    val artifactPath: String? = null
)

enum class ThemePreset(val displayName: String) {
    CHARCOAL_GREEN("Charcoal + Green"),
    BLACK_ORANGE("Black + Orange"),
    WHITE_GREY("White + Grey"),
    BLACK_SILVER("Black + Silver")
}

enum class ThemeMode(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

enum class FileExplorerLayout(val displayName: String, val description: String) {
    EXPANDABLE_TREE("Expandable Tree", "Expand and collapse folders inline in a tree hierarchy"),
    TRADITIONAL_NAV("Traditional Navigation", "Browse folder by folder with breadcrumb trail and back navigation"),
    COMBINED("Combined Layout", "Split view with quick folder tree on the side and file list in the center")
}

data class GitRepoItem(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val isPrivate: Boolean,
    val htmlUrl: String,
    val defaultBranch: String,
    val stars: Int,
    val updatedAt: String
)

data class GitStatusSummary(
    val branch: String,
    val modifiedFiles: List<String>,
    val untrackedFiles: List<String>,
    val deletedFiles: List<String>,
    val aheadCount: Int = 0,
    val behindCount: Int = 0
)

data class ChatMessage(
    val id: String,
    val sender: String, // "user", "assistant", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposalId: String? = null,
    val attachedFiles: List<String> = emptyList()
)

data class FileSearchResult(
    val filePath: String,
    val relativePath: String,
    val fileName: String,
    val lineNumber: Int,
    val lineContent: String,
    val matchStartIndex: Int,
    val matchLength: Int
)
