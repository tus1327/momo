package com.sully.momo.item

import com.sully.momo.Note
import com.sully.momo.R
import com.sully.momo.SettingsAdapter
import com.sully.momo.ViewLayout
import kotlinx.android.synthetic.main.note_cell_latex.view.*
import java.util.regex.Pattern

@ViewLayout(R.layout.note_cell_latex)
class LatexCell(private val note: Note, private val cell: Note.Content.Cell) :
    SettingsAdapter.ViewInjector {

    override fun inject(viewHolder: SettingsAdapter.VH) {
        with(viewHolder.itemView) {

            val pattern = Pattern.compile("(\\\$)(.*)(\\\$)")
            val matcher = pattern.matcher(cell.data)
            val data = matcher.replaceAll("\\\\($2\\\\)")
            mathView.text = data
        }
    }

}

