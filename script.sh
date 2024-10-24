#!/bin/bash

# Número de clientes a iniciar
NUM_CLIENTS=5000
# Archivo de log para registrar errores
ERROR_LOG="error_log.txt"
# Archivo para registrar respuestas del servidor
RESPONSE_LOG="response_log.txt"

# Limpiar archivos de log anteriores
> "$ERROR_LOG"
> "$RESPONSE_LOG"

echo "Asegúrate de que el servidor esté corriendo antes de continuar."
read -p "Presiona Enter para continuar una vez que el servidor esté listo..."

echo "Corriendo clientes..."

# Inicia múltiples clientes en paralelo
for i in $(seq 1 $NUM_CLIENTS); do
  {
    # Usar un here document para simular la entrada
    {
      echo -e "1\n3000\n" | java -jar client/build/libs/client.jar
    } >> "$RESPONSE_LOG" 2>> "$ERROR_LOG"

    # Agregar una pausa para permitir que el servidor procese
    sleep 0.1
  } &
done

# Esperar a que todos los clientes terminen
wait

echo "Clientes ejecutados. Revisa los logs para más detalles."
echo "Errores registrados en: $ERROR_LOG"
echo "Respuestas registradas en: $RESPONSE_LOG"

# Análisis simple de errores (cuántos timeouts hubo)
TIMEOUT_COUNT=$(grep -ci "timeout" "$ERROR_LOG")
SOCKET_TIMEOUT_COUNT=$(grep -ci "SocketTimeoutException" "$ERROR_LOG")
CONNECT_TIMEOUT_COUNT=$(grep -ci "ConnectTimeoutException" "$ERROR_LOG")
READ_TIMEOUT_COUNT=$(grep -ci "ReadTimeoutException" "$ERROR_LOG")

# Mostrar los resultados del análisis
echo "Número de timeouts registrados: $TIMEOUT_COUNT"
echo "SocketTimeoutExceptions: $SOCKET_TIMEOUT_COUNT"
echo "ConnectTimeoutExceptions: $CONNECT_TIMEOUT_COUNT"
echo "ReadTimeoutExceptions: $READ_TIMEOUT_COUNT"
