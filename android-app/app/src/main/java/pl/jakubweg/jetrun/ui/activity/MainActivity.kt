package pl.jakubweg.jetrun.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import pl.jakubweg.jetrun.component.AuthState
import pl.jakubweg.jetrun.ui.screen.AppLoadingScreen
import pl.jakubweg.jetrun.ui.screen.AppLoginScreen
import pl.jakubweg.jetrun.ui.screen.MainAppScreen
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
                    val userViewModel: UserViewModel = viewModel()
                    val state by userViewModel.state.collectAsState()
                    when (state) {
                        is AuthState.Signed -> MainAppScreen(
                            state as AuthState.Signed,
                            userViewModel
                        )
                        AuthState.Authorizing, is AuthState.NotSigned -> AppLoginScreen(
                            userViewModel
                        )
                        AuthState.Unknown -> AppLoadingScreen()
                    }
                }
            }
        }
    }
}


