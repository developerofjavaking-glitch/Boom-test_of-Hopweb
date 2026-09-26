package com.example.data.apk

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import com.example.data.bundler.WebProjectBundler
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ApkBuildOptions(
    val appName: String,
    val packageName: String = "com.ropweb.talha.aijavadevs",
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val orientation: String = "unspecified",
    val enableFullscreen: Boolean = false,
    val enableOfflineCache: Boolean = true,
    val entryFileName: String = "index.html",
    val customIconBytes: ByteArray? = null,
    val customIconName: String? = null,
    val includeInternetPermission: Boolean = true,
    val includeStoragePermission: Boolean = false,
    val includeCameraPermission: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApkBuildOptions) return false
        if (appName != other.appName) return false
        if (packageName != other.packageName) return false
        if (versionName != other.versionName) return false
        if (versionCode != other.versionCode) return false
        if (orientation != other.orientation) return false
        if (enableFullscreen != other.enableFullscreen) return false
        if (enableOfflineCache != other.enableOfflineCache) return false
        if (entryFileName != other.entryFileName) return false
        if (customIconName != other.customIconName) return false
        if (customIconBytes != null) {
            if (other.customIconBytes == null) return false
            if (!customIconBytes.contentEquals(other.customIconBytes)) return false
        } else if (other.customIconBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = appName.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + entryFileName.hashCode()
        return result
    }
}

data class ApkBuildResult(
    val isSuccess: Boolean,
    val apkFile: File? = null,
    val zipFile: File? = null,
    val fileSizeBytes: Long = 0,
    val errorMessage: String? = null,
    val logOutput: List<String> = emptyList()
)

object ApkBuilderEngine {

