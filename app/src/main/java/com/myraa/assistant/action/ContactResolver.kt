package com.myraa.assistant.action

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

sealed class ContactLookupResult {
    data class Found(val displayName: String, val phoneNumber: String) : ContactLookupResult()
    object NoPermission : ContactLookupResult()
    object NotFound : ContactLookupResult()
}

object ContactResolver {
    private val PHONE_PATTERN = Regex("^[0-9+()\\-\\s]{5,}$")

    fun isPhoneNumber(query: String): Boolean {
        val digitsOnly = query.filter { it.isDigit() }
        return digitsOnly.length >= 5 && PHONE_PATTERN.matches(query.trim())
    }

    fun lookup(context: Context, query: String): ContactLookupResult {
        val q = query.trim()
        if (q.isEmpty()) return ContactLookupResult.NotFound

        if (isPhoneNumber(q)) {
            return ContactLookupResult.Found(displayName = q, phoneNumber = q)
        }

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ContactLookupResult.NoPermission
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$q%")

        val matches = mutableListOf<Pair<String, String>>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && matches.size < 5) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) else ""
                    if (name.isNotEmpty() && number.isNotEmpty()) {
                        matches.add(name to number)
                    }
                }
            }
        } catch (_: Exception) {
            return ContactLookupResult.NotFound
        }

        if (matches.isEmpty()) return ContactLookupResult.NotFound

        val target = q.lowercase()
        // Rank exact > prefix > contains
        val sorted = matches.sortedBy { (name, _) ->
            val n = name.lowercase()
            when {
                n == target -> 0
                n.startsWith(target) -> 1
                else -> 2
            }
        }

        val (bestName, bestNumber) = sorted.first()
        return ContactLookupResult.Found(displayName = bestName, phoneNumber = bestNumber)
    }
}
