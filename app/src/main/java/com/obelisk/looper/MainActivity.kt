package com.obelisk.looper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(35,55,35,20) }
  layout.addView(TextView(this).apply { text="Obelisk Floor Looper\n\n30-second floor timeout\n\nEnable Accessibility, open the game, then press Start. The app uses the screenshot proportions you provided. Keep the game open.\n\nExperimental: screen layouts and popup detection may require calibration."; textSize=18f })
  fun btn(label:String, action:()->Unit) { layout.addView(Button(this).apply { text=label; setOnClickListener { action() } }) }
  btn("Enable Accessibility") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
  btn("Start") { if (LooperService.instance==null) Toast.makeText(this,"Enable Accessibility first",Toast.LENGTH_LONG).show() else { LooperService.instance?.startLoop(); Toast.makeText(this,"Started",Toast.LENGTH_SHORT).show() } }
  btn("Test reset (switch to game within 5 seconds)") { if (LooperService.instance==null) Toast.makeText(this,"Enable Accessibility first",Toast.LENGTH_LONG).show() else { LooperService.instance?.testReset(); Toast.makeText(this,"Switch to game NOW",Toast.LENGTH_LONG).show() } }
  btn("Stop") { LooperService.instance?.stopLoop() }
  setContentView(layout)
 }
}