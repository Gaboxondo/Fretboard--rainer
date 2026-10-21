# Regla de Validación con Maven

1. En todos los proyectos basados en Maven, el asistente DEBE SIEMPRE ejecutar y comprobar la compilación y pruebas con Maven (`mvn clean test` o `mvn test-compile`) antes de dar cualquier tarea por terminada o informar al usuario.
2. Comprobar que el entorno de ejecución de Maven y Java corresponda con la versión requerida por el proyecto (ej. Java 21). Si el entorno por defecto tiene otra versión (ej. Java 11), configurar explícitamente `$env:JAVA_HOME` o el toolchain antes de compilar y garantizar que los scripts de ejecución y `pom.xml` compilen de forma consistente.
3. No asumir que el código compila sin haber ejecutado la verificación directa en la terminal.
