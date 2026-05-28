package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SoundRecording
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.sin

// Mapping legacy color tokens to Sleek Interface themes
private val CharcoalBg = SleekBg
private val ConsolePanel = SleekSurface
private val ConsoleBorder = SleekBorder
private val NeonAmber = SleekLavender
private val ToxicGreen = SleekGreen
private val ColdBlue = SleekLightPurple
private val GhostWhite = SleekText
private val MutedSlate = SleekSubText


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundCraftApp(
    viewModel: SoundViewModel,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val savedSounds by viewModel.savedSounds.collectAsStateWithLifecycle()
    val communitySounds by viewModel.communitySounds.collectAsStateWithLifecycle()
    val selectedSound by viewModel.selectedSound.collectAsStateWithLifecycle()
    
    // User credentials / payouts parameters
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userMomoNumber by viewModel.userMomoNumber.collectAsStateWithLifecycle()
    val userMomoCarrier by viewModel.userMomoCarrier.collectAsStateWithLifecycle()

    // Dialog trigger controls
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf<SoundRecording?>(null) }
    var showLicenseDetailsDialog by remember { mutableStateOf<SoundRecording?>(null) }
    
    // Purchasing flow states
    val purchasingSound by viewModel.purchasingSound.collectAsStateWithLifecycle()
    val momoConfirmStatus by viewModel.momoConfirmStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SleekLavender),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Mic Icon",
                                    tint = SleekDeepPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SonicForge GH",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekText,
                                    letterSpacing = (-0.3).sp,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "MASTERING SUITE V4.2",
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.2.sp,
                                    color = SleekLavender,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Latency and Mic confirmation indicator
                        Row(
                            modifier = Modifier
                                .background(SleekBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (micPermissionGranted) SleekGreen else SignalLedRed)
                            )
                            Text(
                                text = "LATENCY: 4ms",
                                color = SleekLavender,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier
                            .testTag("payout_settings_btn")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Payout Setup",
                            tint = SleekSubText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekBg
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SleekBg,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(width = 1.dp, color = SleekBorder, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
            ) {
                listOf(
                    Triple("STUDIO", Icons.Default.Refresh, "Forge"),
                    Triple("AI SUGGEST", Icons.Default.Add, "Collab"),
                    Triple("COMMUNITY", Icons.Default.Share, "Market")
                ).forEach { (tabName, icon, label) ->
                    NavigationBarItem(
                        selected = selectedTab == tabName,
                        onClick = { viewModel.selectTab(tabName) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selectedTab == tabName) SleekLavender else SleekSubText
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == tabName) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == tabName) SleekLavender else SleekSubText
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = SleekBorder
                        ),
                        modifier = Modifier.testTag("tab_$tabName")
                    )
                }
            }
        },
        containerColor = SleekBg,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column {
                when (selectedTab) {
                    "STUDIO" -> {
                        StudioTabScreen(
                            viewModel = viewModel,
                            selectedSound = selectedSound,
                            savedSounds = savedSounds,
                            micPermissionGranted = micPermissionGranted,
                            onRequestMicPermission = onRequestMicPermission,
                            onPublishSound = { showPublishDialog = it }
                        )
                    }
                    "AI SUGGEST" -> {
                        AiSuggestTabScreen(
                            viewModel = viewModel,
                            selectedSound = selectedSound
                        )
                    }
                    "COMMUNITY" -> {
                        CommunityTabScreen(
                            viewModel = viewModel,
                            communitySounds = communitySounds,
                            onTriggerPurchase = { viewModel.triggerMoMoPurchase(it) },
                            onShowLicensing = { showLicenseDetailsDialog = it }
                        )
                    }
                }
            }

            // MODALS & OVERLAYS LIST

            // 1. Payout and email custom configuration modal
            if (showProfileDialog) {
                ProfileSetupDialog(
                    currentEmail = userEmail,
                    currentMomo = userMomoNumber,
                    currentCarrier = userMomoCarrier,
                    onDismiss = { showProfileDialog = false },
                    onSave = { email, momo, carrier ->
                        viewModel.updateProfile(email, momo, carrier)
                        showProfileDialog = false
                    }
                )
            }

            // 2. Licensing customization and community publishing modal
            if (showPublishDialog != null) {
                PublishSoundDialog(
                    sound = showPublishDialog!!,
                    onDismiss = { showPublishDialog = null },
                    onPublish = { price, license ->
                        viewModel.publishToCommunity(showPublishDialog!!, price, license)
                        showPublishDialog = null
                    }
                )
            }

            // 3. Ghanaian Mobile Money purchase checkout overlay
            if (purchasingSound != null) {
                MoMoCheckoutDialog(
                    sound = purchasingSound!!,
                    status = momoConfirmStatus,
                    onDismiss = { viewModel.cancelPurchase() },
                    onConfirmPayment = { number, carrier ->
                        viewModel.confirmMoMoPayout(number, carrier)
                    }
                )
            }

            // 4. Detailed license terms dictionary modal
            if (showLicenseDetailsDialog != null) {
                LicenseDetailsDialog(
                    sound = showLicenseDetailsDialog!!,
                    onDismiss = { showLicenseDetailsDialog = null }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1: THE STUDIO & ENGINE DECK
// -------------------------------------------------------------

@Composable
fun StudioTabScreen(
    viewModel: SoundViewModel,
    selectedSound: SoundRecording?,
    savedSounds: List<SoundRecording>,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    onPublishSound: (SoundRecording) -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val recordWaveform by viewModel.recordingWaveform.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playProgress by viewModel.playProgress.collectAsStateWithLifecycle()

    var customNameInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MODULE A: MIC CAPTURING CONSOLE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ConsolePanel),
                border = BorderStroke(1.dp, ConsoleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CAPSULE INPUT MONITOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Waveform graphic canvas representer!
                    InputMonitorWaveform(
                        isRecording = isRecording,
                        recordProgressSec = recordDurationSec,
                        amplitudeHistory = recordWaveform
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isRecording) {
                        // Custom Name Prompt while capturing
                        OutlinedTextField(
                            value = customNameInput,
                            onValueChange = { customNameInput = it },
                            label = { Text("Sound Label / Asset Tag", color = MutedSlate) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonAmber,
                                unfocusedBorderColor = ConsoleBorder,
                                focusedTextColor = GhostWhite,
                                unfocusedTextColor = GhostWhite
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recording_name_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRecording) {
                            Button(
                                onClick = {
                                    if (!micPermissionGranted) {
                                        // Request
                                        onRequestMicPermission()
                                    }
                                    viewModel.startRecording(micPermissionGranted)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SignalLedRed),
                                modifier = Modifier
                                    .testTag("start_record_btn")
                                    .weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ENGAGE RECORD", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.stopRecording(customNameInput)
                                    customNameInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ToxicGreen),
                                modifier = Modifier
                                    .testTag("stop_record_btn")
                                    .weight(1f)
                            ) {
                                Text("COMMIT CAPTURE", color = CharcoalBg, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // MODULE B: ACTIVE WAV CARVER (DSP RACK)
        if (selectedSound != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ConsolePanel),
                    border = BorderStroke(1.dp, ConsoleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ACTIVE TRANSIENT: ${selectedSound.title}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Duration: ${(selectedSound.durationMs / 1000f)}s | Mode: ${if (selectedSound.isAiReimagined) "AI Carved" else "Raw Clean"}",
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                            }
                            
                            Row {
                                // Delete source file button
                                IconButton(
                                    onClick = { viewModel.deleteSound(selectedSound) },
                                    modifier = Modifier.testTag("delete_sound_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = SignalLedRed)
                                }
                                
                                // Share or List button
                                Button(
                                    onClick = { onPublishSound(selectedSound) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ConsoleBorder),
                                    modifier = Modifier.testTag("publish_sound_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Collab", tint = ToxicGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("List GHS", fontSize = 11.sp, color = ToxicGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Playback track control
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ConsoleBorder)
                        ) {
                            val activeProgress = if (viewModel.getPlayingSoundId() == selectedSound.id) playProgress else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(activeProgress)
                                    .background(ToxicGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Audio dynamic toggle
                        Button(
                            onClick = { viewModel.togglePlayback(selectedSound) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlaying && viewModel.getPlayingSoundId() == selectedSound.id) NeonAmber else ColdBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_play_btn")
                        ) {
                            val promptLabel = if (isPlaying && viewModel.getPlayingSoundId() == selectedSound.id) "PAUSE SIGNAL" else "MONITOR AUDIO STREAM"
                            Text(promptLabel, fontWeight = FontWeight.Bold, color = CharcoalBg)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = ConsoleBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ANALOG EFFECT DECK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedSlate,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // CARVER PARAMETER 1: GAIN
                        DspSliderRow(
                            label = "Signal Input Gain",
                            valueString = "${String.format("%.1f", selectedSound.gainDb)} dB",
                            value = selectedSound.gainDb,
                            onValueChange = {
                                viewModel.updateSoundParameters(
                                    selectedSound, it, selectedSound.lowCutHz,
                                    selectedSound.noiseGateThresholdDb, selectedSound.compressorRatio,
                                    selectedSound.compressorThresholdDb
                                )
                            },
                            range = -12f..12f,
                            tag = "gain"
                        )

                        // CARVER PARAMETER 2: HIGH PASS (LOW CUT)
                        DspSliderRow(
                            label = "High Pass (Low-Cut)",
                            valueString = if (selectedSound.lowCutHz == 0) "Bypassed" else "${selectedSound.lowCutHz} Hz",
                            value = selectedSound.lowCutHz.toFloat(),
                            onValueChange = {
                                viewModel.updateSoundParameters(
                                    selectedSound, selectedSound.gainDb, it.toInt(),
                                    selectedSound.noiseGateThresholdDb, selectedSound.compressorRatio,
                                    selectedSound.compressorThresholdDb
                                )
                            },
                            range = 0f..200f,
                            tag = "lowcut"
                        )

                        // CARVER PARAMETER 3: NOISE FLOATING GATE
                        DspSliderRow(
                            label = "Noise Floor Gate Threshold",
                            valueString = if (selectedSound.noiseGateThresholdDb <= -60f) "Gate Bypass" else "${String.format("%.1f", selectedSound.noiseGateThresholdDb)} dB",
                            value = selectedSound.noiseGateThresholdDb,
                            onValueChange = {
                                viewModel.updateSoundParameters(
                                    selectedSound, selectedSound.gainDb, selectedSound.lowCutHz,
                                    it, selectedSound.compressorRatio, selectedSound.compressorThresholdDb
                                )
                            },
                            range = -60f..-20f,
                            tag = "gate"
                        )

                        // CARVER PARAMETER 4: RATIO
                        DspSliderRow(
                            label = "Compressor Ratio",
                            valueString = if (selectedSound.compressorRatio <= 1.0f) "1:1 No Comp" else "${String.format("%.1f", selectedSound.compressorRatio)}:1",
                            value = selectedSound.compressorRatio,
                            onValueChange = {
                                viewModel.updateSoundParameters(
                                    selectedSound, selectedSound.gainDb, selectedSound.lowCutHz,
                                    selectedSound.noiseGateThresholdDb, it, selectedSound.compressorThresholdDb
                                )
                            },
                            range = 1f..8f,
                            tag = "ratio"
                        )

                        // CARVER PARAMETER 5: COMPRESSOR THRESHOLD
                        DspSliderRow(
                            label = "Compressor Threshold",
                            valueString = "${String.format("%.1f", selectedSound.compressorThresholdDb)} dB",
                            value = selectedSound.compressorThresholdDb,
                            onValueChange = {
                                viewModel.updateSoundParameters(
                                    selectedSound, selectedSound.gainDb, selectedSound.lowCutHz,
                                    selectedSound.noiseGateThresholdDb, selectedSound.compressorRatio, it
                                )
                            },
                            range = -40f..0f,
                            tag = "threshold"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = ConsoleBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "PRO AUDIO RESTORATION SUITE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ToxicGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Switches for Hum Removal & Hiss Suppression
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Hum Removal",
                                    tint = if (selectedSound.humRemovalActive) ToxicGreen else MutedSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("60Hz Hum Notch Removal", color = GhostWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Eliminates power line background hums", color = MutedSlate, fontSize = 10.sp)
                                }
                            }
                            Switch(
                                checked = selectedSound.humRemovalActive,
                                onCheckedChange = {
                                    viewModel.updateRestorationParameters(
                                        selectedSound, it, selectedSound.hissSuppressionActive,
                                        selectedSound.eqPresetName, selectedSound.eqLowGain, selectedSound.eqMidGain, selectedSound.eqHighGain,
                                        selectedSound.deEsserEnabled, selectedSound.deEsserThresholdDb, selectedSound.deEsserFrequencyHz
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ToxicGreen, checkedTrackColor = ToxicGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("hum_removal_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Hiss Suppression",
                                    tint = if (selectedSound.hissSuppressionActive) ToxicGreen else MutedSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("White Noise Hiss Suppressor", color = GhostWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Filters continuous capsule hiss", color = MutedSlate, fontSize = 10.sp)
                                }
                            }
                            Switch(
                                checked = selectedSound.hissSuppressionActive,
                                onCheckedChange = {
                                    viewModel.updateRestorationParameters(
                                        selectedSound, selectedSound.humRemovalActive, it,
                                        selectedSound.eqPresetName, selectedSound.eqLowGain, selectedSound.eqMidGain, selectedSound.eqHighGain,
                                        selectedSound.deEsserEnabled, selectedSound.deEsserThresholdDb, selectedSound.deEsserFrequencyHz
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ToxicGreen, checkedTrackColor = ToxicGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("hiss_suppress_switch")
                            )
                        }

                        // EQ PRESETS SELECTOR
                        Text(
                            text = "3-Band Equalizer (EQ Presets & Manual Carver)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedSlate,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Horizontal preset list
                        val eqPresets = listOf("Flat", "Vocal Clarity", "Bass Boost", "Hiss Cut", "Mid Punch", "Brass Bright")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            items(eqPresets) { preset ->
                                val isSelected = selectedSound.eqPresetName == preset
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) ToxicGreen else ConsoleBorder,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            viewModel.applyEqPreset(selectedSound, preset)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CharcoalBg else GhostWhite
                                    )
                                }
                            }
                        }

                        // EQ Low gain
                        DspSliderRow(
                            label = "EQ Low Band (100Hz)",
                            valueString = "${String.format("%.1f", selectedSound.eqLowGain)} dB",
                            value = selectedSound.eqLowGain,
                            onValueChange = {
                                viewModel.updateRestorationParameters(
                                    selectedSound, selectedSound.humRemovalActive, selectedSound.hissSuppressionActive,
                                    "Custom", it, selectedSound.eqMidGain, selectedSound.eqHighGain,
                                    selectedSound.deEsserEnabled, selectedSound.deEsserThresholdDb, selectedSound.deEsserFrequencyHz
                                )
                            },
                            range = -12f..12f,
                            tag = "eq_low"
                        )

                        // EQ Mid gain
                        DspSliderRow(
                            label = "EQ Mid Band (1.2kHz)",
                            valueString = "${String.format("%.1f", selectedSound.eqMidGain)} dB",
                            value = selectedSound.eqMidGain,
                            onValueChange = {
                                viewModel.updateRestorationParameters(
                                    selectedSound, selectedSound.humRemovalActive, selectedSound.hissSuppressionActive,
                                    "Custom", selectedSound.eqLowGain, it, selectedSound.eqHighGain,
                                    selectedSound.deEsserEnabled, selectedSound.deEsserThresholdDb, selectedSound.deEsserFrequencyHz
                                )
                            },
                            range = -12f..12f,
                            tag = "eq_mid"
                        )

                        // EQ High gain
                        DspSliderRow(
                            label = "EQ High Band (8kHz)",
                            valueString = "${String.format("%.1f", selectedSound.eqHighGain)} dB",
                            value = selectedSound.eqHighGain,
                            onValueChange = {
                                viewModel.updateRestorationParameters(
                                    selectedSound, selectedSound.humRemovalActive, selectedSound.hissSuppressionActive,
                                    "Custom", selectedSound.eqLowGain, selectedSound.eqMidGain, it,
                                    selectedSound.deEsserEnabled, selectedSound.deEsserThresholdDb, selectedSound.deEsserFrequencyHz
                                )
                            },
                            range = -12f..12f,
                            tag = "eq_high"
                        )

                        // DE-ESSER CONTROLS
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "De-esser",
                                    tint = if (selectedSound.deEsserEnabled) ToxicGreen else MutedSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Active Sibilance De-esser", color = GhostWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Compresses sibilant frequency spikes", color = MutedSlate, fontSize = 10.sp)
                                }
                            }
                            Switch(
                                checked = selectedSound.deEsserEnabled,
                                onCheckedChange = {
                                    viewModel.updateRestorationParameters(
                                        selectedSound, selectedSound.humRemovalActive, selectedSound.hissSuppressionActive,
                                        selectedSound.eqPresetName, selectedSound.eqLowGain, selectedSound.eqMidGain, selectedSound.eqHighGain,
                                        it, selectedSound.deEsserThresholdDb, selectedSound.deEsserFrequencyHz
                                    )
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ToxicGreen, checkedTrackColor = ToxicGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("deesser_switch")
                            )
                        }

                        if (selectedSound.deEsserEnabled) {
                            DspSliderRow(
                                label = "De-esser Threshold",
                                valueString = "${String.format("%.1f", selectedSound.deEsserThresholdDb)} dB",
                                value = selectedSound.deEsserThresholdDb,
                                onValueChange = {
                                    viewModel.updateRestorationParameters(
                                        selectedSound, selectedSound.humRemovalActive, selectedSound.hissSuppressionActive,
                                        selectedSound.eqPresetName, selectedSound.eqLowGain, selectedSound.eqMidGain, selectedSound.eqHighGain,
                                        selectedSound.deEsserEnabled, it, selectedSound.deEsserFrequencyHz
                                    )
                                },
                                range = -60f..0f,
                                tag = "deesser_thresh"
                            )

                            DspSliderRow(
                                label = "De-esser Center Freq",
                                valueString = "${selectedSound.deEsserFrequencyHz.toInt()} Hz",
                                value = selectedSound.deEsserFrequencyHz,
                                onValueChange = {
                                    viewModel.updateRestorationParameters(
                                        selectedSound, selectedSound.humRemovalActive, selectedSound.hissSuppressionActive,
                                        selectedSound.eqPresetName, selectedSound.eqLowGain, selectedSound.eqMidGain, selectedSound.eqHighGain,
                                        selectedSound.deEsserEnabled, selectedSound.deEsserThresholdDb, it
                                    )
                                },
                                range = 4000f..10000f,
                                tag = "deesser_freq"
                            )
                        }
                        
                        // IF AI-GEN REMAINED, DISPLAY NARRATION NOTES
                        if (!selectedSound.aiIdeaDescription.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
                                    .background(CharcoalBg)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = "AI Notes", tint = NeonAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI MODEL TRANSCRIPT & COLLAB MATRIX", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = selectedSound.aiIdeaDescription ?: "",
                                        color = GhostWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // MODULE C: SAVED WORKSPACE DRAWER
        item {
            Text(
                text = "SAVED TRANSIENTS & LOOPS (${savedSounds.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MutedSlate,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (savedSounds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Empty", tint = MutedSlate, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No sounds in session folder yet.", color = MutedSlate, fontSize = 13.sp)
                        Text("Tap RECORD above to capture or suggest.", color = MutedSlate, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(savedSounds, key = { it.id }) { sound ->
                val isActiveInRack = selectedSound?.id == sound.id
                val isSoundPlaying = isPlaying && viewModel.getPlayingSoundId() == sound.id
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActiveInRack) ConsoleBorder else ConsolePanel)
                        .clickable { viewModel.selectSound(sound) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayback(sound) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(ConsoleBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSoundPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = "Monitor",
                            tint = if (isActiveInRack) NeonAmber else GhostWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sound.title,
                            color = if (isActiveInRack) NeonAmber else GhostWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Duration: ${sound.durationMs / 1000f}s",
                                color = MutedSlate,
                                fontSize = 11.sp
                            )
                            if (sound.isUploadedToCommunity) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(ToxicGreen.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("LOCKED ${sound.priceGhs} GHS", color = ToxicGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (isActiveInRack) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonAmber)
                        )
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Configure", tint = MutedSlate, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DspSliderRow(
    label: String,
    valueString: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    tag: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = GhostWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(valueString, color = ToxicGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonAmber,
                activeTrackColor = NeonAmber,
                inactiveTrackColor = ConsoleBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dsp_slider_$tag")
        )
    }
}

@Composable
fun InputMonitorWaveform(
    isRecording: Boolean,
    recordProgressSec: Int,
    amplitudeHistory: List<Float>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CharcoalBg)
            .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val middleY = height / 2f

            // Draw clean DAW grid lines (e.g., -6dB, -12dB)
            val gridBrush = Brush.verticalGradient(listOf(ConsoleBorder, Color.Transparent, ConsoleBorder))
            drawRect(brush = gridBrush, alpha = 0.3f)

            // Draw grid center line
            drawLine(
                color = ConsoleBorder,
                start = Offset(0f, middleY),
                end = Offset(width, middleY),
                strokeWidth = 1.dp.toPx()
            )

            if (isRecording && amplitudeHistory.isNotEmpty()) {
                val path = Path()
                val stepX = width / 40f
                var currentX = 0f

                path.moveTo(0f, middleY)
                amplitudeHistory.forEachIndexed { index, amp ->
                    val finalAmpHeight = (amp * (height * 0.45f)).coerceAtLeast(4f)
                    val x = index * stepX
                    path.lineTo(x, middleY - finalAmpHeight)
                    currentX = x
                }
                
                // Mirror bottom part of the dynamic wave
                for (i in amplitudeHistory.indices.reversed()) {
                    val amp = amplitudeHistory[i]
                    val finalAmpHeight = (amp * (height * 0.45f)).coerceAtLeast(4f)
                    val x = i * stepX
                    path.lineTo(x, middleY + finalAmpHeight)
                }
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonAmber, ToxicGreen, NeonAmber)
                    )
                )
            } else if (!isRecording) {
                // Static artistic waveform representation
                val wavePath = Path()
                val points = 60
                val stepX = width / points.toFloat()
                wavePath.moveTo(0f, middleY)
                for (i in 0..points) {
                    val x = i * stepX
                    val sinVal = sin(i.toFloat() * 0.3f)
                    val envelope = sin(i.toFloat() / points.toFloat() * 3.1415f)
                    val yOffset = sinVal * envelope * (height * 0.35f)
                    wavePath.lineTo(x, middleY + yOffset)
                }
                drawPath(
                    path = wavePath,
                    color = ColdBlue,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Overlay status indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(if (isRecording) SignalLedRed else ConsolePanel, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isRecording) "REC: ${recordProgressSec}s" else "MIC READY (L/R)",
                    color = if (isRecording) Color.White else MutedSlate,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "SYSTEM INPUT • 44.1 KHZ • 24BIT",
                color = MutedSlate,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: GEMINI AI SUGGESTION ENGINE
// -------------------------------------------------------------

@Composable
fun AiSuggestTabScreen(
    viewModel: SoundViewModel,
    selectedSound: SoundRecording?
) {
    val aiLoading by viewModel.aiRecommendationLoading.collectAsStateWithLifecycle()
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    var promptInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ConsolePanel),
                border = BorderStroke(1.dp, ConsoleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "REIMAGINE SOUND WITH GEMINI API",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "Feed your recording description or a sound prompt to our sound-designer AI. It will analyze constraints and return recommended hardware DSP coefficients (Gain, Gates, Compression, LP/HP) and design recommendations suited for pro-collaboration.",
                        fontSize = 11.sp,
                        color = MutedSlate,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (selectedSound == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CharcoalBg, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "⚠️ Load/Select a recorded sound first from the 'STUDIO' tab to apply suggestions.",
                                color = NeonAmber,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = "Target Recording: ${selectedSound.title}",
                            fontSize = 12.sp,
                            color = GhostWhite,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = { Text("e.g. Isolate dynamic vocals block, remove Ghana road hum, and optimize for hard highlife beats.", fontSize = 12.sp, color = MutedSlate) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonAmber,
                                unfocusedBorderColor = ConsoleBorder,
                                focusedTextColor = GhostWhite,
                                unfocusedTextColor = GhostWhite
                            ),
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("ai_prompt_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (aiLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = NeonAmber)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Synthesizing DSP Params via Gemini...", color = NeonAmber, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.runAiDSPRecommendation(selectedSound, promptInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("analyze_sound_btn")
                            ) {
                                Text("LAUNCH GEMINI RE-CARVER", color = CharcoalBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (aiResult != null && !aiLoading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ConsolePanel),
                    border = BorderStroke(1.dp, ConsoleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "DAP Status", tint = ToxicGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GEMINI COEFFICIENTS RETURNED", color = ToxicGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("REIMAGINED TITLE:", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                        Text(aiResult!!.reimaginedTitle, fontSize = 14.sp, color = NeonAmber, fontWeight = FontWeight.ExtraBold)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Table display of computed soundboard levels
                        Text("MAPPED HARDWARE VALUES (AUTOMATICALLY LOADED):", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CharcoalBg, RoundedCornerShape(4.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CoefficientGridRow("Auto Input Gain", "${String.format("%.1f", aiResult!!.suggestedGainDb)} dB")
                            CoefficientGridRow("EQ Low-Cut cutoff", "${aiResult!!.suggestedLowCutHz} Hz")
                            CoefficientGridRow("Noise Expander Gate", "${String.format("%.1f", aiResult!!.noiseGateThresholdDb)} dB")
                            CoefficientGridRow("Ratio Coefficient", "${String.format("%.1f", aiResult!!.compressorRatio)}:1")
                            CoefficientGridRow("Compressor Threshold", "${String.format("%.1f", aiResult!!.compressorThresholdDb)} dB")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("DSP ANALYSIS EXPLANATION:", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                        Text(aiResult!!.explanation, fontSize = 12.sp, color = GhostWhite, lineHeight = 16.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        // CHANNELS
                        Text("AI SUGGESTED CREATIVE PROCESSING CHAIN:", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CharcoalBg, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = aiResult!!.creativeChain,
                                fontSize = 12.sp,
                                color = ToxicGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // LAYERING
                        Text("PRO SOUND LAYERING PROFILES & CODES:", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                        Text(aiResult!!.soundLayering, fontSize = 12.sp, color = GhostWhite, lineHeight = 16.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("PRO COLLABORATIVE STRATEGY:", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                        Text(aiResult!!.collaborationIdea, fontSize = 12.sp, color = ColdBlue, lineHeight = 16.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        // EQ CHIP ACTIVATION ROW
                        Button(
                            onClick = {
                                if (selectedSound != null) {
                                    viewModel.applyEqPreset(selectedSound, aiResult!!.suggestedEqPreset)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColdBlue),
                            modifier = Modifier.fillMaxWidth().testTag("apply_preset_ai_btn")
                        ) {
                            Text("APPLY RECOMMENDED EQ: ${aiResult!!.suggestedEqPreset}", color = CharcoalBg, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // SYNTH RUN CARD
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CharcoalBg),
                            border = BorderStroke(1.dp, ConsoleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Synthesis", tint = NeonAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SYNTHESIZE COMPANION SOUND LOOP", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Title: [GEN] ${aiResult!!.newComplementTitle}",
                                    fontSize = 12.sp,
                                    color = GhostWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = aiResult!!.newComplementDesc,
                                    fontSize = 11.sp,
                                    color = MutedSlate,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Button(
                                    onClick = {
                                        if (selectedSound != null) {
                                            viewModel.generateAiCompanionSound(selectedSound)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ToxicGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("synthesize_companion_btn")
                                ) {
                                    Text("ONE-CLICK GENERATE & MOUNT LAYER", color = CharcoalBg, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.selectTab("STUDIO")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ConsoleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RETURN TO RACK EDIT DECK", color = GhostWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoefficientGridRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MutedSlate, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = ToxicGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// -------------------------------------------------------------
// SCREEN 3: COLLABORATION FEED & MOMO PAYOUTS GHANA
// -------------------------------------------------------------

@Composable
fun CommunityTabScreen(
    viewModel: SoundViewModel,
    communitySounds: List<SoundRecording>,
    onTriggerPurchase: (SoundRecording) -> Unit,
    onShowLicensing: (SoundRecording) -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val miningQuotaUsed by viewModel.miningQuotaUsed.collectAsStateWithLifecycle()
    val minedRoyaltiesGhs by viewModel.minedRoyaltiesGhs.collectAsStateWithLifecycle()
    val jobs by viewModel.jobs.collectAsStateWithLifecycle()
    val selectedSound by viewModel.selectedSound.collectAsStateWithLifecycle()
    val activeSound = selectedSound

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MONETIZATION OVERVIE CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ConsolePanel),
                border = BorderStroke(1.dp, ConsoleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GHANA CASH & MOMO MONETIZATION DECK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SoundCraft connects sound designers natively to the Ghanaian creative economy. List your synthesized stems, customized audio loops, and transients. Receive direct checkout payments instantly using MTN Mobile Money, Telecel Cash, or AirtelTigo Money.",
                        fontSize = 11.sp,
                        color = MutedSlate
                    )
                }
            }
        }

        // MODULE D1: AI ROYALTY MINING REGISTRY
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ConsolePanel),
                border = BorderStroke(1.dp, ConsoleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI ROYALTY MINING CONSOLE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ToxicGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(ToxicGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "GHS ${String.format("%.2f", minedRoyaltiesGhs)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ToxicGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Submit clean microphone input recordings to our decentralized AI model dataset to train future sibilance suppressors. Get paid direct royalties per valid submission.",
                        fontSize = 11.sp,
                        color = MutedSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily quota progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Mining Quota Mark:",
                            fontSize = 11.sp,
                            color = GhostWhite
                        )
                        Text(
                            text = "$miningQuotaUsed / 5 Submissions",
                            fontSize = 11.sp,
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = miningQuotaUsed / 5f,
                        color = ToxicGreen,
                        trackColor = ConsoleBorder,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeSound == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CharcoalBg, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a Sound inside 'STUDIO' to mine royalties",
                                color = NeonAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.submitSoundToAiMining(activeSound)
                            },
                            enabled = miningQuotaUsed < 5,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ToxicGreen,
                                disabledContainerColor = ConsoleBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("mine_royalties_btn")
                        ) {
                            val btnLabel = if (miningQuotaUsed >= 5) "DAILY LIMIT REACHED" else "MINE SELECTED: ${activeSound.title}"
                            Text(btnLabel, color = CharcoalBg, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (miningQuotaUsed >= 5) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reset quota to test further or continue mining simulation.",
                            color = MutedSlate,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally).clickable { viewModel.resetMiningQuota() }
                        )
                    }
                }
            }
        }

        // MODULE D2: INTER-USER JOBS BOARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ConsolePanel),
                border = BorderStroke(1.dp, ConsoleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INTER-USER GHS WORKBOARD (COMMISSIONS TRIGGERED)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apply for contracts posted by top artists across Ghana. Platform takes a minor commission towards the big man's development team.",
                        fontSize = 11.sp,
                        color = MutedSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    jobs.forEach { job ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CharcoalBg),
                            border = BorderStroke(1.dp, ConsoleBorder),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(job.employer, fontSize = 10.sp, color = ColdBlue, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (job.status == "OPEN") NeonAmber.copy(alpha = 0.2f) else ToxicGreen.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = job.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (job.status == "OPEN") NeonAmber else ToxicGreen
                                        )
                                    }
                                }

                                Text(job.title, fontSize = 13.sp, color = GhostWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                Text(job.description, fontSize = 11.sp, color = MutedSlate)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = ConsoleBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Bounty: ${job.bountyGhs} GHS", fontSize = 11.sp, color = ToxicGreen, fontWeight = FontWeight.Bold)
                                        Text("Commission (10% to Big Man): ${(job.bountyGhs * job.commissionPercent / 100.0)} GHS", fontSize = 10.sp, color = MutedSlate)
                                    }

                                    if (job.status == "OPEN") {
                                        if (activeSound == null) {
                                            Text("Load Sound in Studio to apply", fontSize = 9.sp, color = MutedSlate)
                                        } else {
                                            Button(
                                                onClick = {
                                                    viewModel.applyToJob(job.id, activeSound)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ToxicGreen),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("Apply with selected", fontSize = 10.sp, color = CharcoalBg, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Text("Payment Cleared (Momo Released)", fontSize = 10.sp, color = ToxicGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "GHANA COLLAB DECK / ACTIVE LISTINGS (${communitySounds.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MutedSlate,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (communitySounds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No listings on the board. Pack your sound and upload it!", color = MutedSlate, fontSize = 12.sp)
                }
            }
        } else {
            items(communitySounds, key = { it.id }) { track ->
                val isTrackPlaying = isPlaying && viewModel.getPlayingSoundId() == track.id
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = ConsolePanel),
                    border = BorderStroke(1.dp, ConsoleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePlayback(track) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ConsoleBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isTrackPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                    contentDescription = "Monitor Track",
                                    tint = if (isTrackPlaying) NeonAmber else GhostWhite
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = GhostWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "By ${track.creatorEmail ?: "accra.stem@soundcraft.gh"}",
                                    color = MutedSlate,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(NeonAmber.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${track.priceGhs} GHS",
                                    color = NeonAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (!track.aiPrompt.isNullOrBlank() || !track.aiIdeaDescription.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "DSP carving: ${track.lowCutHz}Hz EQ, ${track.compressorRatio}:1 compressor settings applied.",
                                fontSize = 11.sp,
                                color = ToxicGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = ConsoleBorder, modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "License: ${track.licenseType}",
                                color = ColdBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onShowLicensing(track) }
                                    .testTag("show_licensing_${track.id}")
                            )

                            Button(
                                onClick = { onTriggerPurchase(track) },
                                colors = ButtonDefaults.buttonColors(containerColor = ToxicGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("buy_btn_${track.id}")
                            ) {
                                Text(
                                    text = "BUY (MOMO)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CharcoalBg
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUBS: DIALOG PANELS (PROFILES, MOMOS, LICENSES)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupDialog(
    currentEmail: String,
    currentMomo: String,
    currentCarrier: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var email by remember { mutableStateOf(currentEmail) }
    var momo by remember { mutableStateOf(currentMomo) }
    var carrier by remember { mutableStateOf(currentCarrier) }
    
    val carriers = listOf("MTN MoMo", "Telecel Cash", "AirtelTigo Money")
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ConsolePanel),
            border = BorderStroke(1.dp, ConsoleBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("payout_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "GHANA PAYOUT PROFILE SETUP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Configure your credentials to receive direct licensing payouts on your mobile database wallet instantly.",
                    fontSize = 11.sp,
                    color = MutedSlate
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Producer Email Address", color = MutedSlate) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonAmber,
                        unfocusedBorderColor = ConsoleBorder,
                        focusedTextColor = GhostWhite,
                        unfocusedTextColor = GhostWhite
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_email_input")
                )

                OutlinedTextField(
                    value = momo,
                    onValueChange = { momo = it },
                    label = { Text("Ghana MoMo Number (e.g. 0244...)", color = MutedSlate) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonAmber,
                        unfocusedBorderColor = ConsoleBorder,
                        focusedTextColor = GhostWhite,
                        unfocusedTextColor = GhostWhite
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_momo_input")
                )

                // Simulating drop down
                Column {
                    Text("MoMo Payout Carrier", color = MutedSlate, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalBg, RoundedCornerShape(4.dp))
                            .border(1.dp, ConsoleBorder, RoundedCornerShape(4.dp))
                            .clickable { expandedDropdown = !expandedDropdown }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(carrier, color = GhostWhite, fontSize = 12.sp)
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Dropdown", tint = NeonAmber, modifier = Modifier.size(12.dp))
                        }
                    }

                    if (expandedDropdown) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ConsoleBorder)
                                .padding(vertical = 4.dp)
                        ) {
                            carriers.forEach { item ->
                                Text(
                                    text = item,
                                    color = if (carrier == item) NeonAmber else GhostWhite,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            carrier = item
                                            expandedDropdown = false
                                        }
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = MutedSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(email, momo, carrier) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                        modifier = Modifier.testTag("save_profile_btn")
                    ) {
                        Text("SAVE PROFILE", color = CharcoalBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PublishSoundDialog(
    sound: SoundRecording,
    onDismiss: () -> Unit,
    onPublish: (Double, String) -> Unit
) {
    var priceInput by remember { mutableStateOf("15") }
    var selectedLicense by remember { mutableStateOf("Royalty-Free Beats") }
    
    val licenseOptions = listOf(
        "Royalty-Free Beats",
        "Commercial Exclusive",
        "Standard License",
        "Creative-Commons Attribution"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ConsolePanel),
            border = BorderStroke(1.dp, ConsoleBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("publish_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "LIST TRANSIENT FOR SALE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Establish licensing price and parameters before publishing to the collaboration server.",
                    fontSize = 11.sp,
                    color = MutedSlate
                )

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Sound Licensing Price (GHS Cedis)", color = MutedSlate) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonAmber,
                        unfocusedBorderColor = ConsoleBorder,
                        focusedTextColor = GhostWhite,
                        unfocusedTextColor = GhostWhite
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_price_input")
                )

                Text("License Level Agreement", color = MutedSlate, fontSize = 11.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalBg, RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    licenseOptions.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selectedLicense == opt) ConsoleBorder else Color.Transparent)
                                .clickable { selectedLicense = opt }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLicense == opt,
                                onClick = { selectedLicense = opt },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(opt, color = GhostWhite, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = MutedSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val computedPrice = priceInput.toDoubleOrNull() ?: 0.0
                            onPublish(computedPrice, selectedLicense)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToxicGreen),
                        modifier = Modifier.testTag("confirm_publish_btn")
                    ) {
                        Text("PUBLISH LOOP", color = CharcoalBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MoMoCheckoutDialog(
    sound: SoundRecording,
    status: String?,
    onDismiss: () -> Unit,
    onConfirmPayment: (String, String) -> Unit
) {
    var checkNumber by remember { mutableStateOf("") }
    var carrierSelect by remember { mutableStateOf("MTN MoMo") }

    Dialog(onDismissRequest = { if (status == null) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ConsolePanel),
            border = BorderStroke(1.dp, ConsoleBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("momo_checkout_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (status == null) {
                    Text(
                        text = "GHANA MOBILE MONEY SEAMLESS CHECKOUT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track: ${sound.title}\nPrice: ${sound.priceGhs} GHS\nPayout Vendor: ${sound.momoCarrier ?: "N/A"}",
                        fontSize = 12.sp,
                        color = GhostWhite,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalBg, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )

                    Text(
                        text = "Enter your MoMo details. A simulated secure network authorization response will dispatch loop content to your workspace.",
                        fontSize = 11.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = checkNumber,
                        onValueChange = { checkNumber = it },
                        placeholder = { Text("e.g. 055729118c", color = MutedSlate) },
                        label = { Text("Your MoMo Mobile Number", color = MutedSlate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = ConsoleBorder,
                            focusedTextColor = GhostWhite,
                            unfocusedTextColor = GhostWhite
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_momo_number")
                    )

                    // Carrier picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("MTN MoMo", "Telecel", "AirtelTigo").forEach { opt ->
                            val isChosen = carrierSelect.contains(opt, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isChosen) NeonAmber else ConsoleBorder)
                                    .clickable { carrierSelect = opt }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(opt, color = if (isChosen) CharcoalBg else GhostWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("ABORT", color = MutedSlate)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirmPayment(checkNumber, carrierSelect)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ToxicGreen),
                            modifier = Modifier.testTag("submit_checkout_btn")
                        ) {
                            Text("PAY GHS ${sound.priceGhs}", color = CharcoalBg, fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    // PROCESS STAGES
                    when (status) {
                        "PENDING" -> {
                            CircularProgressIndicator(color = NeonAmber, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "USSD SECURE AUTHORIZATION PROMPT SENT...",
                                color = NeonAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Verify payment validation envelope in the background network terminal...",
                                color = MutedSlate,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        "SUCCESS" -> {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Paid", tint = ToxicGreen, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "MOMO PAYOUT COMPLETED SUCCESS",
                                color = ToxicGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "License code registered! Transient stem imported to your local 'Workspace' folder successfully.",
                                color = GhostWhite,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        "ERROR" -> {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Fail", tint = SignalLedRed, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "TRANSACTION REFUSED / TIMEOUT",
                                color = SignalLedRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LicenseDetailsDialog(
    sound: SoundRecording,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ConsolePanel),
            border = BorderStroke(1.dp, ConsoleBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "LICENSING LAWS DEPLOY: ${sound.title}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Standard legal parameters associated with the level configured for this stem.",
                    fontSize = 11.sp,
                    color = MutedSlate
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalBg, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Class: ${sound.licenseType}",
                            color = ToxicGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        val details = when (sound.licenseType) {
                            "Royalty-Free Beats" -> "The sound files can be incorporated into commercial beats, Spotify streams, or sound compositions without royalties or profit cuts owed to the initial designer. Clear to list globally."
                            "Commercial Exclusive" -> "Unlocks complete administrative buy-out. Absolute ownership turns over to the buyer. The seller deletes the project files upon payout notification confirmation."
                            "Standard License" -> "Standard non-exclusive synchronized sound mapping. Fits games, YouTube, or podcasts with design attribution."
                            else -> "Creative Commons BY-ND representation. Free to redistribute only when attributing full sound credits to the original Ghanaian sound engineer's profile email."
                        }

                        Text(
                            text = details,
                            color = GhostWhite,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ConsoleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I UNDERSTAND", color = GhostWhite)
                }
            }
        }
    }
}
