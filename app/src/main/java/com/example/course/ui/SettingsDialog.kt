package com.example.course.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.course.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.rg_theme)
        val rbLight = view.findViewById<RadioButton>(R.id.rb_light)
        val rbDark = view.findViewById<RadioButton>(R.id.rb_dark)

        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentTheme = prefs.getString("theme", "light")

        if (currentTheme == "light") {
            rbLight.isChecked = true
        } else {
            rbDark.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = if (checkedId == R.id.rb_light) "light" else "dark"
            prefs.edit().putString("theme", newTheme).apply()
            
            // Notify MainActivity to update background
            (activity as? MainActivity)?.updateTheme(newTheme)
            dismiss()
        }
    }

    companion object {
        const val TAG = "SettingsDialog"
    }
}
