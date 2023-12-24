package com.example.googlemapsdemo.di

import com.example.googlemapsdemo.utils.managers.AppLocationManager
import com.example.googlemapsdemo.utils.managers.AppLocationManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {

    @Binds
    abstract fun bindAppLocationManager(appLocationManagerImpl: AppLocationManagerImpl): AppLocationManager

}