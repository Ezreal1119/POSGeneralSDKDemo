package com.example.posdemo.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.posdemo.R
import com.urovo.utils.BytesUtil
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.UUID

class WebSocketPrintService : Service() {

    companion object {
        private const val CHANNEL_ID = "websocket_print_channel"
        private const val NOTIF_ID = 1001
        private const val ACTION_START = "action_start"
        private const val ACTION_STOP = "action_stop"
        private const val MAC_SIMULATE_PRINTER = "FF:FF:FF:FF:FF:FF"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val PORT_8080 = 8080
        var isRunning = false


        fun start(context: Context) {
            val intent = Intent(context, WebSocketPrintService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // It's a must to run startForeground right after this
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun bind(context: Context, conn: ServiceConnection) {
            val intent = Intent(context, WebSocketPrintService::class.java)
            context.bindService(intent, conn, BIND_AUTO_CREATE)
        }

        fun stop(context: Context) {
            val intent = Intent(context, WebSocketPrintService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Volatile private var server: WebSocketPrinterServer? = null
    private val binder = LocalBinder()
    private var listener: WebSocketPrinterServiceListener? = null

    override fun onCreate() {
        super.onCreate()
        ensureForeground()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                Handler(Looper.getMainLooper()).postDelayed({listener?.onServiceStart()}, 100)
                isRunning = true
                startWebSocketIfNeeded()
                return START_STICKY
            }
            ACTION_STOP -> {
                runCatching {
                    server?.stop()
                    server = null
                    stopForeground(true)
                    stopSelf()
                }.onFailure {
                    it.printStackTrace()
                }
                return START_NOT_STICKY
            }
            else -> {
                startWebSocketIfNeeded()
                return START_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            listener?.onServiceDestroy()
            server?.stop()
            server = null
            isRunning = false
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun setListener(listener: WebSocketPrinterServiceListener?) {
        this.listener = listener
    }

    private fun ensureForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Print WebSocket",
                IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("WebSocket Print Service is running...")
            .setContentText("WebSocket: ws://127.0.0.1:$PORT_8080/print")
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    @Synchronized
    private fun startWebSocketIfNeeded() {
        if (server != null) return
        server = WebSocketPrinterServer(PORT_8080, this) { cmd ->
            Thread {
                var socket: BluetoothSocket? = null
                runCatching {
                    val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw Exception("No BlueAdapter available!")
                    val device = adapter.getRemoteDevice(MAC_SIMULATE_PRINTER)
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID).apply { connect() }
                    val os = socket.outputStream
                    os.write(BytesUtil.hexString2Bytes(cmd))
                    os.flush()
                }.onSuccess {
                    Log.e("Patrick", "startWebSocketIfNeeded: Printing")
                }.onFailure {
                    Log.e("Patrick", "startWebSocketIfNeeded: Print Error")
                    it.printStackTrace()
                }
                socket?.close()
            }.start()
        }.apply { start() }
    }


    inner class LocalBinder : Binder() {
        fun getService(): WebSocketPrintService = this@WebSocketPrintService
    }
}

class WebSocketPrinterServer(
    port: Int,
    private val context: Context,
    private val onCommand: (String) -> Unit,
) : WebSocketServer(InetSocketAddress(port)) {

    override fun onStart() {
        // Will be called when the Server is started
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, "The WebSocket Server has been started at Port=8080", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        // Will be called whenever a client is connected (Handshake only, not yet any message transmission)
        // Only "ws://host:port/print" is allowed
        Log.e("Patrick", "onOpen: ")
        val path = handshake?.resourceDescriptor ?: ""
        if (path != "/print") {
            conn?.close(1008, "Invalid Path")
            return
        }
    }

    override fun onClose(
        conn: WebSocket?,
        code: Int,
        reason: String?,
        remote: Boolean
    ) {
        // Will be called when is WebSocket server is closed
        Log.e("Patrick", "onClose: ")
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        // Will be called once received a message from the WebSocket Client(i.e. call "ws.send(...)" at the Client side)
        Log.e("Patrick", "onMessage: ")
        conn?.send("OK") // ACK
        onCommand(message ?: "")
    }

    override fun onError(conn: WebSocket?, ex: java.lang.Exception?) {
        Log.e("Patrick", "onError: ")
        ex?.printStackTrace()
    }

}

interface WebSocketPrinterServiceListener {
    fun onServiceStart()
    fun onServiceDestroy()
}