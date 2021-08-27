package pl.jakubweg.jetrun.ui.activity

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import pl.jakubweg.jetrun.component.AuthState
import pl.jakubweg.jetrun.ui.theme.JetRunTheme
import pl.jakubweg.jetrun.vm.UserViewModel


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
                            val vm: UserViewModel = viewModel()
                            val state by vm.state.observeAsState()

                            when (state) {
                                AuthState.Authorizing -> {
                                    Text(
                                        text = "Loading...\nPlease wait",
                                        textAlign = TextAlign.Center
                                    )
                                }
                                is AuthState.NotSigned.FailedToAuthorize -> {
                                    Text(
                                        text = "Signing failed: " + (state as AuthState.NotSigned.FailedToAuthorize).reason,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colors.error,
                                        modifier = Modifier.widthIn(max = 200.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    SignInButton(vm)
                                }
                                is AuthState.NotSigned -> {
                                    Text(text = "You are not signed in!")
                                    Spacer(modifier = Modifier.height(16.dp))
                                    SignInButton(vm)
                                }
                                is AuthState.Signed -> {
                                    Text(
                                        text = "AWESOME!\nHello ${(state as AuthState.Signed).user.displayName}",
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { vm.signOut() }) {
                                        Text(text = "SIGN OUT", fontWeight = FontWeight.Bold)
                                    }
                                }
                                else -> Text(text = "??")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignInButton(vm: UserViewModel) {
    val activity: Activity = LocalContext.current as Activity
    Button(onClick = { vm.signIn(activity) }) {
        Text(text = "SIGN IN", fontWeight = FontWeight.Bold)
    }
}