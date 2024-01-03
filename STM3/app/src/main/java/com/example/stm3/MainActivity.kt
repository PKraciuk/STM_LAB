package com.example.stm3

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID


class MainActivity : ComponentActivity() {
    private val requestEnableBt = 1
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var pairedDevicesSpinner: Spinner
    private lateinit var chatHistoryListView: ListView
    private lateinit var messageEditText: EditText
    private lateinit var uuidText: TextView
    private lateinit var sendButton: Button
    private val pairedDevicesList = ArrayList<String>()
    private val bluetoothPermissionsRequestCode = 101
    private lateinit var appUUID: UUID
    private lateinit var connectButton: Button
    private lateinit var awaitButton: Button
    private var selectedDevice: BluetoothDevice? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appUUID = UUID.randomUUID()

        pairedDevicesSpinner = findViewById(R.id.pairedDevicesSpinner)
        chatHistoryListView = findViewById(R.id.chatHistoryListView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        uuidText = findViewById(R.id.uuidTextView)
        connectButton = findViewById(R.id.connectButton)
        awaitButton = findViewById(R.id.awaitButton)

        uuidText.text = appUUID.toString()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(bluetoothAdapter == null) {

        } else if (!bluetoothAdapter!!.isEnabled()) {
            var enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, requestEnableBt)
        }

        connectButton.setOnClickListener {
            selectedDevice?.let { device ->
                ConnectThread(device, appUUID, bluetoothAdapter!!).start()
            }
        }

        awaitButton.setOnClickListener {
            AcceptThread(bluetoothAdapter!!, appUUID).start()
        }

        checkBluetoothPermissions()
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                bluetoothPermissionsRequestCode
            )
        } else {
            setupBluetoothDevicesList()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == bluetoothPermissionsRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupBluetoothDevicesList()
        }
    }


    @SuppressLint("MissingPermission")
    private fun setupBluetoothDevicesList() {
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter?.getBondedDevices() ?: emptySet()
        for (device in pairedDevices) {
            pairedDevicesList.add("${device.name}\n[${device.address}]")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pairedDevicesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pairedDevicesSpinner.adapter = adapter

        pairedDevicesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedDevice = pairedDevices.elementAtOrNull(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedDevice = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    inner class ConnectThread(private val device: BluetoothDevice, private val uuid: UUID, private val bluetoothAdapter: BluetoothAdapter) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect()
                    // Manage the connection in a separate thread
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                    }
                } catch (connectException: IOException) {
                    // Unable to connect; close the socket and return
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Failed to connect to ${device.name}", Toast.LENGTH_SHORT).show()
                    }
                    try {
                        socket.close()
                    } catch (closeException: IOException) {
                        Log.e(TAG, "Could not close the client socket", closeException)
                    }
                    return
                }
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }


    @SuppressLint("MissingPermission")
    inner class AcceptThread(private val bluetoothAdapter: BluetoothAdapter, private val uuid: UUID) : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter.listenUsingRfcommWithServiceRecord("MyService", uuid)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    // Manage connected socket in a separate thread
                    // mmServerSocket?.close() // Close the service socket
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Connection accepted", Toast.LENGTH_SHORT).show()
                    }
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

}
