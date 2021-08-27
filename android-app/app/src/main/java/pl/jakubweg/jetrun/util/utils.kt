package pl.jakubweg.jetrun.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


val <T> MutableLiveData<T>.nonMutable: LiveData<T>
    get() = this