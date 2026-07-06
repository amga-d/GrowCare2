package com.example.growCare.di

import com.example.growCare.data.local.inference.LocalChatInference
import com.example.growCare.data.local.inference.LocalDiseaseInference
import com.example.growCare.data.local.inference.GemmaChatInference
import com.example.growCare.data.local.inference.YoloDiseaseInference
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {

    @Binds
    @Singleton
    abstract fun bindLocalChatInference(
        impl: GemmaChatInference
    ): LocalChatInference

    @Binds
    @Singleton
    abstract fun bindLocalDiseaseInference(
        impl: YoloDiseaseInference
    ): LocalDiseaseInference
}
