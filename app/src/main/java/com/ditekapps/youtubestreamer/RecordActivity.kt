package com.ditekapps.youtubestreamer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import android.os.Environment
import kotlinx.android.synthetic.main.activity_record.*
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import android.media.*
import okhttp3.WebSocket
import kotlin.math.log


class RecordActivity : AppCompatActivity() {
    //    val url = "ws://10.10.48.147:80/stream"
//    val url = "ws://192.168.1.208:80/stream"
//    val url = "wss://echo.websocket.org"
    val url = "ws://10.0.2.2:80/stream"   // Emulator localhost
    lateinit var webSocket: WebSocket

    val FILE_NAME = "new_file"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)


        // wss test
        val client = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        val wsListener = WSListener()


        startButton.setOnClickListener {
            webSocket = client.newWebSocket(request, wsListener)
            startRecording()
        }
        stopButton.setOnClickListener { stopRecording() }
        testButton.setOnClickListener { webSocket.send("Hello there!") }
    }

    private val sampleRate = 16000 // 44100 for music

    private val inChannelConfig = AudioFormat.CHANNEL_IN_MONO
    private val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_8BIT
    var minBufSize = AudioRecord.getMinBufferSize(sampleRate, inChannelConfig, audioFormat)
    val buffer = ByteArray(minBufSize * 4)
    var payloadSize = 0
    var cAmplitude = 0

    var record = false
    var recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate, inChannelConfig, audioFormat, minBufSize * 10
    )

    var track = AudioTrack(
        AudioManager.STREAM_MUSIC, sampleRate,
        outChannelConfig, audioFormat,
        minBufSize * 10, AudioTrack.MODE_STREAM
    )

    val TIMER_INTERVAL = 120
    val framePeriod = sampleRate * TIMER_INTERVAL / 1000

//    fun startRecording(){
//        recorder =
//            AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, inChannelConfig, audioFormat, minBufSize * 10)
//        track = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
//            AudioFormat.CHANNEL_OUT_MONO, audioFormat, minBufSize * 10, AudioTrack.MODE_STREAM)
//        recorder.startRecording()
//        track.play()
//        recorder.setRecordPositionUpdateListener(updateListener)
//        recorder.positionNotificationPeriod = framePeriod
//    }

    fun startRecording() {
        record = true
        val streamThread = Thread {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate, inChannelConfig,
                audioFormat, minBufSize * 10
            )
            recorder.startRecording()

            track = AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate, outChannelConfig,
                audioFormat, minBufSize * 10, AudioTrack.MODE_STREAM
            )
            track.play()

            while (record) {
                //reading data from MIC into buffer
                recorder.read(buffer, 0, buffer.size)
                track.write(buffer, 0, buffer.size)

                // Process buffer data...
//                webSocket.send(buffer.toString())
                var byteString = ByteString.of(buffer, 0, buffer.size)
                val success = webSocket.send(byteString)
                if (!success) {
                    Log.e("WSS", "Sample not sent")
                    webSocket.close(WSListener.SERVER_ERROR_STATUS, "Server not receiving data. Closing websocket.")
                    record = false

                } else {
                    Log.d("WSS", "Sample sent" + webSocket.queueSize())

                }
            }
        }
        streamThread.start()
    }

    fun stopRecording() {
        record = false
        if (recorder.state == AudioRecord.STATE_INITIALIZED) {
            recorder.stop()
            recorder.release()
            Log.i("WSS", "Recoreder was initialized")
        }
        webSocket.close(WSListener.NORMAL_CLOSURE_STATUS, "Goodbye !")

        if (track.state == AudioTrack.STATE_INITIALIZED) {
            track.stop()
            track.release()
            Log.i("WSS", "Player was initialized")
        }
    }


    /*
	 *
	 * Method used for recording.
	 *
	 */
    private val updateListener = object : AudioRecord.OnRecordPositionUpdateListener {
        override fun onPeriodicNotification(recorder: AudioRecord) {
            recorder.read(buffer, 0, buffer.size) // Fill buffer
            track.write(buffer, 0, buffer.size)
            Log.d("WSS", "Sample sent: " + buffer.toString())
//            try {
//                Log.d("WSS", "Sample sent: " + buffer.toString())
//                webSocket.send(buffer.toString())
//            } catch (e: IOException) {
//                Log.e(
//                    "WSS",
//                    "Error occured in updateListener, recording is aborted"
//                )
//                recorder.stop()
//            }
        }

        override fun onMarkerReached(recorder: AudioRecord) {
            // NOT USED
        }
    }


    var mRecorder = MediaRecorder()

    fun startRecorder() {
        mRecorder = MediaRecorder()
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mRecorder.setOutputFile(getFullPath(FILE_NAME))
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

        mRecorder.prepare()
//    try {
//    } catch (e: IOException) {
//        Log.e(LOG_TAG, "prepare() failed")
//    }

        mRecorder.start()
    }

    fun stopRecorder() {
        mRecorder.stop()
        // mRecorder.reset();   // You can reuse the object by going back to setAudioSource() step
        mRecorder.release() // Now the object cannot be reused
        sendAudio()

//        var mPlayer = MediaPlayer()
//        mPlayer.setDataSource(getFullPath(FILE_NAME))
//        mPlayer.prepare()
//        mPlayer.start()

// Stop
//        mPlayer.stop()
//        mPlayer.release()
    }

    fun getFullPath(fileName: String): String {
        val path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyFolder/"
        val dir = File(path)
        if (!dir.exists())
            dir.mkdirs()
        return path + fileName + ".3gp"
    }


    fun sendAudio() {
        var f = File(getFullPath(FILE_NAME));
        var input = FileInputStream(f).getChannel();

        webSocket.send("START");

        sendAudioBytes(input);
        input.close();

        webSocket.send("END");
        webSocket.close(1000, "Goodbye !")
    }

    fun sendAudioBytes(input: FileChannel) {
        var buff = ByteBuffer.allocateDirect(32);

        while (input.read(buff) > 0) {
            buff.flip();
            var bytes = ByteString.of(buff).toString();
            webSocket.send(bytes);
            buff.clear();
        }
    }


}


private class WSListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        output("Websocket opened")
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        output("Receiving : " + text!!)
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        output("Receiving bytes : " + bytes!!.hex())
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        webSocket!!.close(code, null)
        output("Closing : $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        output("Websocket failure!")
    }

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
        const val SERVER_ERROR_STATUS = 1011
    }

    private fun output(txt: String) {
        Log.w("WSS", txt)
    }
}