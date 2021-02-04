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
        val lt = LoadToast(this)
            .setProgressColor(Color.RED)
            .setText(text)
            .setTranslationY(100)
            .setBorderColor(Color.LTGRAY)
            .show()
        val root = findViewById<View>(android.R.id.content) as ViewGroup
        val v = View(this)
        v.setBackgroundColor(Color.RED)
        findViewById<View>(R.id.show).setOnClickListener { lt.show() }
        findViewById<View>(R.id.error).setOnClickListener { lt.error() }
        findViewById<View>(R.id.success).setOnClickListener { lt.success() }
        findViewById<View>(R.id.refresh).setOnClickListener {
            val v = View(this@MainActivity)
            v.setBackgroundColor(Color.rgb((Math.random() * 255).toInt(), (Math.random() * 255).toInt(), (Math.random() * 255).toInt()))
            root.addView(v, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400))
        }
        findViewById<View>(R.id.hide).setOnClickListener { lt.hide() }
    }
}