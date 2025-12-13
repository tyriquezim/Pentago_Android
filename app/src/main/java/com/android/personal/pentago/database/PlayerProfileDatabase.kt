package com.android.personal.pentago.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.personal.pentago.model.PlayerProfile

@Database(entities = [PlayerProfile::class], version = 2)
@TypeConverters(PentagoTypeConverters::class)
abstract class PlayerProfileDatabase: RoomDatabase()
{
    abstract fun playerProfileDAO(): PlayerProfileDao
}