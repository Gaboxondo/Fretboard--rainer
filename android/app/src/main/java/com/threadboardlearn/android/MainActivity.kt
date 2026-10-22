package com.threadboardlearn.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.threadboardlearn.android.audio.AndroidAudioPlayer
import com.threadboardlearn.android.audio.AndroidAudioRecordCapture
import com.threadboardlearn.android.ui.FretboardComposable
import com.threadboardlearn.dsp.PitchDetectionResult
import com.threadboardlearn.engine.Challenge
import com.threadboardlearn.engine.GameEventListener
import com.threadboardlearn.engine.TrainingSession
import com.threadboardlearn.engine.TrainingStats
import com.threadboardlearn.model.GuitarString
import com.threadboardlearn.model.TrainingConfig

class MainActivity : ComponentActivity() {

    private lateinit var audioPlayer: AndroidAudioPlayer
    private lateinit var micCapture: AndroidAudioRecordCapture
    private lateinit var session: TrainingSession
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = TrainingConfig().apply {
            isMicrophoneEnabled = false // Desactivado por defecto hasta pedir permisos
            isAdaptiveLearning = true
            setAllStrings(true)
            timeLimitSeconds = 5 // 5 segundos de cuenta atrás por defecto
        }

        audioPlayer = AndroidAudioPlayer()
        micCapture = AndroidAudioRecordCapture()

        session = TrainingSession(
            config,
            audioPlayer,
            micCapture
        ) { runnable -> mainHandler.post(runnable) }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFFD54F),
                    secondary = Color(0xFF64B5F6),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    onSurface = Color(0xFFE0E0E0)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrainerAppScreen(
                        session = session,
                        config = config
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session.shutdown()
    }
}

