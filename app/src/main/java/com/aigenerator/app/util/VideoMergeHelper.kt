package com.aigenerator.app.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.UUID

object VideoMergeHelper {

    suspend fun mergeAndSaveVideosToGallery(
        context: Context,
        sourceUris: List<String>,
        outputFileName: String
    ): Uri? {
        val inputFiles = sourceUris.mapNotNull { resolveInputFile(context, it) }
        if (inputFiles.size != sourceUris.size) return null

        val outputFile = File(context.cacheDir, outputFileName)
        if (outputFile.exists()) outputFile.delete()

        mergeVideoFiles(inputFiles, outputFile)
        return saveFileToGallery(context, outputFile, outputFileName)
    }

    private fun resolveInputFile(context: Context, source: String): File? {
        val uri = Uri.parse(source)
        return when (uri.scheme) {
            "content" -> copyUriToTempFile(context, uri)
            "file" -> File(uri.path ?: return null).takeIf { it.exists() }
            "http", "https" -> downloadRemoteVideo(uri.toString(), context.cacheDir)
            else -> {
                val file = File(source)
                if (file.exists()) file else null
            }
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val tempFile = File(context.cacheDir, "video_merge_${UUID.randomUUID()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists()) tempFile else null
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadRemoteVideo(url: String, cacheDir: File): File? {
        return try {
            val request = Request.Builder().url(url).get().build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val tempFile = File(cacheDir, "video_download_${UUID.randomUUID()}.mp4")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists()) tempFile else null
        } catch (e: Exception) {
            null
        }
    }

    private fun mergeVideoFiles(inputFiles: List<File>, outputFile: File) {
        if (inputFiles.isEmpty()) throw IllegalArgumentException("No files to merge")

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val buffer = ByteBuffer.allocate(1 * 1024 * 1024)
        var outputTrackIndex = -1
        var presentationTimeUsOffset = 0L

        try {
            inputFiles.forEachIndexed { index, file ->
                val extractor = MediaExtractor()
                extractor.setDataSource(file.absolutePath)
                val trackIndex = findVideoTrack(extractor)
                if (trackIndex < 0) {
                    extractor.release()
                    throw IllegalArgumentException("No video track found in ${file.name}")
                }

                if (index == 0) {
                    outputTrackIndex = muxer.addTrack(extractor.getTrackFormat(trackIndex))
                    muxer.start()
                }

                extractor.selectTrack(trackIndex)
                var lastSampleTimeUs = 0L
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTimeUs = extractor.sampleTime
                    if (sampleTimeUs < 0) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.flags = extractor.sampleFlags
                    bufferInfo.presentationTimeUs = sampleTimeUs + presentationTimeUsOffset
                    muxer.writeSampleData(outputTrackIndex, buffer, bufferInfo)
                    lastSampleTimeUs = sampleTimeUs
                    extractor.advance()
                }

                presentationTimeUsOffset += lastSampleTimeUs + 2500
                extractor.release()
            }
        } finally {
            try {
                muxer.stop()
            } catch (_: Exception) {
            }
            try {
                muxer.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                return index
            }
        }
        return -1
    }

    private fun saveFileToGallery(context: Context, file: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AIGenerator")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { output ->
            FileInputStream(file).use { input ->
                input.copyTo(output)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        return uri
    }
}
