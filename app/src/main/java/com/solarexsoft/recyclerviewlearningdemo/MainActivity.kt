package com.solarexsoft.recyclerviewlearningdemo

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * <pre>
 *    Author: houruhou
 *    CreatAt: 10:02/2020/10/11
 *    Desc:
 * </pre>
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {
        val intent = when (view.id) {
            R.id.rv -> {
                Intent(this, RecyclerViewActivity::class.java)
            }
            else -> {
                Intent()
            }
        }
        startActivity(intent)
    }
}
 