package fr.berliat.hskwidget.domain

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonElement

import fr.berliat.hskwidget.data.model.WriteAssist
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class GenAIAnswerNotJsonException(message: String) : Exception(message)
class GenAIServerErrorException(message: String) : Exception(message)
class GenAIUnexpectedAnswerException(message: String) : Exception(message)

class GenAI(val apiKey: String) {
    suspend fun fixSentence(text: String): WriteAssist {
        val messages = mutableListOf<JsonElement>()
        messages.add(mapOf(
            Pair("role", "assistant"),
            Pair("content", "You're a friendly simplified mandarin teacher with 20 years of experience teaching to native english speakers, using methodologies appropriate for foreigners to fully understand concepts. Users will, each time, send you a sentence in mandarin. Reply using json with 7 fields:\\n- \\\"original_cn\\\": just echo what you received\\n- \\\"corrected_cn\\\": fix the sentence to the best of your abilities. Use daily conversational tone and vocabulary people use in mainland in 2024, not 书面.\\n- \\\"confidence\\\": rate your confidence in the meaning of the sentence, from 0.00 to 1.00. Sometimes, a broken sentence can mean different things. For example, if as a teacher you would ask \\\"what did you mean?\\\" the rating would be low. If the sentence is clear but the flow isn't natural, the confidence would be higher.\\n- \\\"grade\\\": grade the original sentence's accuracy by comparing the input and output. From 0.00 to 1.00 where 0.00 means it was totally broken, and 1.00 there was no correction needed.\\n- \\\"explanations\\\": an *array* of mistakes, when you explain each mistake made, how you fixed it, and explain the reasoning behind the fix so the learner will no longer make the same mistake. Don't write more than 1 paragraph for the reasoning, if possible.\\n- \\\"original_en\\\": the english translation of the corrected sentence.\\n- \\\"corrected_en\\\": the english translation of the corrected sentence."),
        ).toJson())

        messages.add(mapOf(
            Pair("role", "user"),
            Pair("content", "Fix the following sentence, reply in json: $text"),
        ).toJson())

        val response = postLLM(messages)

        if (!response.contains("original_cn") ||
            !response.contains("corrected_cn") ||
            !response.contains("confidence") ||
            !response.contains("explanations") ||
            !response.contains("original_en") ||
            !response.contains("corrected_en") ||
            !response.contains("grade")) {
            throw GenAIUnexpectedAnswerException(response.toString())
        }

        return WriteAssist(
            response["original_cn"].toString(),
            response["corrected_cn"].toString(),
            response["confidence"].toDouble(0.0),
            response["explanations"].toString(),
            response["original_en"].toString(),
            response["corrected_en"].toString(),
            response["grade"].toDouble(0.0),
        )
    }

    suspend fun postLLM(messages: List<JsonElement>) : JsonObject {
        val params = mutableMapOf<String, JsonElement>()

        params["messages"] = JsonArray(messages)

        params["model"] = JsonPrimitive(LLM_MODEL)
        params["temperature"] = JsonPrimitive(LLM_TEMPERATURE)
        params["max_tokens"] = JsonPrimitive(LLM_MAX_TOKENS)
        params["top_p"] = JsonPrimitive(LLM_TOP_P)
        params["stream"] = JsonPrimitive(LLM_STREAM)
        params["stop"] = JsonPrimitive(null)
        params["response_format"] = Json.parseToJsonElement("{\"type\":\"json_object\"}")

        var answer = postJson(params)

        if (answer.containsKey("error")) {
            throw GenAIServerErrorException(answer["error"].toString())
        }

        var content = ""
        try {
            content = answer["choices"]?.jsonArray?.get(0)?.jsonObject
                ?.get("message")?.jsonObject?.get("content").toString()

            content = Json.decodeFromString<String>(content)

            answer = Json.parseToJsonElement(content) as JsonObject
        } catch (e: Exception) {
            throw GenAIUnexpectedAnswerException(e.message ?: content)
        }

        return answer
    }

    private suspend fun postJson(params: Map<String, JsonElement>) : JsonObject {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val jsonBody: JsonObject = buildJsonObject {
            params.forEach { (p, v) -> put(p, v) }
        }

        val response: String = client.post(API_ENDPOINT) {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append("Authorization", "Bearer $apiKey")
            }
            setBody(jsonBody)  // Setting the body content
        }.body()

        println(response)
        val jsonResponse : JsonObject
        try {
            jsonResponse = Json.parseToJsonElement(response) as JsonObject
        } catch (e: Exception) {
            throw GenAIAnswerNotJsonException(e.message ?: "No Json returned")
        }
        client.close()

        return jsonResponse
    }

    companion object {
        private const val API_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        private const val LLM_MODEL = "llama3-8b-8192"
        private const val LLM_TEMPERATURE = 1
        private const val LLM_MAX_TOKENS = 1024
        private const val LLM_TOP_P = 1
        private const val LLM_STREAM = false
    }
}

private fun JsonElement?.toDouble(default: Double): Double {
    return when {
        this == null -> default
        this.jsonPrimitive.isString -> this.jsonPrimitive.content.toDoubleOrNull() ?: default
        this.jsonPrimitive.content.toDoubleOrNull() != null -> this.jsonPrimitive.double
        else -> default
    }
}

private fun Map<String, String>?.toJson(): JsonElement {
    return this?.let { map ->
        buildJsonObject {
            map.forEach { (key, value) ->
                put(key, value)
            }
        }
    } ?: JsonObject(emptyMap())
}
