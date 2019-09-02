package com.sully.momo.item

import com.sully.momo.Note
import com.sully.momo.R
import com.sully.momo.SettingsAdapter
import com.sully.momo.ViewLayout
import kotlinx.android.synthetic.main.note_cell_text.view.*


@ViewLayout(R.layout.note_cell_text)
class TextCell(private val note: Note, private val cell: Note.Content.Cell) :
    SettingsAdapter.ViewInjector {

    override fun inject(viewHolder: SettingsAdapter.VH) {
        with(viewHolder.itemView) {
            textView.setHtml(cell.data)
        }
    }
}
