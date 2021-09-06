package cn.vove7.scrcpy

import android.graphics.Path
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import cn.vove7.scrcpy.common.ControlMessage
import cn.vove7.scrcpy.common.GestureDescription
import cn.vove7.scrcpy.common.Point
import cn.vove7.scrcpy.common.Position
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * # Test
 *
 * @author Vove
 * @date 2021/8/23
 */
object MockTest {

    object Ln {
        fun i(s: String) {
            Log.i("MockTest", s)
        }
    }

    lateinit var sock: Socket
    lateinit var dataInputStream: BufferedReader
    lateinit var dataOutputStream: BufferedWriter

    fun requireSock(listenOut: Boolean = false) {
        if (MockTest::sock.isInitialized) {
            return
        }
        val s = Socket()
        s.connect(InetSocketAddress("127.0.0.1", 9999))
        sock = s
        dataInputStream = BufferedReader(InputStreamReader(sock.inputStream))
        dataOutputStream = BufferedWriter(OutputStreamWriter(sock.outputStream))

        if (listenOut) {
            thread(isDaemon = true) {
                while (true) {
                    println("recv:  ${dataInputStream.readLine()}")
                }
            }
        }
    }


    fun simpleGesture() {
        requireSock(false)

        ControlMessage.createSimpleGesture(
            listOf(listOf(
                Point(100, 0),
                Point(100, 1000),
            )),
            500
        ).post(dataOutputStream)

//        val p = Path()
//        p.reset()
//        p.moveTo(100f, 0f)
//        p.lineTo(100f, 1000f)
//        val gdes = GestureDescription.StrokeDescription(p, 0, 300)
//        val gb = GestureDescription.Builder()
//        gb.addStroke(gdes)
//
//        inputGesture(gb.build())

        Thread.sleep(1000)
    }

    fun scaleGesture() {
        requireSock(false)

        ControlMessage.createInjectKeycode(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HOME, 0, 0)
            .post(dataOutputStream)
        sleep(800)


        //300 300-> 600 600 | 1000 1200 700 700
        ControlMessage.createSimpleGesture(
            listOf(listOf(
                Point(300, 300),
                Point(600, 600),
            ),
                listOf(
                    Point(1000, 1200),
                    Point(700, 700),
                )),
            500
        ).post(dataOutputStream)

//        val p1 = Path()
//        p1.moveTo(300f, 300f)
//        p1.lineTo(600f, 600f)
//        val p2 = Path()
//        p2.moveTo(1000f, 1200f)
//        p2.lineTo(700f, 700f)
//
//        inputGesture(listOf(p1, p2).toGesDesc(500))

        sleep(1000)
    }

    //todo 减少发送量
    fun List<Path>.toGesDesc(duration: Long): GestureDescription {
        val gb = GestureDescription.Builder()
        forEach { p ->
            val gdes = GestureDescription.StrokeDescription(p, 0, duration)
            gb.addStroke(gdes)
        }
        return gb.build()
    }

    @OptIn(ExperimentalTime::class)
    fun inputGesture(gesDesc: GestureDescription) {
        val steps = GestureDescription.MotionEventGenerator
            .getGestureStepsFromGestureDescription(gesDesc, 16)
        val useTime = measureTime {
            steps.forEachIndexed { i, step ->
                Ln.i("step: $step")
                val sendUseTime = measureTime {
                    step.touchPoints.forEachIndexed { ti, point ->
                        if (point.mIsStartOfPath) {
                            send(MotionEvent.ACTION_DOWN,
                                Position(point.mX.toInt(), point.mY.toInt()), ti.toLong())
                        }
                        send(MotionEvent.ACTION_MOVE,
                            Position(point.mX.toInt(), point.mY.toInt()),
                            ti.toLong()
                        )
                        if (point.mIsEndOfPath) {
                            send(MotionEvent.ACTION_UP,
                                Position(point.mX.toInt(), point.mY.toInt()),
                                ti.toLong())
                        }
                    }
                }
                if (i + 1 < steps.size) {//wait
                    val wait = steps[i + 1].timeSinceGestureStart - step.timeSinceGestureStart - sendUseTime.inWholeMilliseconds
                    Ln.i("wait: $wait")
                    if (wait > 0) sleep(wait)
                }
            }
        }
        Ln.i("use time: $useTime")
    }

    fun send(action: Int, p: Position, pointerId: Long) {
        ControlMessage.createInjectTouchEvent(
            action,
            pointerId,
            p,
            1f, 0
        ).post(dataOutputStream)
    }

    fun close() {
        dataInputStream.close()
        dataOutputStream.close()
        sock.close()
    }
}