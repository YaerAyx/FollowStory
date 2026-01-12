package com.example.course.ui

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.course.R
import com.example.course.data.AppDatabase
import com.example.course.utils.CourseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: android.widget.EditText
    private lateinit var btnGo: Button
    private lateinit var btnGetSchedule: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        webView = findViewById(R.id.webview)
        etUrl = findViewById(R.id.et_url)
        btnGo = findViewById(R.id.btn_go)
        btnGetSchedule = findViewById(R.id.btn_get_schedule)

        setupWebView()

        val defaultUrl = "https://jwxt.cjlu.edu.cn/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151&layout=default"
        etUrl.setText(defaultUrl)
        webView.loadUrl(defaultUrl)

        btnGo.setOnClickListener {
            val url = etUrl.text.toString()
            if (url.isNotEmpty()) {
                webView.loadUrl(url)
            }
        }
        
        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                btnGo.performClick()
                true
            } else {
                false
            }
        }

        btnGetSchedule.setOnClickListener {
            grabSchedule()
        }
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        
        CookieManager.getInstance().setAcceptCookie(true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                etUrl.setText(url)
            }
        }
    }

    private fun grabSchedule() {
        // Inject Javascript to get the full HTML content
        webView.evaluateJavascript(
            "(function() { return document.documentElement.outerHTML; })();",
            ValueCallback { html ->
                handleHtmlContent(html)
            })
    }

    private fun handleHtmlContent(rawHtml: String) {
        // rawHtml is JSON encoded string, need to unescape
        // e.g. "\u003Chtml..."
        // Simple unescape: remove surrounding quotes and replace logic if needed
        // Gson can handle this nicely usually, but let's try simple string manipulation first
        
        var cleanHtml = rawHtml
        if (cleanHtml.startsWith("\"") && cleanHtml.endsWith("\"")) {
            cleanHtml = cleanHtml.substring(1, cleanHtml.length - 1)
        }
        
        // Unescape common characters
        cleanHtml = cleanHtml.replace("\\u003C", "<")
            .replace("\\u003E", ">")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")

        lifecycleScope.launch(Dispatchers.IO) {
            val courses = CourseParser.parseHtml(cleanHtml)
            
            if (courses.isNotEmpty()) {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java, "course-database"
                ).build()
                
                db.courseDao().deleteAll() // Clear old data
                db.courseDao().insertAll(courses)
                db.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BrowserActivity, "成功获取 ${courses.size} 节课", Toast.LENGTH_LONG).show()
                    finish() // Close activity and go back to main
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BrowserActivity, "未解析到课程，请确保已登录并显示课表页面", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
