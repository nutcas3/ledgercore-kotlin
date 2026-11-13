package com.example.ledgercore.security

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

class CachedBodyRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    val bodyBytes: ByteArray = request.inputStream.readBytes()

    override fun getInputStream(): ServletInputStream {
        val stream = ByteArrayInputStream(bodyBytes)
        return object : ServletInputStream() {
            override fun read(): Int = stream.read()
            override fun isFinished(): Boolean = stream.available() == 0
            override fun isReady(): Boolean = true
            override fun setReadListener(readListener: ReadListener?) = Unit
        }
    }

    override fun getReader(): BufferedReader =
        BufferedReader(InputStreamReader(inputStream, characterEncoding ?: Charsets.UTF_8.name()))
}
