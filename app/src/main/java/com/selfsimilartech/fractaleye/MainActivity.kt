package com.selfsimilartech.fractaleye

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.TransitionDrawable
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.*
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.*


const val SPLIT = 8193.0
const val NUM_MAP_PARAMS = 4
const val NUM_TEXTURE_PARAMS = 2
const val WRITE_STORAGE_REQUEST_CODE = 0
//const val PLUS_UNICODE = '\u002B'
//const val MINUS_UNICODE = '\u2212'



operator fun Double.times(w: Complex) : Complex {
    return Complex(this*w.x, this*w.y)
}

data class Complex(
        val x : Double,
        val y : Double
) {

    companion object {

        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)
        val i = Complex(0.0, 1.0)

    }


    override fun toString(): String {
        return "($x, $y)"
    }

    fun conj() : Complex{
        return Complex(x, -y)
    }

    fun mod() : Double {
        return sqrt(x*x + y*y)
    }

    operator fun unaryMinus() : Complex {
        return Complex(-x, -y)
    }

    operator fun plus(w: Complex) : Complex {
        return Complex(x + w.x, y + w.y)
    }

    operator fun minus(w: Complex) : Complex {
        return Complex(x - w.x, y - w.y)
    }

    operator fun times(s: Double) : Complex {
        return Complex(s*x, s*y)
    }

    operator fun times(w: Complex) : Complex {
        return Complex(x*w.x - y*w.y, x*w.y + y*w.x)
    }

    operator fun div(s: Double) : Complex {
        return Complex(x/s, y/s)
    }

    operator fun div(w: Complex) : Complex {
        return (this*w.conj())/mod()
    }

}

data class DualDouble (
        var hi : Double,
        var lo : Double
) {

    override fun toString() : String {
        return "{$hi + $lo}"
    }

    private fun quickTwoSum(a: Double, b: Double) : DualDouble {
        val s = a + b
        val e = b - (s - a)
        return DualDouble(s, e)
    }

    private fun twoSum(a: Double, b: Double) : DualDouble {
        val s = a + b
        val v = s - a
        val e = a - (s - v) + (b - v)
        return DualDouble(s, e)
    }

    private fun split(a: Double): DualDouble {
        val t = a * SPLIT
        val aHi = t - (t - a)
        val aLo = a - aHi
        return DualDouble(aHi, aLo)
    }

    private fun twoProd(a: Double, b: Double) : DualDouble {
        val p = a * b
        val aS = split(a)
        val bS = split(b)
        val err = aS.hi * bS.hi - p + aS.hi * bS.lo + aS.lo * bS.hi + aS.lo * bS.lo
        return DualDouble(p, err)
    }

    operator fun unaryMinus() : DualDouble {
        return DualDouble(-hi, -lo)
    }

    operator fun plus(b: DualDouble) : DualDouble {
        var s = twoSum(hi, b.hi)
        val t = twoSum(lo, b.lo)
        s.lo += t.hi
        s = quickTwoSum(s.hi, s.lo)
        s.lo += t.lo
        s = quickTwoSum(s.hi, s.lo)
        return s
    }

    operator fun minus(b: DualDouble) : DualDouble {
        return plus(b.unaryMinus())
    }

    operator fun times(b: DualDouble) : DualDouble {
        var p = twoProd(hi, b.hi)
        p.lo += hi * b.lo
        p.lo += lo * b.hi
        p = quickTwoSum(p.hi, p.lo)
        return p
    }

    operator fun div(b: DualDouble) : DualDouble {

        val xn = 1.0 / b.hi
        val yn = hi * xn
        val diff = minus(b*DualDouble(yn, 0.0))
        val prod = twoProd(xn, diff.hi)
        return DualDouble(yn, 0.0) + prod

    }

}


enum class Precision { SINGLE, DUAL, QUAD, AUTO }
enum class Reaction { POSITION, COLOR, P1, P2, P3, P4 }
enum class Resolution { LOW, MED, HIGH }




