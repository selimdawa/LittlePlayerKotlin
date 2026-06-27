package com.flatcode.littleplayer.Unit

import android.content.Context
import androidx.preference.PreferenceManager
import com.flatcode.littleplayer.R

object THEME {

    fun setThemeOfApp(context: Context) {
        val sharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context.applicationContext)
        if (sharedPreferences.getString("color_option", "ONE") == "ONE") {
            context.setTheme(R.style.Base_Theme_OneTheme)
        } else if (sharedPreferences.getString("color_option", "TWO") == "TWO") {
            context.setTheme(R.style.Base_Theme_TwoTheme)
        } else if (sharedPreferences.getString("color_option", "THREE") == "THREE") {
            context.setTheme(R.style.Base_Theme_ThreeTheme)
        } else if (sharedPreferences.getString("color_option", "FOUR") == "FOUR") {
            context.setTheme(R.style.Base_Theme_FourTheme)
        } else if (sharedPreferences.getString("color_option", "FIVE") == "FIVE") {
            context.setTheme(R.style.Base_Theme_FiveTheme)
        } else if (sharedPreferences.getString("color_option", "SIX") == "SIX") {
            context.setTheme(R.style.Base_Theme_SixTheme)
        } else if (sharedPreferences.getString("color_option", "SEVEN") == "SEVEN") {
            context.setTheme(R.style.Base_Theme_SevenTheme)
        } else if (sharedPreferences.getString("color_option", "EIGHT") == "EIGHT") {
            context.setTheme(R.style.Base_Theme_EightTheme)
        } else if (sharedPreferences.getString("color_option", "NINE") == "NINE") {
            context.setTheme(R.style.Base_Theme_NineTheme)
        } else if (sharedPreferences.getString("color_option", "GRADUAL_ONE") == "GRADUAL_ONE") {
            context.setTheme(R.style.Base_Theme_GradientOneTheme)
        } else if (sharedPreferences.getString("color_option", "GRADUAL_TWO") == "GRADUAL_TWO") {
            context.setTheme(R.style.Base_Theme_GradientTwoTheme)
        } else if (sharedPreferences.getString("color_option", "GRADUAL_THREE") == "GRADUAL_THREE") {
            context.setTheme(R.style.Base_Theme_GradientThreeTheme)
        } else if (sharedPreferences.getString("color_option", "GRADUAL_FOUR") == "GRADUAL_FOUR") {
            context.setTheme(R.style.Base_Theme_GradientFourTheme)
        } else if (sharedPreferences.getString("color_option", "GRADUAL_FIVE") == "GRADUAL_FIVE") {
            context.setTheme(R.style.Base_Theme_GradientFiveTheme)
        } else if (sharedPreferences.getString("color_option", "GRADUAL_SIX") == "GRADUAL_SIX") {
            context.setTheme(R.style.Base_Theme_GradientSixTheme)
        } else if (sharedPreferences.getString("color_option", "GRADUAL_SEVEN") == "GRADUAL_SEVEN") {
            context.setTheme(R.style.Base_Theme_GradientSevenTheme)
        }
    }
}