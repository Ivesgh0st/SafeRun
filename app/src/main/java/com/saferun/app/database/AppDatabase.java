package com.saferun.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

// Informa ao Room quais tabelas existem no banco
@Database(entities = {RunSession.class}, version = 1, exportSchema = false)
// Registra o conversor de datas
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    // Instância única do banco
    private static volatile AppDatabase instance;

    // Dá acesso às operações do banco
    public abstract RunSessionDao runSessionDao();

    // Retorna sempre a mesma instância do banco
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "saferun_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}