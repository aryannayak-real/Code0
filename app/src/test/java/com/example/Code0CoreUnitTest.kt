package com.example

import com.example.data.model.ChangeType
import com.example.data.model.FileExplorerLayout
import com.example.data.model.ProposedFileChange
import com.example.data.model.ThemePreset
import com.example.data.repository.ProposalRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Code0CoreUnitTest {

    private val fakeDao = FakeProposalDao()
    private val fakeRecoveryDao = FakeRecoveryDao()
    private val proposalRepo = ProposalRepository(fakeDao, fakeRecoveryDao)

    @Test
    fun testDiffComputationAddedAndRemovedLines() {
        val original = "line1\nline2\nline3"
        val proposed = "line1\nmodified line2\nline3\nline4"

        val diff = proposalRepo.computeDiff(original, proposed)
        assertNotNull(diff)
        assertTrue(diff.isNotEmpty())

        val addedLines = diff.filter { it.type == ProposalRepository.DiffLineType.ADDED }
        val removedLines = diff.filter { it.type == ProposalRepository.DiffLineType.REMOVED }

        assertTrue("Should have added lines", addedLines.isNotEmpty())
        assertTrue("Should have removed lines", removedLines.isNotEmpty())
    }

    @Test
    fun testProposalSerializationRoundTrip() {
        val changes = listOf(
            ProposedFileChange(
                relativePath = "src/main.py",
                changeType = ChangeType.MODIFIED,
                originalContent = "print('old')",
                proposedContent = "print('new')",
                isSelected = true,
                hasConflict = false
            ),
            ProposedFileChange(
                relativePath = "README.md",
                changeType = ChangeType.CREATED,
                originalContent = "",
                proposedContent = "# New Readme",
                isSelected = true,
                hasConflict = false
            )
        )

        val json = proposalRepo.serializeChanges(changes)
        val deserialized = proposalRepo.parseChanges(json)

        assertEquals(2, deserialized.size)
        assertEquals("src/main.py", deserialized[0].relativePath)
        assertEquals(ChangeType.MODIFIED, deserialized[0].changeType)
        assertEquals("print('new')", deserialized[0].proposedContent)
        assertEquals(ChangeType.CREATED, deserialized[1].changeType)
    }

    @Test
    fun testConflictDetectionWhenFileModifiedOnDisk() {
        val tempDir = File.createTempFile("code0_test_proj", "").apply {
            delete()
            mkdirs()
        }

        try {
            val file = File(tempDir, "script.py")
            file.writeText("original content")

            val proposal = com.example.data.model.ProposalEntity(
                id = "p1",
                projectId = "proj1",
                taskTitle = "Update script",
                description = "Desc",
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                validationStatus = "PASSED",
                changesJson = proposalRepo.serializeChanges(
                    listOf(
                        ProposedFileChange(
                            relativePath = "script.py",
                            changeType = ChangeType.MODIFIED,
                            originalContent = "original content",
                            proposedContent = "new content"
                        )
                    )
                )
            )

            // Disk was altered after proposal creation!
            file.writeText("user edited this concurrently")

            kotlinx.coroutines.runBlocking {
                val verified = proposalRepo.verifyAndDetectConflicts(tempDir, proposal)
                assertTrue("Must flag conflict when disk content differs from proposal baseline", verified[0].hasConflict)
                assertNotNull(verified[0].conflictReason)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testStageDApplyAndStageFUndo() {
        val tempDir = File.createTempFile("code0_apply_test", "").apply {
            delete()
            mkdirs()
        }

        try {
            val file = File(tempDir, "app.kt")
            file.writeText("val initial = 1")

            val proposal = com.example.data.model.ProposalEntity(
                id = "prop_apply",
                projectId = "proj1",
                taskTitle = "Upgrade to val initial = 2",
                description = "Desc",
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                validationStatus = "PASSED",
                changesJson = proposalRepo.serializeChanges(
                    listOf(
                        ProposedFileChange(
                            relativePath = "app.kt",
                            changeType = ChangeType.MODIFIED,
                            originalContent = "val initial = 1",
                            proposedContent = "val initial = 2"
                        )
                    )
                )
            )

            fakeDao.insertProposalDirect(proposal)

            kotlinx.coroutines.runBlocking {
                // Apply Stage D
                val applyResult = proposalRepo.applyProposal(tempDir, "prop_apply")
                assertTrue("Application should succeed", applyResult.success)
                assertEquals(1, applyResult.appliedCount)
                assertEquals("val initial = 2", file.readText())

                // Undo Stage F
                val snapshotId = applyResult.recoverySnapshotId
                assertNotNull(snapshotId)
                val revertSuccess = proposalRepo.revertRecoverySnapshot(tempDir, snapshotId!!)
                assertTrue("Revert should succeed", revertSuccess)
                assertEquals("val initial = 1", file.readText())
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testThemePresetsIntegrity() {
        val defaultPreset = ThemePreset.CHARCOAL_GREEN
        assertEquals("Charcoal + Green", defaultPreset.displayName)
        assertTrue(ThemePreset.values().contains(ThemePreset.BLACK_ORANGE))
        assertTrue(ThemePreset.values().contains(ThemePreset.WHITE_GREY))
        assertTrue(ThemePreset.values().contains(ThemePreset.BLACK_SILVER))
    }

    @Test
    fun testFileExplorerLayouts() {
        assertEquals("Expandable Tree", FileExplorerLayout.EXPANDABLE_TREE.displayName)
        assertEquals("Traditional Navigation", FileExplorerLayout.TRADITIONAL_NAV.displayName)
        assertEquals("Combined Layout", FileExplorerLayout.COMBINED.displayName)
    }
}

// In-memory test implementations for Unit tests
class FakeProposalDao : com.example.data.local.ProposalDao {
    private val map = mutableMapOf<String, com.example.data.model.ProposalEntity>()

    fun insertProposalDirect(p: com.example.data.model.ProposalEntity) { map[p.id] = p }

    override fun getPendingProposalsFlow(): kotlinx.coroutines.flow.Flow<List<com.example.data.model.ProposalEntity>> =
        kotlinx.coroutines.flow.flowOf(map.values.filter { it.status == "PENDING" })

    override fun getProposalsForProjectFlow(projectId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.model.ProposalEntity>> =
        kotlinx.coroutines.flow.flowOf(map.values.filter { it.projectId == projectId })

    override suspend fun getProposalById(id: String): com.example.data.model.ProposalEntity? = map[id]

    override suspend fun insertProposal(proposal: com.example.data.model.ProposalEntity) { map[proposal.id] = proposal }

    override suspend fun updateProposal(proposal: com.example.data.model.ProposalEntity) { map[proposal.id] = proposal }

    override suspend fun updateProposalStatus(id: String, status: String, appliedAt: Long?) {
        map[id]?.let { map[id] = it.copy(status = status, appliedAt = appliedAt) }
    }

    override suspend fun deleteProposalById(id: String) { map.remove(id) }

    override suspend fun purgeRejectedProposals() {
        val toRemove = map.filterValues { it.status == "REJECTED" }.keys
        toRemove.forEach { map.remove(it) }
    }
}

class FakeRecoveryDao : com.example.data.local.RecoveryDao {
    private val map = mutableMapOf<String, com.example.data.model.RecoverySnapshotEntity>()

    override fun getAllSnapshotsFlow(): kotlinx.coroutines.flow.Flow<List<com.example.data.model.RecoverySnapshotEntity>> =
        kotlinx.coroutines.flow.flowOf(map.values.toList())

    override suspend fun getSnapshotById(id: String): com.example.data.model.RecoverySnapshotEntity? = map[id]

    override suspend fun getSnapshotsForProject(projectId: String): List<com.example.data.model.RecoverySnapshotEntity> =
        map.values.filter { it.projectId == projectId }

    override suspend fun insertSnapshot(snapshot: com.example.data.model.RecoverySnapshotEntity) { map[snapshot.id] = snapshot }

    override suspend fun deleteSnapshotById(id: String) { map.remove(id) }

    override suspend fun clearAllSnapshots() { map.clear() }
}
