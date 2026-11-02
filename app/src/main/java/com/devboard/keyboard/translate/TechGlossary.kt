package com.devboard.keyboard.translate

/**
 * قاموس مصطلحات تقنية محلي. ليس بديلاً عن الترجمة الآلية،
 * يُستخدم فقط كبديل محدود عند انقطاع الشبكة، ويُعلن ذلك للمستخدم.
 */
object TechGlossary {

    private val en2ar = mapOf(
        "repository" to "مستودع", "repo" to "مستودع", "commit" to "إيداع",
        "branch" to "فرع", "merge" to "دمج", "pull" to "سحب", "push" to "دفع",
        "clone" to "استنساخ", "build" to "بناء", "compile" to "تصريف",
        "debug" to "تنقيح", "release" to "إصدار", "dependency" to "اعتمادية",
        "dependencies" to "اعتماديات", "gradle" to "غرادل", "workflow" to "سير عمل",
        "artifact" to "مُخرَج بناء", "cache" to "ذاكرة مؤقتة", "permission" to "إذن",
        "permissions" to "أذونات", "layout" to "تخطيط", "activity" to "نشاط",
        "service" to "خدمة", "fragment" to "شظية", "intent" to "نية",
        "manifest" to "بيان التطبيق", "resource" to "مورد", "resources" to "موارد",
        "string" to "سلسلة نصية", "array" to "مصفوفة", "list" to "قائمة",
        "map" to "خريطة", "loop" to "حلقة", "function" to "دالة",
        "class" to "صنف", "object" to "كائن", "interface" to "واجهة",
        "variable" to "متغير", "constant" to "ثابت", "thread" to "خيط تنفيذ",
        "coroutine" to "روتين مشارك", "exception" to "استثناء", "error" to "خطأ",
        "warning" to "تحذير", "log" to "سجل", "network" to "شبكة",
        "database" to "قاعدة بيانات", "query" to "استعلام", "table" to "جدول",
        "keyboard" to "لوحة مفاتيح", "translate" to "ترجمة", "translation" to "ترجمة",
        "settings" to "إعدادات", "clipboard" to "حافظة", "snippet" to "مقتطف",
        "install" to "تثبيت", "uninstall" to "إزالة", "update" to "تحديث",
        "upgrade" to "ترقية", "package" to "حزمة", "version" to "إصدار",
        "server" to "خادم", "client" to "عميل", "request" to "طلب",
        "response" to "استجابة", "timeout" to "انتهاء المهلة", "token" to "رمز وصول",
        "key" to "مفتاح", "value" to "قيمة", "file" to "ملف", "folder" to "مجلد",
        "directory" to "دليل", "path" to "مسار", "terminal" to "طرفية",
        "command" to "أمر", "script" to "سكربت", "run" to "تشغيل",
        "execute" to "تنفيذ", "success" to "نجاح", "failed" to "فشل",
        "signature" to "توقيع", "keystore" to "مخزن مفاتيح", "sdk" to "حزمة تطوير"
    )

    private val ar2en: Map<String, String> by lazy {
        en2ar.entries.associate { (k, v) -> v to k }
    }

    fun lookup(word: String, from: String, to: String): String? {
        val w = word.trim().lowercase()
        if (w.isEmpty() || w.contains(' ')) return null
        return when {
            to == "ar" && from != "ar" -> en2ar[w]
            from == "ar" && to != "ar" -> ar2en[word.trim()]
            else -> null
        }
    }
}
