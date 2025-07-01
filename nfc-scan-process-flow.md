# NFC Scan Process Flow

## 1. Initialization Phase

// User triggers scan from UI
fun scanCardForAction(activity: Activity) {
    // Create NFC card manager
    val cardManager = NFCCardManager()
    cardManager.setCardListener(SatochipCardListenerForAction)
    cardManager.start() // Starts background thread
    
    // Enable NFC reader mode
    val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
    nfcAdapter?.enableReaderMode(
        activity,
        cardManager,
        NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
        null
    )
}

## 2. Card Detection Phase

When a Satochip card is tapped to the device:

// NFCCardManager.onTagDiscovered() is called by Android NFC system
@Override
public void onTagDiscovered(Tag tag) {
    isoDep = IsoDep.get(tag);
    try {
        isoDep.connect();
        isoDep.setTimeout(120000); // 2 minute timeout
    } catch (IOException e) {
        Log.e(TAG, "error connecting to tag");
    }
}

## 3. Connection Monitoring Loop

The NFCCardManager runs a continuous monitoring thread:

public void run() {
    boolean connected = isConnected();
    
    while (true) {
        boolean newConnected = isConnected();
        if (newConnected != connected) {
            connected = newConnected;
            Log.i(TAG, "tag " + (connected ? "connected" : "disconnected"));
            
            if (connected && !isRunning) {
                onCardConnected();
            } else {
                onCardDisconnected();
            }
        }
        SystemClock.sleep(50); // Check every 50ms
    }
}

## 4. Card Connection Callback

When connection is established:

override fun onConnected(cardChannel: CardChannel?) {
    CardState.isConnected.postValue(true)
    SatoLog.d(TAG, "onConnected: Card is connected")
    
    try {
        val cmdSet = SatochipCommandSet(cardChannel)
        CardState.initialize(cmdSet) // Start card communication
        
        // Perform card operations...
        onDisconnected()
        
        // Stop scanning after completion
        CardState.disableScanForAction()
    } catch (e: Exception) {
        SatoLog.e(TAG, "onConnected: an exception has been thrown during card init.")
        onDisconnected()
    }
}

## 5. Card Communication Phase

The app communicates with the Satochip card:

fun onConnection() {
    // Select Satochip applet
    val respdu: APDUResponse = cmdSet.cardSelect("satochip").checkOK()
    
    // Get card status
    val rapduStatus = cmdSet.cardGetStatus()
    cardStatus = ApplicationStatus(rapduStatus)
    
    // Check card setup status
    if (cardStatus.isSetupDone == false) {
        // Handle uninitialized card
    }
    
    // Perform tests based on action type
    when (actionType) {
        TestItems.ScanCard -> {
            testSeedkeeperMemory()
            testGenerateMasterseed()
            // ... more tests
        }
        TestItems.SignMessage -> {
            // Sign hash of message

            // 1. Define the message and path (Path might not be directly used by cardSignTransactionHash)
            val message = "This is a test message to sign."
            val messageBytes = message.toByteArray(Charsets.UTF_8)
            val path = "m/84'/0'/0'/0/0" 

            SatoLog.d("testSatochip", "Signing message: '$message' (Path for context: $path)")

            // 2. Hash the message (SHA-256 recommended for 32 bytes)
            val messageHash = MessageDigest.getInstance("SHA-256").digest(messageBytes)
            SatoLog.d("testSatochip", "Message SHA-256 hash (hex): ${messageHash.toHexString()}")

            // 3. Call the command set method to sign the hash
            // Assumes key number 0 and no 2FA challenge response
            val keyNumber: Byte = 0
            // val keyNumber: Byte = 1 // TODO: use keyNumber 1
            // val keyNumber: Byte = 2
            // val keyNumber: Byte = 3
            val challengeResponse: ByteArray? = null
            val signResponse: APDUResponse = cmdSet.cardSignTransactionHash(keyNumber, messageHash, challengeResponse).checkOK()

            // 4. Process the signature
            val signature = signResponse.data
            SatoLog.d("testSatochip", "Message hash signed successfully! Signature (hex): ${signature.toHexString()}")


        }
    }
}

## 6. Disconnection Phase

When card is removed or operation completes:

override fun onDisconnected() {
    CardState.isConnected.postValue(false)
    CardState.resultCodeLive.postValue(NfcResultCode.Ok)
    SatoLog.d(TAG, "onDisconnected: Card disconnected!")
}

## 7. Cleanup Phase

fun disableScanForAction() {
    if (activity != null) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        nfcAdapter?.disableReaderMode(activity)
    }
}

