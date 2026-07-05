package br.com.notes.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.notes.core.data.model.Bloco
import br.com.notes.core.data.model.RegraCaptura

@Database(
    entities = [Bloco::class, RegraCaptura::class],
    version = 1,
    exportSchema = false,
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}
