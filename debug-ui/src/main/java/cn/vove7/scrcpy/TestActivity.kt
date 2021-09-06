package cn.vove7.scrcpy

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import kotlin.reflect.KCallable

/**
 * # TestActivity
 *
 * @author Vove
 * @date 2021/8/23
 */
class TestActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lv = ListView(this)

        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, listOf(
                MockTest::simpleGesture,
                MockTest::scaleGesture,
        ))


        lv.setOnItemClickListener { _, _, position, _ ->
            thread {
                (lv.adapter as ArrayAdapter<KCallable<*>>)
                        .getItem(position)?.call()
            }
        }

        setContentView(lv)

    }
}