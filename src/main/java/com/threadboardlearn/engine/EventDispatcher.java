package com.threadboardlearn.engine;

/**
 * Despachador desacoplado de eventos para la interfaz de usuario.
 * Permite ejecutar callbacks en el hilo principal correspondiente a la plataforma
 * (SwingUtilities.invokeLater en Desktop, Handler/Looper en Android, o ejecución directa).
 */
@FunctionalInterface
public interface EventDispatcher {
    void dispatch(Runnable action);
}
