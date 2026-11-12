package com.sed.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LaunchApp(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

class MainActivity : Activity() {
    private lateinit var grid: GridView
    private lateinit var adapter: AppAdapter
    private val allApps = mutableListOf<LaunchApp>()
    private val visibleApps = mutableListOf<LaunchApp>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(42), dp(20), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(18, 24, 38), Color.rgb(8, 12, 20))
            )
        }

        val brand = TextView(this).apply {
            text = "SED"
            textSize = 13f
            letterSpacing = 0.28f
            setTextColor(Color.rgb(170, 190, 220))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        root.addView(brand)

        val clock = TextView(this).apply {
            textSize = 42f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(clock)

        val date = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(170, 185, 205))
            setPadding(0, 0, 0, dp(18))
        }
        root.addView(date)
        updateClock(clock, date)

        val search = EditText(this).apply {
            hint = "Search apps"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(145, 155, 175))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.argb(42, 255, 255, 255))
                setStroke(dp(1), Color.argb(35, 255, 255, 255))
            }
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)))

        val label = TextView(this).apply {
            text = "APPS"
            textSize = 11f
            letterSpacing = 0.18f
            setTextColor(Color.rgb(145, 165, 195))
            setPadding(dp(2), dp(20), 0, dp(10))
        }
        root.addView(label)

        grid = GridView(this).apply {
            numColumns = 4
            verticalSpacing = dp(12)
            horizontalSpacing = dp(4)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            isVerticalScrollBarEnabled = false
        }
        adapter = AppAdapter()
        grid.adapter = adapter
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        loadApps()

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

        grid.setOnItemClickListener { _, _, position, _ ->
            val item = visibleApps.getOrNull(position) ?: return@setOnItemClickListener
            val launch = packageManager.getLaunchIntentForPackage(item.packageName)
            if (launch != null) startActivity(launch)
        }
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        allApps.clear()
        resolved.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == packageName) return@mapNotNull null
            LaunchApp(
                ri.loadLabel(packageManager).toString(),
                pkg,
                ri.loadIcon(packageManager)
            )
        }.distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .forEach { allApps.add(it) }

        visibleApps.clear()
        visibleApps.addAll(allApps)
        adapter.notifyDataSetChanged()
    }

    private fun updateClock(clock: TextView, date: TextView) {
        val now = Date()
        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
        clock.postDelayed({ updateClock(clock, date) }, 30_000)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount() = visibleApps.size
        override fun getItem(position: Int) = visibleApps[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val cell = convertView as? LinearLayout ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(8), dp(3), dp(8))
            }
            cell.removeAllViews()
            val icon = ImageView(this@MainActivity).apply {
                setImageDrawable(item.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = item.label
            }
            cell.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)))
            val name = TextView(this@MainActivity).apply {
                text = item.label
                textSize = 11f
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(Color.WHITE)
                setPadding(0, dp(6), 0, 0)
            }
            cell.addView(name, LinearLayout.LayoutParams(-1, dp(34)))
            return cell
        }
    }
}
