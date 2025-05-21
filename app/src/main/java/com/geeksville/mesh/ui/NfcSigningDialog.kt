package com.geeksville.mesh.ui

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.geeksville.mesh.R
import com.geeksville.mesh.nfc.NfcManager
import com.satochip.SatochipCommandSet
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NfcSigningDialog : DialogFragment() {
    @Inject
    lateinit var nfcManager: NfcManager

    private var messageToSign: String? = null
    private var onSigningComplete: ((String) -> Unit)? = null

    companion object {
        private const val TAG = "NfcSigningDialog"
        private const val PIN = "qqqq" // Hard-coded PIN for now

        fun newInstance(message: String, callback: (String) -> Unit): NfcSigningDialog {
            return NfcSigningDialog().apply {
                messageToSign = message
                onSigningComplete = callback
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.nfc_scan_title)
            .setMessage(R.string.nfc_scan_message)
            .setCancelable(false)
            .create()
    }

    override fun onStart() {
        super.onStart()
        nfcManager.initializeNfc(requireActivity())
        nfcManager.enableNfcForegroundDispatch(requireActivity())
    }

    override fun onStop() {
        super.onStop()
        nfcManager.disableNfcForegroundDispatch(requireActivity())
    }

    fun handleNfcIntent(intent: Intent) {
        val commandSet = nfcManager.handleNfcIntent(intent) ?: return
        try {
            // Select the Satochip applet
            commandSet.cardSelect()
            // Get card status
            commandSet.cardGetStatus()
            // Initialize secure channel
            commandSet.cardInitiateSecureChannel()
            // Verify PIN
            commandSet.cardVerifyPIN(PIN.toByteArray())
            // Sign the message
            messageToSign?.let { message ->
                val hash = java.security.MessageDigest.getInstance("SHA-256").digest(message.toByteArray())
                val resp = commandSet.cardSignTransactionHash(0, hash, null)
                val signature = resp.getData()
                val signedMessage = "$message:${signature.joinToString("") { "%02x".format(it) }}"
                onSigningComplete?.invoke(signedMessage)
                dismiss()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
} 