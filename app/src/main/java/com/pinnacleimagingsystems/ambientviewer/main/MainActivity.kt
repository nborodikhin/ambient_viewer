package com.pinnacleimagingsystems.ambientviewer.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import com.pinnacleimagingsystems.ambientviewer.R
import com.pinnacleimagingsystems.ambientviewer.viewer.ViewerActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val PICK_IMAGE_CODE = 123
    }

    private val views by lazy {
        object {
            val loadImageButton = findViewById<View>(R.id.load_image)
            val event = findViewById<TextView>(R.id.event)
        }
    }

    lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = ViewModelProviders.of(this)[MainPresenterImpl::class.java].apply {
            init(application)
        }

        views.loadImageButton.setOnClickListener { _ -> onLoadButtonClicked() }
    }

    override fun onStart() {
        super.onStart()

        presenter.state.eventDescription.observe(this, Observer { text ->
            views.event.text = text
        })

        presenter.state.event.observe(this, Observer { event -> event!!.consume(::onEvent) })
    }

    private fun onEvent(event: MainPresenter.State.Event) {
        when(event) {
            is MainPresenter.State.Event.FileLoaded -> {
                val intent = Intent(this, ViewerActivity::class.java).apply {
                    putExtra(ViewerActivity.PARAM_FILE, event.file)
                }
                startActivity(intent)
            }
        }
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
