package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.bundler.WebProjectBundler
import com.example.data.model.ProjectFileEntity
import org.junit.Assert.assertEquals
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
        assertEquals("HopWeb Studio", appName)
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
}
