package biz.pock.coursebookingapp.data.api.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LoggingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Timber.d(">>> HTTP Request: ${request.url}")
        val requestBody = request.body

        // Request Logging
        val requestLog = buildString {
            appendLine(">>> ┌─── HTTP Request ───────────────────────────")
            appendLine(">>> │ URL: ${request.url}")
            appendLine(">>> │ Method: ${request.method}")
            appendLine(">>> │ Headers:")
            request.headers.forEach { (name, value) ->
                appendLine(">>> │   $name: $value")
            }

            if (requestBody != null) {
                appendLine(">>> │ Body:")
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                buffer.readUtf8().lines().forEach { line ->
                    appendLine(">>> │   $line")
                }
            }
            appendLine(">>> └────────────────────────────────────────────")
        }
        Timber.d(requestLog)

        // Request ausführen und Zeit berechnen
        val startTime = System.nanoTime()
        val response = chain.proceed(request)
        val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

        // Response Logging
        val responseLog = buildString {
            appendLine(">>> ┌─── HTTP Response ──────────────────────────")
            appendLine(">>> │ Duration: ${duration}ms")
            appendLine(">>> │ Status: ${response.code} ${response.message}")
            appendLine(">>> │ Headers:")
            response.headers.forEach { (name, value) ->
                appendLine(">>> │   $name: $value")
            }

            if (response.body != null) {
                appendLine(">>> │ Body:")
                val responseBody = response.peekBody(Long.MAX_VALUE)
                responseBody.string().lines().forEach { line ->
                    appendLine(">>> │   $line")
                }
            }
            appendLine(">>> └────────────────────────────────────────────")
        }
        Timber.d(responseLog)

        return response
    }
}