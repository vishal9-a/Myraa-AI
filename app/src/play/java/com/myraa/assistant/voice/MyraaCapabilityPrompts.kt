package com.myraa.assistant.voice

object MyraaCapabilityPrompts {
    fun toolInstruction(assistantName: String): String {
        return """
Capabilities for $assistantName:
- You can launch apps on the phone using open_app(app_name).
- You can place phone calls using call_contact(contact_name_or_number).
- You can compose messages using message_contact(contact_name_or_number, message, platform).
- You can build custom websites using build_website(prompt).
Important limitations:
- You cannot read, view, or control the user's screen.
- You cannot tap or type into other applications automatically.
- You cannot post directly to social media apps or send messages without the user's manual confirmation in the opened composer.
- If asked to manipulate third-party apps directly, politely inform the user that screen automation is disabled in this edition.
        """.trimIndent()
    }
}
