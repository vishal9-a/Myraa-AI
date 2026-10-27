package com.myraa.assistant.forge

enum class ForgeStage(val displayName: String, val basePercent: Int) {
    PLANNING("Architecting Core Structure", 10),
    STRUCTURE("Writing Semantic HTML5", 25),
    STYLING("Compiling Tailwind & 3D CSS", 45),
    SECTIONS("Building Interactive Sections", 65),
    MOTION("Injecting Three.js & GSAP", 85),
    POLISH("Applying Visual Polish & Safety", 95),
    COMPLETE("Ready to Explore", 100)
}

object ForgeStageDetector {
    fun detect(partialHtml: String): ForgeStage {
        val lower = partialHtml.lowercase()
        return when {
            lower.contains("</html>") -> ForgeStage.COMPLETE
            lower.contains("three") || lower.contains("gsap") || lower.contains("requestanimationframe") -> ForgeStage.MOTION
            lower.contains("<section") || lower.contains("<main") || lower.contains("grid") -> ForgeStage.SECTIONS
            lower.contains("class=") || lower.contains("<style") || lower.contains("tailwind") -> ForgeStage.STYLING
            lower.contains("<body") || lower.contains("<head") -> ForgeStage.STRUCTURE
            else -> ForgeStage.PLANNING
        }
    }

    fun extractSections(partialHtml: String): List<String> {
        val sections = mutableListOf<String>()
        val regex = Regex("""<(section|div)[^>]*(id|class)=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(partialHtml)
        for (m in matches) {
            val name = m.groupValues[3].split(" ").firstOrNull { it.isNotBlank() } ?: "section"
            if (name.length in 3..25 && !sections.contains(name)) {
                sections.add(name)
            }
            if (sections.size >= 8) break
        }
        return sections
    }
}
