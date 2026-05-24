/*
 * Copyright 2019-2021 David Blanc
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

package fr.speekha.httpmocker.demo.ui.legacy

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import fr.speekha.httpmocker.Mode
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.databinding.ActivityMainBinding
import fr.speekha.httpmocker.demo.model.Repo
import fr.speekha.httpmocker.demo.ui.Data
import fr.speekha.httpmocker.demo.ui.MainViewModel
import fr.speekha.httpmocker.demo.ui.Permission
import fr.speekha.httpmocker.demo.ui.State
import io.uniflow.android.livedata.onEvents
import io.uniflow.android.livedata.onStates
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModel<MainViewModel>()
    private val adapter = RepoAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initObservers()
        initViews()
    }

    private fun initObservers() {

        onStates(viewModel) { state ->
            if (state is State) {
                updateDescriptionLabel(state.message)
                when (val data = state.data) {
                    is Data.Loading -> showLoading(true)
                    is Data.Success -> setResult(data.repos)
                    is Data.Error -> setError(data.message)
                    is Data.Empty -> setEmptyList()
                }
            }
        }
        onEvents(viewModel) { uiEvent ->
            if (uiEvent is Permission) {
                checkPermission()
            }
        }
    }

    private fun initViews() {
        binding.results.adapter = adapter
        binding.results.layoutManager = LinearLayoutManager(this)
        setupListeners()
    }

    private fun setupListeners() {
        binding.radioState.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.setMode(
                    when (checkedId) {
                        R.id.stateEnabled -> Mode.ENABLED
                        R.id.stateMixed -> Mode.MIXED
                        R.id.stateRecord -> Mode.RECORD
                        else -> Mode.DISABLED
                    }
                )
            } else {
                if (-1 == binding.radioState.checkedButtonId) binding.radioState.check(R.id.stateDisabled)
            }
        }

        binding.btnCall.setOnClickListener {
            viewModel.callService()
        }
    }

    private fun showLoading(visible: Boolean) {
        binding.results.isVisible = !visible
        binding.loader.isVisible = visible
    }

    private fun setResult(result: List<Repo>?) {
        showLoading(false)
        adapter.repos = result
        adapter.notifyDataSetChanged()
    }

    private fun setEmptyList() {
        showLoading(false)
        adapter.repos = null
        adapter.notifyDataSetChanged()
    }

    private fun setError(message: String?) {
        showLoading(false)
        adapter.repos = null
        adapter.errorMessage = message
        adapter.notifyDataSetChanged()
    }

    private fun checkPermission() {
        if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    private fun updateDescriptionLabel(@StringRes resId: Int) {
        binding.tvMessage.setText(resId)
    }
}
