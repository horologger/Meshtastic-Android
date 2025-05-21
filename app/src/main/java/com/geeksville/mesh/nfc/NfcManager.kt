package com.geeksville.mesh.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.satochip.SatochipCommandSet
import com.satochip.NFCCardChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcManager @Inject constructor() {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null

    fun initializeNfc(activity: Activity) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        
        if (nfcAdapter == null) {
            Log.e(TAG, "NFC is not available on this device")
            return
        }

        // Create a PendingIntent for NFC intents
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Set up intent filters for NFC discovery
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        techLists = arrayOf(arrayOf(IsoDep::class.java.name))
    }

    fun enableNfcForegroundDispatch(activity: Activity) {
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFilters, techLists)
    }

    fun disableNfcForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun handleNfcIntent(intent: Intent): SatochipCommandSet? {
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            Log.e(TAG, "No tag found in intent")
            return null
        }

        val isoDep = IsoDep.get(tag)
        try {
            isoDep.connect()
            isoDep.setTimeout(120000) // 2 minutes timeout
            
            // Create SatochipCommandSet with the NFCCardChannel
            val cardChannel = NFCCardChannel(isoDep)
            return SatochipCommandSet(cardChannel)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to NFC tag", e)
            return null
        }
    }

    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    companion object {
        private const val TAG = "NfcManager"
    }
} 