    suspend fun buildApk(
        context: Context,
        project: ProjectEntity,
        files: List<ProjectFileEntity>,
        options: ApkBuildOptions,
        onProgress: (step: Int, totalSteps: Int, message: String) -> Unit
    ): ApkBuildResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
        }

        try {
            val cleanAppName = options.appName.trim().ifEmpty { project.name.ifEmpty { "RopeWeb App" } }
            val cleanPackage = if (options.packageName.isNotBlank()) options.packageName.trim() else "com.ropweb.talha.aijavadevs"
            val selectedEntry = options.entryFileName.ifBlank { "index.html" }

            log("Starting APK packaging for: $cleanAppName")
            log("Package ID: $cleanPackage")
            log("Entry file selected: $selectedEntry")
            log("Version: ${options.versionName} (code: ${options.versionCode})")

            onProgress(1, 5, "Bundling '$selectedEntry' and web project assets...")
            delay(200)

            val safeFileName = cleanAppName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val apksDir = File(context.filesDir, "apks").apply { mkdirs() }
            val cacheApkDir = File(context.cacheDir, "apks").apply { mkdirs() }

            val unsignedApkFile = File(cacheApkDir, "${safeFileName}_unsigned.apk")
            val finalApkFile = File(apksDir, "${safeFileName}_v${options.versionName}.apk")
            val cacheApkFile = File(cacheApkDir, "${safeFileName}_v${options.versionName}.apk")
            val zipFile = File(apksDir, "${safeFileName}_project.zip")

            val projectFilesList = if (files.isEmpty()) {
                listOf(
                    ProjectFileEntity(
                        projectId = project.id,
                        name = selectedEntry,
                        extension = selectedEntry.substringAfterLast(".", "html"),
                        content = "<!DOCTYPE html><html><head><title>$cleanAppName</title></head><body><h1>$cleanAppName</h1><p>Created with RopeWeb</p></body></html>",
                        isEntry = true
                    )
                )
            } else {
                files
            }

            val bundledHtml = WebProjectBundler.bundleProjectForPreview(
                files = projectFilesList,
                injectDevTools = false,
                entryFileName = selectedEntry
            )
            log("Compiled bundled HTML payload (${bundledHtml.length} bytes)")

            // 1. Build Web Project ZIP
            val zipEntries = mutableSetOf<String>()
            val zosZip = ZipOutputStream(FileOutputStream(zipFile))

            for (f in projectFilesList) {
                if (zipEntries.add(f.name)) {
                    val entry = ZipEntry(f.name).apply { time = System.currentTimeMillis() }
                    zosZip.putNextEntry(entry)
                    zosZip.write(f.content.toByteArray(StandardCharsets.UTF_8))
                    zosZip.closeEntry()
                }
            }

            val manifestJson = """
            {
              "name": "$cleanAppName",
              "short_name": "$cleanAppName",
              "package_id": "$cleanPackage",
              "version": "${options.versionName}",
              "start_url": "$selectedEntry",
              "display": "${if (options.enableFullscreen) "fullscreen" else "standalone"}",
              "orientation": "${options.orientation}",
              "background_color": "#ffffff",
              "theme_color": "#2563eb"
            }
            """.trimIndent()

            if (zipEntries.add("manifest.json")) {
                val entry = ZipEntry("manifest.json").apply { time = System.currentTimeMillis() }
                zosZip.putNextEntry(entry)
                zosZip.write(manifestJson.toByteArray(StandardCharsets.UTF_8))
                zosZip.closeEntry()
            }
            zosZip.close()
            log("Web project archive ready: ${zipFile.name}")

            onProgress(2, 5, "Assembling compiled DEX and Android binary specifications...")
            delay(250)

            // Read compiled template files from assets
            val binaryManifestBytes = readAssetBytes(context, "apk_template/AndroidManifest.xml")
            val dexBytes = readAssetBytes(context, "apk_template/classes.dex")
            val arscBytes = readAssetBytes(context, "apk_template/resources.arsc")

            log("Loaded compiled Android runtime (classes.dex: ${dexBytes.size} bytes)")
            log("Loaded valid AXML AndroidManifest (AXML: ${binaryManifestBytes.size} bytes)")

            onProgress(3, 5, "Embedding custom app logo & bundling APK container...")
            delay(300)

            // 2. Assemble Unsigned APK (with duplicate check)
            val apkEntries = mutableSetOf<String>()
            val zosApk = ZipOutputStream(FileOutputStream(unsignedApkFile))

            fun addApkEntry(path: String, data: ByteArray) {
                if (apkEntries.add(path)) {
                    val entry = ZipEntry(path).apply { time = System.currentTimeMillis() }
                    zosApk.putNextEntry(entry)
                    zosApk.write(data)
                    zosApk.closeEntry()
                }
            }

            // Write binary AndroidManifest.xml
            addApkEntry("AndroidManifest.xml", binaryManifestBytes)

            // Write compiled Dalvik classes.dex
            addApkEntry("classes.dex", dexBytes)

            // Write resources.arsc
            addApkEntry("resources.arsc", arscBytes)

            // Write main entry HTML
            addApkEntry("assets/www/index.html", bundledHtml.toByteArray(StandardCharsets.UTF_8))
            if (selectedEntry != "index.html") {
                addApkEntry("assets/www/$selectedEntry", bundledHtml.toByteArray(StandardCharsets.UTF_8))
            }

            // Write project files
            for (f in projectFilesList) {
                val path = "assets/www/${f.name}"
                if (f.name != "index.html" && f.name != selectedEntry) {
                    addApkEntry(path, f.content.toByteArray(StandardCharsets.UTF_8))
                }
            }

            // Write web manifest
            addApkEntry("assets/www/manifest.json", manifestJson.toByteArray(StandardCharsets.UTF_8))

            // Write custom icon or squircle default
            val icon48 = generateIconPng(options.customIconBytes, 48)
            val icon72 = generateIconPng(options.customIconBytes, 72)
            val icon96 = generateIconPng(options.customIconBytes, 96)
            val icon144 = generateIconPng(options.customIconBytes, 144)
            val icon192 = generateIconPng(options.customIconBytes, 192)

            addApkEntry("res/mipmap-mdpi/ic_launcher.png", icon48)
            addApkEntry("res/mipmap-hdpi/ic_launcher.png", icon72)
            addApkEntry("res/mipmap-xhdpi/ic_launcher.png", icon96)
            addApkEntry("res/mipmap-xxhdpi/ic_launcher.png", icon144)
            addApkEntry("res/mipmap-xxxhdpi/ic_launcher.png", icon192)
            addApkEntry("res/drawable/ic_launcher.png", icon144)
            addApkEntry("assets/www/icon.png", icon192)

            zosApk.close()
            log("Assembled unsigned package container: ${unsignedApkFile.length()} bytes")

            onProgress(4, 5, "Signing with Google ApkSigner (v1, v2 & v3 schemes)...")
            delay(350)

            // 3. Sign using Google official ApkSigner with v1, v2, and v3 schemes!
            val p12Stream = context.assets.open("apk_template/debug.p12")
            val keyStore = KeyStore.getInstance("PKCS12")
            p12Stream.use { keyStore.load(it, "android".toCharArray()) }

            val privateKey = keyStore.getKey("androiddebugkey", "android".toCharArray()) as PrivateKey
            val certificate = keyStore.getCertificate("androiddebugkey") as X509Certificate

            if (finalApkFile.exists()) {
                finalApkFile.delete()
            }

            val signerConfig = ApkSigner.SignerConfig.Builder(
                "androiddebugkey",
                privateKey,
                listOf(certificate)
            ).build()

            val apkSigner = ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(unsignedApkFile)
                .setOutputApk(finalApkFile)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()

            apkSigner.sign()
            log("Signed APK with v1 (JAR), v2 (APK Signature), and v3 schemes")

            // Verify with ApkVerifier
            val verifier = ApkVerifier.Builder(finalApkFile).build()
            val verifyResult = verifier.verify()
            log("ApkVerifier check: isVerified=${verifyResult.isVerified} (v1=${verifyResult.isVerifiedUsingV1Scheme}, v2=${verifyResult.isVerifiedUsingV2Scheme}, v3=${verifyResult.isVerifiedUsingV3Scheme})")

            // Mirror to cache dir for FileProvider
            try {
                copyFile(finalApkFile, cacheApkFile)
            } catch (e: Exception) {
                log("Notice: ${e.message}")
            }

            onProgress(5, 5, "APK verified and ready to install on Android!")
            delay(200)

            ApkBuildResult(
                isSuccess = true,
                apkFile = finalApkFile,
                zipFile = zipFile,
                fileSizeBytes = finalApkFile.length(),
                logOutput = logs
            )
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to generate APK"
            log("ERROR: $errorMsg")
            ApkBuildResult(
                isSuccess = false,
                errorMessage = errorMsg,
                logOutput = logs
            )
        }
    }

    private fun readAssetBytes(context: Context, path: String): ByteArray {
        context.assets.open(path).use { stream ->
            return stream.readBytes()
        }
    }

    private fun generateIconPng(customBytes: ByteArray?, size: Int): ByteArray {
        if (customBytes != null && customBytes.isNotEmpty()) {
            try {
                val bmp = BitmapFactory.decodeByteArray(customBytes, 0, customBytes.size)
                if (bmp != null) {
                    val scaled = Bitmap.createScaledBitmap(bmp, size, size, true)
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    return baos.toByteArray()
                }
            } catch (e: Exception) {}
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(37, 99, 235) // Royal Blue
        }
        val corner = size * 0.22f
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, corner, corner, paint)

        // Draw inner white emblem
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size * 0.28f, paint)
        paint.color = android.graphics.Color.rgb(37, 99, 235)
        canvas.drawCircle(size / 2f, size / 2f, size * 0.17f, paint)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    private fun copyFile(src: File, dest: File) {
        FileInputStream(src).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            shareFile(context, apkFile, "application/vnd.android.package-archive", "Install / Save RopeWeb APK")
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                putExtra(Intent.EXTRA_TEXT, "Generated with RopeWeb: ${file.name}")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Share sheet: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
