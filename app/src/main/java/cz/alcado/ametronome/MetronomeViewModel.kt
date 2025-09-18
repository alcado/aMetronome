package cz.alcado.ametronome

import android.app.Application
import android.media.SoundPool
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Data class for storing the state of the user interface
data class MetronomeUiState(
    val isPlaying: Boolean = false,
    val bpm: Int = 120
)

class MetronomeViewModel(application: Application) : AndroidViewModel(application) {

    // SoundPool for playing the "tick" sound
    private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(1).build()
    private val soundId: Int = soundPool.load(application, R.raw.tick, 1)


    // Internal, changeable status
    private val _uiState = MutableStateFlow(MetronomeUiState())
    // Public, read-only status for UI
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    // A job for the running metronome so that we can cancel it
    private var metronomeJob: Job? = null

    // Functions for starting and stopping the metronome
    fun startStop() {
        if (_uiState.value.isPlaying) {
            // If the metronome is running, we will stop it
            metronomeJob?.cancel()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            // If it is stopped, we will start it
            _uiState.update { it.copy(isPlaying = true) }
            startMetronome()
        }
    }

    // Function for setting BPM from the slider
    fun setBpm(newBpm: Int) {
        _uiState.update { it.copy(bpm = newBpm) }
        // If the metronome is running, restart it with a new BPM
        if (_uiState.value.isPlaying) {
            metronomeJob?.cancel()
            startMetronome()
        }
    }

    private fun startMetronome() {
        metronomeJob = viewModelScope.launch {
            // Calculation of the delay between ticks in milliseconds
            // 60,000 ms (1 minute) / BPM = delay between beats
            val delayMillis = 60_000L / _uiState.value.bpm

            while (isActive) { // The loop runs until the coroutine is canceled
                playSound()
                delay(delayMillis)
            }
        }
    }

    private fun playSound() {
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    // Freeing resources when the ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        soundPool.release()
    }
}
