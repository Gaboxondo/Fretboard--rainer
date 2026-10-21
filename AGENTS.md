# Reglas de Proyecto y Agente

## Validación Obligatoria con Maven
- En todos los proyectos basados en Maven, el asistente DEBE SIEMPRE ejecutar y comprobar la compilación y pruebas con Maven (`mvn clean test` o `mvn test-compile`) antes de dar cualquier tarea por terminada o informar al usuario.
- Comprobar que el entorno de ejecución de Maven y Java corresponda con la versión requerida por el proyecto (ej. Java 21). Si el entorno por defecto tiene otra versión, configurar explícitamente `$env:JAVA_HOME` o el toolchain correspondiente.
- No asumir que el código compila sin haber ejecutado la verificación directa en la terminal.

## Commit y Push Obligatorio al Repositorio Remoto
- Si todo compila, pasa las pruebas y funciona correctamente, el asistente DEBE SIEMPRE crear un commit con un mensaje descriptivo y hacer `git push` al repositorio remoto (`origin`) antes de dar la tarea por finalizada o reportar al usuario.
- Comprobar con `git status` que no queden cambios o archivos nuevos pendientes de subir cuando los tests hayan sido exitosos.

