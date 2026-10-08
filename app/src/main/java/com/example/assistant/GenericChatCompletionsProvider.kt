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

class GenericChatCompletionsProvider(
    private val baseUrlProvider: () -> String = { "https://api.openai.com/v1" },
    private val apiKeyProvider: () -> String? = { null },
    private val modelProvider: () -> String = { "gpt-4o-mini" }
) : AIProvider {

    override val id: String = "generic_chat_completions"
    override val requiresInternet: Boolean = true
    override val isAvailable: Boolean
        get() = !apiKeyProvider().isNullOrBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(request: AIRequest): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()?.trim()
        val baseUrl = baseUrlProvider().trim().removeSuffix("/")
        val model = modelProvider().trim().ifBlank { "gpt-4o-mini" }

        if (apiKey.isNullOrBlank()) {
            return@withContext AIResponse.Error(
                kind = AIErrorKind.SinClave,
                userMessage = "No se ha configurado la clave de acceso para el proveedor de IA.",
                providerId = id
            )
        }

        val endpoint = "$baseUrl/chat/completions"

        val requestBodyJson = buildOpenAIRequestBody(request, model)
        val body = requestBodyJson.toString().toRequestBody("application/json".toMediaType())

        var lastError: AIResponse.Error? = null
        val maxAttempts = 2

        for (attempt in 1..maxAttempts) {
            try {
                val reqBuilder = Request.Builder()
                    .url(endpoint)
                    .post(body)

                if (!apiKey.isNullOrBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                client.newCall(reqBuilder.build()).execute().use { response ->
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        return@withContext parseOpenAIResponse(responseBody)
                    }

                    val errorKind = when (responseCode) {
                        401, 403 -> AIErrorKind.SinClave
                        429 -> AIErrorKind.LimiteExcedido
                        400 -> AIErrorKind.RespuestaInvalida
                        in 500..599 -> AIErrorKind.Desconocido
                        else -> AIErrorKind.Desconocido
                    }

                    val userMsg = when (errorKind) {
                        AIErrorKind.SinClave -> "Credenciales no autorizadas o clave ausente para el proveedor genérico."
                        AIErrorKind.LimiteExcedido -> "Cuota excedida o tasa de consultas saturada en el proveedor."
                        else -> "El proveedor de IA respondió con un error (código $responseCode)."
                    }

                    lastError = AIResponse.Error(errorKind, userMsg, id)

                    if (attempt < maxAttempts && (responseCode in 500..599 || responseCode == 429)) {
                        delay(1000L * attempt)
                    } else {
                        return@withContext lastError!!
                    }
                }
            } catch (e: UnknownHostException) {
                Log.w("GenericAIProvider", "Host no alcanzable")
                return@withContext AIResponse.Error(
                    kind = AIErrorKind.SinInternet,
                    userMessage = "No se puede establecer conexión con el servidor de IA.",
                    providerId = id
                )
            } catch (e: SocketTimeoutException) {
                Log.w("GenericAIProvider", "Timeout en conexión con endpoint de IA")
                if (attempt < maxAttempts) {
                    delay(1000L * attempt)
                    lastError = AIResponse.Error(
                        kind = AIErrorKind.Timeout,
                        userMessage = "Tiempo de espera agotado al conectar con el servidor de IA.",
                        providerId = id
                    )
                } else {
                    return@withContext AIResponse.Error(
                        kind = AIErrorKind.Timeout,
                        userMessage = "Tiempo de espera agotado al conectar con el servidor de IA.",
                        providerId = id
                    )
                }
            } catch (e: IOException) {
                Log.w("GenericAIProvider", "Error de red")
                return@withContext AIResponse.Error(
                    kind = AIErrorKind.SinInternet,
                    userMessage = "Error de comunicación de red con el proveedor de IA.",
                    providerId = id
                )
            } catch (e: Exception) {
                Log.w("GenericAIProvider", "Excepción no controlada")
                return@withContext AIResponse.Error(
                    kind = AIErrorKind.Desconocido,
                    userMessage = "Error al procesar la solicitud con el proveedor configurado.",
                    providerId = id
                )
            }
        }

        lastError ?: AIResponse.Error(
            kind = AIErrorKind.Desconocido,
            userMessage = "Error de comunicación con el servicio de IA.",
            providerId = id
        )
    }

    private fun buildOpenAIRequestBody(request: AIRequest, model: String): JSONObject {
        val root = JSONObject()
        root.put("model", model)
        root.put("temperature", request.temperature)
        root.put("max_tokens", request.maxTokens)

        val messagesArray = JSONArray()

        // Si hay system instruction y no está en los mensajes, incluirla primero
        if (!request.systemInstruction.isNullOrBlank()) {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", request.systemInstruction)
            })
        }

        for (msg in request.messages) {
            val msgObj = JSONObject()
            val roleStr = when (msg.role) {
                AIRole.SYSTEM -> "system"
                AIRole.USER -> "user"
                AIRole.ASSISTANT -> "assistant"
                AIRole.TOOL -> "tool"
            }
            msgObj.put("role", roleStr)
            msgObj.put("content", msg.content)
            if (msg.toolCallId != null) {
                msgObj.put("tool_call_id", msg.toolCallId)
            }
            messagesArray.put(msgObj)
        }
        root.put("messages", messagesArray)

        // Tools
        if (request.tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            for (t in request.tools) {
                val toolObj = JSONObject()
                toolObj.put("type", "function")
                val fnObj = JSONObject()
                fnObj.put("name", t.name)
                fnObj.put("description", t.description)
                try {
                    fnObj.put("parameters", JSONObject(t.parametersJsonSchema))
                } catch (_: Exception) {
                    fnObj.put("parameters", JSONObject().apply { put("type", "object") })
                }
                toolObj.put("function", fnObj)
                toolsArray.put(toolObj)
            }
            root.put("tools", toolsArray)
        }

        return root
    }

    private fun parseOpenAIResponse(jsonStr: String): AIResponse {
        return try {
            val json = JSONObject(jsonStr)
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return AIResponse.Error(
                    kind = AIErrorKind.RespuestaInvalida,
                    userMessage = "No se recibieron opciones de respuesta de la IA.",
                    providerId = id
                )
            }

            val firstChoice = choices.optJSONObject(0)
            val messageObj = firstChoice?.optJSONObject("message")

            // Revisar tool_calls
            val toolCalls = messageObj?.optJSONArray("tool_calls")
            if (toolCalls != null && toolCalls.length() > 0) {
                val firstTool = toolCalls.optJSONObject(0)
                val fnObj = firstTool?.optJSONObject("function")
                val fnName = fnObj?.optString("name", "") ?: ""
                val argsStr = fnObj?.optString("arguments", "{}") ?: "{}"
                val callId = firstTool?.optString("id", "") ?: ""

                if (fnName.isNotBlank()) {
                    return AIResponse.ToolCall(
                        name = fnName,
                        argsJson = argsStr,
                        callId = callId,
                        providerId = id
                    )
                }
            }

            // Contenido de texto
            val content = messageObj?.optString("content", "")?.trim() ?: ""
            if (content.isNotBlank()) {
                AIResponse.Text(content = content, providerId = id)
            } else {
                AIResponse.Error(
                    kind = AIErrorKind.RespuestaInvalida,
                    userMessage = "Respuesta vacía recibida del proveedor de IA.",
                    providerId = id
                )
            }
        } catch (_: Exception) {
            AIResponse.Error(
                kind = AIErrorKind.RespuestaInvalida,
                userMessage = "Error al interpretar la respuesta del proveedor de IA.",
                providerId = id
            )
        }
    }
}
