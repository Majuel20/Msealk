package com.msealk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.msealk.notifications.AppSealNotifications
import com.msealk.ui.AppSealScreen
import com.msealk.ui.theme.MSealkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSealNotifications.createChannel(this)

        setContent {
            MSealkTheme {
                AppSealScreen()
            }
        }
    }
}
