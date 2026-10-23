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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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

    // Selección de Cuerda (null = Todas las cuerdas, o STRING_1 a STRING_6)
    var selectedString by remember { mutableStateOf<GuitarString?>(config.selectedString) }
    var isStringMenuExpanded by remember { mutableStateOf(false) }

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
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Cabecera Horizontal: Logo, Reto con Cuenta Atrás y Botón Empezar/Parar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. Logotipo e Identidad
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Column {
                    Text(
                        text = "Fretboard Trainer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isRunning) "● Sesión activa" else "○ Pausado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRunning) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // B. Tarjeta Central del Reto & Cuenta Atrás
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val challenge = currentChallenge
                if (challenge != null && isRunning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Nota objetivo y Cuerda
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = challenge.targetNote().name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = challenge.targetString().label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64B5F6)
                                )
                                // Mensaje de feedback o tiempo
                                if (isTimedOut) {
                                    Text(
                                        text = "⏱️ ¡Tiempo agotado! Traste(s): ${challenge.validFrets()}",
                                        color = Color(0xFFEF5350),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                } else if (lastReactionTimeMs != null && isLastClickCorrect == true) {
                                    val sec = String.format(java.util.Locale.US, "%.2f", lastReactionTimeMs!! / 1000.0)
                                    Text(
                                        text = "🎉 ¡Correcto! ($sec s)",
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
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
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Text(
                                        text = "⏱️ Sin límite de tiempo",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Barra de progreso y botón de audio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (timeLimitSeconds > 0) {
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
                                        .width(90.dp)
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = barColor,
                                    trackColor = Color(0xFF2C2C2C)
                                )
                            }

                            IconButton(
                                onClick = { session.replayChallengeAudio() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Escuchar nota",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Presiona 'Empezar' para entrenar el diapasón",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            // C. Botón Principal Empezar / Detener
            Button(
                onClick = {
                    if (isRunning) {
                        session.stop()
                    } else {
                        session.start()
                    }
                },
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                    contentColor = if (isRunning) Color.White else Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "Detener" else "Empezar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // 2. Barra de Control Horizontal: Cuerda, Temporizador, Micrófono y Estadísticas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grupo de Controles Izquierdo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Selector de Cuerda (Todas o específica 1ª a 6ª)
                Box {
                    FilterChip(
                        selected = selectedString != null,
                        onClick = { isStringMenuExpanded = true },
                        label = {
                            val strLabel = when (selectedString) {
                                null -> "🎸 Todas las cuerdas"
                                GuitarString.STRING_1 -> "🎸 1ª (Mi)"
                                GuitarString.STRING_2 -> "🎸 2ª (Si)"
                                GuitarString.STRING_3 -> "🎸 3ª (Sol)"
                                GuitarString.STRING_4 -> "🎸 4ª (Re)"
                                GuitarString.STRING_5 -> "🎸 5ª (La)"
                                GuitarString.STRING_6 -> "🎸 6ª (Mi)"
                            }
                            Text(strLabel, fontSize = 12.sp)
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = isStringMenuExpanded,
                        onDismissRequest = { isStringMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🎸 Todas las cuerdas (Aleatorio)") },
                            leadingIcon = if (selectedString == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                config.selectedString = null
                                selectedString = null
                                isStringMenuExpanded = false
                                if (isRunning) session.nextChallenge()
                            }
                        )
                        HorizontalDivider()
                        val stringOptions = listOf(
                            GuitarString.STRING_1 to "1ª Cuerda - Mi (E4 / Aguda)",
                            GuitarString.STRING_2 to "2ª Cuerda - Si (B3)",
                            GuitarString.STRING_3 to "3ª Cuerda - Sol (G3)",
                            GuitarString.STRING_4 to "4ª Cuerda - Re (D3)",
                            GuitarString.STRING_5 to "5ª Cuerda - La (A2)",
                            GuitarString.STRING_6 to "6ª Cuerda - Mi (E2 / Grave)"
                        )
                        stringOptions.forEach { (gStr, text) ->
                            DropdownMenuItem(
                                text = { Text(text) },
                                leadingIcon = if (selectedString == gStr) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    config.selectedString = gStr
                                    selectedString = gStr
                                    isStringMenuExpanded = false
                                    if (isRunning) session.nextChallenge()
                                }
                            )
                        }
                    }
                }

                // Selector de Cuenta Atrás
                Box {
                    FilterChip(
                        selected = timeLimitSeconds > 0,
                        onClick = { isTimerMenuExpanded = true },
                        label = {
                            Text(
                                if (timeLimitSeconds > 0) "⏱️ ${timeLimitSeconds}s" else "⏱️ Sin límite",
                                fontSize = 12.sp
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
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
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
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

                // Micrófono
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
                    label = { Text("Micro", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Adaptativo
                FilterChip(
                    selected = adaptiveEnabled,
                    onClick = {
                        adaptiveEnabled = !adaptiveEnabled
                        config.isAdaptiveLearning = adaptiveEnabled
                    },
                    label = { Text("Adaptativo", fontSize = 12.sp) }
                )
            }

            // Estadísticas en chips compactos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Afinador mini si micrófono está activo
                if (micEnabled && isRunning && lastPitch != null && lastPitch!!.frequencyHz() > 0) {
                    val noteText = lastPitch?.note()?.name ?: "--"
                    val freqText = String.format("%.1f Hz", lastPitch!!.frequencyHz())
                    AssistChip(
                        onClick = {},
                        label = { Text("🎵 $noteText ($freqText)", fontSize = 11.sp, color = Color(0xFF4CAF50)) }
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text("Aciertos: ${stats.correctCount}", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Racha: ${stats.currentStreak}", fontSize = 11.sp) }
                )
                val accuracy = if (stats.totalRounds > 0) {
                    (stats.correctCount * 100.0 / stats.totalRounds).toInt()
                } else 0
                AssistChip(
                    onClick = {},
                    label = { Text("Precisión: $accuracy%", fontSize = 11.sp) }
                )
            }
        }

        // 3. Diapasón Interactivo Táctil Horizontal
        FretboardComposable(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
    }
}
