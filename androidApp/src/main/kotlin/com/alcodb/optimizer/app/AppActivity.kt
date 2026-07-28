package com.alcodb.optimizer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.alcodb.optimizer.common.compose.AlcoDbOptimizerApp

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AlcoDbOptimizerApp(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
