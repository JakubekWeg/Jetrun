package pl.jakubweg.jetrun.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.jakubweg.jetrun.component.AuthState
import pl.jakubweg.jetrun.vm.UserViewModel

@Composable
fun MainAppScreen(
    state: AuthState.Signed,
    userViewModel: UserViewModel
) {
    Text(
        text = "AWESOME!",
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = { userViewModel.signOut() }) {
        Text(text = "SIGN OUT", fontWeight = FontWeight.Bold)
    }
}