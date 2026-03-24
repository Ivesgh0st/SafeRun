package com.saferun.app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;


@Entity(tableName = "run_sessions")
public class RunSession {

    // ID gerado automaticamente pelo banco
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Data e hora que a corrida aconteceu
    public Date date;

    // Distância total percorrida em metros
    public double distanceMeters;

    // Duração total em segundos
    public int durationSeconds;

    // Ponto de início da corrida
    public double startLat;
    public double startLng;
}