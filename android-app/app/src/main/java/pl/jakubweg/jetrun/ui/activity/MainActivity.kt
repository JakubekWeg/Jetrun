package pl.jakubweg.jetrun.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import pl.jakubweg.jetrun.ui.theme.JetRunTheme


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JetRunTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Hello world")
                            Spacer(modifier = Modifier.height(16.dp))
                            val context = LocalContext.current
                            Button(onClick = {
                                Toast.makeText(
                                    context,
                                    "Hello world!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }) {
                                Text(text = "SHOW HELLO", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}