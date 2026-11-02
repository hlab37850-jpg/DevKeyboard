package com.devboard.keyboard.translate

import com.devboard.keyboard.core.Prefs
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

interface Provider {
    val id: String
    val title: String
    fun translate(client: OkHttpClient, text: String, from: String, to: String): TrResult
}

object MyMemoryProvider : Provider {
    override val id = "mymemory"
    override val title = "MyMemory"

    override fun translate(client: OkHttpClient, text: String, from: String, to: String): TrResult {
        val pair = URLEncoder.encode("$from|$to", "UTF-8")
        val q = URLEncoder.encode(text, "UTF-8")
        val mail = Prefs.email.trim()
        val extra = if (mail.isNotEmpty() && mail.contains("@"))
            "&de=" + URLEncoder.encode(mail, "UTF-8") else ""
        val url = "https://api.mymemory.translated.net/get?q=$q&langpair=$pair$extra"

        val req = Request.Builder().url(url).header("User-Agent", "DevKeyboard/1.0").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return TrResult(false, "", "MyMemory HTTP " + resp.code)
            }
            val root = JSONObject(body)
            val status = root.opt("responseStatus")?.toString()?.toIntOrNull() ?: 0
            if (root.optBoolean("quotaFinished", false)) {
                return TrResult(false, "", "انتهت حصة MyMemory اليومية. أضف بريدك في الإعدادات أو بدّل المزوّد.")
            }
            if (status != 200) {
                val details = root.optString("responseDetails", "")
                return TrResult(false, "", "MyMemory: " + if (details.isBlank()) "رمز $status" else details)
            }
            val out = root.optJSONObject("responseData")?.optString("translatedText").orEmpty()
            if (out.isBlank()) return TrResult(false, "", "MyMemory أعاد نتيجة فارغة")
            if (out.startsWith("MYMEMORY WARNING", true)) {
                return TrResult(false, "", "تحذير الحصة من MyMemory، حاول لاحقاً")
            }
            return TrResult(true, decodeEntities(out), "MyMemory")
        }
    }

    private fun decodeEntities(s: String): String = s
        .replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace("&amp;", "&")
}

object LibreProvider : Provider {
    override val id = "libre"
    override val title = "LibreTranslate"

    override fun translate(client: OkHttpClient, text: String, from: String, to: String): TrResult {
        val base = Prefs.libreUrl.trim().trimEnd('/')
        if (base.isEmpty() || !base.startsWith("http")) {
            return TrResult(false, "", "اضبط رابط خادم LibreTranslate في الإعدادات")
        }
        val json = JSONObject()
            .put("q", text)
            .put("source", from)
            .put("target", to)
            .put("format", "text")
        val key = Prefs.libreKey.trim()
        if (key.isNotEmpty()) json.put("api_key", key)

        val req = Request.Builder()
            .url("$base/translate")
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("User-Agent", "DevKeyboard/1.0")
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val err = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
                return TrResult(false, "", "LibreTranslate HTTP ${resp.code} " + err)
            }
            val root = JSONObject(body)
            if (root.has("error")) {
                return TrResult(false, "", "LibreTranslate: " + root.optString("error"))
            }
            val out = root.optString("translatedText")
            if (out.isBlank()) return TrResult(false, "", "LibreTranslate أعاد نتيجة فارغة")
            return TrResult(true, out, "LibreTranslate")
        }
    }
}
