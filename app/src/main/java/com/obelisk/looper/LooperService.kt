package com.obelisk.looper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.widget.Toast
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.Executors

class LooperService : AccessibilityService() {
 companion object { var instance: LooperService? = null }
 private val handler = Handler(Looper.getMainLooper())
 private val executor = Executors.newSingleThreadExecutor()
 private var running = false
 private var lastChange = 0L
 private var lastBanner = false
 private var resetting = false
 private var lastSample = 0L
 private var testPending = false
 private val tick = object: Runnable { override fun run() {
   if (!running) return
   if (!resetting && SystemClock.elapsedRealtime()-lastSample>=1500) {
     lastSample=SystemClock.elapsedRealtime()
     takeScreenshot(Display.DEFAULT_DISPLAY, executor, object: TakeScreenshotCallback {
       override fun onSuccess(result: ScreenshotResult) {
         val bitmap=Bitmap.wrapHardwareBuffer(result.hardwareBuffer,result.colorSpace)
         result.hardwareBuffer.close()
         if (bitmap!=null) { val software=bitmap.copy(Bitmap.Config.ARGB_8888,false); bitmap.recycle(); val visible=hasBanner(software); software.recycle()
           handler.post {
             if (running && !resetting) {
               if (visible && !lastBanner) lastChange=SystemClock.elapsedRealtime()
               lastBanner=visible
             }
           }
         }
       }
       override fun onFailure(errorCode: Int) { }
     })
   }
   if (!resetting && SystemClock.elapsedRealtime()-lastChange>=30_000) reset()
   handler.postDelayed(this,500)
 } }
 // Detects the MAGMA/STONE floor-change panel by its characteristic flat, dark
 // rectangular region. This is a heuristic, not OCR; calibrate on the phone.
 private fun hasBanner(b: Bitmap):Boolean {
   val w=b.width; val h=b.height
   val y=(h*0.246).toInt().coerceIn(0,h-1)
   val x1=(w*0.38).toInt().coerceIn(0,w-1)
   val x2=(w*0.61).toInt().coerceIn(0,w-1)
   var dark=0; var total=0
   for (x in x1..x2 step 3) {
     val c=b.getPixel(x,y)
     val r=android.graphics.Color.red(c); val g=android.graphics.Color.green(c); val bl=android.graphics.Color.blue(c)
     if (r<75 && g<75 && bl<75) dark++
     total++
   }
   return total>0 && dark.toDouble()/total>0.82
 }
 private fun tap(x:Float,y:Float, done:()->Unit={}) {
   val dm=resources.displayMetrics; val p=Path().apply { moveTo(dm.widthPixels*x,dm.heightPixels*y) }
   val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,90)).build()
   val accepted=dispatchGesture(gesture,object: GestureResultCallback() {
     override fun onCompleted(gestureDescription:GestureDescription?) { handler.post { done() } }
     override fun onCancelled(gestureDescription:GestureDescription?) { handler.post { Toast.makeText(this@LooperService,"Tap cancelled",Toast.LENGTH_SHORT).show(); done() } }
   },null)
   if (!accepted) { Toast.makeText(this,"Tap rejected by Android",Toast.LENGTH_SHORT).show(); handler.post { done() } }
 }
 private fun reset() {
   resetting=true
   handler.post { Toast.makeText(this,"Looper: resetting to Floor 1",Toast.LENGTH_SHORT).show() }
   // Screenshot reference 461x1024: elevator (430,402), floor 1 (170,140),
   // menu close (230,755), auto (430,519).
   tap(.932f,.393f) {
     handler.postDelayed({
       tap(.369f,.137f) {
         handler.postDelayed({
           tap(.499f,.737f) {
             handler.postDelayed({
               tap(.933f,.506f) {
                 lastChange=SystemClock.elapsedRealtime(); lastBanner=false; resetting=false
               }
             },500)
           }
         },400)
       }
     },500)
   }
 }
 fun testReset() { handler.postDelayed({ if (!resetting) reset() },5000) }
 fun startLoop() { running=true; resetting=false; lastBanner=false; lastChange=SystemClock.elapsedRealtime(); handler.removeCallbacks(tick); handler.post(tick) }
 fun stopLoop() { running=false; resetting=false; handler.removeCallbacks(tick) }
 override fun onServiceConnected() { instance=this }
 override fun onAccessibilityEvent(event:AccessibilityEvent?) {}
 override fun onInterrupt() { stopLoop() }
 override fun onDestroy() { stopLoop(); instance=null; executor.shutdown(); super.onDestroy() }
}