# Reglas de Proyecto y Agente

## Validación Obligatoria con Maven
- En todos los proyectos basados en Maven, el asistente DEBE SIEMPRE ejecutar y comprobar la compilación y pruebas con Maven (`mvn clean test` o `mvn test-compile`) antes de dar cualquier tarea por terminada o informar al usuario.
- Comprobar que el entorno de ejecución de Maven y Java corresponda con la versión requerida por el proyecto (ej. Java 21). Si el entorno por defecto tiene otra versión, configurar explícitamente `$env:JAVA_HOME` o el toolchain correspondiente.
- No asumir que el código compila sin haber ejecutado la verificación directa en la terminal.

## Validación Obligatoria con Gradle (Android)
- En cualquier cambio que afecte a la parte de Android (`android/**`, Gradle o scripts de compilación de Android), el asistente DEBE SIEMPRE ejecutar y comprobar la compilación con Gradle (`.\gradlew.bat assembleDebug` o verificación de Gradle) antes de realizar cualquier commit, push o dar la tarea por finalizada.
- Comprobar que no haya errores de sincronización o de configuración de tareas en Gradle.

## Commit y Push Obligatorio al Repositorio Remoto
- Si todo compila, pasa las pruebas y funciona correctamente, el asistente DEBE SIEMPRE crear un commit con un mensaje descriptivo y hacer `git push` al repositorio remoto (`origin`) antes de dar la tarea por finalizada o reportar al usuario.
- Comprobar con `git status` que no queden cambios o archivos nuevos pendientes de subir cuando los tests hayan sido exitosos.

## Documentación Bilingüe Obligatoria (Markdown)
- Todos los documentos y archivos Markdown (`.md`) dirigidos al usuario o de documentación del proyecto (ej. `README.md`, guías técnicas en `docs/`) DEBEN mantenerse tanto en Español como en Inglés como mínimo.
- La versión en inglés se ubica en la raíz o ruta base (ej. `README.md`, `docs/ANDROID.md`) y la versión en español en la carpeta `docs/es/` (ej. `docs/es/README.md`, `docs/es/ANDROID.md`).
- Ambos documentos deben enlazar recíprocamente al otro idioma en su cabecera (selector de idioma).
- Al crear, modificar o actualizar cualquier archivo `.md` de documentación, el asistente DEBE SIEMPRE actualizar o generar ambas versiones simultáneamente para mantenerlas permanentemente sincronizadas.


