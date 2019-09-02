package com.sully.momo

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep

@Keep
object DriveIds {
    private const val NAME = "momo-driveIds"

    private lateinit var mSharedPreferences: SharedPreferences

    fun initialize(context: Context): DriveIds {
        mSharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return this
    }

    operator fun get(index: String): String? {
        return mSharedPreferences.getString(index, null)
    }

    operator fun set(index: String, path: String) {
        mSharedPreferences.edit().let {
            it.putString(index, path)
            it.apply()
        }
    }
}