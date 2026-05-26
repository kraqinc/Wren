package com.wren.ide.core.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkClient {
    // Default emulator local loopback mapping to localhost backend
    private const val BASE_URL = "http://10.0.2.2:3000/api"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        
    private val gson = Gson()
    private var token: String? = null

    fun setAuthToken(newToken: String?) {
        token = newToken
    }

    fun getAuthToken(): String? = token

    fun getGson(): Gson = gson

    /**
     * Executes synchronous GET request.
     */
    @Throws(IOException::class)
    fun get(endpoint: String): Response {
        val requestBuilder = Request.Builder()
            .url("$BASE_URL$endpoint")
            .get()
        
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return client.newCall(requestBuilder.build()).execute()
    }

    /**
     * Executes synchronous POST request with optional body.
     */
    @Throws(IOException::class)
    fun post(endpoint: String, body: Any?): Response {
        val jsonString = body?.let { gson.toJson(it) } ?: "{}"
        val requestBody = jsonString.toRequestBody(JSON_MEDIA_TYPE)

        val requestBuilder = Request.Builder()
            .url("$BASE_URL$endpoint")
            .post(requestBody)

        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return client.newCall(requestBuilder.build()).execute()
    }

    /**
     * Executes synchronous PUT request with body.
     */
    @Throws(IOException::class)
    fun put(endpoint: String, body: Any): Response {
        val jsonString = gson.toJson(body)
        val requestBody = jsonString.toRequestBody(JSON_MEDIA_TYPE)

        val requestBuilder = Request.Builder()
            .url("$BASE_URL$endpoint")
            .put(requestBody)

        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return client.newCall(requestBuilder.build()).execute()
    }

    /**
     * Executes synchronous DELETE request.
     */
    @Throws(IOException::class)
    fun delete(endpoint: String): Response {
        val requestBuilder = Request.Builder()
            .url("$BASE_URL$endpoint")
            .delete()

        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return client.newCall(requestBuilder.build()).execute()
    }
}
