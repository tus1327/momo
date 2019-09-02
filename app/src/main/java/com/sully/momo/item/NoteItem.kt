package com.sully.momo.item

import android.annotation.SuppressLint
import android.view.View
import com.sully.momo.Note
import com.sully.momo.R
import com.sully.momo.SettingsAdapter
import com.sully.momo.ViewLayout
import kotlinx.android.synthetic.main.item_note.view.*
import java.text.SimpleDateFormat
import java.util.*


@ViewLayout(R.layout.item_note)
class NoteItem(private val note: Note, private val action: (View) -> Unit) :
    SettingsAdapter.ViewInjector {
    companion object {
        @SuppressLint("ConstantLocale")
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    override fun inject(viewHolder: SettingsAdapter.VH) {
        with(viewHolder.itemView) {
            setOnClickListener { action.invoke(this) }
            titleView.text = note.meta.title
            dateView.text = df.format(Date(note.meta.updatedAt * 1000))
            notebookNameView.visibility = View.GONE
//            notebookName.text = note.notebook.name
            note.meta.tags.joinToString { it }.takeIf { it.isNotBlank() }?.let {
                tagsView.text = it
                tagsView.visibility = View.VISIBLE
            } ?: { tagsView.visibility = View.GONE }.invoke()

        }
    }
}