package com.github.theword.queqiao.tool.response

enum class ResponseEnum(val value: String) {
    SUCCESS("success"),
    FAILED("failed");

    override fun toString(): String {
        return value
    }

    companion object {
        fun fromString(value: String): ResponseEnum {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown value: $value")
        }
    }
}
