package com.ndzl.a15_blog

import android.graphics.text.LineBreakConfig
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Blog (3): Enhancing User Experience & Multitasking — Text / i18n Features
 * Wiki: A15‐(3)‐Enhancing User Experience & Multitasking
 *
 * Demonstrates four text rendering improvements introduced in API 35:
 *
 * 1. **Inter-Character Justification** (JUSTIFICATION_MODE_INTER_CHARACTER)
 *    Distributes extra space between individual characters, not just words.
 *    Particularly useful for CJK (Chinese/Japanese/Korean) text where word
 *    boundaries are less defined.
 *
 * 2. **LINE_BREAK_WORD_STYLE_AUTO**
 *    Automatically applies phrase-based line breaking for short text.
 *    Previously developers had to explicitly choose LINE_BREAK_WORD_STYLE_PHRASE.
 *
 * 3. **elegantTextHeight** (default true in API 35)
 *    Was false by default in earlier APIs, meaning tall scripts (Thai, Myanmar,
 *    Tibetan) could get clipped. Now true by default for better i18n rendering.
 *
 * 4. **CJK Variable Font Support** (NotoSansCJK)
 *    Android 15 ships with NotoSansCJK variable font, supporting weight
 *    variations for Chinese, Japanese, and Korean scripts.
 */
class TextFeaturesActivity : AppCompatActivity() {

    private val TAG = "A15-Blog"

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_text_features)

        // Blog (3): Edge-to-edge insets handling (mandatory in API 35)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupInterCharJustification()
        setupLineBreakAuto()
        setupElegantTextHeight()
    }

    /**
     * Blog (3): Inter-Character Justification
     *
     * JUSTIFICATION_MODE_INTER_CHARACTER distributes extra horizontal space
     * between characters rather than only between words. This is the standard
     * justification approach in CJK typography.
     *
     * Before API 35, only JUSTIFICATION_MODE_INTER_WORD was available.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun setupInterCharJustification() {
        val tv = findViewById<TextView>(R.id.tv_inter_char_justify)
        tv.justificationMode = Layout.JUSTIFICATION_MODE_INTER_CHARACTER
        Log.i(TAG, "Blog (3): Set JUSTIFICATION_MODE_INTER_CHARACTER on TextView")
    }

    /**
     * Blog (3): Automatic Phrase-Based Line Breaking
     *
     * LINE_BREAK_WORD_STYLE_AUTO tells the system to automatically decide
     * whether to apply phrase-based line breaking based on text length and
     * language. Previously, developers had to explicitly choose PHRASE style.
     *
     * This is especially helpful for short UI text (buttons, labels, headers)
     * where breaking mid-phrase looks unnatural.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun setupLineBreakAuto() {
        val tv = findViewById<TextView>(R.id.tv_line_break_auto)
        tv.lineBreakWordStyle = LineBreakConfig.LINE_BREAK_WORD_STYLE_AUTO
        Log.i(TAG, "Blog (3): Set LINE_BREAK_WORD_STYLE_AUTO on TextView")
    }

    /**
     * Blog (3): elegantTextHeight — Now True by Default
     *
     * In APIs < 35, elegantTextHeight defaulted to false, causing tall scripts
     * (Thai สวัสดี, Myanmar, Tibetan) to potentially get clipped vertically.
     * API 35 changes the default to true, ensuring these scripts render with
     * full ascender/descender space.
     *
     * The demo text includes Thai (สวัสดีครับ), Japanese (こんにちは),
     * Chinese (你好世界), and Arabic (مرحبا) to show proper height handling.
     */
    private fun setupElegantTextHeight() {
        val tv = findViewById<TextView>(R.id.tv_elegant_text)
        Log.i(TAG, "Blog (3): elegantTextHeight is: ${tv.isElegantTextHeight} " +
                "(should be true in API 35 by default)")
    }
}
