package com.solarexsoft.recyclerviewlearningdemo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_recyclerview.*
import kotlinx.android.synthetic.main.item_icon0.view.*
import kotlinx.android.synthetic.main.item_icon1.view.*

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

    class RVTestAdapter(private val count: Int) : RecyclerView.Adapter<BaseViewHolder>() {
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

    abstract class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            private const val TYPE_ZERO = R.layout.item_icon0
            private const val TYPE_ONE = R.layout.item_icon1

            fun create(parent: ViewGroup, viewType: Int): BaseViewHolder {
                return when (viewType) {
                    TYPE_ZERO -> {
                        ZeroItemViewHolder(LayoutInflater.from(parent.context)
                            .inflate(viewType, parent, false))
                    }
                    TYPE_ONE -> {
                        OneItemViewHolder(LayoutInflater.from(parent.context)
                            .inflate(viewType, parent, false))
                    }
                    else -> {
                        error("unknown type")
                    }
                }
            }

            fun type(position: Int): Int {
                return if (position % 2 == 0) {
                    TYPE_ZERO
                } else {
                    TYPE_ONE
                }
            }
        }

        abstract fun bind()
    }

    class ZeroItemViewHolder(view: View) : BaseViewHolder(view) {
        val ivIcon: ImageView = view.ivIcon21
        override fun bind() {
            ivIcon.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    class OneItemViewHolder(view: View) : BaseViewHolder(view) {
        val ivIcon: ImageView = view.ivIcon43
        override fun bind() {
            ivIcon.setImageResource(R.mipmap.ic_launcher)
        }
    }

}