package com.aigenerator.app.database

import androidx.room.*
import com.aigenerator.app.model.GenerationMode
import com.aigenerator.app.model.Message
import com.aigenerator.app.model.MessageType
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun fromMsgType(v: MessageType): String = v.name
    @TypeConverter fun toMsgType(v: String): MessageType = MessageType.valueOf(v)
    @TypeConverter fun fromGenMode(v: GenerationMode?): String? = v?.name
    @TypeConverter fun toGenMode(v: String?): GenerationMode? =
        v?.let { GenerationMode.valueOf(it) }
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sid ORDER BY timestamp ASC")
    suspend fun getBySession(sid: String): List<Message>

    @Query("SELECT * FROM messages WHERE sessionId = :sid ORDER BY timestamp ASC")
    fun observeBySession(sid: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages WHERE sessionId = :sid")
    suspend fun deleteBySession(sid: String)

    @Query("SELECT * FROM messages WHERE type IN ('AI_IMAGE','AI_VIDEO') ORDER BY timestamp DESC")
    suspend fun getAllGenerated(): List<Message>
}

@Database(entities = [Message::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
