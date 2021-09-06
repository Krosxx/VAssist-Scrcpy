package cn.vove7.scrcpy

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.run).setOnClickListener {
            run()
        }
    }

    private fun run() = thread {
        kotlin.runCatching {
            MockTest.simpleGesture()
        }.onFailure {
            it.printStackTrace()
        }
    }

}