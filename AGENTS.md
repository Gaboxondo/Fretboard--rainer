# Reglas de Proyecto y Agente

## Validación Obligatoria con Maven
- En todos los proyectos basados en Maven, el asistente DEBE SIEMPRE ejecutar y comprobar la compilación y pruebas con Maven (`mvn clean test` o `mvn test-compile`) antes de dar cualquier tarea por terminada o informar al usuario.
- Comprobar que el entorno de ejecución de Maven y Java corresponda con la versión requerida por el proyecto (ej. Java 21). Si el entorno por defecto tiene otra versión, configurar explícitamente `$env:JAVA_HOME` o el toolchain correspondiente.
- No asumir que el código compila sin haber ejecutado la verificación directa en la terminal.
