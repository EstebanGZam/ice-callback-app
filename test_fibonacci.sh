#!/bin/bash

# Configuración
NUM_CLIENTS=$1          # Número de clientes simultáneos
FIBONACCI_NUMBER=$2     # Número para calcular fibonacci
LOG_FILE="test_results.log"
SUCCESS_COUNT=0
TIMEOUT_COUNT=0
TOTAL_REQUESTS=0

# Validación de argumentos
if [ -z "$NUM_CLIENTS" ] || [ -z "$FIBONACCI_NUMBER" ]; then
    echo "Uso: $0 <número_de_clientes> <número_fibonacci>"
    echo "Ejemplo: $0 10 3000"
    exit 1
fi

# Limpia el archivo de log
echo "Iniciando prueba con $NUM_CLIENTS clientes solicitando Fibonacci($FIBONACCI_NUMBER)" > "$LOG_FILE"
echo "Timestamp,Cliente,Estado,Tiempo_Respuesta,Latencia" >> "$LOG_FILE"

# Función para ejecutar un cliente individual
run_client() {
    local client_id=$1
    
    # Ejecuta el cliente Java y maneja la interacción
    {
        echo "1"  # Selecciona la opción 1
        echo "$FIBONACCI_NUMBER"  # Envía el número para calcular Fibonacci
        sleep 60 # Espera 6 segundos
        echo "exit"  # Sale del cliente
        echo "3"  # Sale del menú principal
    } | java -jar client/build/libs/client.jar 2>&1 | while IFS= read -r line; do
        if [[ $line == *"Request timed out"* ]] || [[ $line == *"InvocationTimeoutException"* ]]; then
            echo "$(date '+%Y-%m-%d %H:%M:%S'),$client_id,TIMEOUT,-1,-1" >> "$LOG_FILE"
            ((TIMEOUT_COUNT++))
            return
        elif [[ $line == *"latency ="* ]]; then
            # Extrae la latencia y el tiempo de procesamiento
            local latency=$(echo "$line" | grep -o 'latency = [0-9]\+' | awk '{print $3}')
            local processing_time=$(echo "$line" | grep -o 'processing time = [0-9]\+' | awk '{print $4}')
            echo "$(date '+%Y-%m-%d %H:%M:%S'),$client_id,SUCCESS,$processing_time,$latency" >> "$LOG_FILE"
            ((SUCCESS_COUNT++))
            return
        elif [[ $line == *"CommunicatorDestroyedException"* ]]; then
            echo "$(date '+%Y-%m-%d %H:%M:%S'),$client_id,ERROR,-1,-1" >> "$LOG_FILE"
            return
        fi
    done
    ((TOTAL_REQUESTS++))
}

# Ejecuta los clientes en paralelo
echo "Iniciando $NUM_CLIENTS clientes..."
for ((i=1; i<=$NUM_CLIENTS; i++)); do
    run_client $i &
    echo "Cliente $i iniciado"
done

# Espera a que todos los clientes terminen
wait
