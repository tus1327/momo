package com.sully.momo.item

import com.sully.momo.R
import com.sully.momo.SettingsAdapter
import com.sully.momo.ViewLayout
import kotlinx.android.synthetic.main.note_cell_text.view.*

@ViewLayout(R.layout.note_cell_text)
class ErrorCell : SettingsAdapter.ViewInjector {

    override fun inject(viewHolder: SettingsAdapter.VH) {
        with(viewHolder.itemView) {
            textView.text = "ERROR"
        }
    }
}

