package com.example.zeguiaapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.util.UUID
import kotlin.io.outputStream
import android.Manifest
import android.os.Build
import android.content.Context
import android.bluetooth.BluetoothManager


class MainActivity : AppCompatActivity() {
    private lateinit var btAdapter: BluetoothAdapter
    private var btSocket: BluetoothSocket? = null
    private val address: String = "CC:DB:A7:62:8D:96" // MAC da sua ESP32
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        btAdapter = bluetoothManager.adapter

        val btnBluetooth = findViewById<android.widget.Button>(R.id.bluetooth)
        val swLED = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.LED)

        btnBluetooth.setOnClickListener{
            conectarBluetooth()
        }

        swLED.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
            {
                enviarComando("1")
            }
            else
            {
                enviarComando("0")
            }
        }
    }
    private fun conectarBluetooth() {
        if (!btAdapter.isEnabled) {
            Toast.makeText(this, "Ative o Bluetooth!", Toast.LENGTH_SHORT).show()
            return
        }

        val dispositivo = btAdapter.getRemoteDevice(address)

        try {
            btSocket = dispositivo.createRfcommSocketToServiceRecord(MY_UUID)
            btSocket?.connect()
            Toast.makeText(this, "Conectado!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Falha na conexão", Toast.LENGTH_SHORT).show()
        }
    }


    private fun enviarComando(sinal: String)
    {
        try
        {
            btSocket?.outputStream?.write(sinal.toByteArray());
        }
        catch (e: IOException )
        {
            Toast.makeText(this, "Erro ao enviar dados", Toast.LENGTH_SHORT).show();
        }

    }
}

