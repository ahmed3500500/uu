package com.snapsave.pro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.File
import java.util.UUID
import android.content.Intent
import androidx.compose.material.icons.filled.Share
import kotlinx.coroutines.launch
import android.app.DownloadManager
import android.net.Uri
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build

class MainActivity : ComponentActivity() {

    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize PRDownloader
        PRDownloader.initialize(applicationContext)
        
        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)
        
        // Initialize AdMob
        MobileAds.initialize(this) {}
        loadInterstitialAd()

        setContent {
            SnapSaveProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        showAd = { showInterstitial() },
                        shareApp = { shareApp() }
                    )
                }
            }
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        // Test ID for Interstitial
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        mInterstitialAd = null
                        loadInterstitialAd()
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        mInterstitialAd = null
                    }
                }
            }
        })
    }

    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            // Ad not ready, try loading again
            loadInterstitialAd()
        }
    }

    private fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out this amazing video downloader app: SnapSave Pro! Download now.")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
}

@Composable
fun SnapSaveProTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFFBB86FC),
        secondary = Color(0xFF03DAC6),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
    )
    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}

@Composable
fun MainScreen(showAd: () -> Unit, shareApp: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Add coroutine scope
    var url by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "Permission Required for Notifications", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                 permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
             }
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // permissionLauncher handles one permission, if needed more complex handling required but for now ok
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = shareApp) {
                Icon(Icons.Default.Share, contentDescription = "Share App", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Top Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SnapSave Pro",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Enter Video Link") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        
            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = progressMessage)
            } else {
                Button(
                    onClick = {
                        if (url.isNotEmpty()) {
                            isProcessing = true
                            progressMessage = "Extracting Link..."
                            
                            coroutineScope.launch {
                                val directLink = VideoExtractor.extractVideoUrl(url)
                                if (directLink != null) {
                                    progressMessage = "Downloading..."
                                    downloadVideo(context, directLink) { success, file ->
                                        isProcessing = false
                                        if (success && file != null) {
                                            // Save to Gallery/MediaStore
                                            val saved = MediaSaver.saveVideoToGallery(context, file, file.name)
                                            if (saved) {
                                                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Downloaded but failed to save to Gallery", Toast.LENGTH_SHORT).show()
                                            }
                                            showAd()
                                        } else {
                                            Toast.makeText(context, "Download Failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    isProcessing = false
                                    Toast.makeText(context, "Failed to extract video link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Video")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (url.isNotEmpty()) {
                            isProcessing = true
                            progressMessage = "Extracting Link..."
                            
                            coroutineScope.launch {
                                val directLink = VideoExtractor.extractVideoUrl(url)
                                if (directLink != null) {
                                    progressMessage = "Downloading for Conversion..."
                                    downloadVideo(context, directLink) { success, file ->
                                        if (success && file != null) {
                                            progressMessage = "Converting to MP3..."
                                            convertVideoToMp3(file) { conversionSuccess, audioFile ->
                                                if (conversionSuccess && audioFile != null) {
                                                    // Save Audio to MediaStore
                                                    val saved = MediaSaver.saveAudioToGallery(context, audioFile, audioFile.name)
                                                    isProcessing = false
                                                    if (saved) {
                                                        Toast.makeText(context, "MP3 Saved to Music!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Converted but failed to save", Toast.LENGTH_SHORT).show()
                                                    }
                                                    showAd()
                                                } else {
                                                    isProcessing = false
                                                    Toast.makeText(context, "Conversion Failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            isProcessing = false
                                            Toast.makeText(context, "Download Failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    isProcessing = false
                                    Toast.makeText(context, "Failed to extract video link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert to MP3")
                }
            }
        }

        // ... AdView ...
    }
}

fun downloadVideo(context: Context, url: String, callback: (Boolean, File?) -> Unit) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Video")
            .setDescription("Downloading video from SnapSave Pro")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        // Save to public Downloads directory directly (Scoped Storage friendly)
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Register receiver to listen for completion
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                try {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (downloadId == id) {
                        // Check if successful
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (statusIndex >= 0) {
                                val status = cursor.getInt(statusIndex)
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    // File is in Downloads folder
                                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                                    callback(true, file)
                                } else {
                                    callback(false, null)
                                }
                            } else {
                                callback(false, null)
                            }
                        } else {
                            callback(false, null)
                        }
                        cursor.close()
                        context.unregisterReceiver(this)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(false, null)
                    try { context.unregisterReceiver(this) } catch(e: Exception) {}
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        
        Toast.makeText(context, "Download Started...", Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Download Init Error: ${e.message}", Toast.LENGTH_LONG).show()
        callback(false, null)
    }
}

fun convertVideoToMp3(inputFile: File, callback: (Boolean, File?) -> Unit) {
    val outputFilePath = inputFile.absolutePath.replace(".mp4", ".mp3")
    val outputFile = File(outputFilePath)
    
    // Simple extraction command
    val command = "-i \"${inputFile.absolutePath}\" -vn -acodec libmp3lame -q:a 2 \"$outputFilePath\""
    
    FFmpegKit.executeAsync(command) { session ->
        if (session.returnCode.isValueSuccess) {
            callback(true, outputFile)
        } else {
            callback(false, null)
        }
    }
}
