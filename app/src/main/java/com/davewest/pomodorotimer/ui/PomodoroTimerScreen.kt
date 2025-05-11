package com.davewest.pomodorotimer.ui

import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import android.Manifest
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.davewest.pomodorotimer.model.Phase
import com.davewest.pomodorotimer.R
import com.davewest.pomodorotimer.ui.theme.DarkColorScheme
import com.davewest.pomodorotimer.ui.theme.LightColorScheme
import com.davewest.pomodorotimer.ui.theme.Typography
import com.davewest.pomodorotimer.viewmodel.PomodoroViewModel
import org.koin.androidx.compose.koinViewModel

// Define Circle Colors
val workColor = Color(0xFF4CAF50)
val breakColor = Color(0xFFF44336)

/**
 * Entry composable wiring UI to ViewModel state.
 */
@Composable
fun PomodoroTimerScreen(
    onNavigateToSettings: () -> Unit = {},
    // Get ViewModel from Koin
    viewModel: PomodoroViewModel = koinViewModel()
) {
    val context = LocalContext.current

    // Find whether we have permission for POST_NOTIFICATIONS
    val hasNotificationPermission = remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 2) Permission‐request launcher (Android 13+)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission.value = granted
    }

    // 3) On first composition, if running on 33+ and not granted, ask
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && !hasNotificationPermission.value
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // LaunchEffect to run whenever 'finishEvents' changes
    LaunchedEffect(Unit) {
        viewModel.finishEvents.collect { newPhase ->
            // Don't do anything if we don't have permission
            if (!hasNotificationPermission.value) return@collect

            // 1) Build & send a notification
            val notification = NotificationCompat.Builder(context, "pomodoro_channel")
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(
                    when (newPhase) {
                        Phase.WORK        -> "Ready to Work!"
                        Phase.SHORT_BREAK -> "Short Break Time!"
                        Phase.LONG_BREAK  -> "Long Break Time!"
                    }
                )
                .setContentText("Switched to ${newPhase.name.lowercase().replace('_',' ')}")
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build()

            NotificationManagerCompat.from(context)
                .notify(/* id = */ 1001, notification)

            // 2) Fire a vibration
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ via VibratorManager
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                // pre-31 fallback
                context.getSystemService(Vibrator::class.java)
            }

            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // API 26+ precise control
                    val effect = VibrationEffect.createOneShot(
                        500L,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                    v.vibrate(effect)
                } else {
                    // pre-26 deprecated overload
                    @Suppress("DEPRECATION")
                    v.vibrate(500L)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        PreTimerUI(viewModel)
        Spacer(modifier = Modifier.height(64.dp))

        PhaseCircleWithTimer(viewModel)

        Spacer(modifier = Modifier.height(80.dp))
        PostTimerUI(viewModel)
    }
}

@Composable
private fun PreTimerUI(
    viewModel: PomodoroViewModel
) {
    val phase by viewModel.phase.collectAsState()
    val cycleCount by viewModel.cycleCount.collectAsState()
    val cyclesBeforeLongBreak = viewModel.cyclesBeforeLongBreak

    val phaseText = remember(phase) {
        when (phase) {
            Phase.WORK        -> "Work"
            Phase.SHORT_BREAK -> "Short Break"
            Phase.LONG_BREAK  -> "Long Break"
        }
    }
    Text(
        text = phaseText,
        fontSize = 56.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
    val cyclesText = remember(phase, cycleCount, cyclesBeforeLongBreak) {
        if (phase == Phase.WORK) {
            "${cyclesBeforeLongBreak - cycleCount} cycle${if (cyclesBeforeLongBreak - cycleCount != 1) "s" else ""} until a long break"
        } else {
            " "
        }
    }
    Text(
        text = cyclesText,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PhaseCircleWithTimer(
    viewModel: PomodoroViewModel,
    modifier: Modifier = Modifier,
    outerSize: Dp = 300.dp,
    ringThickness: Dp = 24.dp,
    ringGap: Dp = 12.dp
) {
    val phase by viewModel.phase.collectAsState()
    val circleColor = remember(phase) {
        if (phase == Phase.WORK) workColor else breakColor
    }

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // Progress ring
        ProgressRing(
            viewModel = viewModel,
            size      = outerSize,
            color     = circleColor,
            thickness = ringThickness
        )

        // Inner filled circle inset by gap + thickness
        val inset = ringThickness + ringGap
        Box(
            modifier = Modifier
                .size(outerSize - inset * 2f)
                .background(color = circleColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            TimerDisplay(viewModel)
        }
    }
}

@Composable
fun ProgressRing(
    viewModel: PomodoroViewModel,
    size: Dp,
    color: Color,
    thickness: Dp,
    startAngleOffset: Float = -90f,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.progress.collectAsState()

    Canvas(modifier = modifier.size(size)) {
        val strokePx   = thickness.toPx()
        val halfStroke = strokePx / 2f
        val radius     = this.size.minDimension / 2f - halfStroke
        val topLeft    = Offset(center.x - radius, center.y - radius)
        val arcSize    = Size(radius * 2f, radius * 2f)

        drawArc(
            color      = Color.LightGray,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter  = false,
            style      = Stroke(width = strokePx),
            topLeft    = topLeft,
            size       = arcSize
        )

        drawArc(
            color = color,
            startAngle = startAngleOffset,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokePx),
            topLeft = topLeft,
            size = arcSize
        )
    }
}


@Composable
private fun TimerDisplay(
    viewModel: PomodoroViewModel
) {
    val timeRemainingSec by viewModel.secondsLeft.collectAsState()
    // Format mm:ss
    val minutes = timeRemainingSec / 60
    val seconds = timeRemainingSec % 60

    Text(
        text = "%02d:%02d".format(minutes,seconds),
        style = MaterialTheme.typography.headlineLarge,
        color = Color.Black,
        fontSize = 56.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PostTimerUI(
    viewModel: PomodoroViewModel
) {
    val isRunning by viewModel.isRunning.collectAsState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val buttonText = remember(isRunning) {
            if (isRunning) "Pause" else "Start"
        }
        Button(
            onClick = { viewModel.startPause() },
            modifier = Modifier
                .defaultMinSize(minWidth = 120.dp, minHeight = 56.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 20.sp
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Button(
            onClick = { viewModel.reset() },
            modifier = Modifier
                .defaultMinSize(minWidth = 120.dp, minHeight = 56.dp)
        ) {
            Text(
                text = "Reset",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun PomodoroTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

@Preview(
    name = "Light Mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun PomodoroTimerPreview() {
    PomodoroTimerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            PomodoroTimerScreen()
        }
    }
}
