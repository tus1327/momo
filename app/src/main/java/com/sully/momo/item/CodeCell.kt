package com.sully.momo.item

import com.sully.momo.Note
import com.sully.momo.R
import com.sully.momo.SettingsAdapter
import com.sully.momo.ViewLayout
import kotlinx.android.synthetic.main.note_cell_code.view.*


@ViewLayout(R.layout.note_cell_code)
class CodeCell(private val note: Note, private val cell: Note.Content.Cell) :
    SettingsAdapter.ViewInjector {

    override fun inject(viewHolder: SettingsAdapter.VH) {
        with(viewHolder.itemView) {
            codeView.setCode(cell.data, cell.language)
        }
    }
}
