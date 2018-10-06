package com.pinnacleimagingsystems.ambientviewer.viewer

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

class ViewerActivity : AppCompatActivity() {
    companion object {
        const val PARAM_FILE = "file"

        private const val FRAGMENT_TAG = "viewerFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFullscreen()

        with (supportFragmentManager) {
            var fragment = findFragmentByTag(FRAGMENT_TAG)
            if (fragment == null) {
                fragment = ViewerFragment().apply {
                    arguments = Bundle().apply {
                        val file = intent.extras!!.getString(PARAM_FILE)!!
                        putString(PARAM_FILE, file)
                    }
                }

                beginTransaction()
                        .add(android.R.id.content, fragment, FRAGMENT_TAG)
                        .commit()
            }
        }
    }

    private fun setupFullscreen() {
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                makeUiFullscreen()
            }
        }

        makeUiFullscreen()
    }

    private fun makeUiFullscreen() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
    }
}
