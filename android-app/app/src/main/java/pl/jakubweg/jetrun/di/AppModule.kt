package pl.jakubweg.jetrun.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.jakubweg.jetrun.component.AuthComponent
import pl.jakubweg.jetrun.component.FirebaseAuthComponent
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
}