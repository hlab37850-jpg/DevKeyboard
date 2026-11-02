package com.devboard.keyboard.translate

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.devboard.keyboard.core.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object TranslationRepository {

    private const val MAX_CHARS = 2000
    private const val CHUNK = 450
    private const val CACHE_MAX = 400

    private lateinit var ctx: Context
    private lateinit var cacheFile: File
    private val cache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > CACHE_MAX
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun init(c: Context) {
        if (::ctx.isInitialized) return
        ctx = c.applicationContext
        cacheFile = File(ctx.filesDir, "tr_cache.json")
        loadCache()
    }

    private fun loadCache() {
        runCatching {
            if (!cacheFile.exists()) return
            val o = JSONObject(cacheFile.readText())
            o.keys().forEach { k -> cache[k] = o.getString(k) }
        }
    }

    private fun saveCache() {
        runCatching {
            val o = JSONObject()
            synchronized(cache) { cache.forEach { (k, v) -> o.put(k, v) } }
            cacheFile.writeText(o.toString())
        }
    }

    fun clearCache() {
        synchronized(cache) { cache.clear() }
        runCatching { if (cacheFile.exists()) cacheFile.delete() }
    }

    fun cacheSize(): Int = synchronized(cache) { cache.size }

    private fun provider(): Provider =
        if (Prefs.provider == "libre") LibreProvider else MyMemoryProvider

    private fun online(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun key(p: String, f: String, t: String, s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("$p|$f|$t|$s".toByteArray())
        return bytes.take(16).joinToString("") { "%02x".format(it) }
    }

    private fun split(text: String): List<String> {
        if (text.length <= CHUNK) return listOf(text)
        val out = ArrayList<String>()
        var start = 0
        while (start < text.length) {
            var end = minOf(start + CHUNK, text.length)
            if (end < text.length) {
                val brk = text.lastIndexOfAny(charArrayOf('\n', '.', '!', '?', '،', ' '), end)
                if (brk > start + 80) end = brk + 1
            }
            out.add(text.substring(start, end))
            start = end
        }
        return out
    }

    suspend fun translate(raw: String, from: String, to: String): TrResult = withContext(Dispatchers.IO) {
        val text = raw.trim()
        if (text.isEmpty()) return@withContext TrResult(false, "", "لا يوجد نص للترجمة")
        if (text.length > MAX_CHARS) {
            return@withContext TrResult(false, "", "النص أطول من $MAX_CHARS حرف، اختصره حفاظاً على الحصة")
        }
        if (from == to) return@withContext TrResult(true, text, "اللغة المصدر = الهدف")

        val p = provider()
        val ck = key(p.id, from, to, text)
        if (Prefs.translationCache) {
            val hit = synchronized(cache) { cache[ck] }
            if (hit != null) return@withContext TrResult(true, hit, "من الذاكرة المؤقتة")
        }

        if (!online()) {
            val g = TechGlossary.lookup(text, from, to)
            return@withContext if (g != null)
                TrResult(true, g, "قاموس تقني محلي (لا يوجد اتصال)")
            else
                TrResult(false, "", "لا يوجد اتصال بالإنترنت، ولا يوجد مصطلح مطابق في القاموس المحلي")
        }

        val parts = split(text)
        val sb = StringBuilder()
        for ((i, part) in parts.withIndex()) {
            val res = try {
                p.translate(client, part, from, to)
            } catch (e: java.net.SocketTimeoutException) {
                TrResult(false, "", "انتهت مهلة الاتصال بالخادم")
            } catch (e: java.net.UnknownHostException) {
                TrResult(false, "", "تعذّر الوصول إلى خادم الترجمة (DNS)")
            } catch (e: javax.net.ssl.SSLException) {
                TrResult(false, "", "خطأ في اتصال TLS: " + (e.message ?: ""))
            } catch (e: Exception) {
                TrResult(false, "", "خطأ: " + (e.javaClass.simpleName) + " " + (e.message ?: ""))
            }
            if (!res.ok) {
                if (i == 0) {
                    val g = TechGlossary.lookup(text, from, to)
                    if (g != null) return@withContext TrResult(true, g, "قاموس محلي بعد فشل الشبكة: " + res.note)
                }
                return@withContext res
            }
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(res.text.trim())
        }

        val finalText = sb.toString()
        if (Prefs.translationCache) {
            synchronized(cache) { cache[ck] = finalText }
            saveCache()
        }
        val note = p.title + if (parts.size > 1) " (${parts.size} أجزاء)" else ""
        TrResult(true, finalText, note)
    }
}
