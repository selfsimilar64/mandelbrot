package com.selfsimilartech.fractaleye

import android.graphics.Point
import android.util.Log
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

fun Double.roundEven() : Int {
    val f = floor(this).toInt()
    return if (f % 2 == 0) f else f + 1
}

class Resolution(width: Int, private val builders: List<Resolution>? = null) {

    // R4320(4320), R5040(5040), R5760(5760),   // RG16F largeheap only

    val size = Point(width, 0)

    fun getBuilder() = builders?.last { it.size.x <= SCREEN.size.x } ?: this


    companion object {

        val R45    = Resolution( 45                         )
        val R60    = Resolution( 60                         )
        val R90    = Resolution( 90                         )
        val R120   = Resolution( 120                        )
        val R180   = Resolution( 180                        )
        val R240   = Resolution( 240                        )
        val R360   = Resolution( 360                        )
        val R480   = Resolution( 480                        )
        val R720   = Resolution( 720                        )
        val R1080  = Resolution( 1080, listOf(R360)         )
        val R1440  = Resolution( 1440, listOf(R720)         )
        val R2160  = Resolution( 2160, listOf(R720)         )
        val R2880  = Resolution( 2880, listOf(R720)         )
//        val R3600  = Resolution( 3600, listOf(R720)         )
//        val R4320  = Resolution( 4320, listOf(R720, R1440)  )
//        val R5040  = Resolution( 5040, listOf(R720)         )
//        val R5760  = Resolution( 5760, listOf(R720, R1440)  )

        val all = arrayListOf(
                R45, R60, R90, R120, R180, R240, R360, R480, R720, R1080, R1440, R2160, R2880
                //R3600
                //R4320, R5040, R5760
        )
        val working = ArrayList(all.minus(listOf(R45, R60, R90)))

        fun valueOf(width: Int) : Resolution? {
            return all.first { it.size.x == width }
        }

        fun initialize(screenRatio: Double) {
            all.forEach {
                it.size.y = (it.size.x.toDouble()*screenRatio).roundEven()
                Log.e("RESOLUTION", "(${it.size.x}, ${it.size.x.toDouble()*screenRatio} -> ${it.size.y}")
            }
            R1080.size.y = (R360.size.y*3.0).toInt()
            R1440.size.y = (R720.size.y*2.0).toInt()
            R2160.size.y = (R720.size.y*3.0).toInt()
            R2880.size.y = (R720.size.y*4.0).toInt()
            NUM_VALUES_GT_SCREEN_DIMS = all.count { it.size.x > SCREEN.size.x }
            NUM_VALUES_FREE = working.indexOf(R1080) + 1
        }

        fun addResolution(width: Int) {
            val newRes = Resolution(width)
            all.add(all.indexOfFirst { newRes.size.x < it.size.x }, newRes)
            working.add(working.indexOfFirst { newRes.size.x < it.size.x }, newRes)
        }


        val BG = R180
        val THUMB = R240
        var SCREEN = R1080
        val MAX_FREE = R1080
        var NUM_VALUES_GT_SCREEN_DIMS = 0
        val NUM_VALUES_WORKING = { working.size }
        var NUM_VALUES_FREE = 0

    }
}