class Position(
        var x: Double = 0.0,
        var y: Double = 0.0,
        var scale: Double = 1.0,
        var rotation: Double = 0.0
) {

    private val xInit = x
    private val yInit = y
    private val scaleInit = scale
    private val rotationInit = rotation

    private fun translate(dPos: DoubleArray) {

        // update complex coordinates
        x += dPos[0]
        y += dPos[1]

        // Log.d("FRACTAL", "translation (coordinates) -- dx: ${dPos[0]}, dy: ${dPos[1]}")

    }
    fun translate(dx: Float, dy: Float) {
        // dx, dy --> [0, 1]

        // update complex coordinates
        val tx = dx*scale
        val ty = dy*scale
        val sinTheta = sin(-rotation)
        val cosTheta = cos(rotation)
        x -= tx*cosTheta - ty*sinTheta
        y += tx*sinTheta + ty*cosTheta

        // updatePositionEditTexts()
        // updateDisplayParams(Reaction.POSITION)
        // Log.d("FRACTAL", "translation (pixels) -- dx: ${dScreenPos[0]}, dy: ${dScreenPos[1]}")

    }
    fun scale(dScale: Float, prop: DoubleArray) {

        val qx = prop[0]*scale
        val qy = prop[1]*scale
        val sinTheta = sin(rotation)
        val cosTheta = cos(rotation)
        val focus = doubleArrayOf(
                x + qx*cosTheta - qy*sinTheta,
                y + qx*sinTheta + qy*cosTheta
        )

        translate(focus.negative())
        x /= dScale
        y /= dScale
        translate(focus)
        scale /= dScale

    }
    fun rotate(dTheta: Float, prop: DoubleArray) {

        //Log.d("FRACTAL", "dTheta: $dTheta")

        // calculate focus in complex coordinates
        var qx = prop[0]*scale
        var qy = prop[1]*scale
        val sinTheta = sin(rotation)
        val cosTheta = cos(rotation)
        val focus = doubleArrayOf(
                x + qx*cosTheta - qy*sinTheta,
                y + qx*sinTheta + qy*cosTheta
        )

        //Log.d("FRACTAL", "focus (coordinates) -- x: ${focus[0]}, y: ${focus[1]}")

        val sindTheta = sin(-dTheta)
        val cosdTheta = cos(dTheta)

        //Log.d("FRACTAL", "previous coords: (${x}, ${y})")

        translate(focus.negative())
        qx = x
        qy = y
        x = qx*cosdTheta - qy*sindTheta
        y = qx*sindTheta + qy*cosdTheta
        translate(focus)
        rotation -= dTheta.toDouble()
        //Log.d("FRACTAL", "rotation: ${rotation}")

        //Log.d("FRACTAL", "new coords: (${x}, ${y})")


        // updatePositionEditTexts()
        // updateDisplayParams(Reaction.POSITION)

    }
    fun reset() {
        x = xInit
        y = yInit
        scale = scaleInit
        rotation = rotationInit
    }

}

class PositionList(
        val default : Position = Position(),
        val julia   : Position = Position(scale = default.scale)
)




class SettingsConfig (val params: MutableMap<String, Any>) {

    val resolution         = { params["resolution"]        as Resolution }
    val precision          = { params["precision"]         as Precision  }
    val continuousRender   = { params["continuousRender"]  as Boolean    }
    val displayParams      = { params["displayParams"]     as Boolean    }

}


fun MotionEvent.focalLength() : Float {
    val f = focus()
    val pos = floatArrayOf(x, y)
    val dist = floatArrayOf(pos[0] - f[0], pos[1] - f[1])
    return sqrt(dist[0].toDouble().pow(2.0) +
            dist[1].toDouble().pow(2.0)).toFloat()
}
fun MotionEvent.focus() : FloatArray {
    return if (pointerCount == 1) floatArrayOf(x, y)
    else { floatArrayOf((getX(0) + getX(1))/2.0f, (getY(0) + getY(1))/2.0f) }
}
fun DoubleArray.mult(s: Double) : DoubleArray {
    return DoubleArray(this.size) {i: Int -> s*this[i]}
}
fun DoubleArray.negative() : DoubleArray {
    return doubleArrayOf(-this[0], -this[1])
}
fun splitSD(a: Double) : FloatArray {

    val b = FloatArray(2)
    b[0] = a.toFloat()
    b[1] = (a - b[0].toDouble()).toFloat()
    return b

}
fun splitDD(a: DualDouble) : FloatArray {

    val b = FloatArray(4)
    b[0] = a.hi.toFloat()
    b[1] = (a.hi - b[0].toDouble()).toFloat()
    b[2] = a.lo.toFloat()
    b[3] = (a.lo - b[2].toDouble()).toFloat()
    return b

}


class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager) {

    private val mFragmentList = ArrayList<Fragment>()
    private val mFragmentTitleList = ArrayList<String>()

    override fun getItem(position: Int) : Fragment {
        return mFragmentList[position]
    }

    override fun getCount() : Int {
        return mFragmentList.size
    }

