package com.callbackdev.thatstheword.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.callbackdev.thatstheword.core.data.InterfaceLanguage
import com.callbackdev.thatstheword.core.data.SystemInterfaceLanguage
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.words.WordFiles
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ThatsTheWordDatabase =
        Room.databaseBuilder(context, ThatsTheWordDatabase::class.java, ThatsTheWordDatabase.NAME).build()

    /** `files/datastore/settings.preferences_pb`, the file the backup rules name. */
    @Provides
    @Singleton
    fun settingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    fun wordFiles(@ApplicationContext context: Context): WordFiles = AssetWordFiles(context.assets)

    /**
     * Not a singleton: each read takes the zone the phone is in now, so a phone carried across a
     * time zone gives the local date it is there (PLANNING.md §4.6).
     */
    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
interface DataBindings {
    @Binds
    fun interfaceLanguage(impl: SystemInterfaceLanguage): InterfaceLanguage
}
