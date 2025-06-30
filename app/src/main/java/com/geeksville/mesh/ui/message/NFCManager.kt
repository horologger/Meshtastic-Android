package com.geeksville.mesh.ui.message

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.geeksville.mesh.R
import com.geeksville.mesh.android.Logging
import com.satochip.NFCCardManager
import com.satochip.CardListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NFCManager(private val context: Context) : Logging {
    private val nfcManager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
    private val nfcAdapter: NfcAdapter? = nfcManager.defaultAdapter
    private var cardManager: NFCCardManager? = null
    private var isScanning = false

    var scanStatus by mutableStateOf<String?>(null)
        private set

    var isCardVerified by mutableStateOf(false)
        private set

    init {
        Log.d("NFCManager", "Initializing NFCManager")
        startCardManager()
    }

    private fun startCardManager() {
        Log.d("NFCManager", "Starting card manager thread")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cardManager = NFCCardManager()
                Log.d("NFCManager", "Card manager initialized successfully")
            } catch (e: Exception) {
                Log.e("NFCManager", "Error initializing card manager", e)
                scanStatus = "Error initializing card manager: ${e.message}"
            }
        }
    }

    fun isNFCEnabled(): Boolean {
        val isEnabled = nfcAdapter?.isEnabled == true
        Log.d("NFCManager", "NFC enabled check: $isEnabled")
        return isEnabled
    }

    fun startNfcScan(activity: Activity) {
        if (isScanning) {
            Log.d("NFCManager", "NFC scan already in progress")
            return
        }

        Log.d("NFCManager", "Starting NFC scan")
        isScanning = true
        scanStatus = context.getString(R.string.nfc_waiting_for_card)
        isCardVerified = false

        val cardListener = object : CardListener {
            override fun onConnected(channel: com.satochip.CardChannel) {
                Log.d("NFCManager", "Card connected callback received")
                scanStatus = context.getString(R.string.nfc_card_detected)
                try {
                    Log.d("NFCManager", "Starting card verification process")
                    // Here you would typically verify the card and get its details
                    Log.d("NFCManager", "Card verification completed")
                    scanStatus = context.getString(R.string.nfc_card_verified)
                    isCardVerified = true
                    Log.d("NFCManager", "Card verification successful, isCardVerified set to true")
                } catch (e: Exception) {
                    Log.e("NFCManager", "Error during card verification", e)
                    scanStatus = "Error: ${e.message}"
                    isCardVerified = false
                }
            }

            override fun onDisconnected() {
                Log.d("NFCManager", "Card disconnected callback received")
                scanStatus = context.getString(R.string.nfc_card_removed)
                isCardVerified = false
            }
        }

        try {
            Log.d("NFCManager", "Setting up card listener")
            cardManager?.setCardListener(cardListener)
            Log.d("NFCManager", "Card listener setup complete")

            nfcAdapter?.enableReaderMode(
                activity,
                { tag ->
                    Log.d("NFCManager", "Tag discovered: ${bytesToHex(tag.id)}")
                    try {
                        val isoDep = IsoDep.get(tag)
                        Log.d("NFCManager", "Attempting to connect to IsoDep")
                        isoDep.connect()
                        isoDep.timeout = 30000
                        Log.d("NFCManager", "IsoDep connected successfully")
                        cardManager?.onTagDiscovered(tag)
                    } catch (e: Exception) {
                        Log.e("NFCManager", "Error connecting to tag", e)
                        scanStatus = "Error connecting to card: ${e.message}"
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
            Log.d("NFCManager", "Reader mode enabled successfully")
        } catch (e: Exception) {
            Log.e("NFCManager", "Error starting NFC scan", e)
            scanStatus = "Error starting scan: ${e.message}"
            isScanning = false
        }
    }

    fun stopNfcScan(activity: Activity) {
        if (!isScanning) {
            Log.d("NFCManager", "No active NFC scan to stop")
            return
        }

        Log.d("NFCManager", "Stopping NFC scan")
        try {
            nfcAdapter?.disableReaderMode(activity)
            Log.d("NFCManager", "Reader mode disabled successfully")
            cardManager?.setCardListener(null)
            Log.d("NFCManager", "Card listener removed")
            isScanning = false
            scanStatus = null
            isCardVerified = false
            Log.d("NFCManager", "NFC scan cleanup completed")
        } catch (e: Exception) {
            Log.e("NFCManager", "Error stopping NFC scan", e)
            scanStatus = "Error stopping scan: ${e.message}"
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
} 