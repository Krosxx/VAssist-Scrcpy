package cn.vove7.scrcpy

import android.view.KeyEvent
import android.view.MotionEvent
import cn.vove7.scrcpy.MockTest.dataOutputStream
import cn.vove7.scrcpy.MockTest.requireSock
import cn.vove7.scrcpy.common.ControlMessage
import cn.vove7.scrcpy.common.Position
import org.junit.Test
import java.lang.Thread.sleep

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {


    @Test
    fun simpleTest() {
        requireSock(true)
        ControlMessage.createEmpty(ControlMessage.TYPE_EXPAND_NOTIFICATION_PANEL)
                .post(dataOutputStream)
        sleep(1000)
        ControlMessage.createInjectKeycode(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0)
                .post(dataOutputStream)
        sleep(500)
        ControlMessage.createInjectKeycode(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0, 0)
                .post(dataOutputStream)

        ControlMessage.createEmpty(ControlMessage.TYPE_GET_CLIPBOARD).post(dataOutputStream)

        ControlMessage.createSetClipboard("ControlMessage.TYPE_GET_CLIPBOARD 你好").post(dataOutputStream)

    }

    @Test
    fun powerSaveMode() {
        requireSock(false)
        ControlMessage.createPowerSaveModeEnable(false)
                .post(dataOutputStream)

        sleep(2000)
    }

    @Test
    fun screenOn() {
        requireSock(false)
//        ControlMessage.createInjectKeycode(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_WAKEUP, 0, 0)
//                .post(dataOutputStream)
        ControlMessage.createInjectKeycode(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_WAKEUP, 0, 0)
                .post(dataOutputStream)
        sleep(1000)
    }

    @Test
    fun inputText() {
        requireSock()
        ControlMessage.createInjectText("12345678")
                .post(dataOutputStream)
    }

    @Test
    fun quickClick() {
        requireSock(false)

        repeat(100) {
            ControlMessage.createInjectTouchEvent(MotionEvent.ACTION_DOWN, 0,
                    Position(500,500),1f, 0)
                    .post(dataOutputStream)
            sleep(20)
            ControlMessage.createInjectTouchEvent(MotionEvent.ACTION_UP, 0,
                    Position(500,500),1f, 0)
                    .post(dataOutputStream)
            sleep(10)
        }

    }

}