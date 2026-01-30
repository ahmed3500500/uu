package com.snapsave.pro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object VideoExtractor {

    suspend fun extractVideoUrl(originalUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Check if it's already a direct link
                if (originalUrl.endsWith(".mp4", ignoreCase = true) || 
                    originalUrl.endsWith(".mkv", ignoreCase = true) ||
                    originalUrl.endsWith(".webm", ignoreCase = true)) {
                    return@withContext originalUrl
                }

                // Determine platform based on URL (Simple check)
                // Note: Real-world extraction often requires handling cookies, headers, and specific APIs.
                // This is a basic generic extractor using OpenGraph tags which works for many public videos.
                
                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                
                val doc = Jsoup.connect(originalUrl)
                    .userAgent(userAgent)
                    .timeout(10000)
                    .ignoreContentType(true) // Important: Allow parsing non-standard content types
                    .get()

                // Try to find og:video
                val ogVideo = doc.select("meta[property=og:video]").attr("content")
                if (ogVideo.isNotEmpty()) return@withContext ogVideo

                // Try to find og:video:secure_url
                val ogVideoSecure = doc.select("meta[property=og:video:secure_url]").attr("content")
                if (ogVideoSecure.isNotEmpty()) return@withContext ogVideoSecure
                
                // Twitter/X specific (sometimes uses twitter:player:stream)
                val twitterStream = doc.select("meta[name=twitter:player:stream]").attr("content")
                if (twitterStream.isNotEmpty()) return@withContext twitterStream

                // TikTok specific (Video URL often in scripts or JSON data - highly complex to scrape without API)
                // Attempting a generic script regex search for .mp4 URLs as a fallback
                val html = doc.html()
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
