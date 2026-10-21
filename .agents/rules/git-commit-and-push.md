# Regla de Commit y Push Obligatorio al Repositorio Remoto

1. Siempre que se realicen cambios, se resuelvan tareas o se creen nuevas características, una vez que la compilación y los tests hayan pasado con éxito (`mvn clean test`), el asistente DEBE SIEMPRE:
   - Añadir los archivos modificados/creados relevantes (`git add`).
   - Crear un commit con un mensaje descriptivo y conciso en español o inglés (`git commit -m "..."`).
   - Hacer push inmediato a la rama correspondiente en el repositorio remoto (`git push origin <rama>`).
2. Verificar con `git status` que el directorio de trabajo quede limpio y sincronizado con el repositorio remoto antes de dar la tarea por concluida.
