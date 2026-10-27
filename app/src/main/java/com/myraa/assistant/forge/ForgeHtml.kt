package com.myraa.assistant.forge

object ForgeHtml {
    fun stripFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```html")) {
            text = text.substring(7)
        } else if (text.startsWith("```")) {
            text = text.substring(3)
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length - 3)
        }
        return text.trim()
    }

    fun looksLikeHtml(content: String): Boolean {
        val lower = content.lowercase()
        return lower.contains("<html") || lower.contains("<!doctype") || (lower.contains("<head") && lower.contains("<body"))
    }

    fun repairTruncated(html: String): String {
        var result = stripFences(html)
        if (!result.contains("</html>", ignoreCase = true)) {
            if (!result.contains("</body>", ignoreCase = true)) {
                if (result.contains("<script", ignoreCase = true) && !result.contains("</script>", ignoreCase = true)) {
                    result += "\n</script>"
                }
                if (result.contains("<style", ignoreCase = true) && !result.contains("</style>", ignoreCase = true)) {
                    result += "\n</style>"
                }
                result += "\n</body>"
            }
            result += "\n</html>"
        }
        return result
    }

    fun withSafetyNet(html: String): String {
        val safetyNetScript = """
            <script id="myraa-safety-net">
            (function() {
                if (window.__myraa_safety_net_installed) return;
                window.__myraa_safety_net_installed = true;
                setTimeout(function() {
                    var elements = document.querySelectorAll('body *:not(canvas):not(script):not(style)');
                    for (var i = 0; i < elements.length; i++) {
                        var el = elements[i];
                        var style = window.getComputedStyle(el);
                        if (style.opacity === '0' || style.visibility === 'hidden') {
                            if (style.position !== 'fixed' && style.pointerEvents !== 'none' && !el.getAttribute('aria-hidden')) {
                                el.style.transition = 'opacity 0.6s ease';
                                el.style.opacity = '1';
                                el.style.visibility = 'visible';
                            }
                        }
                    }
                }, 1400);
            })();
            </script>
        """.trimIndent()

        return if (html.contains("</body>", ignoreCase = true)) {
            html.replace("</body>", "$safetyNetScript\n</body>")
        } else {
            "$html\n$safetyNetScript"
        }
    }
}
