package com.saferun.app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

// Esta classe representa UMA corrida salva no banco
@Entity(tableName = "run_sessions")
public class RunSession {

    // ID único gerado automaticamente pelo banco
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Data e hora que a corrida aconteceu
    public Date date;

    // Distância total percorrida em metros
    public double distanceMeters;

    // Duração total em segundos
    public int durationSeconds;

    // Ponto de início da corrida (coordenadas GPS)
    public double startLat;
    public double startLng;
}