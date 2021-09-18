package pl.jakubweg.jetrun.ui.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.jakubweg.jetrun.component.AuthState
import pl.jakubweg.jetrun.vm.UserViewModel

@ExperimentalAnimationApi
@Composable
fun AppLoginScreen(userViewModel: UserViewModel) {
    AnimatedVisibility(visible = true) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(.8f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .scrollable(rememberScrollState(), Orientation.Vertical),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {

                    Text(
                        text = "Welcome in JetRunner",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    StateText(userViewModel.state.collectAsState().value)

                    LoginButtons(userViewModel)
                }
            }
        }
    }
}

@Composable
fun StateText(state: AuthState) {
    val errorMessage = remember(state) {
        (state as? AuthState.NotSigned.FailedToAuthorize)?.run { "Failed to proceed:\n${reason}" }
            ?: if (state === AuthState.Authorizing) "Signing in progress" else "Sign in to continue"
    }
    val errorColor = MaterialTheme.colors.error
    val currentStatusColor = remember(state) {
        if (state is AuthState.NotSigned.FailedToAuthorize) errorColor
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
}

@Composable
private fun LoginButtons(
    userViewModel: UserViewModel
) {
    val activity: Activity = LocalContext.current as Activity

    Button(onClick = {
        userViewModel.signInWithGithub(activity)
    }) {
        Text("Continue via GitHub")
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(onClick = {
        userViewModel.signInAnonymously()
    }) {
        Text("Continue anonymously")
    }
}