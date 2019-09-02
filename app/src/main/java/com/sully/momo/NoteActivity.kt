package com.sully.momo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.sully.momo.item.*
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.content_note.*
import java.io.File

class NoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        val file = File(intent.getStringExtra(EXTRA_FILE) ?: "")
        if (!file.exists()) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
            finish()
        }

        setSupportActionBar(toolbar)


        val note = DriveApi.loadNote(file)
        title = note.content.title
        val items = note.content.cells.map {
            when (it.type) {
                "text" -> TextCell(note, it)
                "markdown" -> MarkdownCell(note, it)
                "code" -> CodeCell(note, it)
                "latex" -> LatexCell(note, it)
                "diagram" -> DiagramCell(note, it)
                else -> ErrorCell()
            }
        }

        recyclerView.adapter = SettingsAdapter.newInstance(items)
//        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))


        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    companion object {

        const val TAG = "NoteActivity"
        const val EXTRA_FILE = "Extra.File"

        fun newIntent(context: Context, note: Note): Intent {
            return Intent(context, NoteActivity::class.java)
                .putExtra(EXTRA_FILE, note.dir.absolutePath)
        }
    }

}
