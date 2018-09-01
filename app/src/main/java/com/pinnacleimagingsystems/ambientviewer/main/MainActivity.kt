package com.pinnacleimagingsystems.ambientviewer.main

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import com.pinnacleimagingsystems.ambientviewer.R

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val PICK_IMAGE_CODE = 123
    }

    class Views(activity: Activity) {
        val loadImageButton: View = activity.findViewById(R.id.load_image)
        val event: TextView = activity.findViewById(R.id.event)
    }

    lateinit var views: Views

    lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = ViewModelProviders.of(this)[MainPresenterImpl::class.java].apply {
            init(application)
        }

        views = Views(this)

        views.loadImageButton.setOnClickListener { _ -> onLoadButtonClicked() }
    }

    override fun onStart() {
        super.onStart()

        presenter.state.event.observe(this, Observer { text ->
            views.event.text = text
        })
    }

    private fun onLoadButtonClicked() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        if (intent.resolveActivity(packageManager) == null) {
            return
        }

        startActivityForResult(intent, PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_CODE && data != null && data.data != null) {
            presenter.onFileSelected(data.data!!)
        }
    }
}
