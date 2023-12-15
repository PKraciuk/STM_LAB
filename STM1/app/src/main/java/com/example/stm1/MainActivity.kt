package com.example.stm1

import DrawView
import GameData
import android.content.Context
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.*
import java.net.ServerSocket
import java.net.Socket


class MainActivity : ComponentActivity() {

    private var serverSocket: ServerSocket? = null
    private val serverPort = 8080
    private var serverThread: SocketServerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_screen)

        setupStartScreen()
    }

    private fun setupStartScreen() {
        val hostButton = findViewById<Button>(R.id.btnHostGame)
        val joinButton = findViewById<Button>(R.id.btnJoinGame)
        val ipEditText = findViewById<EditText>(R.id.etEnterIp)
        val ipAddressTextView = findViewById<TextView>(R.id.tvDisplayIp) // Assuming you have a TextView for displaying IP
        val drawView = DrawView(this)

        hostButton.setOnClickListener {
            val ip = getLocalIpAddress()
            ipAddressTextView.text = "IP Address: $ip" // Display IP Address on the screen
            Log.d("MainActivity", "Host button clicked, IP: $ip")
            serverThread = SocketServerThread(this, drawView)
            serverThread!!.start()
        }

        joinButton.setOnClickListener {
            val ip = ipEditText.text.toString()
            Client(ip, this, drawView).execute()
        }
    }

    private inner class SocketServerThread(private val context: Context, private val drawView: DrawView) : Thread() {
        override fun run() {
            try {
                serverSocket = ServerSocket(serverPort)
                while (!isInterrupted) {
                    val socket = serverSocket!!.accept()
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val output = PrintStream(socket.getOutputStream())

                    runOnUiThread {
                        val hostButton = findViewById<Button>(R.id.btnHostGame)
                        val joinButton = findViewById<Button>(R.id.btnJoinGame)
                        val ipEditText = findViewById<EditText>(R.id.etEnterIp)
                        val ipAddressTextView = findViewById<TextView>(R.id.tvDisplayIp)
                        hostButton.setVisibility(View.GONE);
                        joinButton.setVisibility(View.GONE);
                        ipEditText.setVisibility(View.GONE);
                        ipAddressTextView.setVisibility(View.GONE);
                        val layout = findViewById<RelativeLayout>(R.id.main_layout)
                        layout.addView(drawView)

                    }

                    while (!socket.isClosed) {
                        val enemyPaddle = input.readLine()
                        drawView.setEnemyPaddle(enemyPaddle.toFloat())

                        val serverGameData = drawView.prepareGameData()
                        val serverGameDataJson = serverGameData.toJson()
                        output.println(serverGameDataJson)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class Client(private val dstAddress: String, private val context: Context, private val drawView: DrawView) : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void): String? {
            try {
                val socket = Socket(dstAddress, serverPort)
                val output = PrintStream(socket.getOutputStream())
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                runOnUiThread {
                    val hostButton = findViewById<Button>(R.id.btnHostGame)
                    val joinButton = findViewById<Button>(R.id.btnJoinGame)
                    val ipEditText = findViewById<EditText>(R.id.etEnterIp)
                    val ipAddressTextView = findViewById<TextView>(R.id.tvDisplayIp)
                    hostButton.setVisibility(View.GONE);
                    joinButton.setVisibility(View.GONE);
                    ipEditText.setVisibility(View.GONE);
                    ipAddressTextView.setVisibility(View.GONE);
                    val layout = findViewById<RelativeLayout>(R.id.main_layout)
                    layout.addView(drawView)
                }

                while (!socket.isClosed) {
                    // Send only paddle position
                    val paddleX: Float =
                        drawView.getPaddleX() // Assuming you have a method to get paddle X position
                    output.println(paddleX)
                    Log.wtf("I SENT",paddleX.toString())

                    // Receive and update game state
                    val serverGameDataJson = input.readLine()
                    Log.wtf("RECEIVED FROM SERVER", serverGameDataJson.toString())
                    runOnUiThread {
                        drawView.updateGameState(GameData.fromJson(serverGameDataJson))
                    }
                }

                socket.close()
                return null
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }
    }

    private fun getLocalIpAddress(): String {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        return Formatter.formatIpAddress(ipAddress)
    }

    override fun onDestroy() {
        super.onDestroy()
        serverThread?.interrupt()
        serverSocket?.close()
    }
}
