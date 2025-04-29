package com.iapptech.nfcreader

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iapptech.nfcreader.ui.theme.NfcReaderTheme
import java.math.BigInteger

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null

    private val _tagInfo = mutableStateOf("Scan an NFC tag...")
    val tagInfo: State<String> = _tagInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")

        // NFC Setup
        val intent = Intent(this, javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))

        techLists = arrayOf(
            arrayOf(NfcA::class.java.name),
            arrayOf(NfcB::class.java.name),
            arrayOf(NfcF::class.java.name),
        )

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Set Compose UI
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "NFC Reader",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = tagInfo.value,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: intent.action=${intent.action}")

        if (intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return
            val tagIdByte: ByteArray = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID) ?: return

            // Log raw UID bytes
            Log.d(TAG, "Raw UID bytes: ${tagIdByte.joinToString(":") { String.format("%02X", it) }}")
            Log.d(TAG, "UID Byte Array Length: ${tagIdByte.size}")

            // Check if it's larger than 4 bytes and apply reversal if needed
            val cardId = if (tagIdByte.size > 4) {
                val reversedId = tagIdByte.reversedArray() // reverse for Little-endian
                BigInteger(1, reversedId).toString()
            } else {
                BigInteger(1, tagIdByte).toString() // Direct interpretation for 4 bytes
            }

            val tagIdHex = tagIdByte.joinToString(":") { String.format("%02X", it) }

            Log.d(TAG, "Tag UID (HEX): $tagIdHex")
            Log.d(TAG, "Card ID (Decimal): $cardId")

            val builder = StringBuilder()
            builder.append("Tag UID (HEX): $tagIdHex\n")
            builder.append("Card ID (Decimal): $cardId\n")

            val techList = tag.techList.joinToString(", ")
            builder.append("Supported Techs: $techList\n")

            // NDEF
            val ndef = Ndef.get(tag)
            ndef?.let {
                builder.append("NDEF Type: ${it.type}\n")
                builder.append("Max Size: ${it.maxSize}\n")
                builder.append("Writable: ${it.isWritable}\n")

                val ndefMessage = it.cachedNdefMessage
                ndefMessage?.records?.forEach { record ->
                    val recordText = String(record.payload)
                    Log.d(TAG, "NDEF Record: $recordText")
                    builder.append("NDEF Record: $recordText\n")
                }
            }

            // Mifare Classic
            MifareClassic.get(tag)?.let {
                builder.append("Mifare Classic\n")
                builder.append("Type: ${it.type}\nSectors: ${it.sectorCount}\nBlocks: ${it.blockCount}\nSize: ${it.size}\n")
            }

            // NfcA
            NfcA.get(tag)?.let {
                builder.append("NfcA\n")
                builder.append("ATQA: ${it.atqa.joinToString { b -> String.format("%02X", b) }}\n")
                builder.append("SAK: ${it.sak}\n")
            }

            // NfcB
            NfcB.get(tag)?.let {
                builder.append("NfcB\n")
                builder.append("Application Data: ${it.applicationData?.joinToString { b -> String.format("%02X", b) }}\n")
            }

            // ISO-DEP
            IsoDep.get(tag)?.let {
                builder.append("IsoDep\n")
                builder.append("Timeout: ${it.timeout}ms\n")
            }

            // Update Compose UI
            _tagInfo.value = builder.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists)
    }

    override fun onPause() {
        Log.d(TAG, "onPause: ")
        nfcAdapter?.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        super.onDestroy()
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    NfcReaderTheme {
//        Greeting("Android")
//    }
//}