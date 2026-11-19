package com.ggkeymapper

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {
    private val green = Color.rgb(57, 232, 121)
    private val black = Color.rgb(5, 9, 7)
    private lateinit var editor: HudEditorView
    private lateinit var keyInput: EditText
    private lateinit var profileInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        showEditor()
    }

    private fun showEditor() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 10, 14, 10); setBackgroundColor(black) }
        val title = TextView(this).apply { text = "GG KEY MAPPER  •  EDITOR HUD"; textSize = 21f; setTextColor(green); gravity = Gravity.CENTER }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        status = TextView(this).apply { text = "Selecione uma tecla e arraste o marcador para a posição desejada."; textSize = 12f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }
        root.addView(status)
        editor = HudEditorView(this).apply {
            mappings = MappingStore.load(this@MainActivity)
            if (mappings.isEmpty()) mappings.add(KeyMapping(KeyEvent.KEYCODE_BUTTON_A, .5f, .5f))
            selectedIndex = 0
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        root.addView(editor)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        keyInput = EditText(this).apply { hint = "Código da tecla (ex.: KeyCode 96)"; inputType = InputType.TYPE_CLASS_NUMBER; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine(true); setText(editor.mappings.getOrNull(0)?.keyCode?.toString() ?: "") }
        controls.addView(keyInput, LinearLayout.LayoutParams(0, dp(48), 1f))
        controls.addView(Button(this).apply {
            text = "ADICIONAR"
            setOnClickListener {
                val code = keyInput.text.toString().toIntOrNull()
                if (code == null) { toast("Digite o código numérico da tecla."); return@setOnClickListener }
                editor.mappings.add(KeyMapping(code, .5f, .5f)); editor.selectedIndex = editor.mappings.lastIndex; editor.invalidate(); status.text = "Tecla $code adicionada. Arraste o marcador."
            }
        })
        root.addView(controls)
        val profileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        profileInput = EditText(this).apply { hint = "Nome do perfil"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine(true) }
        profileRow.addView(profileInput, LinearLayout.LayoutParams(0, dp(48), 1f))
        profileRow.addView(Button(this).apply { text = "SALVAR PERFIL"; setOnClickListener { MappingStore.saveProfile(this@MainActivity, profileInput.text.toString(), editor.mappings); toast("Perfil salvo neste aparelho.") } })
        profileRow.addView(Button(this).apply { text = "CARREGAR"; setOnClickListener { chooseProfile() } })
        root.addView(profileRow)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        actions.addView(Button(this).apply {
            text = "SALVAR MAPEAMENTO"
            setOnClickListener {
                val code = keyInput.text.toString().toIntOrNull()
                if (editor.selectedIndex in editor.mappings.indices && code != null) editor.mappings[editor.selectedIndex] = editor.mappings[editor.selectedIndex].copy(keyCode = code)
                MappingStore.save(this@MainActivity, editor.mappings); toast("Mapeamento salvo.")
            }
        })
        actions.addView(Button(this).apply { text = "ACESSIBILIDADE"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        actions.addView(Button(this).apply { text = "ATIVAR BOLINHA"; setOnClickListener { startOverlay() } })
        actions.addView(Button(this).apply {
            text = "REMOVER SELECIONADO"
            setOnClickListener {
                if (editor.selectedIndex in editor.mappings.indices) {
                    editor.mappings.removeAt(editor.selectedIndex); editor.selectedIndex = editor.mappings.lastIndex; editor.invalidate()
                    keyInput.setText(editor.mappings.getOrNull(editor.selectedIndex)?.keyCode?.toString() ?: "")
                }
            }
        })
        root.addView(actions)
        editor.onSelectionChanged = { keyInput.setText(editor.mappings.getOrNull(editor.selectedIndex)?.keyCode?.toString() ?: "") }
        setContentView(root)
    }

    private fun chooseProfile() {
        val names = MappingStore.profileNames(this)
        if (names.isEmpty()) { toast("Nenhum perfil salvo."); return }
        android.app.AlertDialog.Builder(this).setTitle("Carregar perfil").setItems(names.toTypedArray()) { _, which ->
            val loaded = MappingStore.loadProfile(this, names[which])
            if (loaded != null) {
                editor.mappings = loaded; editor.selectedIndex = if (loaded.isEmpty()) -1 else 0; editor.invalidate()
                keyInput.setText(loaded.firstOrNull()?.keyCode?.toString() ?: ""); toast("Perfil carregado.")
            }
        }.show()
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName")))
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
