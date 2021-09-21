package pl.jakubweg.jetrun.di

import android.content.Context
import android.location.LocationManager
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import pl.jakubweg.jetrun.component.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideContext(@ApplicationContext ctx: Context): Context = ctx

    @Singleton
    @Provides
    fun provideAuth(): AuthComponent = FirebaseAuthComponent(FirebaseAuth.getInstance())

    @Singleton
    @Provides
    fun provideWorkoutTracker(
        t: TimerCoroutineComponent,
        l: LocationProviderComponent,
        s: WorkoutStatsComponent,
        time: TimeComponent
    ): WorkoutTrackerComponent = WorkoutTrackerComponent(t, l, s, time)

    @Provides
    @Singleton
    fun provideTimerCoroutineComponent(d: CoroutineDispatcher): TimerCoroutineComponent =
        TimerCoroutineComponent(d)

    @Singleton
    @Provides
    fun provideTimeComponent(): TimeComponent = RealTimeComponent()

    @Singleton
    @Provides
    fun provideStatsComponent() = WorkoutStatsComponent()

    @Singleton
    @Provides
    fun provideAndroidLocationManager(context: Context) =
        context.getSystemService(LocationManager::class.java)!!

    @Singleton
    @Provides
    fun provideDefaultDispatcher() = Dispatchers.Default
}