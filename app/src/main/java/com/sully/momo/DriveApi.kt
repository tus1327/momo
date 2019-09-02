package com.sully.momo

import android.util.LruCache
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import java.io.FileReader
import com.google.api.services.drive.model.File as DriveFile


object DriveApi {
    fun initialize(drive: Drive, externalFileDir: File) {
        this.drive = drive
        this.externalFileDir = externalFileDir
    }

    private lateinit var drive: Drive
    private lateinit var externalFileDir: File

    private val TAG = "DriveSync"

    private val rootDirectoryName = "Quiver.qvlibrary"

    private val updatedItems = PublishSubject.create<Pair<DriveFile, File>>()

    fun syncAll() {
        if (Preferences.startPageToken.isNotBlank()) {
            Timber.d("has startPageToken : ${Preferences.startPageToken}")
            val newStartPageToken = updateChanges()
            Preferences.startPageToken = newStartPageToken
        } else {
            val startPageToken = drive.Changes().startPageToken.request().blockingGet()
            val rootFiles = drive.Files().list().apply {
                q = "name = '${rootDirectoryName}' and trashed = false"
                fields = "files(id, mimeType,  name, modifiedTime, size)"
                spaces = "drive"
            }.request().blockingGet()

            rootFiles.files.firstOrNull()?.let { driveFile ->
                syncItem(driveFile, File(externalFileDir, driveFile.name))
            }

            Preferences.startPageToken = startPageToken.startPageToken
        }
    }

    private fun syncItem(driveFile: DriveFile, localFile: File) {
        when (driveFile.mimeType) {
            "application/vnd.google-apps.folder" -> {
                if (!localFile.exists()) {
                    localFile.mkdirs()
                    Timber.tag(TAG).d("mkdir : ${localFile.absolutePath}")
                    DriveIds[driveFile.id] = localFile.absolutePath
                    updatedItems.onNext(driveFile to localFile)
                }

                val fileList = drive.Files().list().apply {
                    q = "'${driveFile.id}' in parents and trashed = false"
                    fields = "files(id, mimeType,  name, modifiedTime, size)"
                    spaces = "drive"
                }.request().blockingGet()
                fileList.files.forEach { f ->
                    syncItem(f, File(localFile, f.name))
                }
            }
            else -> {
                with(localFile) {
                    if (!exists()) {
                        createNewFile()
                    }

                    if (length() != driveFile.getSize() || lastModified() != driveFile.modifiedTime.value) {
                        outputStream().use {
                            drive.files().get(driveFile.id).executeMediaAndDownloadTo(it)
                        }
                        setLastModified(driveFile.modifiedTime.value)
                        Timber.tag(TAG).d("download : ${this.absolutePath}")
                        DriveIds[driveFile.id] = localFile.absolutePath
                        updatedItems.onNext(driveFile to localFile)
                    }
                }
            }
        }
    }

    private fun updateChanges(): String {
        val changeList = drive.Changes()
            .list(Preferences.startPageToken)
            .apply {
                fields = "newStartPageToken, changes(file(id, mimeType,  name, modifiedTime, size))"
            }
            .request().blockingGet()

        changeList.changes.forEach { change ->
            Timber.d("change : $change")
            DriveIds[change.file.id]?.let { filePath ->
                syncItem(change.file, File(filePath))
            }
        }
        return changeList.newStartPageToken
    }

    private fun <T> DriveRequest<T>.request(): Single<T> {
        return Single.fromCallable {
            Timber.tag("DC").v(">> $this")
            val result = execute()
            Timber.tag("DC").v("<< $result")
            return@fromCallable result
        }.subscribeOn(Schedulers.io())
    }

    fun loadNotebooks(): Single<List<Notebook>> {
        return Single.fromCallable {
            File(externalFileDir, rootDirectoryName)
                .listFiles { _, name -> name.endsWith("qvnotebook") }
                .map { file ->
                    notebookMap[file] ?: {
                        val metaFile = file.listFiles { _, name -> name == "meta.json" }.first()
                        val type = object : TypeToken<Notebook.Meta>() {}.type
                        val meta =
                            Gson().fromJson<Notebook.Meta>(JsonReader(FileReader(metaFile)), type)
                        val notebook = Notebook(file, meta)
                        notebookMap.put(file, notebook)
                        notebook

                    }.invoke()
                }
        }.subscribeOn(Schedulers.io())
    }

    fun loadNotes(notebook: Notebook): Single<List<Note>> {
        return Single.fromCallable {
            notebook.dir.listFiles { _, name -> name.endsWith("qvnote") }
                .map { file ->
                    loadNote(file)
                }
        }.subscribeOn(Schedulers.io())
    }

    fun loadNote(file: File): Note {
        return noteMap[file] ?: {
            val note = Note(file)
            noteMap.put(file, note)
            note
        }.invoke()
    }

    private val notebookMap: LruCache<File, Notebook> = LruCache(1000)
    private val noteMap: LruCache<File, Note> = LruCache(1000)
}