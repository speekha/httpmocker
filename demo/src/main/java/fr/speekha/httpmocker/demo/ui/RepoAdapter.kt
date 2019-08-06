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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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

    override fun getItemCount(): Int = repos?.size ?: 1

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) = with(viewHolder) {
        val list = repos
        if (list != null) {
            val repo = list[position]
            repoName.text = repo.name
            topContributor.text = repo.topContributor ?: "Error retrieving contributor"
        } else {
            repoName.text = errorMessage ?: "No result to display"
            topContributor.text = null
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val repoName: TextView = itemView.findViewById(R.id.repoName)
        val topContributor: TextView = itemView.findViewById(R.id.topContributor)
    }

}