@Composable
fun TrainerAppScreen(
    session: TrainingSession,
    config: TrainingConfig
) {
    var isRunning by remember { mutableStateOf(false) }
    var currentChallenge by remember { mutableStateOf<Challenge?>(null) }
    var stats by remember { mutableStateOf<TrainingStats>(session.stats) }
    var lastPitch by remember { mutableStateOf<PitchDetectionResult?>(null) }
    var lastClick by remember { mutableStateOf<Pair<GuitarString, Int>?>(null) }
    var isLastClickCorrect by remember { mutableStateOf<Boolean?>(null) }
    var micEnabled by remember { mutableStateOf(config.isMicrophoneEnabled) }
    var adaptiveEnabled by remember { mutableStateOf(config.isAdaptiveLearning) }
    var allStrings by remember { mutableStateOf(config.isAllStrings) }

    // Estados de cuenta atrás y temporizador
    var timeLimitSeconds by remember { mutableStateOf(config.timeLimitSeconds) }
    var secondsRemaining by remember { mutableStateOf(config.timeLimitSeconds) }
    var percentElapsed by remember { mutableStateOf(0.0) }
    var isTimedOut by remember { mutableStateOf(false) }
    var lastReactionTimeMs by remember { mutableStateOf<Long?>(null) }
    var isTimerMenuExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            config.isMicrophoneEnabled = true
            micEnabled = true
            if (isRunning) {
                session.stop()
                session.start()
            }
        } else {
            config.isMicrophoneEnabled = false
            micEnabled = false
        }
    }

    // Configurar listener de la sesión
    DisposableEffect(session) {
        session.setListener(object : GameEventListener {
            override fun onChallengeStarted(challenge: Challenge) {
                currentChallenge = challenge
                lastClick = null
                isLastClickCorrect = null
                isTimedOut = false
                lastReactionTimeMs = null
                secondsRemaining = config.timeLimitSeconds
                percentElapsed = 0.0
            }

            override fun onTimerTick(secRemaining: Int, pctElapsed: Double) {
                secondsRemaining = secRemaining
                percentElapsed = pctElapsed
            }

            override fun onChallengeSuccess(challenge: Challenge, reactionTimeMs: Long) {
                isLastClickCorrect = true
                isTimedOut = false
                lastReactionTimeMs = reactionTimeMs
                percentElapsed = 0.0
            }

            override fun onChallengeTimeout(challenge: Challenge) {
                isLastClickCorrect = false
                isTimedOut = true
                secondsRemaining = 0
                percentElapsed = 1.0
            }

            override fun onPitchActivity(pitchResult: PitchDetectionResult) {
                lastPitch = pitchResult
            }

            override fun onStatsUpdated(updatedStats: TrainingStats) {
                stats = updatedStats
            }

            override fun onSessionStateChanged(running: Boolean) {
                isRunning = running
                if (!running) {
                    currentChallenge = null
                    isTimedOut = false
                    lastClick = null
                    isLastClickCorrect = null
                    percentElapsed = 0.0
                }
            }
        })

        onDispose {
            session.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cabecera con Logotipo Oficial de la Aplicación
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Fretboard Trainer Logo",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column {
                    Text(
                        text = "Fretboard Trainer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Entrena notas y agilidad",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }

            AssistChip(
                onClick = {},
                label = { Text(if (isRunning) "En curso" else "Pausado") },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (isRunning) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                )
            )
        }

        // 2. Tarjeta del Reto Actual con Cuenta Atrás
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val challenge = currentChallenge
                if (challenge != null && isRunning) {
                    Text(
                        text = "Toca la nota:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = challenge.targetNote().name,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "En: ${challenge.targetString().label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64B5F6)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Feedback de Estado / Mensaje de Temporizador
                    if (isTimedOut) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33F44336)
                        ) {
                            Text(
                                text = "⏱️ ¡Tiempo agotado! Traste(s): ${challenge.validFrets()}",
                                color = Color(0xFFEF5350),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 14.sp
                            )
                        }
                    } else if (lastReactionTimeMs != null && isLastClickCorrect == true) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x334CAF50)
                        ) {
                            val sec = String.format(java.util.Locale.US, "%.2f", lastReactionTimeMs!! / 1000.0)
                            Text(
                                text = "🎉 ¡Correcto! ($sec s)",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 14.sp
                            )
                        }
                    } else if (timeLimitSeconds > 0) {
                        val timerColor = when {
                            percentElapsed < 0.55 -> Color(0xFF4CAF50)
                            percentElapsed < 0.80 -> Color(0xFFFFA726)
                            else -> Color(0xFFEF5350)
                        }
                        Text(
                            text = "⏱️ ${secondsRemaining}s restantes",
                            color = timerColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = "⏱️ Modo libre (Sin límite)",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }

                    // Barra Visual de Cuenta Atrás (Progreso animado)
                    if (timeLimitSeconds > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val progressFraction = (1.0 - percentElapsed).toFloat().coerceIn(0f, 1f)
                        val barColor = when {
                            isTimedOut -> Color(0xFFEF5350)
                            isLastClickCorrect == true -> Color(0xFF4CAF50)
                            percentElapsed < 0.55 -> Color(0xFF4CAF50)
                            percentElapsed < 0.80 -> Color(0xFFFFA726)
                            else -> Color(0xFFEF5350)
                        }

                        LinearProgressIndicator(
                            progress = { if (isTimedOut) 0f else if (isLastClickCorrect == true) 1f else progressFraction },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(8.dp)
                                .clip(CircleShape),
                            color = barColor,
                            trackColor = Color(0xFF2C2C2C)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    IconButton(
                        onClick = { session.replayChallengeAudio() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Escuchar nota de referencia",
                            tint = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "Presiona 'Empezar' para entrenar",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        // 3. Diapasón Interactivo Táctil (Muestra solución en caso de timeout)
        FretboardComposable(
            maxFret = config.maxFret,
            targetString = currentChallenge?.targetString(),
            lastClickedFret = lastClick,
            isLastClickCorrect = isLastClickCorrect,
            solutionFrets = if (isTimedOut) currentChallenge?.validFrets() else null,
            onFretClicked = { string: GuitarString, fret: Int ->
                if (isRunning && !isTimedOut) {
                    lastClick = Pair(string, fret)
                    val correct = session.submitFretClick(string, fret)
                    isLastClickCorrect = correct
                }
            }
        )

        // 4. VU Meter & Afinador Micrófono
        AnimatedVisibility(visible = micEnabled && isRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Micrófono",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val noteText = lastPitch?.note()?.name ?: "--"
                        val freqText = if (lastPitch != null && lastPitch!!.frequencyHz() > 0) {
                            String.format("%.1f Hz", lastPitch!!.frequencyHz())
                        } else "--"
                        Text(text = "Tono: $noteText ($freqText)", fontSize = 14.sp)
                    }

                    // Indicador de RMS / Nivel
                    val rms = (lastPitch?.rms() ?: 0.0).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { rms },
                        modifier = Modifier
                            .width(100.dp)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFF333333)
                    )
                }
            }
        }

        // 5. Estadísticas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "Aciertos", value = "${stats.correctCount}")
            StatItem(label = "Racha", value = "${stats.currentStreak} (Max: ${stats.bestStreak})")
            val accuracy = if (stats.totalRounds > 0) {
                (stats.correctCount * 100.0 / stats.totalRounds).toInt()
            } else 0
            StatItem(label = "Precisión", value = "$accuracy%")
        }

        // 6. Controles y Opciones (Fila 1: Filtros de práctica)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = micEnabled,
                onClick = {
                    if (!micEnabled) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        config.isMicrophoneEnabled = false
                        micEnabled = false
                    }
                },
                label = { Text("Micrófono") },
                leadingIcon = {
                    Icon(
                        imageVector = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null
                    )
                }
            )

            FilterChip(
                selected = adaptiveEnabled,
                onClick = {
                    adaptiveEnabled = !adaptiveEnabled
                    config.isAdaptiveLearning = adaptiveEnabled
                },
                label = { Text("Adaptativo") }
            )

            FilterChip(
                selected = allStrings,
                onClick = {
                    val newVal = !allStrings
                    allStrings = newVal
                    config.setAllStrings(newVal)
                },
                label = { Text("6 Cuerdas") }
            )
        }

        // 6.1 Selector de Temporizador / Cuenta Atrás
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box {
                FilterChip(
                    selected = timeLimitSeconds > 0,
                    onClick = { isTimerMenuExpanded = true },
                    label = {
                        Text(if (timeLimitSeconds > 0) "⏱️ Cuenta atrás: ${timeLimitSeconds}s" else "⏱️ Sin límite")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Cambiar cuenta atrás"
                        )
                    }
                )

                DropdownMenu(
                    expanded = isTimerMenuExpanded,
                    onDismissRequest = { isTimerMenuExpanded = false }
                ) {
                    val timerOptions = listOf(
                        3 to "3 segundos (Rápido)",
                        5 to "5 segundos (Estándar)",
                        8 to "8 segundos (Intermedio)",
                        10 to "10 segundos (Tranquilo)",
                        0 to "Sin límite de tiempo"
                    )

                    timerOptions.forEach { (seconds, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            leadingIcon = if (timeLimitSeconds == seconds) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            onClick = {
                                config.timeLimitSeconds = seconds
                                timeLimitSeconds = seconds
                                secondsRemaining = seconds
                                percentElapsed = 0.0
                                isTimerMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 7. Botón Principal Empezar / Parar
        Button(
            onClick = {
                if (isRunning) {
                    session.stop()
                } else {
                    session.start()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                contentColor = if (isRunning) Color.White else Color.Black
            )
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "Detener Sesión" else "Empezar Entrenamiento",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
