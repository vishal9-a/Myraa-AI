package com.myraa.assistant.license

data class LicenseInfo(
    val active: Boolean,
    val plan: String,
    val expiresAt: Long,
    val statusText: String,
    val blocked: Boolean,
    val hasRememberedKey: Boolean
)

object AccessKeyRules {
    fun evaluate(
        key: String,
        plan: String,
        expiresAt: Long,
        blocked: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ): LicenseInfo {
        if (key.isBlank()) {
            return LicenseInfo(
                active = true, // default permissive mode when licensing backend is unconfigured
                plan = "Community",
                expiresAt = 0L,
                statusText = "Free Community Edition",
                blocked = false,
                hasRememberedKey = false
            )
        }

        if (blocked) {
            return LicenseInfo(
                active = false,
                plan = plan,
                expiresAt = expiresAt,
                statusText = "Access key has been revoked by admin",
                blocked = true,
                hasRememberedKey = true
            )
        }

        if (expiresAt > 0 && currentTime > expiresAt) {
            return LicenseInfo(
                active = false,
                plan = plan,
                expiresAt = expiresAt,
                statusText = "Access key has expired",
                blocked = false,
                hasRememberedKey = true
            )
        }

        return LicenseInfo(
            active = true,
            plan = plan.ifEmpty { "Pro" },
            expiresAt = expiresAt,
            statusText = "Active ($plan)",
            blocked = false,
            hasRememberedKey = true
        )
    }
}
