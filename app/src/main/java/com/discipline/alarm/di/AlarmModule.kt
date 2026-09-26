package com.discipline.alarm.di

import com.discipline.alarm.data.alarm.AndroidAlarmScheduler
import com.discipline.alarm.domain.alarm.AlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(
        impl: AndroidAlarmScheduler
    ): AlarmScheduler
}
