package com.saferun.app.database;

import androidx.room.TypeConverter;
import java.util.Date;

// O Room não sabe salvar Date diretamente
// Este arquivo ensina ele a converter Date <-> número
public class DateConverter {

    // Ao LER do banco: transforma número em Date
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    // Ao SALVAR no banco: transforma Date em número
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}