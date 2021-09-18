package pl.jakubweg.jetrun.ui.activity

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import pl.jakubweg.jetrun.component.AuthState
import pl.jakubweg.jetrun.component.AuthState.NotSigned.FailedToAuthorize
import pl.jakubweg.jetrun.ui.theme.JetRunTheme
import pl.jakubweg.jetrun.vm.UserViewModel

@ExperimentalAnimationApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JetRunTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val vm: UserViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    when (state) {
                        is AuthState.Signed -> MainAppLayout()
                        AuthState.Authorizing, is AuthState.NotSigned -> LoginLayout()
                        AuthState.Unknown -> AppLoadingScreen()
                    }
                }
            }
        }
    }


}

@ExperimentalAnimationApi
@Composable
private fun AppLoadingScreen() {
    AnimatedVisibility(visible = true) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val rememberInfiniteTransition = rememberInfiniteTransition()

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.primary.copy(alpha = .4f)),
                contentAlignment = Alignment.Center
            ) {
                val rotation by rememberInfiniteTransition.animateFloat(
                    0f, 360f, infiniteRepeatable(
                        animation = tween(2_000, easing = LinearEasing),
                    )
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary,
                    modifier = Modifier
                        .size(40.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}


@ExperimentalAnimationApi
@Composable
private fun LoginLayout() {
    AnimatedVisibility(visible = true) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(.8f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .scrollable(rememberScrollState(), Orientation.Vertical),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val vm: UserViewModel = viewModel()
                    val activity: Activity = LocalContext.current as Activity

                    Text(
                        text = "Welcome in JetRunner",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    val state by vm.state.collectAsState()
                    val errorMessage = remember(state) {
                        (state as? FailedToAuthorize)?.run { "Failed to proceed:\n${reason}" }
                            ?: if (state === AuthState.Authorizing) "Signing in progress" else "Sign in to continue"
                    }
                    val errorColor = MaterialTheme.colors.error
                    val currentStatusColor = remember(state) {
                        if (state is FailedToAuthorize) errorColor
                        else Color.Unspecified
                    }

                    Text(
                        modifier = Modifier.fillMaxWidth().animateContentSize().padding(4.dp),
                        text = errorMessage,
                        textAlign = TextAlign.Center,
                        color = currentStatusColor,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Button(onClick = {
                        vm.signInWithGithub(activity)
                    }) {
                        Text("Continue via GitHub")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(onClick = {
                        vm.signInAnonymously()
                    }) {
                        Text("Continue anonymously")
                    }
                }
            }
        }
    }
}


@Composable
private fun MainAppLayout() {
    val vm: UserViewModel = viewModel()
    Text(
        text = "AWESOME!",
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = { vm.signOut() }) {
        Text(text = "SIGN OUT", fontWeight = FontWeight.Bold)
    }
}
