package com.flatcode.littleplayer.utils

import android.content.Context
import androidx.preference.PreferenceManager
import com.flatcode.littleplayer.R

object THEME {
    fun setThemeOfApp(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val option = prefs.getString("color_option", "ONE")

        val themeRes = when (option) {
            "ONE" -> R.style.Base_Theme_OneTheme
            "TWO" -> R.style.Base_Theme_TwoTheme
            "THREE" -> R.style.Base_Theme_ThreeTheme
            "FOUR" -> R.style.Base_Theme_FourTheme
            "FIVE" -> R.style.Base_Theme_FiveTheme
            "SIX" -> R.style.Base_Theme_SixTheme
            "SEVEN" -> R.style.Base_Theme_SevenTheme
            "EIGHT" -> R.style.Base_Theme_EightTheme
            "NINE" -> R.style.Base_Theme_NineTheme
            "GRADUAL_ONE" -> R.style.Base_Theme_GradientOneTheme
            "GRADUAL_TWO" -> R.style.Base_Theme_GradientTwoTheme
            "GRADUAL_THREE" -> R.style.Base_Theme_GradientThreeTheme
            "GRADUAL_FOUR" -> R.style.Base_Theme_GradientFourTheme
            "GRADUAL_FIVE" -> R.style.Base_Theme_GradientFiveTheme
            "GRADUAL_SIX" -> R.style.Base_Theme_GradientSixTheme
            "GRADUAL_SEVEN" -> R.style.Base_Theme_GradientSevenTheme
            else -> R.style.Base_Theme_OneTheme
        }
        context.setTheme(themeRes)
    }
}