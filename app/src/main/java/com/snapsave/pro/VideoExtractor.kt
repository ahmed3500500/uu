package com.snapsave.pro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest

object VideoExtractor {

    suspend fun extractVideoUrl(originalUrl: String): String? {
        return withContext(Dispatchers.IO) {
            // 1. Try youtubedl-android (The "Radical Solution" for all sites)
            try {
                val request = YoutubeDLRequest(originalUrl)
                // Get the best single file video/audio (avoiding DASH/HLS manifests if possible for simple download)
                // or just "best". 'best' usually returns the best quality.
                // For direct playback/download without ffmpeg merge on client, we might want 'best[ext=mp4]/best'
                request.addOption("-f", "b/best") 
                // We don't want to download, just get info
                // getInfo will fetch the URL.
                
                val streamInfo = YoutubeDL.getInstance().getInfo(request)
                if (!streamInfo.url.isNullOrEmpty()) {
                    return@withContext streamInfo.url
                }
            } catch (e: Exception) {
                // YoutubeDL failed, fall back to Jsoup
                e.printStackTrace()
            }

            try {
                // Check if it's already a direct link
                if (originalUrl.endsWith(".mp4", ignoreCase = true) || 
                    originalUrl.endsWith(".mkv", ignoreCase = true) ||
                    originalUrl.endsWith(".webm", ignoreCase = true)) {
                    return@withContext originalUrl
                }

                // Better User Agent (Desktop often gives better meta tags for Facebook)
                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
                
                // Allow redirect handling
                val connection = Jsoup.connect(originalUrl)
                    .userAgent(userAgent)
                    .timeout(15000)
                    .ignoreContentType(true)
                    .followRedirects(true)
                
                val response = connection.execute()
                val doc = response.parse()
                val finalUrl = response.url().toString()
                val html = doc.html()

                // --- Specialized Facebook Logic ---
                if (finalUrl.contains("facebook.com") || finalUrl.contains("fb.watch")) {
                    // Try to find HD url first
                    val hdUrlRegex = """(hd_src|browser_native_hd_url)"\s*:\s*"([^"]+)"""".toRegex()
                    val hdMatch = hdUrlRegex.find(html)
                    if (hdMatch != null && hdMatch.groupValues.size > 2) {
                        var url = hdMatch.groupValues[2].replace("\\/", "/")
                        return@withContext url
                    }

                    // Try to find SD url
                    val sdUrlRegex = """(sd_src|browser_native_sd_url)"\s*:\s*"([^"]+)"""".toRegex()
                    val sdMatch = sdUrlRegex.find(html)
                    if (sdMatch != null && sdMatch.groupValues.size > 2) {
                        var url = sdMatch.groupValues[2].replace("\\/", "/")
                        return@withContext url
                    }
                }
                
                // --- General OpenGraph Logic (Fallback) ---
                // Try to find og:video
                val ogVideo = doc.select("meta[property=og:video]").attr("content")
                if (ogVideo.isNotEmpty()) return@withContext ogVideo

                // Try to find og:video:secure_url
                val ogVideoSecure = doc.select("meta[property=og:video:secure_url]").attr("content")
                if (ogVideoSecure.isNotEmpty()) return@withContext ogVideoSecure
                
                // Twitter/X specific
                val twitterStream = doc.select("meta[name=twitter:player:stream]").attr("content")
                if (twitterStream.isNotEmpty()) return@withContext twitterStream

                // Generic MP4 Regex Fallback (Last resort)
                val mp4Regex = """https?://[^"']+\.mp4""".toRegex()
                val match = mp4Regex.find(html)
                if (match != null) return@withContext match.value

                return@withContext null
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}
