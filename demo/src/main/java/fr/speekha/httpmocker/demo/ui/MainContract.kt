package fr.speekha.httpmocker.demo.ui

import fr.speekha.httpmocker.demo.model.Repo

interface MainContract {

    interface View {
        fun setResult(result: List<Repo>)
        fun setError(message: String?)
        fun checkPermission()
        fun updateStorageLabel(enabled: Boolean)
    }

    interface Presenter {
        fun stop()
        fun callService()
        fun setMode(mode: Int)
    }
}