package com.sully.momo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.navigation.NavigationView
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.sully.momo.item.NoteItem
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_SIGN_IN = 1234
        const val TAG = "MainActivity"
        const val INBOX = "Inbox"
        const val TRASH = "Trash"
        const val ALL = "All"
    }

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val disposables = CompositeDisposable()

    private val notes: MutableList<Note> = mutableListOf()
    private val adapter = SettingsAdapter.newInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeButtonEnabled(true)

        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerContainer,
            toolbar,
            R.string.notebooks,
            R.string.notebooks
        )
        drawerContainer.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        drawer.setNavigationItemSelectedListener(navigationItemSelectedListener)

        Timber.tag(TAG).d("MainActivity.onCreate")

        GoogleSignIn.getLastSignedInAccount(this)?.let { googleAccount ->
            onReadyAccount(googleAccount)
        } ?: {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE))
                .build()
            val client = GoogleSignIn.getClient(this, signInOptions)
            startActivityForResult(client.signInIntent, REQUEST_SIGN_IN)
        }.invoke()

        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult=$requestCode")
        when (requestCode) {
            REQUEST_SIGN_IN -> {
                if (resultCode == RESULT_OK && data != null) {
                    GoogleSignIn.getSignedInAccountFromIntent(data).result?.let { googleAccount ->
                        onReadyAccount(googleAccount)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onReadyAccount(googleAccount: GoogleSignInAccount) {
        val drive = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            GoogleAccountCredential.usingOAuth2(this, listOf(DriveScopes.DRIVE_FILE)).apply {
                selectedAccount = googleAccount.account
            }
        ).setApplicationName(getString(R.string.app_name)).build()

        DriveApi.initialize(drive, getExternalFilesDir(null)!!)

        Completable.fromAction { DriveApi.syncAll() }
            .subscribeOn(Schedulers.io())
            .subscribeBy {
                loadAllNotebooks()
            }.addTo(disposables)
    }


    private fun loadAllNotebooks() {
        DriveApi.loadNotebooks()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { notebooks -> updateDrawer(notebooks) }
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMap { DriveApi.loadNotes(it).toObservable() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = { notes ->
                this.notes.addAll(notes)
                this.notes.sortByDescending { it.meta.updatedAt }
                updateUI()
            }, onError = { e ->
                Timber.e(e)

            })
            .addTo(disposables)
    }

    private fun updateUI() {
        adapter.reloadItems(notes.map { note ->
            NoteItem(note) { startActivity(NoteActivity.newIntent(this, note)) }
        })
        adapter.notifyDataSetChanged()
    }

    private val menuMap: MutableMap<Int, Notebook> = mutableMapOf()

    private fun updateDrawer(notebooks: List<Notebook>) {
        Timber.tag(TAG).d("$notebooks")
        val notebookList = notebooks.sortedWith(Comparator { o1, o2 ->
            when {
                o1.meta.uuid == INBOX -> return@Comparator -1
                o2.meta.uuid == INBOX -> return@Comparator 1
                o1.meta.uuid == TRASH -> return@Comparator -1
                o2.meta.uuid == TRASH -> return@Comparator 1
                else -> o1.meta.name.compareTo(o2.meta.name)
            }
        })

        menuMap.clear()
        drawer.menu.clear()
        notebookList.forEachIndexed { index, notebook ->
            val menuIndex = 200 + index
            menuMap[menuIndex] = notebook
            drawer.menu.add(2, menuIndex, 0, notebook.meta.name)
        }
    }

    private val navigationItemSelectedListener =
        NavigationView.OnNavigationItemSelectedListener { menuItem ->
            this.notes.clear()
            menuMap[menuItem.itemId]?.let { notebook ->
                DriveApi.loadNotes(notebook).observeOn(AndroidSchedulers.mainThread()).subscribeBy {
                    this.notes.addAll(it)
                    this.notes.sortByDescending { it.meta.updatedAt }
                    updateUI()
                }
            }
            drawerContainer.closeDrawers()
            true
        }
}


