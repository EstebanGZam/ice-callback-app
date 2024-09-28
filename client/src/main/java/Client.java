import Demo.PrinterPrx;
import Demo.Response;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {

	// Escáner para leer la entrada del usuario
	private static final Scanner scanner = new Scanner(System.in);

	// Listas para almacenar métricas de rendimiento
	private static final List<Long> latencies = new ArrayList<>();
	private static final List<Long> processingTimes = new ArrayList<>();
	private static final List<Long> networkPerformance = new ArrayList<>();
	private static final List<Long> jitters = new ArrayList<>();
	private static final List<Double> missingRates = new ArrayList<>();
	private static final List<String> sentMessages = new ArrayList<>();
	private static final List<Double> unprocessRates = new ArrayList<>();
	private static final List<Double> throughput = new ArrayList<>();

	// Variables para contar solicitudes exitosas y totales
	private static int successfulRequests = 0;
	private static int totalRequests = 0;

	// Proxy para interactuar con el servicio remoto (Printer)
	private static PrinterPrx service;

	public static void main(String[] args) {
		List<String> extraArgs = new ArrayList<>();
		// Inicializa el comunicador ICE y obtiene el proxy del servicio
		try (Communicator communicator = Util.initialize(args, "client.cfg", extraArgs)) {
			// Verifica y establece el proxy del servicio remoto
			service = PrinterPrx.checkedCast(communicator.propertyToProxy("Printer.Proxy"));
			if (service == null) {
				throw new Error("Invalid proxy");
			}
			// Muestra el menú principal al usuario
			displayMenu(communicator);
		} catch (UnknownHostException e) {
			// Maneja posibles excepciones de red
			throw new RuntimeException(e);
		}
	}

	// Método que muestra el menú principal y gestiona las opciones seleccionadas por el usuario
	private static void displayMenu(Communicator communicator) throws UnknownHostException {
		boolean exit = false;
		while (!exit) {
			System.out.println("\n====================================================================================");
			System.out.println("----- MAIN MENU -------");
			System.out.println("1. Send a message to the server");
			System.out.println("2. Generate performance report");
			System.out.println("3. Exit");

			// Solicita al usuario que elija una opción
			System.out.print("Choose an option: ");
			String choice = scanner.nextLine().trim();

			// Ejecuta la acción correspondiente según la opción seleccionada
			switch (choice) {
				case "1":
					sendMessageToServer(communicator); // Enviar un mensaje al servidor
					break;
				case "2":
					generateReport(); // Generar el informe de rendimiento
					break;
				case "3":
					exit = true; // Salir del programa
					break;
				default:
					System.out.println("Invalid option. Please try again.");
			}
		}
	}

	// Método que permite enviar un mensaje al servidor y recopila métricas de rendimiento
	private static void sendMessageToServer(Communicator communicator) throws UnknownHostException {
		// Obtiene el nombre de usuario y el hostname de la máquina local
		String username = System.getProperty("user.name").replace(" ", "").trim();
		String hostname = Inet4Address.getLocalHost().getHostName().trim();

		boolean exit = false;
		while (!exit) {
			String prefix = username + ":" + hostname + "=>"; // Prefijo para el mensaje
			System.out.println("====================================================================================");
			String input;
			do {
				// Solicita al usuario que ingrese un mensaje
				System.out.print(prefix);
				input = scanner.nextLine();
			} while (input.isEmpty());

			// Si el usuario escribe "exit", se sale del bucle
			exit = input.equalsIgnoreCase("exit");
			if (exit) {
				System.out.println("Thank you for using our services. See you soon!");
			} else {
				sentMessages.add(input); // Agrega el mensaje enviado a la lista

				// Registra el tiempo de inicio del envío
				long start = System.currentTimeMillis();
				try {
					// Envia el mensaje al servidor y recibe la respuesta
					Response response = service.printString(prefix + input);
					long latency = System.currentTimeMillis() - start; // Calcula la latencia
					latencies.add(latency); // Agrega la latencia a la lista

					// Almacena el tiempo de procesamiento de la respuesta
					long processingTime = response.responseTime;
					processingTimes.add(processingTime);

					// Calcula el rendimiento de la red
					long netPerformance = latency - processingTime;
					networkPerformance.add(netPerformance);

					// Almacena los valores de throughput y tasa de solicitudes no procesadas
					throughput.add(response.throughput);
					unprocessRates.add(response.unprocessRate);

					// Muestra la respuesta del servidor y las métricas correspondientes
					System.out.println("Server response: " + response.value);
					System.out.println("Processing time: " + processingTime + " ms");
					System.out.println("Latency: " + latency + " ms");
					System.out.println("Network Performance: " + netPerformance + " ms");

					// Calcula el jitter si hay más de una medición de latencia
					if (latencies.size() > 1) {
						calculateJitter();
					}
					// Incrementa los contadores de solicitudes exitosas y totales
					successfulRequests++;
					totalRequests++;

					// Calcula la tasa de solicitudes no recibidas
					calculateMissingRate();
				} catch (RuntimeException e) {
					// Maneja la excepción en caso de que no se pueda procesar la solicitud
					totalRequests++;
					System.err.println("Request could not be processed");

					// Agrega valores predeterminados para las métricas en caso de fallo
					calculateMissingRate();
					latencies.add(0L);
					processingTimes.add(0L);
					networkPerformance.add(0L);
					calculateJitter();
					throughput.add(Double.NaN);
					unprocessRates.add(Double.NaN);
				}
			}
		}
	}

	// Método que calcula el jitter (variación de latencia) entre las solicitudes
	private static void calculateJitter() {
		long jitter = 0;
		for (int i = 1; i < latencies.size(); i++) {
			// Suma las diferencias absolutas entre latencias consecutivas
			jitter += Math.abs(latencies.get(i) - latencies.get(i - 1));
		}
		jitter /= (latencies.size() - 1); // Calcula el jitter promedio
		jitters.add(jitter);  // Almacena el valor de jitter
		System.out.println("Jitter: " + jitter + " ms");
	}

	// Método que calcula la tasa de solicitudes no recibidas
	private static void calculateMissingRate() {
		// Calcula el porcentaje de solicitudes fallidas
		double missingRate = (double) (totalRequests - successfulRequests) / totalRequests * 100;
		missingRates.add(missingRate);  // Almacena el valor de la tasa de solicitudes fallidas
		System.out.println("Missing Rate: " + missingRate + " %");
	}

	// Método para generar un informe de rendimiento con las métricas recogidas
	private static void generateReport() {
		System.out.println("\n--- Performance Report ---");
		System.out.println("| Sent Message                                      | Latency (ms) | Processing Time (ms) | Net Performance (ms) | Jitter (ms) | Missing Rate (%) | Unprocess Rate (%) | Throughput |");
		System.out.println("|---------------------------------------------------|--------------|----------------------|----------------------|-------------|------------------|--------------------|------------|");

		// Itera sobre todas las métricas almacenadas y las imprime en formato de tabla
		for (int i = 0; i < latencies.size(); i++) {
			System.out.printf("| %-51s | %12d | %22d | %22d | %11d | %16.2f | %18.2f | %10.2f |\n",
					sentMessages.get(i), latencies.get(i), processingTimes.get(i), networkPerformance.get(i),
					(i == 0 ? 0 : jitters.get(i - 1)), missingRates.get(i), unprocessRates.get(i), throughput.get(i));
		}
	}
}
