package com.sully.momo.item

import com.sully.momo.Note
import com.sully.momo.R
import com.sully.momo.SettingsAdapter
import com.sully.momo.ViewLayout
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import kotlinx.android.synthetic.main.note_cell_text.view.*


@ViewLayout(R.layout.note_cell_text)
class MarkdownCell(private val note: Note, private val cell: Note.Content.Cell) :
    SettingsAdapter.ViewInjector {

    override fun inject(viewHolder: SettingsAdapter.VH) {
        with(viewHolder.itemView) {
            Markwon.builder(context).apply {
                usePlugins(
                    listOf(
                        TablePlugin.create(context),
                        StrikethroughPlugin.create(),
                        TaskListPlugin.create(context)
                    )
                )
            }.build().apply {
                setParsedMarkdown(textView, render(parse(cell.data)))
            }
        }
    }
}