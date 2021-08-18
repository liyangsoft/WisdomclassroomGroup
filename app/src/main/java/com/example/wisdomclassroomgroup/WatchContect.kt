package com.example.wisdomclassroomgroup

import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import com.sc.lesa.mediashar.jlib.server.SocketClientThread
import com.sc.lesa.mediashar.jlib.threads.VideoPlayThread
import com.sc.lesa.mediashar.jlib.threads.VoicePlayThread
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import kotlin.concurrent.thread


class WatchContect : AppCompatActivity(), SurfaceHolder.Callback {
    private lateinit var mSurfaceView: SurfaceView

    //    private lateinit var mSurfaceView1: SurfaceView
    lateinit var mSurfaceHolder: SurfaceHolder

    //    lateinit var mSurfaceHolder1: SurfaceHolder
    lateinit var ip: String
    lateinit var socketClientThread: SocketClientThread
    var currentport:Int = 0

    //    lateinit var socketClientThread1: SocketClientThread
    var mdiaPlayThread: VideoPlayThread? = null
    var mdiaPlayThread1: VideoPlayThread? = null
    var voicePlayThread: VoicePlayThread? = null


    companion object {
        fun buildIntent(intent: Intent, ip: String): Intent {
            intent.putExtra("Address", ip)
            return intent
        }
    }


    override fun onResume() {
        super.onResume()
        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//        val actionBar = supportActionBar
//        actionBar!!.hide()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_contect)


        mSurfaceView = findViewById<SurfaceView>(R.id.surfaceView_watch)
//        mSurfaceView1 = findViewById<SurfaceView>(R.id.surfaceView_1)
        mSurfaceHolder = mSurfaceView.holder
//        mSurfaceHolder1 = mSurfaceView1.holder
        val intent = intent
        ip = intent.getStringExtra("Address")!!

        mSurfaceHolder.addCallback(this)

    }

    var preTime: Long = 0
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val currentTime = Date().time
            if (currentTime - preTime > 2000) {
                Toast.makeText(this, "再按一次退出程序！", Toast.LENGTH_SHORT).show()
                preTime = currentTime
                return true
            }
            clear()
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun init() {
        currentport = IPUtil.getPort(this)

        thread(true) {
            socketClientThread = ClientThread(ip)
            try {
                socketClientThread.connect()
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@WatchContect, "${"错误"}:${e.message}", Toast.LENGTH_SHORT).show()
                }
                return@thread
            }
            socketClientThread.start()
            mdiaPlayThread = VideoPlayThread(mSurfaceHolder.surface, socketClientThread.dataPackList)
            mdiaPlayThread!!.start()
            voicePlayThread = VoicePlayThread(socketClientThread.dataPackList)
            voicePlayThread!!.start()


            /*  voicePlayThread = VoicePlayThread(socketClientThread.dataPackList)
              voicePlayThread!!.start()*/
        }
    }

    fun clear() {
        socketClientThread.exit()
        mdiaPlayThread?.exit()
        voicePlayThread?.exit()
    }


    private inner class ClientThread(ip: String) : SocketClientThread(ip, currentport) {
        override fun onError(t: Throwable) {
            runOnUiThread {
                Toast.makeText(this@WatchContect, "${"错误"}:${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun surfaceChanged(p0: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {

    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        init()
    }

}