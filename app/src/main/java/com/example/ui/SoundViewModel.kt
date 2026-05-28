package com.example.ui

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.api.GeminiSoundReimaginer
import com.example.api.ReimagineResult
import com.example.data.SoundDatabase
import com.example.data.SoundRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class AiJob(
    val id: String,
    val title: String,
    val employer: String,
    val status: String, // "OPEN", "COMPLETED"
    val bountyGhs: Double,
    val commissionPercent: Double = 10.0,
    val description: String,
    val requiredPresetName: String = "Vocal Clarity"
)

class SoundViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SoundViewModel"
    private val context = application.applicationContext

    // Local room database initialization
    private val database: SoundDatabase by lazy {
        Room.databaseBuilder(
            context,
            SoundDatabase::class.java,
            "soundcraft_db"
        ).fallbackToDestructiveMigration().build()
    }
    
    private val dao = database.soundDao()

    // Sounds states
    val savedSounds: StateFlow<List<SoundRecording>> = dao.getAllSoundsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communitySounds: StateFlow<List<SoundRecording>> = dao.getCommunitySoundsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen state definitions
    private val _selectedTab = MutableStateFlow("STUDIO")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    // Active Sound States for playbacks and edits
    private val _selectedSound = MutableStateFlow<SoundRecording?>(null)
    val selectedSound: StateFlow<SoundRecording?> = _selectedSound.asStateFlow()

    // MoMo Ghana Profile details (State saved in shared preference for demo permanence)
    private val sharedPrefs = context.getSharedPreferences("SoundCraftPrefs", Context.MODE_PRIVATE)
    
    private val _userEmail = MutableStateFlow(sharedPrefs.getString("email", "engineer.accra@soundcraft.gh") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userMomoNumber = MutableStateFlow(sharedPrefs.getString("momo", "0244123456") ?: "")
    val userMomoNumber: StateFlow<String> = _userMomoNumber.asStateFlow()

    private val _userMomoCarrier = MutableStateFlow(sharedPrefs.getString("carrier", "MTN MoMo") ?: "")
    val userMomoCarrier: StateFlow<String> = _userMomoCarrier.asStateFlow()

    fun updateProfile(email: String, momo: String, carrier: String) {
        _userEmail.value = email
        _userMomoNumber.value = momo
        _userMomoCarrier.value = carrier
        sharedPrefs.edit().apply {
            putString("email", email)
            putString("momo", momo)
            putString("carrier", carrier)
            apply()
        }
    }

    // --- MIC RECORDING ENGINE ---
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    // Real-time custom waveform data populated during recording
    private val _recordingWaveform = MutableStateFlow<List<Float>>(emptyList())
    val recordingWaveform: StateFlow<List<Float>> = _recordingWaveform.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordTimerJob: Job? = null
    private var isSimulatedRecording = false

    fun startRecording(micPermissionGranted: Boolean) {
        if (_isRecording.value) return
        
        _recordingWaveform.value = emptyList()
        _recordingDurationSec.value = 0
        _currentAmplitude.value = 0f
        
        if (!micPermissionGranted) {
            // Force high-fidelity simulation and feedback
            startMockRecording()
            return
        }

        try {
            val fileName = "soundcraft_${System.currentTimeMillis()}.m4a"
            recordingFile = File(context.cacheDir, fileName)
            
            // Safe initialization matching multiple APIs
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(recordingFile!!.absolutePath)
                prepare()
                start()
            }
            
            mediaRecorder = recorder
            _isRecording.value = true
            isSimulatedRecording = false
            startRecordMonitoringLoop()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder, entering Simulation Mode", e)
            startMockRecording()
        }
    }

    private fun startMockRecording() {
        _isRecording.value = true
        isSimulatedRecording = true
        
        // Setup mock file
        recordingFile = File(context.cacheDir, "simulated_mic_${System.currentTimeMillis()}.m4a")
        
        recordTimerJob = viewModelScope.launch {
            var duration = 0
            while (_isRecording.value) {
                delay(100)
                duration += 100
                _recordingDurationSec.value = duration / 1000
                
                // Synthesize dynamic amplitude points for wave graphics
                val amp = (0.2f + 0.6f * Math.random().toFloat()) * (if (duration % 400 < 150) 0.3f else 1.0f)
                _currentAmplitude.value = amp
                
                // Append amplitude points to waveform history (limit to 40 viewable slots)
                val currentList = _recordingWaveform.value.toMutableList()
                if (currentList.size > 40) currentList.removeAt(0)
                currentList.add(amp)
                _recordingWaveform.value = currentList
            }
        }
    }

    private fun startRecordMonitoringLoop() {
        recordTimerJob = viewModelScope.launch {
            var durationMs = 0
            while (_isRecording.value) {
                delay(100)
                durationMs += 100
                _recordingDurationSec.value = durationMs / 1000
                
                // Query MediaRecorder amplitude safely
                val ampVal = try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    // Normalize standard max amplitude range (0 - 32767) to (0.0 - 1.0)
                    (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                } catch (e: Exception) {
                    0.1f
                }
                
                _currentAmplitude.value = ampVal
                val currentList = _recordingWaveform.value.toMutableList()
                if (currentList.size > 40) currentList.removeAt(0)
                currentList.add(ampVal)
                _recordingWaveform.value = currentList
            }
        }
    }

    fun stopRecording(customTitle: String = "Mic Transient Loop") {
        if (!_isRecording.value) return
        
        _isRecording.value = false
        recordTimerJob?.cancel()
        recordTimerJob = null
        
        if (!isSimulatedRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Exception release MediaRecorder", e)
            } finally {
                mediaRecorder = null
            }
        }

        val durationMs = (_recordingDurationSec.value * 1000L).coerceAtLeast(1000L)
        val finalTitle = customTitle.ifBlank { "Mic Input ${System.currentTimeMillis() % 10000}" }
        val filePath = recordingFile?.absolutePath ?: ""

        // Store standard newly recorded sound
        viewModelScope.launch {
            val newSound = SoundRecording(
                title = finalTitle,
                filePath = filePath,
                durationMs = durationMs
            )
            val id = dao.insertSound(newSound)
            val savedOne = dao.getSoundById(id)
            if (savedOne != null) {
                _selectedSound.value = savedOne
            }
            Log.d(TAG, "Sound saved successfully! Path: $filePath")
        }
    }

    // --- PLAYBACK ENGINE ---
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playProgress = MutableStateFlow(0f)
    val playProgress: StateFlow<Float> = _playProgress.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var playTrackerJob: Job? = null
    private var playingSoundId = -1L

    fun getPlayingSoundId(): Long = playingSoundId

    fun togglePlayback(sound: SoundRecording) {
        if (_isPlaying.value && playingSoundId == sound.id) {
            pausePlayback()
        } else {
            startPlayback(sound)
        }
    }

    private fun startPlayback(sound: SoundRecording) {
        pausePlayback() // Halt existing audio streams

        playingSoundId = sound.id
        _isPlaying.value = true

        // For Simulation Sample files or empty actual files, we run simulation playback!
        val isSimulatedPath = sound.isSimulationSample || sound.filePath.isBlank() || !File(sound.filePath).exists()

        if (isSimulatedPath) {
            startMockPlayback()
            return
        }

        try {
            val player = MediaPlayer()
            player.setDataSource(sound.filePath)
            player.prepare()
            
            // Adjust volume based on user-edited Gain DB parameter dynamically!
            // Gain linear multiplier: multiplier = 10^(db / 20)
            val linearGain = Math.pow(10.0, (sound.gainDb / 20.0)).toFloat().coerceIn(0f, 2.5f)
            player.setVolume(linearGain, linearGain)
            
            player.setOnCompletionListener {
                _isPlaying.value = false
                _playProgress.value = 1.0f
                playingSoundId = -1
                playTrackerJob?.cancel()
                try {
                    player.release()
                } catch (ex: Exception) {
                    Log.e(TAG, "Error releasing finished player", ex)
                }
                if (mediaPlayer == player) {
                    mediaPlayer = null
                }
            }
            
            player.start()
            mediaPlayer = player
            
            // Launch player position tracking
            playTrackerJob = viewModelScope.launch {
                try {
                    val durationVal = try { player.duration.toFloat() } catch (ex: Exception) { 1000f }
                    val total = if (durationVal > 0f) durationVal else 1000f
                    while (true) {
                        val isPlayActive = try { player.isPlaying } catch (ex: Exception) { false }
                        if (!isPlayActive) break
                        
                        val currentPos = try { player.currentPosition.toFloat() } catch (ex: Exception) { 0f }
                        _playProgress.value = (currentPos / total).coerceIn(0f, 1f)
                        delay(100)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in playback tracking loop", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error trying to play media file, falling back to Simulator Playback", e)
            startMockPlayback()
        }
    }

    private fun startMockPlayback() {
        playTrackerJob = viewModelScope.launch {
            var currentMs = 0
            val totalMs = 8000 // Mock a standard play envelope loop length
            while (_isPlaying.value) {
                delay(100)
                currentMs += 100
                _playProgress.value = currentMs.toFloat() / totalMs.toFloat()
                if (currentMs >= totalMs) {
                    _isPlaying.value = false
                    _playProgress.value = 1.0f
                    playingSoundId = -1
                    break
                }
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playingSoundId = -1
        playTrackerJob?.cancel()
        playTrackerJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // silent catch
        } finally {
            mediaPlayer = null
        }
    }

    // --- GEMINI DSP ANALYSIS SUGGESTION ---
    private val _aiRecommendationLoading = MutableStateFlow(false)
    val aiRecommendationLoading: StateFlow<Boolean> = _aiRecommendationLoading.asStateFlow()

    private val _aiResult = MutableStateFlow<ReimagineResult?>(null)
    val aiResult: StateFlow<ReimagineResult?> = _aiResult.asStateFlow()

    // AI Royalty Mining States
    private val _miningQuotaUsed = MutableStateFlow(sharedPrefs.getInt("mining_quota_used", 1))
    val miningQuotaUsed: StateFlow<Int> = _miningQuotaUsed.asStateFlow()
    
    private val _minedRoyaltiesGhs = MutableStateFlow(sharedPrefs.getFloat("mined_royalties_ghs", 45.50f))
    val minedRoyaltiesGhs: StateFlow<Float> = _minedRoyaltiesGhs.asStateFlow()

    // Jobs Board States for inter user gains with commissions
    private val _jobs = MutableStateFlow(listOf(
        AiJob(
            id = "job1",
            title = "Clean Osu Street Noise for Highlife Intro",
            employer = "Sarkodie's Sound Desk",
            status = "OPEN",
            bountyGhs = 350.0,
            commissionPercent = 10.0,
            description = "Need a 10s street sound with hum removal and background hiss suppressed. High sibilance control around vocal highs.",
            requiredPresetName = "Vocal Clarity"
        ),
        AiJob(
            id = "job2",
            title = "Azonto Snappy Clap Compression",
            employer = "Killbeatz GH Stems",
            status = "OPEN",
            bountyGhs = 250.0,
            commissionPercent = 12.0,
            description = "Looking for a crisp claps stack with aggressive ratio matching highlife transient peaks. Hum-removed.",
            requiredPresetName = "Mid Punch"
        ),
        AiJob(
            id = "job3",
            title = "De-essed Vocal Hook for Drill Chords",
            employer = "Kumerica Drill Collective",
            status = "OPEN",
            bountyGhs = 500.0,
            commissionPercent = 10.0,
            description = "Raw mic recording has massive high-frequency sibilance. Target with De-esser and hum suppression filters.",
            requiredPresetName = "Hiss Cut"
        )
    ))
    val jobs: StateFlow<List<AiJob>> = _jobs.asStateFlow()

    fun runAiDSPRecommendation(sound: SoundRecording, prompt: String) {
        _aiRecommendationLoading.value = true
        _aiResult.value = null
        
        viewModelScope.launch {
            val response = GeminiSoundReimaginer.suggestDSPAndReimagine(
                prompt = prompt,
                soundTitle = sound.title,
                soundDurationSec = sound.durationMs / 1000f
            )
            
            _aiResult.value = response
            _aiRecommendationLoading.value = false
            
            // Auto update selected sound with the new AI suggested params which can be edited!
            val updatedObj = sound.copy(
                isAiReimagined = true,
                aiPrompt = prompt,
                aiIdeaDescription = "${response.explanation}\n\n[COLLAB ADVICE]\n${response.collaborationIdea}",
                gainDb = response.suggestedGainDb,
                lowCutHz = response.suggestedLowCutHz,
                noiseGateThresholdDb = response.noiseGateThresholdDb,
                compressorRatio = response.compressorRatio,
                compressorThresholdDb = response.compressorThresholdDb,
                title = response.reimaginedTitle,
                
                // Saving new fields
                aiCreativeChain = response.creativeChain,
                aiSoundLayering = response.soundLayering,
                aiComplementTitle = response.newComplementTitle,
                aiComplementDesc = response.newComplementDesc,
                aiSuggestedEqPreset = response.suggestedEqPreset
            )
            
            dao.updateSound(updatedObj)
            _selectedSound.value = updatedObj
        }
    }

    // --- PARAMETERS EDIT MANIPULATIONS ---
    fun updateSoundParameters(
        sound: SoundRecording,
        gain: Float,
        lowCut: Int,
        gate: Float,
        ratio: Float,
        threshold: Float
    ) {
        viewModelScope.launch {
            val updated = sound.copy(
                gainDb = gain,
                lowCutHz = lowCut,
                noiseGateThresholdDb = gate,
                compressorRatio = ratio,
                compressorThresholdDb = threshold
            )
            dao.updateSound(updated)
            _selectedSound.value = updated
        }
    }

    // --- RESTORATION PARAMETERS SAVE METHODS ---
    fun updateRestorationParameters(
        sound: SoundRecording,
        humRemoval: Boolean,
        hissSuppression: Boolean,
        eqPreset: String,
        eqLow: Float,
        eqMid: Float,
        eqHigh: Float,
        deEsser: Boolean,
        deEsserThresh: Float,
        deEsserFreq: Float
    ) {
        viewModelScope.launch {
            val updated = sound.copy(
                humRemovalActive = humRemoval,
                hissSuppressionActive = hissSuppression,
                eqPresetName = eqPreset,
                eqLowGain = eqLow,
                eqMidGain = eqMid,
                eqHighGain = eqHigh,
                deEsserEnabled = deEsser,
                deEsserThresholdDb = deEsserThresh,
                deEsserFrequencyHz = deEsserFreq
            )
            dao.updateSound(updated)
            _selectedSound.value = updated
        }
    }

    fun applyEqPreset(sound: SoundRecording, presetName: String) {
        val (low, mid, high) = when (presetName) {
            "Vocal Clarity" -> Triple(-2.0f, 3.5f, 5.0f)
            "Bass Boost" -> Triple(6.0f, 0.0f, -1.5f)
            "Hiss Cut" -> Triple(1.0f, -1.0f, -6.0f)
            "Mid Punch" -> Triple(-1.0f, 4.0f, 1.0f)
            "Brass Bright" -> Triple(-2.0f, 2.0f, 6.0f)
            else -> Triple(0.0f, 0.0f, 0.0f) // Flat
        }
        viewModelScope.launch {
            val updated = sound.copy(
                eqPresetName = presetName,
                eqLowGain = low,
                eqMidGain = mid,
                eqHighGain = high
            )
            dao.updateSound(updated)
            _selectedSound.value = updated
        }
    }

    fun generateAiCompanionSound(sound: SoundRecording) {
        viewModelScope.launch {
            val companionTitle = sound.aiComplementTitle ?: "AI Companion Layer"
            val companionDesc = sound.aiComplementDesc ?: "Synthesized sound layer to complement original recording."
            
            val companionSound = SoundRecording(
                title = "[GEN] " + companionTitle,
                filePath = "", // Treated as a simulated synthesis loop
                durationMs = sound.durationMs,
                isAiReimagined = true,
                aiPrompt = "Synthesized to complement ${sound.title}",
                aiIdeaDescription = companionDesc,
                gainDb = 0.0f,
                lowCutHz = 60,
                noiseGateThresholdDb = -50f,
                compressorRatio = 2.0f,
                compressorThresholdDb = -15f,
                isSimulationSample = true, // Play back using the simulator logic
                
                // Restore & preset matches
                eqPresetName = sound.aiSuggestedEqPreset,
                aiComplementTitle = companionTitle,
                aiComplementDesc = companionDesc
            )
            val newId = dao.insertSound(companionSound)
            val saved = dao.getSoundById(newId)
            if (saved != null) {
                _selectedSound.value = saved // Load companion sound into Active Transient console
                _selectedTab.value = "STUDIO" // Go back to forge panel to inspect
            }
        }
    }

    // --- AI ROYALTY MINING METHODS ---
    fun submitSoundToAiMining(sound: SoundRecording) {
        if (_miningQuotaUsed.value >= 5) return
        viewModelScope.launch {
            val nextQuota = _miningQuotaUsed.value + 1
            val addedRoyalties = 15.0f // 15 GHS per submission
            val nextRoyalties = _minedRoyaltiesGhs.value + addedRoyalties
            _miningQuotaUsed.value = nextQuota
            _minedRoyaltiesGhs.value = nextRoyalties
            sharedPrefs.edit().apply {
                putInt("mining_quota_used", nextQuota)
                putFloat("mined_royalties_ghs", nextRoyalties)
                apply()
            }
        }
    }
    
    fun resetMiningQuota() {
        _miningQuotaUsed.value = 0
        sharedPrefs.edit().putInt("mining_quota_used", 0).apply()
    }

    // --- JOBS BOARD APPLICATION METHOD ---
    fun applyToJob(jobId: String, sound: SoundRecording) {
        viewModelScope.launch {
            val updated = _jobs.value.map { job ->
                if (job.id == jobId) {
                    job.copy(status = "COMPLETED")
                } else job
            }
            _jobs.value = updated
            
            val jobObj = _jobs.value.find { it.id == jobId }
            if (jobObj != null) {
                val commission = jobObj.bountyGhs * (jobObj.commissionPercent / 100.0)
                val netGains = jobObj.bountyGhs - commission
                val nextRoyalties = _minedRoyaltiesGhs.value + netGains.toFloat()
                _minedRoyaltiesGhs.value = nextRoyalties
                sharedPrefs.edit().putFloat("mined_royalties_ghs", nextRoyalties).apply()
            }
        }
    }

    fun selectSound(sound: SoundRecording?) {
        _selectedSound.value = sound
    }

    // --- LICENSING AND UPLOADS ---
    fun publishToCommunity(
        sound: SoundRecording,
        priceGhs: Double,
        licenseType: String
    ) {
        viewModelScope.launch {
            val updated = sound.copy(
                isLicensedForSale = priceGhs > 0,
                priceGhs = priceGhs,
                licenseType = licenseType,
                creatorEmail = _userEmail.value,
                momoNumber = _userMomoNumber.value,
                momoCarrier = _userMomoCarrier.value,
                isUploadedToCommunity = true
            )
            dao.updateSound(updated)
            _selectedSound.value = updated
        }
    }

    fun deleteSound(sound: SoundRecording) {
        viewModelScope.launch {
            if (_selectedSound.value?.id == sound.id) {
                _selectedSound.value = null
            }
            dao.deleteSound(sound)
        }
    }

    // --- MONETIZATION MOCK PAYMENT FLOW ---
    private val _purchasingSound = MutableStateFlow<SoundRecording?>(null)
    val purchasingSound: StateFlow<SoundRecording?> = _purchasingSound.asStateFlow()

    private val _momoConfirmStatus = MutableStateFlow<String?>(null) // PENDING, SUCCESS, ERROR, null
    val momoConfirmStatus: StateFlow<String?> = _momoConfirmStatus.asStateFlow()

    fun triggerMoMoPurchase(sound: SoundRecording) {
        _purchasingSound.value = sound
        _momoConfirmStatus.value = null
    }

    fun confirmMoMoPayout(buyerMomo: String, buyerCarrier: String) {
        viewModelScope.launch {
            _momoConfirmStatus.value = "PENDING"
            // Simulate realistic MoMo processing latency
            delay(2500)
            
            val soundToBuy = _purchasingSound.value
            if (soundToBuy != null) {
                // Import bought simulation sound into buyer's "My Desk" saved sounds list directly!
                val importedSound = SoundRecording(
                    title = "[BOUGHT] " + soundToBuy.title,
                    filePath = soundToBuy.filePath,
                    durationMs = soundToBuy.durationMs,
                    gainDb = soundToBuy.gainDb,
                    lowCutHz = soundToBuy.lowCutHz,
                    noiseGateThresholdDb = soundToBuy.noiseGateThresholdDb,
                    compressorRatio = soundToBuy.compressorRatio,
                    compressorThresholdDb = soundToBuy.compressorThresholdDb,
                    isAiReimagined = soundToBuy.isAiReimagined,
                    aiIdeaDescription = "Purchased loop. Monetization paid to ${soundToBuy.momoCarrier} Account (${soundToBuy.momoNumber}) via simulated MoMo checkout.",
                    licenseType = soundToBuy.licenseType,
                    priceGhs = soundToBuy.priceGhs
                )
                dao.insertSound(importedSound)
                _momoConfirmStatus.value = "SUCCESS"
                delay(1500)
                _purchasingSound.value = null
                _momoConfirmStatus.value = null
                _selectedTab.value = "STUDIO" // Jump to studio view to play bought sound
            } else {
                _momoConfirmStatus.value = "ERROR"
            }
        }
    }

    fun cancelPurchase() {
        _purchasingSound.value = null
        _momoConfirmStatus.value = null
    }

    // Database Seed for Ghana audio engineer samples
    init {
        viewModelScope.launch {
            // Count entries
            dao.getAllSoundsFlow().collect { list ->
                if (list.isEmpty()) {
                    seedDatabase()
                }
            }
        }
    }

    private suspend fun seedDatabase() {
        val seedTracks = listOf(
            SoundRecording(
                title = "Accra Osu Street Ambient",
                filePath = "",
                durationMs = 9500,
                gainDb = 1.5f,
                lowCutHz = 90,
                noiseGateThresholdDb = -45f,
                compressorRatio = 2.2f,
                compressorThresholdDb = -18f,
                isSimulationSample = true,
                isLicensedForSale = true,
                priceGhs = 25.0,
                licenseType = "Royalty-Free Beats",
                creatorEmail = "producer.osu@gmail.com",
                momoNumber = "0243912182",
                momoCarrier = "MTN MoMo",
                aiIdeaDescription = "Dense ambient recording featuring high-pass cutoff below 90Hz to weed out massive background truck rumbles. Great texture overlay element."
            ),
            SoundRecording(
                title = "Kasoa Market Vocal Hustle",
                filePath = "",
                durationMs = 6000,
                gainDb = 4.0f,
                lowCutHz = 110,
                noiseGateThresholdDb = -35f,
                compressorRatio = 3.5f,
                compressorThresholdDb = -12f,
                isSimulationSample = true,
                isLicensedForSale = true,
                priceGhs = 40.0,
                licenseType = "Commercial Exclusive",
                creatorEmail = "scrapper.kasoa@hotmail.com",
                momoNumber = "0554182991",
                momoCarrier = "MTN MoMo",
                aiIdeaDescription = "Aggressive noise-gating isolation to single out street hawkers calling. Incredible raw background texture for traditional highlife or modern drill drops."
            ),
            SoundRecording(
                title = "Azonto Clapping Stack Loop",
                filePath = "",
                durationMs = 4500,
                gainDb = 0.0f,
                lowCutHz = 150,
                noiseGateThresholdDb = -55f,
                compressorRatio = 1.8f,
                compressorThresholdDb = -20f,
                isSimulationSample = true,
                isLicensedForSale = true,
                priceGhs = 15.0,
                licenseType = "Royalty-Free Beats",
                creatorEmail = "kobby.beats@yahoo.com",
                momoNumber = "0208182991",
                momoCarrier = "Telecel Cash",
                aiIdeaDescription = "High-velocity snappy acoustic claps recorded with mic input. Restored transient envelope."
            ),
            SoundRecording(
                title = "Ghana Drill Sub-Heavy Hat",
                filePath = "",
                durationMs = 8000,
                gainDb = 3.2f,
                lowCutHz = 40,
                noiseGateThresholdDb = -50f,
                compressorRatio = 4.0f,
                compressorThresholdDb = -15f,
                isSimulationSample = true,
                isLicensedForSale = true,
                priceGhs = 50.0,
                licenseType = "Standard License",
                creatorEmail = "momo.drill@gmail.com",
                momoNumber = "0501827725",
                momoCarrier = "AirtelTigo Money",
                aiIdeaDescription = "Ultra heavy sub transient. Compression is kept high to compress heavy bass drums. Ready to incorporate into hard drill beats."
            )
        )
        for (track in seedTracks) {
            dao.insertSound(track)
        }
    }
}
