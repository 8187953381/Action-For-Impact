package com.afi.actionforimpact

import android.graphics.Point
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import com.afi.lexsdk.Constants.RTMP_URL
import com.takusemba.rtmppublisher.Publisher
import com.takusemba.rtmppublisher.PublisherListener

class StreamingRTMP : AppCompatActivity(), PublisherListener {

    private lateinit var publisher: Publisher
    private lateinit var glView: GLSurfaceView
    private lateinit var container: RelativeLayout
    private lateinit var publishButton: Button
    private lateinit var cameraButton: ImageView
    private lateinit var label: TextView

    private var url : String = ""
    private val handler = Handler()
    private var thread: Thread? = null
    private var isCounting = false
    private val TAG = "RTMP_STREAM_TEST"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try{
            setContentView(R.layout.activity_rtmpstreaming)
            glView = findViewById(R.id.surface_view)
            container = findViewById(R.id.container)
            publishButton = findViewById(R.id.toggle_publish)
            cameraButton = findViewById(R.id.toggle_camera)
            label = findViewById(R.id.live_label)
            url = intent.getStringExtra(RTMP_URL)

            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val width = size.x
            val height = size.y
            Log.e("checkSize","Width"+ width)
            Log.e("checkSize","height" + height)


            if (url.isBlank()) {
                Log.d(TAG,"URL1: "+url)
                Toast.makeText(this, R.string.error_empty_url, Toast.LENGTH_SHORT)
                        .apply { setGravity(Gravity.CENTER, 0, 0) }
                        .run { show() }
            } else {
                Log.d(TAG,"URL2: "+url)
                publisher = Publisher.Builder(this)
                        .setGlView(glView)
                        .setUrl(url)
                        .setSize(Publisher.Builder.DEFAULT_WIDTH, Publisher.Builder.DEFAULT_HEIGHT)
                        .setAudioBitrate(Publisher.Builder.DEFAULT_AUDIO_BITRATE)
                        .setVideoBitrate(Publisher.Builder.DEFAULT_VIDEO_BITRATE)
                        .setCameraMode(Publisher.Builder.DEFAULT_MODE)
                        .setListener(this)
                        .build()

                publishButton.setOnClickListener {
                    if (publisher.isPublishing) {
                        Log.d(TAG,"Stop Publishing: "+url)
                        publisher.stopPublishing()
                    } else {
                        Log.d(TAG,"Start bublishing: "+url)
                        publisher.startPublishing()
                    }
                }

                cameraButton.setOnClickListener {
                    publisher.switchCamera()
                }
            }
        } catch(ex : Exception){
            Toast.makeText(this,"Something went wrong. Please try after sometime.",Toast.LENGTH_LONG).show()
        }

    }

    override fun onResume() {
        super.onResume()
        if (url.isNotBlank()) {
            updateControls()
        }
    }



    override fun onStarted() {
        Toast.makeText(this, R.string.started_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updateControls()
        startCounting()
    }

    override fun onStopped() {
        Toast.makeText(this, R.string.stopped_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updateControls()
        stopCounting()
    }

    override fun onDisconnected() {
        Toast.makeText(this, R.string.disconnected_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updateControls()
        stopCounting()
    }

    override fun onFailedToConnect() {
        Toast.makeText(this, R.string.failed_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updateControls()
        stopCounting()
    }

    private fun updateControls() {
        publishButton.text = getString(if (publisher.isPublishing) R.string.stop_publishing else R.string.start_publishing)
    }



    private fun startCounting() {
        isCounting = true
        label.text = getString(R.string.publishing_label, 0L.format(), 0L.format())
        label.visibility = View.VISIBLE
        val startedAt = System.currentTimeMillis()
        var updatedAt = System.currentTimeMillis()
        thread = Thread {
            while (isCounting) {
                if (System.currentTimeMillis() - updatedAt > 1000) {
                    updatedAt = System.currentTimeMillis()
                    handler.post {
                        val diff = System.currentTimeMillis() - startedAt
                        val second = diff / 1000 % 60
                        val min = diff / 1000 / 60
                        label.text = getString(R.string.publishing_label, min.format(), second.format())
                    }
                }
            }
        }
        thread?.start()
    }

    private fun stopCounting() {
        isCounting = false
        label.text = ""
        label.visibility = View.GONE
        thread?.interrupt()
    }

    private fun Long.format(): String {
        return String.format("%02d", this)
    }
}
