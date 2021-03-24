package net.steamcrafted.loadtoastlib

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import net.steamcrafted.loadtoast.LoadToast

// Example activity
class MainActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val text = "This is a long loading text..."

        val loadToast = LoadToast(this)
                .setProgressColor(Color.RED)
                .setText(text)
                .setTranslationY(100)
                .setBorderColor(Color.LTGRAY)
                .show()

        findViewById<View>(R.id.show).setOnClickListener {
            loadToast.show()
        }

        findViewById<View>(R.id.error).setOnClickListener {
            loadToast.error()
        }

        findViewById<View>(R.id.success).setOnClickListener {
            loadToast.success()
        }

        val v = View(this)
        v.setBackgroundColor(Color.RED)
        val root = findViewById<View>(android.R.id.content) as ViewGroup
        findViewById<View>(R.id.refresh).setOnClickListener {
            v.setBackgroundColor(Color.rgb((Math.random() * 255).toInt(), (Math.random() * 255).toInt(), (Math.random() * 255).toInt()))
            root.addView(v, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400))
        }

        findViewById<View>(R.id.hide).setOnClickListener {
            loadToast.hide()
        }
    }
}