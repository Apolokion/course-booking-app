package biz.pock.coursebookingapp.app

import android.content.Context
import android.content.SharedPreferences
import biz.pock.coursebookingapp.shared.SHARED_PREFS
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Klasse als Dagger/Hilt Modul markieren
@Module
// Modul wird in der SingletonComponent installiert,
// also zur gesamten Laufzeit der App nur eine Instanz
@InstallIn(SingletonComponent::class)
object AppModule {

    // Provider Funktion für SharedPrefs um eine SharedPrefs Instanz als
    // Singleton Pattern zu erstellen
    @Provides
    @Singleton
    fun provideSharedPreferences(
        // Der Context wird von Hilt durch @ApplicationContext bereitgestellt
        @ApplicationContext context: Context
    ): SharedPreferences {
        // Erzeugt die Instanz und gibt sie zurück
        return context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
    }

    // Provider Funktion für Gson
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

}