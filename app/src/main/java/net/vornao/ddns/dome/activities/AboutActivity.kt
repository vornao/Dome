package net.vornao.ddns.dome.activities

import net.vornao.ddns.dome.handler.DatabaseHelper.Companion.getInstance
import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.os.Bundle
import net.vornao.ddns.dome.R
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.ViewGroup
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import java.util.*

class AboutActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables") //we don't need this for now
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val relativeLayout = findViewById<RelativeLayout>(R.id.relativeAbout)
        val resIDs = intArrayOf(R.drawable.ic_outline_lightbulb_24, R.drawable.ic_lightbulb)
        val rnd = Random()
        relativeLayout.setOnTouchListener { v: View, event: MotionEvent ->

            // easter egg
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                val lp = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT)
                val iv = ImageView(applicationContext)
                lp.setMargins(x, y, 0, 0)
                iv.layoutParams = lp
                iv.setImageDrawable(resources.getDrawable(resIDs[rnd.nextInt(resIDs.size) % 2], theme))
                iv.animate()
                        .translationY(resources.displayMetrics.heightPixels / 3f)
                        .setInterpolator(AccelerateInterpolator())
                        .setInterpolator(BounceInterpolator()).duration = 1500
                (v as ViewGroup).addView(iv)
            }
            false
        }


        // animate view every 1500ms
        val textViews = arrayOf(
                findViewById(R.id.apptitle),
                findViewById(R.id.subtitle),
                findViewById(R.id.description0),
                findViewById(R.id.description1),
                findViewById(R.id.description2),
                findViewById(R.id.footer0),
                findViewById<TextView>(R.id.footer1))
        for (t in textViews) {
            t.visibility = View.GONE
            t.animation = AnimationUtils.loadAnimation(applicationContext, R.anim.textview_animation)
        }
        Thread {
            for (t in textViews) {
                try {
                    Thread.sleep(1500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                runOnUiThread { t.visibility = View.VISIBLE }
            }
        }.start()
    }
}