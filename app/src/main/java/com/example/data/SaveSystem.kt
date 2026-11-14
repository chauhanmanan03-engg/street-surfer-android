package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player_save")
data class PlayerSaveEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 500,
    val keys: Int = 3,
    val highScore: Long = 0L,
    val permanentMultiplier: Int = 1,
    val hoverboardsOwned: Int = 3,
    val headstartsOwned: Int = 1,
    val equippedCharacterId: String = "char_blaze",
    val equippedBoardId: String = "board_sunburst",
    val soundEnabled: Boolean = true,
    val missionSetIndex: Int = 0
)

@Entity(tableName = "powerup_upgrades")
data class PowerUpUpgradeEntity(
    @PrimaryKey val powerUpId: String,
    val level: Int = 1
)

@Entity(tableName = "active_missions")
data class MissionEntity(
    @PrimaryKey val slotIndex: Int, // 0, 1, 2
    val missionKey: String,
    val title: String,
    val metricType: String,
    val targetValue: Int,
    val currentProgress: Int,
    val isCompleted: Boolean,
    val isSingleRun: Boolean
)

@Entity(tableName = "unlockable_items")
data class UnlockableItemEntity(
    @PrimaryKey val itemId: String,
    val category: String, // "CHARACTER" or "BOARD"
    val isUnlocked: Boolean
)

@Dao
interface StreetSurferDao {
    @Query("SELECT * FROM player_save WHERE id = 1")
    fun observePlayerSave(): Flow<PlayerSaveEntity?>

    @Query("SELECT * FROM player_save WHERE id = 1")
    suspend fun getPlayerSave(): PlayerSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerSave(save: PlayerSaveEntity)

    @Query("SELECT * FROM powerup_upgrades")
    fun observeUpgrades(): Flow<List<PowerUpUpgradeEntity>>

    @Query("SELECT * FROM powerup_upgrades")
    suspend fun getUpgrades(): List<PowerUpUpgradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUpgrade(upgrade: PowerUpUpgradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUpgrades(upgrades: List<PowerUpUpgradeEntity>)

    @Query("SELECT * FROM active_missions ORDER BY slotIndex ASC")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM active_missions ORDER BY slotIndex ASC")
    suspend fun getMissions(): List<MissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMissions(missions: List<MissionEntity>)

    @Query("SELECT * FROM unlockable_items")
    fun observeUnlockables(): Flow<List<UnlockableItemEntity>>

    @Query("SELECT * FROM unlockable_items")
    suspend fun getUnlockables(): List<UnlockableItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnlockable(item: UnlockableItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnlockables(items: List<UnlockableItemEntity>)
}

@Database(
    entities = [
        PlayerSaveEntity::class,
        PowerUpUpgradeEntity::class,
        MissionEntity::class,
        UnlockableItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StreetSurferDatabase : RoomDatabase() {
    abstract fun streetSurferDao(): StreetSurferDao

    companion object {
        @Volatile
        private var INSTANCE: StreetSurferDatabase? = null

        fun getInstance(context: Context): StreetSurferDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StreetSurferDatabase::class.java,
                    "street_surfer_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
