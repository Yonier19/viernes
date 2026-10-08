package com.example.assistant

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class GeminiAIProvider(
    private val apiKeyProvider: () -> String? = { null },
    private val modelProvider: () -> String = { "gemini-2.5-flash" }
) : AIProvider {

    override val id: String = "gemini_online"
    override val requiresInternet: Boolean = true
    override val isAvailable: Boolean
        get() = !apiKeyProvider().isNullOrBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun getEndpoint(model: String): String =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    override suspend fun generate(request: AIRequest): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()?.trim()
        if (apiKey.isNullOrBlank()) {
            return@withContext AIResponse.Error(
                kind = AIErrorKind.SinClave,
                userMessage = "No se ha configurado la clave API de Gemini. Configúrala en Ajustes.",
                providerId = id
            )
        }

        val model = modelProvider().trim().ifBlank { "gemini-2.5-flash" }
        val endpoint = "${getEndpoint(model)}?key=$apiKey"

        val requestBodyJson = buildGeminiRequestBody(request)
        val body = requestBodyJson.toString().toRequestBody("application/json".toMediaType())

        var lastError: AIResponse.Error? = null
        val maxAttempts = 2

        for (attempt in 1..maxAttempts) {
            try {
                val httpRequest = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        return@withContext parseGeminiResponse(responseBody)
                    }

                    // Categorizar error HTTP
                    val errorKind = when (responseCode) {
                        401, 403 -> AIErrorKind.SinClave
                        429 -> AIErrorKind.LimiteExcedido
                        400 -> AIErrorKind.RespuestaInvalida
                        in 500..599 -> AIErrorKind.Desconocido
                        else -> AIErrorKind.Desconocido
                    }

                    val userMsg = when (errorKind) {
                        AIErrorKind.SinClave -> "La clave API configurada es inválida o no tiene permisos."
                        AIErrorKind.LimiteExcedido -> "Se ha alcanzado la cuota de consultas del modelo. Intenta en unos momentos."
                        else -> "El servidor de IA respondió con un error (código $responseCode)."
                    }

                    lastError = AIResponse.Error(errorKind, userMsg, id)

                    // Reintento en errores transitorios (5xx o 429) si no es el último intento
                    if (attempt < maxAttempts && (responseCode in 500..599 || responseCode == 429)) {
                        delay(1000L * attempt)
                    } else {
                        return@withContext lastError!!
                    }
                }
            } catch (e: UnknownHostException) {
                Log.w("GeminiAIProvider", "Network error: Host no alcanzable (sin internet)")
                return@withContext AIResponse.Error(
                    kind = AIErrorKind.SinInternet,
                    userMessage = "No hay conexión a internet para consultar a Gemini.",
                    providerId = id
                )
            } catch (e: SocketTimeoutException) {
                Log.w("GeminiAIProvider", "Network timeout en consulta a Gemini")
                if (attempt < maxAttempts) {
                    delay(1000L * attempt)
                    lastError = AIResponse.Error(
                        kind = AIErrorKind.Timeout,
                        userMessage = "Tiempo de espera agotado al conectar con Gemini.",
                        providerId = id
                    )
                } else {
                    return@withContext AIResponse.Error(
                        kind = AIErrorKind.Timeout,
                        userMessage = "Tiempo de espera agotado al conectar con Gemini.",
                        providerId = id
                    )
                }
            } catch (e: IOException) {
                Log.w("GeminiAIProvider", "IO error en solicitud de IA")
                return@withContext AIResponse.Error(
                    kind = AIErrorKind.SinInternet,
                    userMessage = "Error de red al intentar comunicar con Gemini.",
                    providerId = id
                )
            } catch (e: Exception) {
                Log.w("GeminiAIProvider", "Excepción inesperada en llamada a Gemini")
                return@withContext AIResponse.Error(
                    kind = AIErrorKind.Desconocido,
                    userMessage = "No fue posible procesar la respuesta con el proveedor en línea.",
                    providerId = id
                )
            }
        }

        lastError ?: AIResponse.Error(
            kind = AIErrorKind.Desconocido,
            userMessage = "Error de comunicación con Gemini.",
            providerId = id
        )
    }

    private fun buildGeminiRequestBody(request: AIRequest): JSONObject {
        val root = JSONObject()

        // System Instruction
        val sysInstruction = request.systemInstruction
        if (!sysInstruction.isNullOrBlank()) {
            val systemPart = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", sysInstruction) })
                })
            }
            root.put("systemInstruction", systemPart)
        }

        // Contents (Historial y Mensajes)
        val contentsArray = JSONArray()
        for (msg in request.messages) {
            val contentObj = JSONObject()
            val role = when (msg.role) {
                AIRole.USER -> "user"
                AIRole.ASSISTANT -> "model"
                AIRole.SYSTEM -> "user"
                AIRole.TOOL -> "function"
            }
            contentObj.put("role", role)

            val partsArray = JSONArray()
            partsArray.put(JSONObject().apply {
                put("text", msg.content)
            })
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }
        root.put("contents", contentsArray)

        // Tools (Function Declarations) si existen
        if (request.tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            val toolObj = JSONObject()
            val functionDeclarations = JSONArray()

            for (t in request.tools) {
                val fnObj = JSONObject().apply {
                    put("name", t.name)
                    put("description", t.description)
                    if (t.parametersJsonSchema.isNotBlank() && t.parametersJsonSchema != "{}") {
                        try {
                            put("parameters", JSONObject(t.parametersJsonSchema))
                        } catch (_: Exception) {
                            put("parameters", JSONObject().apply {
                                put("type", "object")
                            })
                        }
                    }
                }
                functionDeclarations.put(fnObj)
            }

            toolObj.put("functionDeclarations", functionDeclarations)
            toolsArray.put(toolObj)
            root.put("tools", toolsArray)
        }

        // Generation Config
        val genConfig = JSONObject().apply {
            put("temperature", request.temperature)
            put("maxOutputTokens", request.maxTokens)
        }
        root.put("generationConfig", genConfig)

        return root
    }

    private fun parseGeminiResponse(jsonStr: String): AIResponse {
        return try {
            val json = JSONObject(jsonStr)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return AIResponse.Error(
                    kind = AIErrorKind.RespuestaInvalida,
                    userMessage = "La IA no generó ninguna respuesta válida.",
                    providerId = id
                )
            }

            val firstCandidate = candidates.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts == null || parts.length() == 0) {
                return AIResponse.Error(
                    kind = AIErrorKind.RespuestaInvalida,
                    userMessage = "Respuesta vacía recibida de la IA.",
                    providerId = id
                )
            }

            // Revisar si contiene functionCall
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                val fnCall = part.optJSONObject("functionCall")
                if (fnCall != null) {
                    val fnName = fnCall.optString("name", "")
                    val argsObj = fnCall.optJSONObject("args")
                    val argsJson = argsObj?.toString() ?: "{}"
                    if (fnName.isNotBlank()) {
                        return AIResponse.ToolCall(
                            name = fnName,
                            argsJson = argsJson,
                            providerId = id
                        )
                    }
                }
            }

            // Si es respuesta de texto
            val firstPart = parts.optJSONObject(0)
            val text = firstPart?.optString("text", "")?.trim() ?: ""

            if (text.isNotBlank()) {
                AIResponse.Text(content = text, providerId = id)
            } else {
                AIResponse.Error(
                    kind = AIErrorKind.RespuestaInvalida,
                    userMessage = "Respuesta sin contenido de texto recibido de la IA.",
                    providerId = id
                )
            }
        } catch (_: Exception) {
            AIResponse.Error(
                kind = AIErrorKind.RespuestaInvalida,
                userMessage = "No fue posible procesar el formato de respuesta de la IA.",
                providerId = id
            )
        }
    }
}
