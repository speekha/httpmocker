/*
 * Copyright 2019 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.speekha.httpmocker.demo.ui

import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf


class MainActivity : AppCompatActivity(), MainContract.View {
    private val presenter: MainContract.Presenter by inject { parametersOf(this) }

    private val adapter = RepoAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radioState.setOnCheckedChangeListener { _, checkedId ->
            presenter.setMode(
                when (checkedId) {
                    R.id.stateEnabled -> 1
                    R.id.stateMixed -> 2
                    R.id.stateRecord -> 3
                    else -> 0
                }
            )
        }

        btnCall.setOnClickListener {
            presenter.callService()
        }

        results.adapter = adapter
        results.layoutManager = LinearLayoutManager(this)
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun setResult(result: List<Repo>) {
        adapter.repos = result
        adapter.notifyDataSetChanged()
    }

    override fun setError(message: String?) {
        adapter.repos = null
        adapter.errorMessage = message
        adapter.notifyDataSetChanged()
    }

    override fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    override fun updateStorageLabel(enabled: Boolean) {
        tvDirectory.isEnabled = enabled
    }
}
