package com.discipline.alarm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val image1Path: String = "",
    val image2Path: String = "",
    val image3Path: String = "",
    val embedding1: ByteArray? = null,
    val embedding2: ByteArray? = null,
    val embedding3: ByteArray? = null,
    val threshold: Float = 0.75f,
    val createdAt: Long = System.currentTimeMillis()
)
