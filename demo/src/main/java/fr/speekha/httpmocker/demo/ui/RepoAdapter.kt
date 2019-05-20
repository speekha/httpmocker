package fr.speekha.httpmocker.demo.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import fr.speekha.httpmocker.demo.R
import fr.speekha.httpmocker.demo.model.Repo

class RepoAdapter(
    private val context: Context
) : RecyclerView.Adapter<RepoAdapter.ViewHolder>() {

    var repos: List<Repo>? = null

    var errorMessage: String? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.repo_layout, viewGroup, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = repos?.let { it.size } ?: 1

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) = with(viewHolder) {
        val list = repos
        if (list != null) {
            textView.text = list[position].name
        } else {
            textView.text = errorMessage ?: "No result to display"
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.text)
    }

}