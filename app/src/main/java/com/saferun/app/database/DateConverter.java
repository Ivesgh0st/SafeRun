package com.saferun.app.database;

import androidx.room.TypeConverter;
import java.util.Date;


public class DateConverter {

    // LER: transforma número em Date
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    // SALVAR: transforma Date em número
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}