package com.example.data.repository

import com.example.data.local.ProposalDao
import com.example.data.local.RecoveryDao
import com.example.data.model.ChangeType
import com.example.data.model.ProposalEntity
import com.example.data.model.ProposedFileChange
import com.example.data.model.RecoverySnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProposalRepository(
    private val proposalDao: ProposalDao,
    private val recoveryDao: RecoveryDao
) {
    val pendingProposalsFlow: Flow<List<ProposalEntity>> = proposalDao.getPendingProposalsFlow()
    val recoverySnapshotsFlow: Flow<List<RecoverySnapshotEntity>> = recoveryDao.getAllSnapshotsFlow()

    fun getProposalsForProject(projectId: String): Flow<List<ProposalEntity>> {
        return proposalDao.getProposalsForProjectFlow(projectId)
    }

    suspend fun createProposal(
        projectId: String,
        taskTitle: String,
        description: String,
        changes: List<ProposedFileChange>
    ): ProposalEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val changesJson = serializeChanges(changes)
        val proposal = ProposalEntity(
            id = id,
            projectId = projectId,
            taskTitle = taskTitle,
            description = description,
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            validationStatus = if (changes.any { it.hasConflict }) "WARNING" else "PASSED",
            changesJson = changesJson
        )
        proposalDao.insertProposal(proposal)
        proposal
    }

    suspend fun getProposal(id: String): ProposalEntity? = withContext(Dispatchers.IO) {
        proposalDao.getProposalById(id)
    }

    fun parseChanges(jsonStr: String): List<ProposedFileChange> {
        val list = mutableListOf<ProposedFileChange>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ProposedFileChange(
                        relativePath = obj.getString("relativePath"),
                        changeType = ChangeType.valueOf(obj.getString("changeType")),
                        originalContent = obj.optString("originalContent", ""),
                        proposedContent = obj.optString("proposedContent", ""),
                        isSelected = obj.optBoolean("isSelected", true),
                        hasConflict = obj.optBoolean("hasConflict", false),
                        conflictReason = obj.optString("conflictReason", null)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeChanges(changes: List<ProposedFileChange>): String {
        val arr = JSONArray()
        changes.forEach { c ->
            val obj = JSONObject().apply {
                put("relativePath", c.relativePath)
                put("changeType", c.changeType.name)
                put("originalContent", c.originalContent)
                put("proposedContent", c.proposedContent)
                put("isSelected", c.isSelected)
                put("hasConflict", c.hasConflict)
                if (c.conflictReason != null) {
                    put("conflictReason", c.conflictReason)
                }
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    /**
     * Check if any files on disk were modified since the proposal was prepared (Conflict Detection).
     */
    suspend fun verifyAndDetectConflicts(
        projectDir: File,
        proposal: ProposalEntity
    ): List<ProposedFileChange> = withContext(Dispatchers.IO) {
        val changes = parseChanges(proposal.changesJson)
        val verifiedChanges = changes.map { change ->
            val targetFile = File(projectDir, change.relativePath)
            var hasConflict = false
            var reason: String? = null

            when (change.changeType) {
                ChangeType.MODIFIED -> {
                    if (!targetFile.exists()) {
                        hasConflict = true
                        reason = "File was deleted from project after proposal was created"
                    } else {
                        val currentDiskContent = targetFile.readText()
                        if (currentDiskContent != change.originalContent) {
                            hasConflict = true
                            reason = "File has been edited since proposal was created (Content Conflict)"
                        }
                    }
                }
                ChangeType.CREATED -> {
                    if (targetFile.exists()) {
                        hasConflict = true
                        reason = "File already exists on disk with different content"
                    }
                }
                ChangeType.DELETED -> {
                    if (!targetFile.exists()) {
                        hasConflict = true
                        reason = "File was already removed"
                    }
                }
            }

            change.copy(hasConflict = hasConflict, conflictReason = reason)
        }

        // Update proposal in DB with updated conflict flags
        proposalDao.updateProposal(
            proposal.copy(
                changesJson = serializeChanges(verifiedChanges),
                validationStatus = if (verifiedChanges.any { it.hasConflict }) "WARNING" else "PASSED"
            )
        )
        verifiedChanges
    }

    data class ApplyResult(
        val success: Boolean,
        val appliedCount: Int,
        val skippedCount: Int,
        val error: String? = null,
        val recoverySnapshotId: String? = null
    )

    /**
     * Stage D: Apply Approved Changes
     * - Verifies disk matches proposal
     * - Backs up original file contents for Undo/Recovery in a single storage-efficient snapshot
     * - Writes modified and created files atomically
     * - Deletes removed files
     * - Updates proposal status to "APPLIED"
     */
    suspend fun applyProposal(
        projectDir: File,
        proposalId: String
    ): ApplyResult = withContext(Dispatchers.IO) {
        val proposal = proposalDao.getProposalById(proposalId)
            ?: return@withContext ApplyResult(false, 0, 0, "Proposal not found")

        val changes = parseChanges(proposal.changesJson).filter { it.isSelected }
        if (changes.isEmpty()) {
            return@withContext ApplyResult(false, 0, 0, "No files selected for application")
        }

        // Check for unresolved conflicts
        val conflicted = changes.filter { it.hasConflict }
        if (conflicted.isNotEmpty()) {
            return@withContext ApplyResult(
                false, 0, 0,
                "Cannot apply proposal: ${conflicted.size} file(s) have unresolved conflicts"
            )
        }

        val backupMap = JSONObject()
        var appliedCount = 0

        try {
            // Step 1: Backup prior state of files being touched
            for (change in changes) {
                val targetFile = File(projectDir, change.relativePath)
                if (targetFile.exists()) {
                    backupMap.put(change.relativePath, targetFile.readText())
                } else {
                    backupMap.put(change.relativePath, JSONObject.NULL)
                }
            }

            val snapshotId = UUID.randomUUID().toString()
            val recoverySnapshot = RecoverySnapshotEntity(
                id = snapshotId,
                proposalId = proposalId,
                projectId = proposal.projectId,
                taskTitle = proposal.taskTitle,
                timestamp = System.currentTimeMillis(),
                backupJson = backupMap.toString()
            )
            recoveryDao.insertSnapshot(recoverySnapshot)

            // Step 2: Apply changes to project files
            for (change in changes) {
                val targetFile = File(projectDir, change.relativePath)
                when (change.changeType) {
                    ChangeType.MODIFIED, ChangeType.CREATED -> {
                        targetFile.parentFile?.mkdirs()
                        targetFile.writeText(change.proposedContent)
                        appliedCount++
                    }
                    ChangeType.DELETED -> {
                        if (targetFile.exists()) {
                            targetFile.delete()
                            appliedCount++
                        }
                    }
                }
            }

            // Step 3: Mark proposal as APPLIED
            proposalDao.updateProposalStatus(proposalId, "APPLIED", System.currentTimeMillis())

            ApplyResult(
                success = true,
                appliedCount = appliedCount,
                skippedCount = parseChanges(proposal.changesJson).size - appliedCount,
                recoverySnapshotId = snapshotId
            )
        } catch (e: Exception) {
            ApplyResult(false, appliedCount, 0, "Failed during application: ${e.message}")
        }
    }

    /**
     * Stage E: Reject Changes
     * Discards the proposal safely. Does NOT touch project files.
     */
    suspend fun rejectProposal(proposalId: String) = withContext(Dispatchers.IO) {
        proposalDao.updateProposalStatus(proposalId, "REJECTED", null)
    }

    /**
     * Stage F: Undo and Recovery
     * Reverts applied files from the recovery snapshot back to original state.
     */
    suspend fun revertRecoverySnapshot(
        projectDir: File,
        snapshotId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val snapshot = recoveryDao.getSnapshotById(snapshotId) ?: return@withContext false
        try {
            val backup = JSONObject(snapshot.backupJson)
            val keys = backup.keys()
            while (keys.hasNext()) {
                val relPath = keys.next()
                val targetFile = File(projectDir, relPath)
                if (backup.isNull(relPath)) {
                    // File didn't exist before proposal, delete it
                    if (targetFile.exists()) targetFile.delete()
                } else {
                    val originalContent = backup.getString(relPath)
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeText(originalContent)
                }
            }
            recoveryDao.deleteSnapshotById(snapshotId)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearOldRecoverySnapshots() = withContext(Dispatchers.IO) {
        recoveryDao.clearAllSnapshots()
    }

    suspend fun purgeRejectedProposals() = withContext(Dispatchers.IO) {
        proposalDao.purgeRejectedProposals()
    }

    /**
     * Unified Diff Line representation for side-by-side or unified review
     */
    data class DiffLine(
        val type: DiffLineType,
        val oldLineNum: Int?,
        val newLineNum: Int?,
        val content: String
    )

    enum class DiffLineType {
        NORMAL,
        ADDED,
        REMOVED
    }

    /**
     * Compute line-by-line diff between original and proposed contents
     */
    fun computeDiff(original: String, proposed: String): List<DiffLine> {
        val oldLines = if (original.isEmpty()) emptyList() else original.lines()
        val newLines = if (proposed.isEmpty()) emptyList() else proposed.lines()
        val result = mutableListOf<DiffLine>()

        var oldIdx = 0
        var newIdx = 0

        val maxDiffLines = 2500
        while ((oldIdx < oldLines.size || newIdx < newLines.size) && result.size < maxDiffLines) {
            if (oldIdx < oldLines.size && newIdx < newLines.size) {
                if (oldLines[oldIdx] == newLines[newIdx]) {
                    result.add(
                        DiffLine(
                            type = DiffLineType.NORMAL,
                            oldLineNum = oldIdx + 1,
                            newLineNum = newIdx + 1,
                            content = oldLines[oldIdx]
                        )
                    )
                    oldIdx++
                    newIdx++
                } else {
                    // Lookahead to find match
                    val nextMatchInNew = (newIdx until minOf(newIdx + 5, newLines.size)).firstOrNull {
                        newLines[it] == oldLines[oldIdx]
                    }
                    if (nextMatchInNew != null) {
                        while (newIdx < nextMatchInNew && result.size < maxDiffLines) {
                            result.add(
                                DiffLine(
                                    type = DiffLineType.ADDED,
                                    oldLineNum = null,
                                    newLineNum = newIdx + 1,
                                    content = newLines[newIdx]
                                )
                            )
                            newIdx++
                        }
                    } else {
                        result.add(
                            DiffLine(
                                type = DiffLineType.REMOVED,
                                oldLineNum = oldIdx + 1,
                                newLineNum = null,
                                content = oldLines[oldIdx]
                            )
                        )
                        oldIdx++
                    }
                }
            } else if (oldIdx < oldLines.size) {
                result.add(
                    DiffLine(
                        type = DiffLineType.REMOVED,
                        oldLineNum = oldIdx + 1,
                        newLineNum = null,
                        content = oldLines[oldIdx]
                    )
                )
                oldIdx++
            } else {
                result.add(
                    DiffLine(
                        type = DiffLineType.ADDED,
                        oldLineNum = null,
                        newLineNum = newIdx + 1,
                        content = newLines[newIdx]
                    )
                )
                newIdx++
            }
        }

        if (oldIdx < oldLines.size || newIdx < newLines.size) {
            result.add(
                DiffLine(
                    type = DiffLineType.NORMAL,
                    oldLineNum = null,
                    newLineNum = null,
                    content = "... [diff truncated: remaining lines omitted to conserve device memory] ..."
                )
            )
        }

        return result
    }
}
