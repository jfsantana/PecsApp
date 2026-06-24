package com.pecsapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pecsapp.data.model.PecImage

@Database(entities = [PecImage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pecImageDao(): PecImageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pecs_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Insert default images on creation
                            db.execSQL("INSERT INTO pec_images (label, imagePath, isActive, isYoutube, tapCount) VALUES ('Comida', 'android.resource://com.pecsapp/drawable/ic_food', 1, 0, 0)")
                            db.execSQL("INSERT INTO pec_images (label, imagePath, isActive, isYoutube, tapCount) VALUES ('Agua', 'android.resource://com.pecsapp/drawable/ic_water', 1, 0, 0)")
                            db.execSQL("INSERT INTO pec_images (label, imagePath, isActive, isYoutube, tapCount) VALUES ('Baño', 'android.resource://com.pecsapp/drawable/ic_toilet', 1, 0, 0)")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}