package fr.speekha.httpmocker.demo.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(), MainContract.View {

    private val presenter: MainContract.Presenter by inject()

    var adapter = RepoAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter.view = this

        radioState.setOnCheckedChangeListener { _, checkedId ->
            presenter.setMode(when(checkedId) {
                R.id.stateEnabled -> 1
                R.id.stateMixed -> 2
                else -> 0
            })
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
}
