package com.sully.momo

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType

@Keep
object Preferences {
    private const val NAME = "momo"

    private lateinit var mSharedPreferences: SharedPreferences

    fun initialize(context: Context): Preferences {
        mSharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return this
    }

    var startPageToken: String by PreferenceDelegate()

    class PreferenceDelegate<T> : ReadWriteProperty<Any?, T> {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return when (property.returnType) {
                String::class.createType() -> mSharedPreferences.getString(property.name, "") as T
                Float::class.createType() -> mSharedPreferences.getFloat(property.name, 0.0f) as T
                Int::class.createType() -> mSharedPreferences.getInt(property.name, 0) as T
                Boolean::class.createType() -> mSharedPreferences.getBoolean(
                    property.name,
                    false
                ) as T
                Long::class.createType() -> mSharedPreferences.getLong(property.name, 0L) as T
                else -> throw IllegalArgumentException("${property.name} variable type is not supported.")
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val editor = mSharedPreferences.edit()
            when (value) {
                is String -> editor.putString(property.name, value)
                is Float -> editor.putFloat(property.name, value)
                is Int -> editor.putInt(property.name, value)
                is Boolean -> editor.putBoolean(property.name, value)
                is Long -> editor.putLong(property.name, value)
                else -> throw IllegalArgumentException("${property.name} variable type is not supported.")
            }
            editor.apply()
        }
    }
}