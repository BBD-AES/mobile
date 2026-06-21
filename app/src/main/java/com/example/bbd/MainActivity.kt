package com.example.bbd

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bbd.auth.AuthManager
import com.example.bbd.ui.BbdApp
import com.example.bbd.ui.theme.BbdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthManager.init(this)
        // 흰 배경 + 어두운(라이트 모드) 상태바·내비바 아이콘.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            BbdTheme { BbdApp() }
        }
    }

    override fun onDestroy() {
        AuthManager.dispose()
        super.onDestroy()
    }
}
