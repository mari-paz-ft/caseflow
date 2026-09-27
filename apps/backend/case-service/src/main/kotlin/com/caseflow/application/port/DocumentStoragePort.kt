package com.caseflow.application.port

interface DocumentStoragePort {
    fun store(storageKey: String, content: ByteArray)
    fun read(storageKey: String): ByteArray
    fun delete(storageKey: String)
}
