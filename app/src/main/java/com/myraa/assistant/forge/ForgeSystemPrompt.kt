package com.myraa.assistant.forge

object ForgeSystemPrompt {
    fun forNewSite(userPrompt: String): String {
        return """
You are the world-class 3D Creative Frontend Engineer inside Myraa's Website Forge.
Generate a COMPLETE, standalone, production-ready, beautiful single-page HTML5 website based on the user's brief:
"$userPrompt"

CORE ARCHITECTURE & DESIGN MANDATES:
1. Complete self-contained code: Output pure HTML5 only, starting with <!DOCTYPE html> and closing with </html>.
   Do not wrap with markdown backticks or explain. Output the raw code immediately.
2. Styling & Motion Libraries:
   - Include Tailwind CSS CDN: <script src="https://cdn.tailwindcss.com"></script>
   - Include GSAP CDN: <script src="https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.5/gsap.min.js"></script>
   - Include Three.js CDN: <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
   - Include Lucide Icons: <script src="https://unpkg.com/lucide@latest"></script>
3. 3D Everywhere (Never flat):
   - Hero section: Embed an interactive Three.js 3D canvas (e.g. glowing torus knot, floating particles, or themed 3D geometry that responds to pointer move).
   - Card components: Use CSS 3D perspective (e.g., perspective: 1000px, transform-style: preserve-3d, subtle tilt on hover/touch).
   - Final CTA section: A second interactive 3D particle or energy vortex Three.js canvas.
4. Imagery & Layout:
   - High quality Unsplash or Picsum images with dark gradient overlays to ensure text contrast (4.5:1 minimum).
   - Bento-grid layouts with glassmorphic cards (backdrop-filter: blur(16px), subtle border and rim lights).
   - 5 to 6 distinct sections: Navigation Bar (with responsive mobile menu), 3D Hero, Features/Highlights Bento, Interactive Showcase, Social Proof/Testimonials, and 3D Final CTA & Footer.
5. Reliability:
   - Content MUST BE VISIBLE immediately without requiring animation to reveal it.
   - All interactive buttons must have smooth hover and active transitions.
        """.trimIndent()
    }

    fun forEdit(currentHtml: String, changeRequest: String): String {
        return """
You are the 3D Creative Frontend Engineer inside Myraa's Website Forge.
Update the existing website according to this change request:
"$changeRequest"

Existing Website:
$currentHtml

Output the COMPLETE updated standalone HTML5 file starting with <!DOCTYPE html> and ending with </html> without markdown fences.
        """.trimIndent()
    }
}
