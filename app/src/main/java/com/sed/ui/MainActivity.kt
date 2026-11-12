package com.sed.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class LaunchApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : Activity() {
    private val allApps = mutableListOf<LaunchApp>()
    private val visibleApps = mutableListOf<LaunchApp>()
    private val dockApps = mutableListOf<LaunchApp>()
    private lateinit var grid: GridView
    private lateinit var adapter: AppAdapter
    private lateinit var pageDots: TextView
    private lateinit var pageLabel: TextView
    private lateinit var dock: LinearLayout
    private var page = 0
    private val pageSize = 20
    private var downX = 0f
    private var downY = 0f
    private var searching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(54), dp(22), dp(12))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(11, 19, 42), Color.rgb(35, 42, 79), Color.rgb(12, 24, 48)))
        }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val dateStack = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        dateStack.addView(label(SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault()), 11f, 0xFFB9C7E8.toInt(), true))
        dateStack.addView(label(SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date()), 15f, Color.WHITE, false))
        top.addView(dateStack)
        top.addView(TextView(this).apply {
            text = "●  ${batteryPercent()}%"
            textSize = 12f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(13), dp(8), dp(13), dp(8)); background = panel(30)
        })
        root.addView(top)

        val clock = TextView(this).apply {
            text = now(); textSize = 62f; letterSpacing = -.045f
            setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setPadding(0, dp(17), 0, 0)
        }
        root.addView(clock)
        root.addView(label("A little space, beautifully organized.", 13f, 0xFFB9C7E8.toInt(), false).apply {
            setPadding(dp(2), 0, 0, dp(17))
        })

        val widgets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val calendar = widget("CALENDAR", SimpleDateFormat("d", Locale.getDefault()).format(Date()),
            SimpleDateFormat("MMMM  •  EEEE", Locale.getDefault()).format(Date()), 0xFF8FB8FF.toInt())
        val batteryWidget = widget("BATTERY", "${batteryPercent()}%", "Power status", 0xFFB8A7FF.toInt())
        widgets.addView(calendar, LinearLayout.LayoutParams(0, dp(105), 1f).apply { marginEnd = dp(9) })
        widgets.addView(batteryWidget, LinearLayout.LayoutParams(0, dp(105), 1f))
        root.addView(widgets)

        val search = EditText(this).apply {
            hint = "⌕   Search"
            textSize = 14f; setSingleLine(true); setTextColor(Color.WHITE)
            setHintTextColor(0xFFC2CBE0.toInt()); setPadding(dp(17), 0, dp(17), 0)
            background = panel(25)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(17) })

        val appHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(15), dp(2), dp(5))
        }
        appHeader.addView(label("APPLICATIONS", 10f, 0xFFB9C7E8.toInt(), true),
            LinearLayout.LayoutParams(0, -2, 1f))
        pageLabel = label("PAGE 1", 9f, 0xFFB9C7E8.toInt(), true)
        appHeader.addView(pageLabel)
        root.addView(appHeader)

        grid = GridView(this).apply {
            numColumns = 4; verticalSpacing = dp(7); horizontalSpacing = dp(5)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            clipToPadding = false
        }
        adapter = AppAdapter()
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, position, _ ->
            val app = if (searching) visibleApps.getOrNull(position) else visibleApps.getOrNull(page * pageSize + position)
            launch(app)
        }
        grid.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; false }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (!searching && abs(dx) > dp(55) && abs(dx) > abs(dy) * 1.25f) {
                        changePage(if (dx < 0) 1 else -1); true
                    } else false
                }
                else -> false
            }
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))

        pageDots = TextView(this).apply {
            gravity = Gravity.CENTER; textSize = 13f; setTextColor(0xFFDCE5FF.toInt())
            setPadding(0, dp(2), 0, dp(8))
        }
        root.addView(pageDots)

        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(dp(8), dp(9), dp(8), dp(9)); background = panel(27)
        }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(3) })
        setContentView(root)

        loadApps()
        refreshPage()
        renderDock()
        updateClock(clock)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim()?.lowercase(Locale.getDefault()).orEmpty()
                searching = q.isNotEmpty()
                visibleApps.clear()
                if (searching) visibleApps.addAll(allApps.filter { it.label.lowercase(Locale.getDefault()).contains(q) })
                page = 0
                refreshPage()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        allApps.clear()
        resolved.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == packageName) return@mapNotNull null
            LaunchApp(ri.loadLabel(packageManager).toString(), pkg, ri.loadIcon(packageManager))
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }.forEach { allApps.add(it) }
        val preferred = listOf("com.google.android.dialer", "com.google.android.apps.messaging",
            "com.google.android.GoogleCamera", "com.android.camera2", "com.android.chrome")
        dockApps.clear()
        preferred.mapNotNull { p -> allApps.firstOrNull { it.packageName == p } }.take(4).forEach { dockApps.add(it) }
        if (dockApps.size < 4) allApps.filter { a -> dockApps.none { it.packageName == a.packageName } }
            .take(4 - dockApps.size).forEach { dockApps.add(it) }
        visibleApps.clear(); visibleApps.addAll(allApps)
    }

    private fun refreshPage() {
        val count = if (searching) visibleApps.size else allApps.size
        val pages = if (searching) 1 else maxOf(1, (count + pageSize - 1) / pageSize)
        page = page.coerceIn(0, pages - 1)
        pageLabel.text = if (searching) "SEARCH" else "PAGE ${page + 1}"
        pageDots.text = if (searching) "${visibleApps.size} RESULTS" else
            (0 until pages).joinToString("   ") { if (it == page) "●" else "○" }
        adapter.notifyDataSetChanged()
    }

    private fun changePage(delta: Int) {
        val pages = maxOf(1, (allApps.size + pageSize - 1) / pageSize)
        page = (page + delta).coerceIn(0, pages - 1)
        refreshPage()
    }

    private fun renderDock() {
        dock.removeAllViews()
        dockApps.forEach { app ->
            val holder = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(53), 1f)
                isClickable = true; setOnClickListener { launch(app) }
            }
            holder.addView(ImageView(this).apply {
                setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
            }, FrameLayout.LayoutParams(dp(45), dp(45), Gravity.CENTER))
            dock.addView(holder)
        }
    }

    private fun launch(app: LaunchApp?) {
        app ?: return
        packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
    }

    private fun updateClock(clock: TextView) {
        clock.text = now()
        clock.postDelayed({ updateClock(clock) }, 1000)
    }
    private fun now() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    private fun batteryPercent(): Int {
        val intent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (scale > 0) level * 100 / scale else 0
    }
    private fun label(value: String, size: Float, color: Int, medium: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (medium) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private fun widget(kicker: String, value: String, detail: String, accent: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(15), dp(11), dp(10), dp(10)); background = panel(23)
        addView(label(kicker, 9f, accent, true))
        addView(label(value, 27f, Color.WHITE, false).apply { setPadding(0, dp(2), 0, 0) })
        addView(label(detail, 10f, 0xFFBEC9E2.toInt(), false))
    }
    private fun panel(radius: Int) = GradientDrawable().apply {
        cornerRadius = dp(radius).toFloat()
        setColor(0x24FFFFFF)
        setStroke(dp(1), 0x30FFFFFF)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount(): Int = if (searching) visibleApps.size else
            minOf(pageSize, (allApps.size - page * pageSize).coerceAtLeast(0))
        override fun getItem(position: Int): LaunchApp = if (searching) visibleApps[position] else allApps[page * pageSize + position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val app = getItem(position)
            val cell = convertView as? LinearLayout ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(1), dp(5), dp(1), dp(5))
            }
            cell.removeAllViews()
            val tile = FrameLayout(this@MainActivity).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(0x14FFFFFF)
                    setStroke(dp(1), 0x18FFFFFF)
                }
            }
            tile.addView(ImageView(this@MainActivity).apply {
                setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = app.label
            }, FrameLayout.LayoutParams(dp(52), dp(52), Gravity.CENTER))
            cell.addView(tile, LinearLayout.LayoutParams(dp(60), dp(60)))
            cell.addView(TextView(this@MainActivity).apply {
                text = app.label; textSize = 10.5f; gravity = Gravity.CENTER
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(Color.WHITE); setPadding(0, dp(5), 0, 0)
            }, LinearLayout.LayoutParams(-1, dp(19)))
            return cell
        }
    }
}