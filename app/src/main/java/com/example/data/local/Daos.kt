package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AIProviderEntity
import com.example.data.model.BuildDiagnosticEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProposalEntity
import com.example.data.model.RecoverySnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModifiedAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface ProposalDao {
    @Query("SELECT * FROM proposals WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingProposalsFlow(): Flow<List<ProposalEntity>>

    @Query("SELECT * FROM proposals WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getProposalsForProjectFlow(projectId: String): Flow<List<ProposalEntity>>

    @Query("SELECT * FROM proposals WHERE id = :id LIMIT 1")
    suspend fun getProposalById(id: String): ProposalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProposal(proposal: ProposalEntity)

    @Update
    suspend fun updateProposal(proposal: ProposalEntity)

    @Query("UPDATE proposals SET status = :status, appliedAt = :appliedAt WHERE id = :id")
    suspend fun updateProposalStatus(id: String, status: String, appliedAt: Long?)

    @Query("DELETE FROM proposals WHERE id = :id")
    suspend fun deleteProposalById(id: String)

    @Query("DELETE FROM proposals WHERE status = 'REJECTED'")
    suspend fun purgeRejectedProposals()
}

@Dao
interface RecoveryDao {
    @Query("SELECT * FROM recovery_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshotsFlow(): Flow<List<RecoverySnapshotEntity>>

    @Query("SELECT * FROM recovery_snapshots WHERE id = :id LIMIT 1")
    suspend fun getSnapshotById(id: String): RecoverySnapshotEntity?

    @Query("SELECT * FROM recovery_snapshots WHERE projectId = :projectId ORDER BY timestamp DESC")
    suspend fun getSnapshotsForProject(projectId: String): List<RecoverySnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: RecoverySnapshotEntity)

    @Query("DELETE FROM recovery_snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: String)

    @Query("DELETE FROM recovery_snapshots")
    suspend fun clearAllSnapshots()
}

@Dao
interface AIProviderDao {
    @Query("SELECT * FROM ai_providers ORDER BY name ASC")
    fun getAllProvidersFlow(): Flow<List<AIProviderEntity>>

    @Query("SELECT * FROM ai_providers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProvider(): AIProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: AIProviderEntity)

    @Update
    suspend fun updateProvider(provider: AIProviderEntity)

    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteProviderById(id: String)

    @Query("UPDATE ai_providers SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE ai_providers SET isActive = 1 WHERE id = :id")
    suspend fun setActiveProvider(id: String)
}

@Dao
interface BuildDiagnosticDao {
    @Query("SELECT * FROM build_diagnostics ORDER BY timestamp DESC")
    fun getAllDiagnosticsFlow(): Flow<List<BuildDiagnosticEntity>>

    @Query("SELECT * FROM build_diagnostics WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT 20")
    suspend fun getDiagnosticsForProject(projectId: String): List<BuildDiagnosticEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnostic(diagnostic: BuildDiagnosticEntity)

    @Query("DELETE FROM build_diagnostics")
    suspend fun clearAllDiagnostics()
}
