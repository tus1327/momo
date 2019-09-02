package com.sully.momo

import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

class SettingsAdapter private constructor(private var items: List<ViewInjector>) :
    RecyclerView.Adapter<SettingsAdapter.VH>() {

    companion object {
        fun newInstance(): SettingsAdapter {
            return SettingsAdapter(mutableListOf())
        }

        fun newInstance(items: List<ViewInjector>): SettingsAdapter {
            return SettingsAdapter(items)
        }

        val viewTypeLayoutMap = mutableMapOf<Int, Int>()
        val classViewTypeMap = mutableMapOf<KClass<*>, Int>()
        val incremental = AtomicInteger(0)
    }

    private var rippleEffectEnabled = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(viewTypeLayoutMap[viewType]!!, parent, false)
        return VH(itemView)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        item.inject(holder)

        if (rippleEffectEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val itemView = holder.itemView
            if (itemView.hasOnClickListeners()) {
                val outValue = TypedValue()
                itemView.context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                if (outValue.resourceId != 0) {
                    itemView.foreground =
                        ResourcesCompat.getDrawable(
                            itemView.resources,
                            outValue.resourceId,
                            itemView.context.theme
                        )
                }
            } else {
                itemView.foreground = null
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position] as Any

        val viewTypeId = classViewTypeMap.getOrPut(item::class) { incremental.getAndIncrement() }
        viewTypeLayoutMap.getOrPut(viewTypeId) {
            item.javaClass.getAnnotation(ViewLayout::class.java)?.layoutId
                ?: throw IllegalStateException("item should be annotated @ViewLayout : $item")
        }
        return viewTypeId
    }

    fun reloadItems(newItems: List<ViewInjector>) {
        items = newItems
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val intMap by lazy { mutableMapOf<Int, View>() }

        fun <T : View> present(@IdRes id: Int, block: (T) -> Unit) {
            val view = if (itemView !is ViewGroup) {
                // <include>를 사용할 경우 view의 id가 override 될 수 있는데,
                // 이 경우 findViewById를 통해 view를 찾을 수 없게된다.
                // 따라서, 단일 View일 경우에는 바로 itemView를 사용하자.
                itemView
            } else {
                intMap.getOrPut(id) {
                    itemView.findViewById(id)
                        ?: throw IllegalArgumentException(
                            "can't find view " + itemView.resources.getResourceName(
                                id
                            )
                        )
                }
            }
            @Suppress("UNCHECKED_CAST")
            (view as T).apply(block)
        }

        fun present(layoutId: Int, text: CharSequence?, function: ((TextView) -> Unit)? = null) {
            present<TextView>(layoutId) {
                if (text != null && text.isNotBlank()) {
                    it.visibility = View.VISIBLE
                    it.text = text
                } else {
                    it.visibility = View.GONE
                }
                function?.run {
                    it.apply(this)
                }
            }
        }
    }

    interface ViewInjector {
        fun inject(viewHolder: VH)
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ViewLayout(@LayoutRes val layoutId: Int)

fun SettingsAdapter.ViewInjector.injectTo(view: View) {
    inject(SettingsAdapter.VH(view))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (view.hasOnClickListeners()) {
            val outValue = TypedValue()
            view.context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                outValue,
                true
            )
            if (outValue.resourceId != 0) {
                view.foreground = ResourcesCompat.getDrawable(
                    view.resources,
                    outValue.resourceId,
                    view.context.theme
                )
            }
        }
    }
}
