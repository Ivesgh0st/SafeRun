package com.saferun.app.utils;

public class FallDetector {

    // Se a aceleração cair abaixo disso, o celular está em queda livre
    private static final float FREE_FALL_THRESHOLD = 2.0f;

    // Se a aceleração subir acima disso após a queda, houve impacto
    private static final float IMPACT_THRESHOLD = 20.0f;

    // Tempo máximo entre queda livre e impacto (0,8 segundos)
    private static final long FALL_WINDOW_MS = 800;

    private boolean freeFallDetected = false;
    private long freeFallTimestamp = 0;

   
    public boolean detect(float x, float y, float z) {

        // Calcula a força total de aceleração nos 3 eixos
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

        long now = System.currentTimeMillis();

        if (!freeFallDetected) {

            // FASE 1: procura queda livre (aceleração muito baixa)
            if (magnitude < FREE_FALL_THRESHOLD) {
                freeFallDetected = true;
                freeFallTimestamp = now;
            }

        } else {

            // Se demorou mais de 800ms, reseta e começa de novo
            if (now - freeFallTimestamp > FALL_WINDOW_MS) {
                freeFallDetected = false;
                return false;
            }

            // FASE 2: procura impacto (aceleração muito alta)
            if (magnitude > IMPACT_THRESHOLD) {
                freeFallDetected = false;
                return true; // QUEDA CONFIRMADA!
            }
        }

        return false; // Nenhuma queda detectada
    }

    // Reseta o detector manualmente
    public void reset() {
        freeFallDetected = false;
        freeFallTimestamp = 0;
    }
}