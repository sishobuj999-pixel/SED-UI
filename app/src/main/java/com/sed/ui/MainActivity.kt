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

data class LaunchApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : Activity() {
    private lateinit var grid: GridView
    private lateinit var adapter: AppAdapter
    private val allApps = mutableListOf<LaunchApp>()
    private val visibleApps = mutableListOf<LaunchApp>()
    private val dockApps = mutableListOf<LaunchApp>()
    private var searchBox: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(54), dp(22), dp(10))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(20, 34, 66), Color.rgb(37, 37, 78), Color.rgb(10, 18, 38))
            )
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dateBlock = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        val day = TextView(this).apply {
            text = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())
            textSize = 11f; letterSpacing = .18f; setTextColor(Color.rgb(190, 205, 235))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val date = TextView(this).apply {
            text = SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date())
            textSize = 16f; setTextColor(Color.WHITE); setPadding(0, dp(3), 0, 0)
        }
        dateBlock.addView(day); dateBlock.addView(date)
        topRow.addView(dateBlock)

        val battery = batteryPercent()
        val batteryChip = TextView(this).apply {
            text = "▰  $battery%"
            textSize = 12f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = glass(22)
        }
        topRow.addView(batteryChip)
        root.addView(topRow)

        val clock = TextView(this).apply {
            text = timeNow()
            textSize = 58f; letterSpacing = -.04f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setPadding(0, dp(22), 0, 0)
        }
        root.addView(clock)
        val greeting = TextView(this).apply {
            text = "Your space. Your pace."
            textSize = 14f; setTextColor(Color.rgb(190, 202, 226))
            setPadding(dp(2), 0, 0, dp(18))
        }
        root.addView(greeting)

        val widgetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val calendarCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(12), dp(13))
            background = glass(22)
            layoutParams = LinearLayout.LayoutParams(0, dp(82), 1f).apply { marginEnd = dp(8) }
        }
        calendarCard.addView(TextView(this).apply {
            text = SimpleDateFormat("MMM", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())
            textSize = 10f; letterSpacing = .16f; setTextColor(Color.rgb(190, 205, 240))
        })
        calendarCard.addView(TextView(this).apply {
            text = SimpleDateFormat("d", Locale.getDefault()).format(Date())
            textSize = 27f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        })
        val focusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(10), dp(12), dp(10))
            background = glass(22)
            layoutParams = LinearLayout.LayoutParams(0, dp(82), 1f)
        }
        focusCard.addView(TextView(this).apply {
            text = "SED UI"; textSize = 10f; letterSpacing = .16f
            setTextColor(Color.rgb(190, 205, 240))
        })
        focusCard.addView(TextView(this).apply {
            text = "Light. Clean. Yours."
            textSize = 14f; setTextColor(Color.WHITE)
            setPadding(0, dp(5), 0, 0)
        })
        widgetRow.addView(calendarCard); widgetRow.addView(focusCard)
        root.addView(widgetRow)

        val search = EditText(this).apply {
            hint = "⌕    Search apps"
            textSize = 14f; setSingleLine(true)
            setTextColor(Color.WHITE); setHintTextColor(Color.rgb(195, 202, 220))
            setPadding(dp(16), 0, dp(16), 0)
            background = glass(24)
        }
        searchBox = search
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(17) })

        val appsTitle = TextView(this).apply {
            text = "YOUR APPS"
            textSize = 10f; letterSpacing = .2f
            setTextColor(Color.rgb(190, 205, 235))
            setPadding(dp(3), dp(17), 0, dp(7))
        }
        root.addView(appsTitle)

        grid = GridView(this).apply {
            numColumns = 4
            verticalSpacing = dp(5)
            horizontalSpacing = dp(4)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            clipToPadding = false
        }
        adapter = AppAdapter(false)
        grid.adapter = adapter
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(8), dp(7), dp(8))
            background = glass(28)
        }
        root.addView(dock, LinearLayout.LayoutParams(-1, dp(82)).apply { topMargin = dp(7) })

        setContentView(root)
        loadApps()
        updateClock(clock)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim()?.lowercase(Locale.getDefault()).orEmpty()
                visibleApps.clear()
                visibleApps.addAll(allApps.filter { it.label.lowercase(Locale.getDefault()).contains(q) })
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        grid.setOnItemClickListener { _, _, position, _ -> launch(visibleApps.getOrNull(position)) }
        dock.tag = "dock"
        renderDock(dock)
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        allApps.clear()
        resolved.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == packageName) return@mapNotNull null
            LaunchApp(ri.loadLabel(packageManager).toString(), pkg, ri.loadIcon(packageManager))
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
            .forEach { allApps.add(it) }
        visibleApps.clear(); visibleApps.addAll(allApps)
        val preferred = listOf("com.google.android.dialer", "com.android.contacts", "com.google.android.apps.messaging", "com.android.camera2", "com.google.android.GoogleCamera", "com.android.chrome")
        dockApps.clear()
        preferred.mapNotNull { p -> allApps.firstOrNull { it.packageName == p } }.take(4).forEach { dockApps.add(it) }
        if (dockApps.size < 4) allApps.filter { a -> dockApps.none { it.packageName == a.packageName } }.take(4 - dockApps.size).forEach { dockApps.add(it) }
        adapter.notifyDataSetChanged()
        val dock = findViewById<LinearLayout>(android.R.id.content)?.findViewWithTag<LinearLayout>("dock")
        if (dock != null) renderDock(dock)
    }

    private fun renderDock(dock: LinearLayout) {
        dock.removeAllViews()
        dockApps.forEach { app ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
                isClickable = true
                setOnClickListener { launch(app) }
            }
            item.addView(ImageView(this).apply {
                setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(43), dp(43)))
            dock.addView(item)
        }
    }

    private fun launch(app: LaunchApp?) {
        app ?: return
        packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
    }

    private fun updateClock(clock: TextView) {
        clock.text = timeNow()
        clock.postDelayed({ updateClock(clock) }, 1000)
    }

    private fun timeNow() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    private fun batteryPercent(): Int {
        val intent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (scale > 0) (level * 100 / scale) else 0
    }

    private fun glass(radius: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(radius).toFloat()
        setColor(Color.argb(34, 255, 255, 255))
        setStroke(dp(1), Color.argb(35, 255, 255, 255))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class AppAdapter(private val dockMode: Boolean) : BaseAdapter() {
        override fun getCount() = visibleApps.size
        override fun getItem(position: Int) = visibleApps[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val cell = convertView as? LinearLayout ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(2), dp(7), dp(2), dp(7))
            }
            cell.removeAllViews()
            val iconWrap = FrameLayout(this@MainActivity).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(15).toFloat()
                    setColor(Color.argb(18, 255, 255, 255))
                }
            }
            val icon = ImageView(this@MainActivity).apply {
                setImageDrawable(item.icon); scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = item.label
            }
            iconWrap.addView(icon, FrameLayout.LayoutParams(dp(51), dp(51), Gravity.CENTER))
            cell.addView(iconWrap, LinearLayout.LayoutParams(dp(57), dp(57)))
            val name = TextView(this@MainActivity).apply {
                text = item.label; textSize = 10.5f; gravity = Gravity.CENTER
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(Color.WHITE); setPadding(0, dp(5), 0, 0)
            }
            cell.addView(name, LinearLayout.LayoutParams(-1, dp(19)))
            return cell
        }
    }
}