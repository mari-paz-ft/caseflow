package com.caseflow.domain.rule

import java.nio.charset.StandardCharsets.US_ASCII
import java.security.MessageDigest

object PdfDocumentRules {
    const val MAX_SIZE_BYTES = 5L * 1024 * 1024
    private val signature = "%PDF-".toByteArray(US_ASCII)

    fun hasPdfMime(contentType: String?): Boolean =
        contentType?.substringBefore(';')?.trim()?.equals("application/pdf", ignoreCase = true) == true

    fun hasPdfSignature(content: ByteArray): Boolean =
        content.size >= signature.size && content.copyOfRange(0, signature.size).contentEquals(signature)

    fun isAcceptableUpload(contentType: String?, content: ByteArray): Boolean =
        hasPdfMime(contentType) && content.size.toLong() <= MAX_SIZE_BYTES && hasPdfSignature(content)

    fun sha256(content: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(content)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
