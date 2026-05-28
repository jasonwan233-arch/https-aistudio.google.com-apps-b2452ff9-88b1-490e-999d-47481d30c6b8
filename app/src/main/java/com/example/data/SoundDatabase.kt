package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sounds")
data class SoundRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isAiReimagined: Boolean = false,
    val aiPrompt: String? = null,
    val aiIdeaDescription: String? = null,
    // DSP parameters
    val gainDb: Float = 0.0f,
    val lowCutHz: Int = 0, // 0 = bypassed/off
    val noiseGateThresholdDb: Float = -60.0f, // -60 = bypassed/off
    val compressorRatio: Float = 1.0f, // 1.0 = 1:1 (bypassed/off)
    val compressorThresholdDb: Float = -20.0f,
    // Licensing & Monetization (Ghana Localized)
    val isLicensedForSale: Boolean = false,
    val priceGhs: Double = 0.0,
    val licenseType: String = "Royalty-Free", // Standard, Royalty-Free, Commercial, Creative-Commons
    val creatorEmail: String? = null,
    val momoNumber: String? = null,
    val momoCarrier: String? = null, // MTN MoMo, Telecel Cash, AirtelTigo Money
    val isUploadedToCommunity: Boolean = false,
    // Flag to mark built-in simulation tracks
    val isSimulationSample: Boolean = false,
    
    // Audio Restoration Suite Parameters
    val humRemovalActive: Boolean = false,
    val hissSuppressionActive: Boolean = false,
    val eqLowGain: Float = 0.0f,
    val eqMidGain: Float = 0.0f,
    val eqHighGain: Float = 0.0f,
    val eqPresetName: String = "Flat", // Flat, Vocal Clarity, Bass Boost, Hiss Cut, Mid Punch, Brass Bright
    val deEsserThresholdDb: Float = -60.0f, // -60.0f = bypassed/off, up to 0.0f
    val deEsserFrequencyHz: Float = 6000.0f, // 4000Hz to 10000Hz sibilance focus
    val deEsserEnabled: Boolean = false,

    // AI Creative Intelligence Suggestions Fields
    val aiCreativeChain: String? = null,
    val aiSoundLayering: String? = null,
    val aiComplementTitle: String? = null,
    val aiComplementDesc: String? = null,
    val aiSuggestedEqPreset: String = "Flat"
)

@Dao
interface SoundDao {
    @Query("SELECT * FROM sounds ORDER BY createdAt DESC")
    fun getAllSoundsFlow(): Flow<List<SoundRecording>>

    @Query("SELECT * FROM sounds WHERE isSimulationSample = 1 OR isUploadedToCommunity = 1 ORDER BY createdAt DESC")
    fun getCommunitySoundsFlow(): Flow<List<SoundRecording>>

    @Query("SELECT * FROM sounds WHERE id = :id")
    suspend fun getSoundById(id: Long): SoundRecording?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSound(sound: SoundRecording): Long

    @Delete
    suspend fun deleteSound(sound: SoundRecording)

    @Update
    suspend fun updateSound(sound: SoundRecording)
}

@Database(entities = [SoundRecording::class], version = 2, exportSchema = false)
abstract class SoundDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao
}
