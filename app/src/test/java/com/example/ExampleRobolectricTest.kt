package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.apk.ApkBuildOptions
import com.example.data.apk.ApkBuilderEngine
import com.example.data.bundler.WebProjectBundler
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFileEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RopeWeb", appName)
    }

    @Test
    fun `bundler correctly merges html css and js`() {
        val htmlFile = ProjectFileEntity(
            projectId = "p1",
            name = "index.html",
            extension = "html",
            content = "<html><head><link rel=\"stylesheet\" href=\"style.css\"></head><body><h1>Hello</h1><script src=\"script.js\"></script></body></html>",
            isEntry = true
        )
        val cssFile = ProjectFileEntity(
            projectId = "p1",
            name = "style.css",
            extension = "css",
            content = "h1 { color: red; }",
            isEntry = false
        )
        val jsFile = ProjectFileEntity(
            projectId = "p1",
            name = "script.js",
            extension = "js",
            content = "console.log('test');",
            isEntry = false
        )

        val bundled = WebProjectBundler.bundleProjectForPreview(listOf(htmlFile, cssFile, jsFile))
        assertTrue(bundled.contains("h1 { color: red; }"))
        assertTrue(bundled.contains("console.log('test');"))
        assertTrue(bundled.contains("HopWebBridge"))
    }

    @Test
    fun `apk builder generates valid signed apk file`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val project = ProjectEntity(
            id = "proj123",
            name = "RopeWeb Test App",
            packageName = "com.ropweb.talha.aijavadevs"
        )
        val files = listOf(
            ProjectFileEntity(
                projectId = "proj123",
                name = "index.html",
                extension = "html",
                content = "<html><body><h1>Test</h1></body></html>",
                isEntry = true
            ),
            ProjectFileEntity(
                projectId = "proj123",
                name = "style.css",
                extension = "css",
                content = "body { background: white; }",
                isEntry = false
            ),
            ProjectFileEntity(
                projectId = "proj123",
                name = "script.js",
                extension = "js",
                content = "console.log('RopeWeb');",
                isEntry = false
            )
        )

        val result = ApkBuilderEngine.buildApk(
            context = context,
            project = project,
            files = files,
            options = ApkBuildOptions(
                appName = "RopeWeb Test App",
                packageName = "com.ropweb.talha.aijavadevs"
            )
        ) { _, _, _ -> }

        assertTrue("Build should succeed: ${result.errorMessage}", result.isSuccess)
        assertNotNull(result.apkFile)
        assertTrue(result.apkFile!!.exists())
        assertTrue("APK size should be > 0 bytes", result.apkFile!!.length() > 0)
    }
}
