package biz.pock.coursebookingapp.di

import android.content.Context
import biz.pock.coursebookingapp.data.api.interfaces.NetworkConnection
import biz.pock.coursebookingapp.data.api.interceptors.LoggingInterceptor
import biz.pock.coursebookingapp.data.auth.TokenManager
import biz.pock.coursebookingapp.utils.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): LoggingInterceptor {
        return LoggingInterceptor()
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkConnection {
        return NetworkUtils(context)
    }

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

}