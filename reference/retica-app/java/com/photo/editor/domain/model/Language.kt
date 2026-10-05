package com.photo.editor.domain.model

import android.os.LocaleList
import androidx.annotation.DrawableRes
import com.photo.editor.R
import java.util.Locale

enum class Language(
    val code: String,
    @DrawableRes
    val drawableId: Int,
) {
    English(code = "en",  drawableId = R.drawable.ic_language_english),
    Japanese(code = "ja",  drawableId = R.drawable.ic_language_japanese),
    Hindi(code = "hi", drawableId = R.drawable.ic_language_hindi),
    Korean(code = "ko",  drawableId = R.drawable.ic_language_korean),
    French(code = "fr", drawableId = R.drawable.ic_language_french),
    German(code = "de", drawableId = R.drawable.ic_language_germany),
    Portuguese(code = "pt",  drawableId = R.drawable.ic_language_portuguese),
    Spanish(code = "es",  drawableId = R.drawable.ic_language_espano),

    Bangla(code = "bn", drawableId = R.drawable.ic_language_hindi),
    Marathi(code = "mr", drawableId = R.drawable.ic_language_hindi),
    Telugu(code = "te", drawableId = R.drawable.ic_language_hindi),
    Tamil(code = "ta", drawableId = R.drawable.ic_language_hindi),
    Gujarati(code = "gu", drawableId = R.drawable.ic_language_hindi),
    Urdu(code = "ur", drawableId = R.drawable.ic_language_hindi),
    Kannada(code = "kn", drawableId = R.drawable.ic_language_hindi),
    Odia(code = "or", drawableId = R.drawable.ic_language_hindi),
    ;

//    SimplifiedChinese(code = "zh", displayName = "Simplified Chinese", drawableId = R.drawable.ic_language_chinese),
//    Russian(code = "ru", displayName = "Russian", drawableId = R.drawable.ic_language_russian),
//    Bengali(code = "bn", displayName = "Bengali", drawableId = R.drawable.ic_language_bengali),
//    Marathi(code = "mr", displayName = "Marathi", drawableId = R.drawable.ic_language_marathi),
//    Telugu(code = "te", displayName = "Telugu", drawableId = R.drawable.ic_language_telugu),
//    Turkish(code = "tr", displayName = "Turkish", drawableId = R.drawable.ic_language_turkish),
//    Tamil(code = "ta", displayName = "Tamil", drawableId = R.drawable.ic_language_tamil),
//    Vietnamese(code = "vi", displayName = "Vietnamese", drawableId = R.drawable.ic_language_vietnamese),;
//    Italian(code = "it", displayName = "Italian", drawableId = R.drawable.ic_language_italian),
//    Thailand(code = "th", displayName = "Thai", drawableId = R.drawable.ic_language_thai);

    companion object {
        val defaultList = listOf(Japanese, Korean, English, Hindi, French, German, Portuguese, Spanish)
        val hindiList = listOf(Hindi, Bangla, Marathi, Telugu, Tamil, Gujarati, Urdu, Kannada, Odia)

        fun getDefault(): Language = English

        fun getByCode(code: String?): Language {
            if (code == null) return getDefault()
            return entries.firstOrNull { it.code == code } ?: English
        }

        fun getSortedList(): List<Language> {
            val defaultLocale = LocaleList.getDefault().get(0) ?: Locale.getDefault()

            val list = entries.toMutableList()
            val defaultCode = defaultLocale.language
            val indexOfDefault = list.indexOfFirst { defaultCode == it.code }
            if (indexOfDefault > 0) {
                val item = list.removeAt(indexOfDefault)
                list.add(0, item)
            }
            return list
        }
    }
}

