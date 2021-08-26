package pl.jakubweg.jetrun

import androidx.compose.runtime.livedata.observeAsState
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.jakubweg.jetrun.ui.theme.JetRunTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JetRunTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Greeting("Android")
                }
            }
        }
    }
}

@HiltViewModel
class MyViewModel @Inject constructor() : ViewModel() {
    fun clicked() {
        viewModelScope.launch {
            delay(1000)
            withContext(Dispatchers.Default) {
                counter.postValue((counter.value ?: 0) + 1)
            }
        }
    }

    val counter = MutableLiveData(5)
}

@Composable
fun Greeting(name: String, vm: MyViewModel = viewModel()) {
    val counter by vm.counter.observeAsState()

    Column {
        Text(text = "Hello $name!\nHow are you?\nCounter is $counter")
        Button(onClick = { vm.clicked() }) {
            Text(text = "CLICK ME PLEASE")
        }

        var isBig by remember { mutableStateOf(true) }

        val size by animateDpAsState(targetValue = (if(isBig) 180 else 80).dp)
        Box(modifier = Modifier
            .size(size)
            .background(Color.Red)
            .clickable { isBig = !isBig })
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    JetRunTheme {

        Greeting("Android")
    }
}