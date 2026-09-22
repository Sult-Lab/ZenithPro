package com.techsultan.zenithpro.core.util

object ErrorSanitizer {

    fun clean(raw: String?): String {
        if (raw.isNullOrBlank()) return "Something went wrong. Please try again."

        if (raw.contains("invalid_credentials", ignoreCase = true)) {
            return "Invalid email or password. Please try again."
        }

        if (raw.contains("validation_failed", ignoreCase = true) || raw.contains("invalid format", ignoreCase = true)) {
            return "Unable to validate email address. Please check the format and try again."
        }

        // Already clean messages — pass through
        if (raw.length < 100 && !containsInternals(raw)) return raw

        // Contains Supabase/Postgres internals — replace
        return "Something went wrong. Please try again."
    }

    private fun containsInternals(message: String): Boolean {
        val internals = listOf(
            "supabase", "postgres", "pg_", "relation \"",
            "column \"", "ERROR:", "DETAIL:", "HINT:",
            "42P", "23505", "23503", "supabase.co",
            "edge-runtime", "deno", "stack trace",
        )
        return internals.any { message.lowercase().contains(it) }
    }
}