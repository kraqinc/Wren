package com.wren.ide.core.network

import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Estado de conexión observable -- para que cualquier pantalla pueda mostrar
 * un banner real de "reconectando" o "no se pudo reconectar" en vez de que
 * cada request falle en silencio o la app se sienta rota sin explicación. */
enum class ConnectionState {
    CONNECTED,
    RECONNECTING,
    FAILED,
}

object NetworkClient {
    // Production Railway backend. This MUST point at the real deployed API —
    // the previous "10.0.2.2" value only resolves on the Android emulator's
    // loopback to a locally-running backend, so on any real device every
    // request failed instantly and silently, which is why login never worked.
    // Se abandonó el backend de Railway -- todo corre ahora en Vercel.
    // IMPORTANTE: reemplaza esta URL por el dominio real que te da Vercel al
    // desplegar wren-backend-vercel (el monolito Next.js). Por defecto
    // Vercel te da algo como "wren-backend-vercel.vercel.app", pero
    // confírmalo en tu dashboard -- si esto no coincide EXACTO con tu
    // deployment real, todas las llamadas de red van a fallar en silencio
    // (mismo síntoma que el bug original de "10.0.2.2").
    private const val BASE_URL = "https://wren-backend-vercel.vercel.app/api"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private const val MAX_RETRIES = 3
    private val RETRY_DELAYS_MS = longArrayOf(1000, 2000, 4000)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var token: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun setAuthToken(newToken: String?) {
        token = newToken
    }

    fun getAuthToken(): String? = token

    fun getGson(): Gson = gson

    /** El usuario tocó "reintentar" en el banner de error -- vuelve a CONNECTED
     * para que la próxima llamada arranque desde cero (no se queda en FAILED
     * para siempre bloqueando reintentos futuros). */
    fun resetConnectionState() {
        _connectionState.value = ConnectionState.CONNECTED
    }

    /**
     * Ejecuta [call] y, si falla por un problema de red (IOException — sin
     * conexión, timeout, DNS, servidor caído), reintenta con backoff mientras
     * actualiza [connectionState] para que la UI muestre el estado real:
     * RECONNECTING mientras reintenta, FAILED si se agotan los intentos,
     * CONNECTED en cuanto una respuesta llega. Errores que no son de red
     * (4xx/5xx con respuesta del servidor) no cuentan como fallo de conexión
     * -- esos ya llegaron, solo que con un código de error, y el propio
     * caller decide qué hacer con ellos.
     */
    @Throws(IOException::class)
    private fun executeWithRetry(requestBuilder: Request.Builder): Response {
        var lastError: IOException? = null

        for (attempt in 0..MAX_RETRIES) {
            try {
                val response = client.newCall(requestBuilder.build()).execute()
                _connectionState.value = ConnectionState.CONNECTED
                return response
            } catch (e: IOException) {
                lastError = e
                if (attempt < MAX_RETRIES) {
                    _connectionState.value = ConnectionState.RECONNECTING
                    Thread.sleep(RETRY_DELAYS_MS[attempt])
                }
            }
        }

        _connectionState.value = ConnectionState.FAILED
        throw lastError ?: IOException("No se pudo conectar con el servidor.")
    }

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

        return executeWithRetry(requestBuilder)
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

        return executeWithRetry(requestBuilder)
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

        return executeWithRetry(requestBuilder)
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

        return executeWithRetry(requestBuilder)
    }
}
