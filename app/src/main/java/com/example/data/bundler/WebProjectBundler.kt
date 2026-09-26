package com.example.data.bundler

import com.example.data.model.ProjectFileEntity

object WebProjectBundler {

    private const val DEVTOOLS_CONSOLE_BRIDGE = """
<script id="__hopweb_devtools_bridge__">
(function() {
  function serialize(arg) {
    if (arg === null) return "null";
    if (arg === undefined) return "undefined";
    if (typeof arg === "object") {
      try {
        return JSON.stringify(arg, null, 2);
      } catch (e) {
        return Object.prototype.toString.call(arg);
      }
    }
    return String(arg);
  }

  function sendLog(level, args, line) {
    var message = Array.prototype.slice.call(args).map(serialize).join(" ");
    if (window.HopWebBridge && typeof window.HopWebBridge.onConsoleMessage === "function") {
      window.HopWebBridge.onConsoleMessage(level, message, line || 0);
    }
  }

  var origLog = console.log;
  var origInfo = console.info;
  var origWarn = console.warn;
  var origError = console.error;

  console.log = function() {
    sendLog("LOG", arguments);
    if (origLog) origLog.apply(console, arguments);
  };
  console.info = function() {
    sendLog("INFO", arguments);
    if (origInfo) origInfo.apply(console, arguments);
  };
  console.warn = function() {
    sendLog("WARN", arguments);
    if (origWarn) origWarn.apply(console, arguments);
  };
  console.error = function() {
    sendLog("ERROR", arguments);
    if (origError) origError.apply(console, arguments);
  };

  window.onerror = function(message, source, lineno, colno, error) {
    var desc = message + (lineno ? " (Line: " + lineno + ")" : "");
    if (window.HopWebBridge && typeof window.HopWebBridge.onConsoleMessage === "function") {
      window.HopWebBridge.onConsoleMessage("ERROR", desc, lineno || 0);
    }
    return false;
  };

  window.addEventListener("unhandledrejection", function(event) {
    var reason = event.reason ? (event.reason.message || String(event.reason)) : "Unhandled Promise Rejection";
    if (window.HopWebBridge && typeof window.HopWebBridge.onConsoleMessage === "function") {
      window.HopWebBridge.onConsoleMessage("ERROR", "Promise Rejection: " + reason, 0);
    }
  });
})();
</script>
"""

    /**
     * Inlines CSS, JavaScript, and devtools bridge to make a self-contained HTML payload
     * that runs reliably offline in Android WebView.
     */
    fun bundleProjectForPreview(
        files: List<ProjectFileEntity>,
        injectDevTools: Boolean = true
    ): String {
        val entryFile = files.find { it.isEntry || it.name.equals("index.html", ignoreCase = true) }
            ?: files.find { it.extension.equals("html", ignoreCase = true) }
            ?: return createFallbackHtml(files)

        var html = entryFile.content
        val filesByName = files.associateBy { it.name }

        // Replace <link rel="stylesheet" href="..."> with inline <style>
        val linkRegex = Regex("""<link\s+[^>]*rel=["']stylesheet["'][^>]*href=["']([^"']+)["'][^>]*>|<link\s+[^>]*href=["']([^"']+)["'][^>]*rel=["']stylesheet["'][^>]*>""", RegexOption.IGNORE_CASE)
        html = linkRegex.replace(html) { matchResult ->
            val href = matchResult.groups[1]?.value ?: matchResult.groups[2]?.value ?: ""
            val cleanName = href.substringAfterLast("/")
            val cssFile = filesByName[cleanName] ?: filesByName[href]
            if (cssFile != null) {
                "<style data-hopweb-file=\"$cleanName\">\n${cssFile.content}\n</style>"
            } else {
                matchResult.value // leave untouched if external URL or missing
            }
        }

        // If there is style.css in files but not linked in index.html, inject it
        filesByName["style.css"]?.let { styleFile ->
            if (!html.contains("data-hopweb-file=\"style.css\"") && !html.contains(styleFile.content)) {
                html = injectBeforeOrEnd(html, "</head>", "<style data-hopweb-file=\"style.css\">\n${styleFile.content}\n</style>")
            }
        }

        // Replace <script src="..."> with inline <script>
        val scriptRegex = Regex("""<script\s+[^>]*src=["']([^"']+)["'][^>]*>\s*</script>""", RegexOption.IGNORE_CASE)
        html = scriptRegex.replace(html) { matchResult ->
            val src = matchResult.groups[1]?.value ?: ""
            val cleanName = src.substringAfterLast("/")
            val jsFile = filesByName[cleanName] ?: filesByName[src]
            if (jsFile != null) {
                "<script data-hopweb-file=\"$cleanName\">\n${jsFile.content}\n</script>"
            } else {
                matchResult.value // leave untouched if external CDN script
            }
        }

        // If there is script.js in files but not linked in index.html, inject it
        filesByName["script.js"]?.let { scriptFile ->
            if (!html.contains("data-hopweb-file=\"script.js\"") && !html.contains(scriptFile.content)) {
                html = injectBeforeOrEnd(html, "</body>", "<script data-hopweb-file=\"script.js\">\n${scriptFile.content}\n</script>")
            }
        }

        // Inject viewport meta if not present
        if (!html.contains("name=\"viewport\"", ignoreCase = true) && !html.contains("name='viewport'", ignoreCase = true)) {
            val viewportMeta = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n"
            html = injectBeforeOrEnd(html, "<head>", viewportMeta, injectAfter = true)
        }

        // Inject DevTools console bridge
        if (injectDevTools) {
            html = if (html.contains("<head>", ignoreCase = true)) {
                injectBeforeOrEnd(html, "<head>", DEVTOOLS_CONSOLE_BRIDGE, injectAfter = true)
            } else {
                DEVTOOLS_CONSOLE_BRIDGE + "\n" + html
            }
        }

        return html
    }

    private fun injectBeforeOrEnd(source: String, targetTag: String, contentToInject: String, injectAfter: Boolean = false): String {
        val index = source.indexOf(targetTag, ignoreCase = true)
        if (index == -1) {
            return source + "\n" + contentToInject
        }
        return if (injectAfter) {
            val insertPos = index + targetTag.length
            source.substring(0, insertPos) + "\n" + contentToInject + "\n" + source.substring(insertPos)
        } else {
            source.substring(0, index) + "\n" + contentToInject + "\n" + source.substring(index)
        }
    }

    private fun createFallbackHtml(files: List<ProjectFileEntity>): String {
        val cssContent = files.filter { it.extension == "css" }.joinToString("\n") { it.content }
        val jsContent = files.filter { it.extension == "js" }.joinToString("\n") { it.content }
        val htmlContent = files.filter { it.extension == "html" }.joinToString("\n") { it.content }

        return """<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
  $cssContent
  </style>
  $DEVTOOLS_CONSOLE_BRIDGE
</head>
<body>
  $htmlContent
  <script>
  $jsContent
  </script>
</body>
</html>"""
    }
}
