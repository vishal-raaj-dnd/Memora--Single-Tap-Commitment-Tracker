package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Clean, lightweight helper for recording voice commitments directly in-app
 * to be processed by Sarvam Saaras Speech-to-Text API.
 */
class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    fun startRecording(): File? {
        return try {
            val cacheDir = context.cacheDir
            val outputFile = File(cacheDir, "sarvam_voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(16000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            isRecording = true
            Log.d("AudioRecorderHelper", "Started recording to: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start recording: ${e.message}", e)
            stopRecording()
            null
        }
    }

    fun stopRecording(): File? {
        return try {
            if (isRecording) {
                recorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.w("AudioRecorderHelper", "Recorder stop exception: ${e.message}")
                    }
                    release()
                }
                recorder = null
                isRecording = false
                Log.d("AudioRecorderHelper", "Stopped recording: ${currentOutputFile?.absolutePath} (${currentOutputFile?.length()} bytes)")
                currentOutputFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error stopping recording: ${e.message}", e)
            recorder?.release()
            recorder = null
            isRecording = false
            null
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
