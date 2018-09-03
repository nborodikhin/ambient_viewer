package com.pinnacleimagingsystems.ambientviewer.viewer

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.pinnacleimagingsystems.ambientviewer.R
import java.io.File

class ViewerActivity : AppCompatActivity() {
    companion object {
        const val PARAM_FILE = "file"
    }

    private val views by lazy { object {
        val image = findViewById<ImageView>(R.id.image)
    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val file = intent.extras!!.getString(PARAM_FILE)!!

        views.image.setImageURI(Uri.fromFile(File(file)))
    }
}
