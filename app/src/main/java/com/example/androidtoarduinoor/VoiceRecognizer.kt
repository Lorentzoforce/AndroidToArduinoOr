package com.example.androidtoarduinoor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import org.vosk.android.RecognitionListener
import org.json.JSONObject

/**
 * VoiceRecognizer — Offline speech recognition implemented using Vosk (Kaldi).
 * Supports auto-restart, offline model, and permission checks.
 */
class VoiceRecognizer(private val context: Context) : RecognitionListener {

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null

    private var restartJob: Job? = null
    private var onResultCallback: ((String) -> Unit)? = null

    private val TAG = "VoiceRecognizer"

    /**
     * Start listening for speech.
     * @param onResult Callback invoked whenever recognized text is available.
     */
    fun startListening(onResult: (String) -> Unit) {
        Log.i(TAG, "🔊 startListening() called")
        onResultCallback = onResult

        // 权限检测 / Permission check
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "❌ No microphone permission, cannot start recognition")
            return
        }

        // 加载模型 / Load model
        if (model == null) {
            Log.i(TAG, "📦 Loading Vosk model...")
            StorageService.unpack(
                context,
                "model",
                "model",
                { unpackedModel ->
                    model = unpackedModel
                    Log.i(TAG, "✅ Model loaded successfully! Starting recognition.")
                    startService()
                },
                { e ->
                    Log.e(TAG, "❌ Model loading failed: ${e.message}")
                }
            )
        } else {
            startService()
        }
    }

    /** 启动语音识别服务 / Start speech recognition service */
    private fun startService() {
        try {
            model?.let {
                recognizer = Recognizer(it, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
                Log.i(TAG, "🎙️ Vosk SpeechService started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start speech recognition: ${e.message}")
        }
    }

    /** 停止语音识别并释放资源 / Stop recognition and release resources */
    fun stopListening() {
        Log.i(TAG, "🛑 Stopping speech recognition")
        restartJob?.cancel()
        try {
            speechService?.stop()
            speechService?.shutdown()
            recognizer?.close()
            model?.close()
        } catch (_: Exception) {}
        speechService = null
        recognizer = null
        model = null
    }

    /** 自动重启监听 / Auto-restart listening */
    private fun restartListening() {
        restartJob?.cancel()
        restartJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            startService()
        }
    }

    // region --- RecognitionListener implementation ---
    override fun onPartialResult(hypothesis: String?) {
        hypothesis?.let {
            val text = extractText(it)
            if (text.isNotEmpty()) {
                Log.d(TAG, "🟡 Partial recognition: $text")
                onResultCallback?.invoke(text)
            }
        }
    }

    override fun onResult(hypothesis: String?) {
        hypothesis?.let {
            val text = extractText(it)
            if (text.isNotEmpty()) {
                Log.i(TAG, "✅ Final recognition: $text")
                onResultCallback?.invoke(text)
            }
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        hypothesis?.let {
            val text = extractText(it)
            if (text.isNotEmpty()) {
                Log.i(TAG, "🏁 Final result: $text")
                onResultCallback?.invoke(text)
            }
        }
        restartListening()
    }

    override fun onError(e: Exception?) {
        Log.e(TAG, "❌ Recognition error: ${e?.message}")
        restartListening()
    }

    override fun onTimeout() {
        Log.w(TAG, "⏰ Timeout, restarting listening")
        restartListening()
    }
    // endregion

    /** 提取 JSON 返回中的文字字段 / Extract text field from JSON result */
    private fun extractText(json: String): String {
        return try {
            val obj = JSONObject(json)
            obj.optString("text", "")
        } catch (e: Exception) {
            ""
        }
    }
}
