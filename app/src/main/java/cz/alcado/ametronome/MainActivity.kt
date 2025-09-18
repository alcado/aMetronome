@file:OptIn(ExperimentalTextApi::class)

package cz.alcado.ametronome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.alcado.ametronome.ui.theme.AMetronomeTheme

// BPM range
const val MIN_BPM = 40
const val MAX_BPM = 240


// Function for creating a variable Oswald font with custom weight
fun oswaldFont(weight: Int) = FontFamily(
    Font(
        R.font.oswald_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight)
        )
    )
)

class MainActivity : ComponentActivity() {
    // Creating a ViewModel instance
    private val viewModel: MetronomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AMetronomeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Getting the current state from ViewModel
                    val uiState by viewModel.uiState.collectAsState()
                    MetronomeScreen(
                        uiState = uiState,
                        onBpmChange = { newBpm -> viewModel.setBpm(newBpm) },
                        onStartStopClick = { viewModel.startStop() }
                    )
                }
            }
        }
    }
}

@Composable
fun MetronomeVisualizer(
    isPlaying: Boolean,
    bpm: Int,
    modifier: Modifier = Modifier
) {
    val dotColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    // Status for target position - varies between 0f and 1f
    var targetPosition by remember { mutableFloatStateOf(0f) }

    // Animation of position with current BPM
    val dotPosition by animateFloatAsState(
        targetValue = targetPosition,
        animationSpec = tween(
            durationMillis = (60_000 / bpm), // Full cycle
            easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
        ),
        finishedListener = {
            // Change direction after animation completion
            if (isPlaying) {
                targetPosition = if (targetPosition == 0f) 1f else 0f
            }
        },
        label = "dot_position"
    )

    // LaunchedEffect for starting/stopping animation
    LaunchedEffect(isPlaying, bpm) {
        if (isPlaying) {
            // Start animation by changing the target position
            targetPosition = if (targetPosition == 0f) 1f else 0f
        } else {
            // Stops at current position
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(40.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val lineY = canvasHeight * 0.7f
        val dotRadius = with(density) { 6.dp.toPx() }

        // Drawing a horizontal line
        drawLine(
            color = lineColor,
            start = Offset(0f, lineY),
            end = Offset(canvasWidth, lineY),
            strokeWidth = with(density) { 2.dp.toPx() }
        )

        // Draw a dot only when the metronome is running
        if (isPlaying) {
            val dotX = canvasWidth * dotPosition
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(dotX, lineY)
            )
        }
    }
}

@Composable
fun MetronomeScreen(
    uiState: MetronomeUiState,
    onBpmChange: (Int) -> Unit,
    onStartStopClick: () -> Unit
) {
    var showBpmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual metronome
        MetronomeVisualizer(
            isPlaying = uiState.isPlaying,
            bpm = uiState.bpm,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // BPM label
        Text(
            text = "BPM",
            fontSize = 16.sp,
            fontFamily = oswaldFont(500), // Medium weight
            //color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // BPM control with minus/plus buttons
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minus button
            Button(
                onClick = {
                    val newBpm = (uiState.bpm - 1).coerceAtLeast(MIN_BPM)
                    onBpmChange(newBpm)
                },
                enabled = uiState.bpm > MIN_BPM,
                modifier = Modifier.size(60.dp)
            ) {
                Text(
                    text = "−",
                    fontSize = 24.sp,
                    fontFamily = oswaldFont(700) // Bold weight
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Clickable text with BPM - opens a dialog when clicked
            Text(
                text = "${uiState.bpm}",
                fontSize = 48.sp,
                fontFamily = oswaldFont(700), // Bold weight
                modifier = Modifier
                    .width(120.dp)
                    .clickable { showBpmDialog = true }
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Plus button
            Button(
                onClick = {
                    val newBpm = (uiState.bpm + 1).coerceAtMost(MAX_BPM)
                    onBpmChange(newBpm)
                },
                enabled = uiState.bpm < MAX_BPM,
                modifier = Modifier.size(60.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 24.sp,
                    fontFamily = oswaldFont(700) // Bold weight
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // BPM slider
        Slider(
            value = uiState.bpm.toFloat(),
            onValueChange = { newValue -> onBpmChange(newValue.toInt()) },
            valueRange = MIN_BPM.toFloat()..MAX_BPM.toFloat(), // Range from MIN_BPM to MAX_BPM
            steps = (MAX_BPM - MIN_BPM) - 1 // (MAX_BPM - MIN_BPM) - 1, for stepping one at a time
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Button Start/Stop
        Button(
            onClick = onStartStopClick,
            modifier = Modifier
                .height(80.dp)
                .width(200.dp)
        ) {
            Text(
                text = if (uiState.isPlaying) "STOP" else "START",
                fontSize = 24.sp,
                fontFamily = oswaldFont(600) // SemiBold weight
            )
        }
    }

    // Dialog for manual BPM entry
    if (showBpmDialog) {
        BpmInputDialog(
            currentBpm = uiState.bpm,
            onBpmSet = { newBpm ->
                onBpmChange(newBpm)
                showBpmDialog = false
            },
            onDismiss = { showBpmDialog = false }
        )
    }
}

@Composable
fun BpmInputDialog(
    currentBpm: Int,
    onBpmSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var bpmText by remember { mutableStateOf(currentBpm.toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Set BPM",
                fontFamily = oswaldFont(600) // SemiBold weight
            )
        },
        text = {
            Column {
                Text(
                    "Set BPM value ($MIN_BPM-$MAX_BPM):",
                    fontFamily = oswaldFont(400) // Regular weight
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { newValue ->
                        bpmText = newValue
                        isError = false
                    },
                    label = {
                        Text(
                            "BPM",
                            fontFamily = oswaldFont(400) // Regular weight
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = if (isError) {
                        {
                            Text(
                                "Set value between $MIN_BPM and $MAX_BPM",
                                fontFamily = oswaldFont(400) // Regular weight
                            )
                        }
                    } else null,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bpm = bpmText.toIntOrNull()
                    if (bpm != null && bpm in MIN_BPM..MAX_BPM) {
                        onBpmSet(bpm)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(
                    "OK",
                    fontFamily = oswaldFont(600) // SemiBold weight
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    fontFamily = oswaldFont(400) // Regular weight
                )
            }
        }
    )
}

// Preview for display in Android Studio
@Preview(showBackground = true)
@Composable
fun MetronomeScreenPreview() {
    AMetronomeTheme {
        MetronomeScreen(
            uiState = MetronomeUiState(isPlaying = false, bpm = 120),
            onBpmChange = {},
            onStartStopClick = {}
        )
    }
}