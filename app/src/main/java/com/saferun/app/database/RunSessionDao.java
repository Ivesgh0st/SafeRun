package com.saferun.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

// DAO = interface que define o que podemos fazer no banco
@Dao
public interface RunSessionDao {

    // Salva uma nova corrida
    @Insert
    void insert(RunSession session);

    // Busca todas as corridas da mais recente para a mais antiga
    @Query("SELECT * FROM run_sessions ORDER BY date DESC")
    List<RunSession> getAllSessionsOrderedByDate();

    // Busca todas sem ordenação (para calcular totais)
    @Query("SELECT * FROM run_sessions")
    List<RunSession> getAllSessions();
}