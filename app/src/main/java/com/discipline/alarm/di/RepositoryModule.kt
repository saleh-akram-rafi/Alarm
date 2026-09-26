package com.discipline.alarm.di

import com.discipline.alarm.data.repository.AlarmRepositoryImpl
import com.discipline.alarm.data.repository.MissionRepositoryImpl
import com.discipline.alarm.domain.repository.AlarmRepository
import com.discipline.alarm.domain.repository.MissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(
        alarmRepositoryImpl: AlarmRepositoryImpl
    ): AlarmRepository

    @Binds
    @Singleton
    abstract fun bindMissionRepository(
        missionRepositoryImpl: MissionRepositoryImpl
    ): MissionRepository
}
