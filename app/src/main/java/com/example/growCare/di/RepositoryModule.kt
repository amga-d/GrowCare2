package com.example.growCare.di

import dagger.Module
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository dependency injection module
 * Binds repository interfaces to their implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: com.example.growCare.data.repository.AuthRepositoryImpl
    ): com.example.growCare.domain.repository.AuthRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        impl: com.example.growCare.data.repository.WeatherRepositoryImpl
    ): com.example.growCare.domain.repository.WeatherRepository

    @Binds
    @Singleton
    abstract fun bindDetectionRepository(
        impl: com.example.growCare.data.repository.DetectionRepositoryImpl
    ): com.example.growCare.domain.repository.DetectionRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: com.example.growCare.data.repository.ChatRepositoryImpl
    ): com.example.growCare.domain.repository.ChatRepository

    @Binds
    @Singleton
    abstract fun bindTipsRepository(
        impl: com.example.growCare.data.repository.TipsRepositoryImpl
    ): com.example.growCare.domain.repository.TipsRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: com.example.growCare.data.repository.UserRepositoryImpl
    ): com.example.growCare.domain.repository.UserRepository
}
