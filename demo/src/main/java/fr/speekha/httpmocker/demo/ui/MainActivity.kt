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

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import fr.speekha.httpmocker.MockResponseInterceptor
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModel<MainViewModel>()
    private val adapter = RepoAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initObservers()
        initViews()
    }

    private fun initObservers() {
        observeDataLoading()
        observeState()
    }

    private fun initViews() {
        results.adapter = adapter
        results.layoutManager = LinearLayoutManager(this)
        setupListeners()
    }

    private fun setupListeners() {
        radioState.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.setMode(
                    when (checkedId) {
                        R.id.stateEnabled -> MockResponseInterceptor.Mode.ENABLED
                        R.id.stateMixed -> MockResponseInterceptor.Mode.MIXED
                        R.id.stateRecord -> MockResponseInterceptor.Mode.RECORD
                        else -> MockResponseInterceptor.Mode.DISABLED
                    }
                )
            } else {
                if (-1 == radioState.checkedButtonId) radioState.check(R.id.stateDisabled)
            }
        }

        btnCall.setOnClickListener {
            viewModel.callService()
        }
    }

    private fun observeState() {
        observe(viewModel.getState()) { state ->
            when (state) {
                is State.Permission -> checkPermission()
                is State.Message -> updateDescriptionLabel(state.message)
            }
        }
    }

    private fun observeDataLoading() {
        observe(viewModel.getData()) { data ->
            when (data) {
                is Data.Loading -> showLoading(true)
                is Data.Success -> setResult(data.repos)
                is Data.Error -> setError(data.message)
            }
        }
    }

    private fun showLoading(visible: Boolean) {
        results.isVisible = !visible
        loader.isVisible = visible
    }

    private fun setResult(result: List<Repo>) {
        showLoading(false)
        adapter.repos = result
        adapter.notifyDataSetChanged()
    }

    private fun setError(message: String?) {
        showLoading(false)
        adapter.repos = null
        adapter.errorMessage = message
        adapter.notifyDataSetChanged()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    private fun updateDescriptionLabel(@StringRes resId: Int) {
        tvMessage.setText(resId)
    }
}

fun <T : Any, L : LiveData<T>> LifecycleOwner.observe(liveData: L, body: (T?) -> Unit) {
    liveData.observe(this, Observer(body))
}