    fun addFrag(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    override fun getPageTitle(position: Int) : CharSequence {
        return mFragmentTitleList[position]
    }

}


class MainActivity : AppCompatActivity(),
        SettingsFragment.OnParamChangeListener {

    private lateinit var f : Fractal
    private lateinit var fsv : FractalSurfaceView
    private lateinit var uiQuickButtons : List<View>
    private lateinit var displayParamRows : List<LinearLayout>

    private var orientation = Configuration.ORIENTATION_UNDEFINED



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//        orientation = baseContext.resources.configuration.orientation
//        Log.d("MAIN ACTIVITY", "orientation: $orientation")
//        val orientationChanged = (savedInstanceState?.getInt("orientation") ?: orientation) != orientation

        // get screen dimensions
        val displayMetrics = baseContext.resources.displayMetrics
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val aspectRatio = screenHeight.toDouble() / screenWidth

        // val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        // val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0



        // get initial values for fractalConfig

//        val map = ComplexMap.all[savedInstanceState?.getString("map")] ?: ComplexMap.mandelbrot
//
//        map.position.x = savedInstanceState?.getDouble("x") ?: 0.0
//        map.position.y = savedInstanceState?.getDouble("y") ?: 0.0
//        map.position.scale = savedInstanceState?.getDouble("scale") ?: 1.0
//        map.position.rotation = savedInstanceState?.getDouble("rotation") ?: 0.0
//        map.juliaMode = savedInstanceState?.getBoolean("juliaMode") ?: false
//        // params
//
//        val texture = Texture.all[savedInstanceState?.getString("texture")] ?: Texture.escape
//        // texture.params[0] = savedInstanceState?.getDouble("q1") ?: 0.0
//        // texture.params[1] = savedInstanceState?.getDouble("q2") ?: 0.0
//
//        val palette = ColorPalette.all[savedInstanceState?.getString("palette")] ?: ColorPalette.p5
//        palette.frequency = savedInstanceState?.getFloat("frequency") ?: 1f
//        palette.phase = savedInstanceState?.getFloat("phase") ?: 0f
//
//        val maxIter = savedInstanceState?.getInt("maxIter") ?: 255
//        val bailoutRadius = savedInstanceState?.getFloat("bailoutRadius") ?: 1e5f
//        val sensitivity = savedInstanceState?.getDouble("paramSensitivity") ?: 1.0




        // get initial values for settingsConfig

        val resolution = Resolution.valueOf(savedInstanceState?.getString("resolution") ?: Resolution.HIGH.name)
        val precision = Precision.valueOf(savedInstanceState?.getString("precision") ?: "AUTO")
        val continuousRender = savedInstanceState?.getBoolean("continuousRender") ?: false
        val displayParamsBoolean = savedInstanceState?.getBoolean("displayParams") ?: true


        val sc = SettingsConfig(mutableMapOf(
                "resolution"        to resolution,
                "precision"         to precision,
                "continuousRender"  to continuousRender,
                "displayParams"     to displayParamsBoolean,
                "saveToFile"        to false,
                "render"            to false
        ))


        // create fractal
        f = Fractal(
                this,
                sc,
                intArrayOf(screenWidth, screenHeight)
        )

//        if (orientationChanged) {
//            f.switchOrientation()
//            Log.d("MAIN", "orientation changed")
//        }


        fsv = FractalSurfaceView(f, this)
        fsv.layoutParams = ViewGroup.LayoutParams(screenWidth, screenHeight)
        fsv.hideSystemUI()

        setContentView(R.layout.activity_main)

        val fractalLayout = findViewById<FrameLayout>(R.id.layout_main)
        fractalLayout.addView(fsv)


        val displayParams = findViewById<LinearLayout>(R.id.displayParams)
        displayParamRows = listOf(
            findViewById(R.id.displayParamRow1),
            findViewById(R.id.displayParamRow2),
            findViewById(R.id.displayParamRow3),
            findViewById(R.id.displayParamRow4)
        )
        displayParams.removeViews(1, displayParams.childCount - 1)


        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        val buttonBackgrounds = arrayOf(
            resources.getDrawable(R.drawable.round_button_unselected, null),
            resources.getDrawable(R.drawable.round_button_selected, null)
        )
        uiQuickButtons = listOf(
            findViewById(R.id.transformButton),
            findViewById(R.id.colorButton),
            findViewById(R.id.paramButton1),
            findViewById(R.id.paramButton2),
            findViewById(R.id.paramButton3),
            findViewById(R.id.paramButton4)
        )
        val uiQuickButtonListener = View.OnClickListener {
            val s = when (it) {
                is Button -> it.text.toString()
                is ImageButton -> it.contentDescription.toString()
                else -> ""
            }
            fsv.reaction = Reaction.valueOf(s)
            (displayParams.getChildAt(0) as TextView).text = when (fsv.reaction) {
                Reaction.POSITION, Reaction.COLOR -> s
                else -> "PARAMETER ${s[1]}"
            }

            if (fsv.f.sc.displayParams()) {
                displayParams.removeViews(1, displayParams.childCount - 1)
                for (i in 0 until fsv.numDisplayParams()) {
                    displayParams.addView(displayParamRows[i])
                }
            }

            for (b in uiQuickButtons) {
                val btd = b.background as TransitionDrawable
                if (b == it) { btd.startTransition(0) }
                else { btd.resetTransition() }
            }
            fsv.f.updateDisplayParams(Reaction.valueOf(s), true)
        }
        for (b in uiQuickButtons) {
            b.background = TransitionDrawable(buttonBackgrounds)
            b.setOnClickListener(uiQuickButtonListener)
        }
        val diff = f.map.params.size - uiQuick.childCount + 2
        uiQuick.removeViews(0, abs(diff))
        uiQuick.bringToFront()
        uiQuickButtons[0].performClick()

        val buttonScroll = findViewById<HorizontalScrollView>(R.id.buttonScroll)
        val leftArrow = findViewById<ImageView>(R.id.leftArrow)
        val rightArrow = findViewById<ImageView>(R.id.rightArrow)
        leftArrow.alpha = 0f
        rightArrow.alpha = 0f

        buttonScroll.viewTreeObserver.addOnScrollChangedListener {
            if (uiQuick.width > buttonScroll.width) {
                val scrollX = buttonScroll.scrollX
                val scrollEnd = uiQuick.width - buttonScroll.width
                // Log.d("MAIN ACTIVITY", "scrollX: $scrollX")
                // Log.d("MAIN ACTIVITY", "scrollEnd: $scrollEnd")
                when {
                    scrollX > 5 -> leftArrow.alpha = 1f
                    scrollX < 5 -> leftArrow.alpha = 0f
                }
                when {
                    scrollX < scrollEnd - 5 -> rightArrow.alpha = 1f
                    scrollX > scrollEnd - 5 -> rightArrow.alpha = 0f
                }
            }
            else {
                leftArrow.alpha = 0f
                rightArrow.alpha = 0f
            }
        }


        val uiFullTabs = findViewById<TabLayout>(R.id.uiFullTabs)
        uiFullTabs.tabGravity = TabLayout.GRAVITY_FILL





        // initialize fragments and set UI params from fractal
        val fractalEditFragment = FractalEditFragment()
        val settingsFragment = SettingsFragment()
        // val saveFragment = SaveFragment()

        fractalEditFragment.f = f
        fractalEditFragment.fsv = fsv

        settingsFragment.config = f.sc






        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFrag( fractalEditFragment,  "Fractal" )
        adapter.addFrag( settingsFragment,  "Settings" )

        val viewPager = findViewById<ViewPager>(R.id.viewPager)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(uiFullTabs))

        uiFullTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
                Log.d("MAIN ACTIVITY", "tab: ${tab.text}")
                when (tab.text) {
                    "fractal" -> {
                        Log.d("MAIN ACTIVITY", "Fractal tab selected")
                    }
                    else -> {}
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })





        // val phi = 0.5*(sqrt(5.0) + 1.0)
        // val uiFullHeightOpen = (screenHeight*(1.0 - 1.0/phi)).toInt()
        // val uiFullHeightFullscreen = screenHeight - statusBarHeight
        val uiFullHeightOpen = screenHeight/2
        // fsv.y = -uiFullHeightOpen/2f

        val uiFull = findViewById<LinearLayout>(R.id.uiFull)
        uiFull.layoutParams.height = 1
        uiFull.bringToFront()

        val uiFullButton = findViewById<ImageButton>(R.id.uiFullButton)
        uiFullButton.setOnClickListener {
            val hStart : Int
            val hEnd : Int
            if (uiFull.height == 1) {
                hStart = 1
                hEnd = uiFullHeightOpen
            }
            else {
                hStart = uiFullHeightOpen
                hEnd = 1
            }

            val anim = ValueAnimator.ofInt(hStart, hEnd)
            anim.addUpdateListener { animation ->
                val intermediateHeight = animation?.animatedValue as Int
                val c = ConstraintSet()
                c.clone(overlay)
                c.constrainHeight(R.id.uiFull, intermediateHeight)
                c.applyTo(overlay)
                fsv.y = -uiFull.height/2.0f
            }
            anim.duration = 300
            anim.start()
        }

        val overlay = findViewById<ConstraintLayout>(R.id.overlay)
        overlay.bringToFront()

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)


    }


    fun addMapParams(n: Int) {
        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        for (i in 1..n) { uiQuick.addView(uiQuickButtons[uiQuick.childCount], 0) }
    }
    fun removeMapParams(n: Int) {
        val uiQuick = findViewById<LinearLayout>(R.id.uiQuick)
        for (i in 1..n) { uiQuick.removeView(uiQuickButtons[uiQuick.childCount - 1]) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) { fsv.hideSystemUI() }
    }
    override fun onSaveInstanceState(outState: Bundle?) {

        Log.d("MAIN ACTIVITY", "saving instance state !!")

        // save FractalConfig values
//        outState?.putString(        "map",               f.map.name          )
//        outState?.putString(        "texture",           f.texture.name      )
//        outState?.putDouble(        "sensitivity",       f.sensitivity  )
//        outState?.putDoubleArray(   "coords",            f.coords()            )
//        outState?.putDoubleArray(   "savedCoords",       f.savedCoords()       )
//        outState?.putDoubleArray(   "scale",             f.scale()             )
//        outState?.putDoubleArray(   "savedScale",        f.savedScale()        )
//        outState?.putInt(           "maxIter",           f.maxIter()           )
//        outState?.putFloat(         "bailoutRadius",     f.bailoutRadius()     )
//        outState?.putString(        "palette",           f.palette().name      )
//        outState?.putFloat(         "frequency",         f.frequency()         )
//        outState?.putFloat(         "phase",             f.phase()             )
//
//        // save SettingsConfig values
//        outState?.putString(    "resolution",        f.sc.resolution().name   )
//        outState?.putBoolean(   "continuousRender",  f.sc.continuousRender()  )
//        outState?.putBoolean(   "displayParams",     f.sc.displayParams()     )
//        outState?.putString(    "precision",         f.sc.precision().name    )

        super.onSaveInstanceState(outState)
    }
    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        when (fragment) {
            is SettingsFragment -> fragment.setOnParamChangeListener(this)
        }
    }
    override fun onSettingsParamsChanged(key: String, value: Any) {
        if (f.sc.params[key] != value) {
            Log.d("MAIN ACTIVITY", "$key set from ${f.sc.params[key]} to $value")
            f.sc.params[key] = value
            when (key) {
                "resolution" -> {
                    f.resolutionChanged = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                }
                "precision" -> {
                    f.renderShaderChanged = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                }
                "continuousRender" -> {
                    f.renderProfileChanged = true
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                }
                "displayParams" -> {
                    val displayParams = findViewById<LinearLayout>(R.id.displayParams)
                    if (value as Boolean) {
                        for (i in 0 until fsv.numDisplayParams()) {
                            displayParams.addView(displayParamRows[i])
                        }
                        fsv.f.updateDisplayParams(fsv.reaction, false)
                    }
                    else {
                        displayParams.removeViews(1, displayParams.childCount - 1)
                    }
                }
                "saveToFile" -> {
                    if (fsv.r.isRendering) {
                        val toast = Toast.makeText(baseContext, "Please wait for the image to finish rendering", Toast.LENGTH_SHORT)
                        toast.show()
                    }
                    else {
                        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                    WRITE_STORAGE_REQUEST_CODE)
                        }
                        else {
                            fsv.r.saveImage = true
                            fsv.requestRender()
                            val toast = Toast.makeText(baseContext, "Image saved to Gallery", Toast.LENGTH_SHORT)
                            toast.show()
                        }
                    }
                    f.sc.params[key] = false
                }
                "render" -> {
                    fsv.r.renderToTex = true
                    fsv.requestRender()
                    f.sc.params[key] = false
                }
            }
        }
        else {
            Log.d("MAIN ACTIVITY", "$key already set to $value")
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            WRITE_STORAGE_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    fsv.r.saveImage = true
                    fsv.requestRender()
                    val toast = Toast.makeText(baseContext, "Image saved to Gallery", Toast.LENGTH_SHORT)
                    toast.show()
                } else {
                    val toast = Toast.makeText(baseContext, "Image not saved - storage permission required", Toast.LENGTH_LONG)
                    toast.show()
                }
                return
            }
            else -> {}
        }
    }


}
