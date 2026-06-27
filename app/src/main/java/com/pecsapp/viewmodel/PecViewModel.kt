package com.pecsapp.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pecsapp.data.db.AppDatabase
import com.pecsapp.data.model.PecImage
import com.pecsapp.data.repository.PecImageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlertSoundOption(
    val key: String,
    val label: String
)

class PecViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PecImageRepository
    val images: StateFlow<List<PecImage>>
    private val appContext = application.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmToneGenerator by lazy { ToneGenerator(AudioManager.STREAM_ALARM, 100) }
    private var activeRingtone: Ringtone? = null
    private var stopAlertJob: Job? = null
    private val _alertSoundOptions = MutableStateFlow(buildAlertSoundOptions())
    val alertSoundOptions: StateFlow<List<AlertSoundOption>> = _alertSoundOptions.asStateFlow()
    private val _selectedAlertSoundKey = MutableStateFlow(
        preferences.getString(PREF_ALERT_SOUND_KEY, DEFAULT_ALERT_SOUND_KEY) ?: DEFAULT_ALERT_SOUND_KEY
    )
    val selectedAlertSoundKey: StateFlow<String> = _selectedAlertSoundKey.asStateFlow()

    companion object {
        private const val PREFS_NAME = "pecs_settings"
        private const val PREF_ALERT_SOUND_KEY = "alert_sound_key"
        private const val BUILTIN_DING_DONG = "builtin:ding_dong"
        private const val BUILTIN_BEEP = "builtin:beep"
        private const val BUILTIN_BELL = "builtin:bell"
        private const val DEFAULT_ALERT_SOUND_KEY = BUILTIN_DING_DONG
    }

    init {
        val dao = AppDatabase.getDatabase(application).pecImageDao()
        repository = PecImageRepository(dao)

        images = repository.allActiveImages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun onImageTapped(image: PecImage) {
        playCommunicationAlert(selectedAlertSoundKey.value)
        viewModelScope.launch {
            repository.incrementTapCount(image.id)
        }
    }

    fun updateAlertSound(soundKey: String) {
        _selectedAlertSoundKey.value = soundKey
        preferences.edit().putString(PREF_ALERT_SOUND_KEY, soundKey).apply()
        playCommunicationAlert(soundKey)
    }

    private fun playCommunicationAlert(soundKey: String) {
        stopAlertJob?.cancel()
        activeRingtone?.stop()

        when (soundKey) {
            BUILTIN_DING_DONG -> {
                alarmToneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1400)
                return
            }

            BUILTIN_BEEP -> {
                alarmToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 1200)
                return
            }

            BUILTIN_BELL -> {
                alarmToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
                return
            }
        }

        val alarmUri = Uri.parse(soundKey)

        val ringtone = RingtoneManager.getRingtone(appContext, alarmUri)

        if (ringtone != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }

            activeRingtone = ringtone
            ringtone.play()
            stopAlertJob = viewModelScope.launch {
                delay(1500)
                activeRingtone?.stop()
            }
        } else {
            alarmToneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1400)
        }
    }

    private fun buildAlertSoundOptions(): List<AlertSoundOption> {
        val options = linkedMapOf<String, AlertSoundOption>()

        fun addOption(key: String, label: String) {
            options.putIfAbsent(key, AlertSoundOption(key = key, label = label))
        }

        addOption(BUILTIN_DING_DONG, "Ding dong")
        addOption(BUILTIN_BEEP, "Beep corto")
        addOption(BUILTIN_BELL, "Campana fuerte")

        addSystemDefaultOption(::addOption, RingtoneManager.TYPE_NOTIFICATION, "Notificación predeterminada")
        addSystemDefaultOption(::addOption, RingtoneManager.TYPE_ALARM, "Alarma predeterminada")
        addSystemDefaultOption(::addOption, RingtoneManager.TYPE_RINGTONE, "Tono predeterminado")

        addRingtoneOptions(::addOption, RingtoneManager.TYPE_NOTIFICATION, maxCount = 4)
        addRingtoneOptions(::addOption, RingtoneManager.TYPE_ALARM, maxCount = 4)

        return options.values.toList()
    }

    private fun addSystemDefaultOption(
        addOption: (String, String) -> Unit,
        type: Int,
        fallbackLabel: String
    ) {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(appContext, type)
            ?: RingtoneManager.getDefaultUri(type)
        if (uri != null) {
            val title = RingtoneManager.getRingtone(appContext, uri)?.getTitle(appContext) ?: fallbackLabel
            addOption(uri.toString(), title)
        }
    }

    private fun addRingtoneOptions(
        addOption: (String, String) -> Unit,
        type: Int,
        maxCount: Int
    ) {
        val ringtoneManager = RingtoneManager(appContext).apply { setType(type) }
        val cursor = ringtoneManager.cursor ?: return
        cursor.use {
            val limit = minOf(maxCount, cursor.count)
            for (index in 0 until limit) {
                val uri = ringtoneManager.getRingtoneUri(index) ?: continue
                val title = ringtoneManager.getRingtone(index)?.getTitle(appContext) ?: continue
                addOption(uri.toString(), title)
            }
        }
    }

    fun insertImage(image: PecImage) {
        viewModelScope.launch {
            repository.insert(image)
        }
    }

    fun deleteImage(image: PecImage) {
        viewModelScope.launch {
            repository.delete(image)
        }
    }

    fun setImageActive(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.setActive(id, isActive)
        }
    }

    override fun onCleared() {
        stopAlertJob?.cancel()
        activeRingtone?.stop()
        alarmToneGenerator.release()
        super.onCleared()
    }
}