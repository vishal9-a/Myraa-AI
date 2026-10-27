package com.myraa.assistant.action

import org.json.JSONArray

object MyraaToolCatalog {
    fun declarations(): JSONArray {
        val array = JSONArray()

        // open_app
        array.put(
            MyraaToolHelper.functionDecl(
                name = "open_app",
                description = "Launches an installed Android app by name or label on the device.",
                properties = mapOf("app_name" to "Name of the app to launch (e.g., YouTube, WhatsApp, Camera)"),
                required = listOf("app_name")
            )
        )

        // call_contact
        array.put(
            MyraaToolHelper.functionDecl(
                name = "call_contact",
                description = "Places a phone call to a saved contact name or direct phone number.",
                properties = mapOf("contact_name_or_number" to "Contact display name or phone digits to call"),
                required = listOf("contact_name_or_number")
            )
        )

        // message_contact
        array.put(
            MyraaToolHelper.functionDecl(
                name = "message_contact",
                description = "Prepares and opens a message composer for SMS or WhatsApp. The user reviews and sends.",
                properties = mapOf(
                    "contact_name_or_number" to "Contact name or phone number",
                    "message" to "Text content of the message",
                    "platform" to "Messaging platform: 'sms' or 'whatsapp' (default 'whatsapp')"
                ),
                required = listOf("contact_name_or_number", "message")
            )
        )

        // build_website
        array.put(
            MyraaToolHelper.functionDecl(
                name = "build_website",
                description = "Invokes the 3D Website Forge reactor to design, stream, and render a complete animated website based on a description.",
                properties = mapOf("prompt" to "Detailed description of the website to build (e.g. 'a modern cozy cafe website in Hindi and English with menu and booking')"),
                required = listOf("prompt")
            )
        )

        // Companion Vision & Memory Tools
        array.put(
            MyraaToolHelper.functionDecl(
                name = "look_at_screen",
                description = "Uses Screen Vision to see what is currently open on the user's phone screen and understand the content, layout, or buttons.",
                properties = mapOf("question" to "What specific detail to look for on the screen"),
                required = emptyList()
            )
        )

        array.put(
            MyraaToolHelper.functionDecl(
                name = "look_at_camera",
                description = "Activates Camera Sight on user's request ('Myraa, mala bagh' / 'look at me') to see the user or what's in front of the phone.",
                properties = mapOf(
                    "camera" to "Which camera to use: 'front' (default, to see the user) or 'back' (to see surroundings)"
                ),
                required = emptyList()
            )
        )

        array.put(
            MyraaToolHelper.functionDecl(
                name = "remember_fact",
                description = "Permanently stores a personal fact, habit, routine, relationship, or preference about the user into long-term memory.",
                properties = mapOf(
                    "key" to "Topic or descriptor of the memory (e.g. 'favorite_coffee', 'brother_name')",
                    "value" to "Detail to remember",
                    "category" to "Category: 'PERSONAL', 'PREFERENCE', 'ROUTINE', 'RELATIONSHIP', 'GOAL', 'WORK', or 'PROMISE'"
                ),
                required = listOf("key", "value")
            )
        )

        array.put(
            MyraaToolHelper.functionDecl(
                name = "recall_memory",
                description = "Searches Myraa's permanent memory for saved personal facts, preferences, or conversation episodes.",
                properties = mapOf("query" to "Search topic or question"),
                required = listOf("query")
            )
        )

        array.put(
            MyraaToolHelper.functionDecl(
                name = "forget_memory",
                description = "Deletes a specific remembered topic or wipes all remembered data if user requests.",
                properties = mapOf("topic" to "Topic key to forget, or 'all' to wipe everything"),
                required = listOf("topic")
            )
        )

        array.put(
            MyraaToolHelper.functionDecl(
                name = "read_notifications",
                description = "Reads or summarizes the user's recent incoming notifications, messages, and app alerts.",
                properties = mapOf("limit" to "Maximum number of notifications to read (default 5)"),
                required = emptyList()
            )
        )

        return array
    }
}
