package com.sully.momo.item

import com.sully.momo.Note
import com.sully.momo.R
import com.sully.momo.SettingsAdapter
import com.sully.momo.ViewLayout

@ViewLayout(R.layout.note_cell_diagram)
class DiagramCell(private val note: Note, private val cell: Note.Content.Cell) :
    SettingsAdapter.ViewInjector {

    override fun inject(viewHolder: SettingsAdapter.VH) {
        with(viewHolder.itemView) {
        }
    }
}

