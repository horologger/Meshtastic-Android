package com.geeksville.mesh.ui.message

import android.app.Activity
import android.nfc.NfcAdapter
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.util.encoders.Hex
import org.satochip.android.NFCCardManager
import org.satochip.client.SatochipCommandSet
import org.satochip.io.CardChannel
import org.satochip.io.CardListener
import org.satochip.io.APDUResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Handles NFC scanning and message signing with Satochip cards
 */
class NfcScanner(private val activity: Activity) {
    private val TAG = "NfcScanner"
    private var nfcAdapter: NfcAdapter? = null
    private var cardManager: NFCCardManager? = null
    private var isScanning = false

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
    }

    /**
     * Starts NFC scanning for message signing
     * @param message The message to sign
     * @param onSuccess Callback with the signature
     * @param onError Callback with error message
     */
    fun startScanning(
        message: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (nfcAdapter == null) {
            onError("NFC is not available on this device")
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            onError("NFC is disabled. Please enable NFC in settings.")
            return
        }

        if (isScanning) {
            onError("NFC scanning is already in progress")
            return
        }

        isScanning = true

        // Create card manager and listener
        cardManager = NFCCardManager()
        cardManager?.setCardListener(object : CardListener {
            override fun onConnected(cardChannel: CardChannel?) {
                if (cardChannel == null) {
                    onError("Failed to establish connection with card")
                    stopScanning()
                    return
                }

                // Handle card connection in background thread
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val signature = signMessageWithCard(cardChannel, message)
                        withContext(Dispatchers.Main) {
                            onSuccess(signature)
                            stopScanning()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error signing message: ${e.message}")
                        withContext(Dispatchers.Main) {
                            onError("Failed to sign message: ${e.message}")
                            stopScanning()
                        }
                    }
                }
            }

            override fun onDisconnected() {
                Log.d(TAG, "Card disconnected")
                if (isScanning) {
                    stopScanning()
                }
            }
        })

        // Start the card manager thread
        cardManager?.start()

        // Enable NFC reader mode
        nfcAdapter?.enableReaderMode(
            activity,
            cardManager,
            NfcAdapter.FLAG_READER_NFC_A or 
            NfcAdapter.FLAG_READER_NFC_B or 
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )

        Log.d(TAG, "NFC scanning started")
    }

    /**
     * Stops NFC scanning and cleans up resources
     */
    fun stopScanning() {
        if (!isScanning) return

        isScanning = false

        // Disable NFC reader mode
        nfcAdapter?.disableReaderMode(activity)

        // Stop card manager
        cardManager?.let { manager ->
            try {
                manager.interrupt()
            } catch (e: Exception) {
                Log.w(TAG, "Error interrupting card manager: ${e.message}")
            }
        }

        cardManager = null
        Log.d(TAG, "NFC scanning stopped")
    }

    /**
     * Signs a message using the connected Satochip card
     * 
     * Note: The Satochip card must be properly initialized before signing:
     * 1. Card must be set up (PIN configured)
     * 2. BIP32 seed must be imported or generated
     * 3. PIN may need to be verified depending on card configuration
     */
    private suspend fun signMessageWithCard(cardChannel: CardChannel, message: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Create command set for card communication
                val cmdSet = SatochipCommandSet(cardChannel)
                
                // Select Satochip applet
                val selectResponse = cmdSet.cardSelect("satochip")
                if (selectResponse.getSw() != 0x9000) {
                    throw Exception("Failed to select Satochip applet")
                }

                // Get card status
                val statusResponse = cmdSet.cardGetStatus()
                if (statusResponse.getSw() != 0x9000) {
                    throw Exception("Failed to get card status")
                }

                val cardStatus = cmdSet.applicationStatus ?: throw Exception("Failed to get application status")
                val pinString = "qqqq"
                val pinBytes = pinString.toByteArray(Charsets.UTF_8)
                val wrongPinBytes = "0000".toByteArray(Charsets.UTF_8)
                var respApdu = APDUResponse(ByteArray(0), 0x00, 0x00)

                // check setup status
                if (cardStatus.isSetupDone == false) {
                    try {
                        respApdu = cmdSet.cardSetup(5, pinBytes) ?: respApdu
                    } catch (error: Exception) {
                        throw Exception("Start Satochip tests: Error: $error")
                    }
                }
                // verify PIN
                cmdSet.setPin0(pinBytes)
                cmdSet.cardVerifyPIN()

                // Initialize secure channel if needed
                try {
                    cmdSet.cardInitiateSecureChannel()
                } catch (e: Exception) {
                    Log.w(TAG, "Secure channel initialization failed, continuing without it: ${e.message}")
                }

                // 1. Convert message to bytes and hash it using SHA-256
                val messageBytes = message.toByteArray(Charsets.UTF_8)
                val messageHash = MessageDigest.getInstance("SHA-256").digest(messageBytes)
                Log.d(TAG, "Message: '$message'")
                Log.d(TAG, "Message SHA-256 hash (hex): ${Hex.toHexString(messageHash)}")

                // 2. Sign the hash using cardSignTransactionHash
                val keyNumber: Byte = 0 // Use key number 0
                val challengeResponse: ByteArray? = null // No 2FA challenge response
                
                val signResponse = cmdSet.cardSignTransactionHash(keyNumber, messageHash, challengeResponse)
                
                if (signResponse.getSw() == 0x9000) {
                    // 3. Get the signature from the response
                    val signature = signResponse.getData()
                    val signatureHex = Hex.toHexString(signature)
                    Log.d(TAG, "Message hash signed successfully! Signature (hex): $signatureHex")
                    return@withContext signatureHex
                } else {
                    val errorCode = String.format("0x%04X", signResponse.getSw())
                    val errorMessage = when (signResponse.getSw().toInt()) {
                        0x9C14 -> "Card needs to be initialized with a BIP32 seed first"
                        0x9C03 -> "Operation not allowed - card may need PIN verification"
                        0x9C04 -> "Card setup not completed"
                        else -> "Card returned error code: $errorCode"
                    }
                    throw Exception(errorMessage)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in signMessageWithCard: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Checks if NFC is available and enabled
     */
    fun isNfcAvailable(): Boolean {
        return nfcAdapter != null && nfcAdapter!!.isEnabled
    }

    /**
     * Gets NFC status message
     */
    fun getNfcStatusMessage(): String {
        return when {
            nfcAdapter == null -> "NFC is not available on this device"
            !nfcAdapter!!.isEnabled -> "NFC is disabled. Please enable NFC in settings."
            else -> "NFC is available and enabled"
        }
    }
} 