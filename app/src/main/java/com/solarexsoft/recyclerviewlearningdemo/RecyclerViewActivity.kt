package com.solarexsoft.recyclerviewlearningdemo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_recyclerview.*
import kotlinx.android.synthetic.main.item_icon0.view.*

/**
 * <pre>
 *    Author: houruhou
 *    CreatAt: 10:16/2020/10/11
 *    Desc:
 * </pre>
 */

class RecyclerViewActivity : AppCompatActivity() {
    companion object {
        const val TAG = "RV"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recyclerview)

        rv_main.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rv_main.adapter = RVTestAdapter(10000)
    }

    class RVTestAdapter(private val count: Int): RecyclerView.Adapter<BaseViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            Log.d(TAG, "onCreateViewHolder viewType = $viewType")
            return BaseViewHolder.create(parent, viewType)
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            Log.d(TAG, "onBindViewHolder")
            holder.bind()
        }

        override fun getItemCount(): Int {
            return count
        }

        override fun getItemViewType(position: Int): Int {
            return BaseViewHolder.type(position)
        }
    }

    abstract class BaseViewHolder(parent: ViewGroup, layoutResId: Int): RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
    ) {
        companion object {
            private const val TYPE_ZERO = R.layout.item_icon0
            private const val TYPE_ONE = R.layout.item_icon1

            fun create(parent: ViewGroup, viewType: Int): BaseViewHolder {
                return when (viewType) {
                    TYPE_ZERO -> {
                       ZeroItemViewHolder(parent, viewType)
                    }
                    TYPE_ONE -> {
                        OneItemViewHolder(parent, viewType)
                    }
                    else -> {
                        error("unknown type")
                    }
                }
            }

            fun type(position: Int): Int {
                return if (position % 2 == 0) {
                    R.layout.item_icon0
                } else {
                    R.layout.item_icon1
                }
            }
        }
        abstract fun bind()
    }

    class ZeroItemViewHolder(parent: ViewGroup, layoutResId: Int): BaseViewHolder(parent, layoutResId) {
        override fun bind() {
            itemView.iv_icon.setImageResource(R.drawable.ic_launcher_background)
        }
    }
    class OneItemViewHolder(parent: ViewGroup, layoutResId: Int): BaseViewHolder(parent, layoutResId) {
        override fun bind() {
            itemView.iv_icon.setImageResource(R.mipmap.ic_launcher)
        }
    }

}