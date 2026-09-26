package com.discipline.alarm.di

import com.discipline.alarm.ml.FeatureExtractor
import com.discipline.alarm.ml.TFLiteFeatureExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MlModule {

    @Binds
    @Singleton
    abstract fun bindFeatureExtractor(
        impl: TFLiteFeatureExtractor
    ): FeatureExtractor
}
