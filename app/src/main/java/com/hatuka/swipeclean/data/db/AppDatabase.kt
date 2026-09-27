package com.hatuka.swipeclean.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.hatuka.swipeclean.core.review.DecisionState

@Database(
    entities = [DecisionEntity::class, DailyStatsEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun decisions(): DecisionDao
    abstract fun stats(): StatsDao

    companion object {
        const val NAME = "swipeclean.db"
    }
}

class Converters {
    @TypeConverter
    fun fromState(state: DecisionState): String = state.name

    @TypeConverter
    fun toState(value: String): DecisionState = DecisionState.valueOf(value)
}
