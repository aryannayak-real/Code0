package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AIProviderEntity
import com.example.data.model.BuildDiagnosticEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProposalEntity
import com.example.data.model.RecoverySnapshotEntity

@Database(
    entities = [
        ProjectEntity::class,
        ProposalEntity::class,
        RecoverySnapshotEntity::class,
        AIProviderEntity::class,
        BuildDiagnosticEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class Code0Database : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun proposalDao(): ProposalDao
    abstract fun recoveryDao(): RecoveryDao
    abstract fun aiProviderDao(): AIProviderDao
    abstract fun buildDiagnosticDao(): BuildDiagnosticDao

    companion object {
        @Volatile
        private var INSTANCE: Code0Database? = null

        fun getInstance(context: Context): Code0Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Code0Database::class.java,
                    "code0_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
