import Demo.CallbackReceiverPrx;
import Demo.CallbackSenderPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Client {

	// Escáner para leer la entrada del usuario
	private static final Scanner scanner = new Scanner(System.in);

	// Variables para contar solicitudes exitosas y totales
	private static int successfulRequests = 0;
	private static int totalRequests = 0;

	private static final LinkedHashMap<String, PerformanceMetricsForAMessage> messages = new LinkedHashMap<>();

	public static void main(String[] args) {
		List<String> extraArgs = new ArrayList<>();
		// Inicializa el comunicador ICE y obtiene el proxy del servicio
		try (Communicator communicator = Util.initialize(args, "client.cfg", extraArgs)) {
			// Verifica y establece el proxy del servicio remoto
			CallbackSenderPrx service = CallbackSenderPrx
					.checkedCast(communicator.propertyToProxy("CallbackSender.Proxy"))
					.ice_twoway().ice_secure(false);
			if (service == null) {
				throw new Error("Invalid proxy");
			}
			// Configuración del Cliente y lógica de interacción con el usuario
			configureAndInteract(service, communicator);
		} catch (UnknownHostException e) {
			// Maneja posibles excepciones de red
			throw new RuntimeException(e);
		}
	}

	private static void configureAndInteract(CallbackSenderPrx sender, Communicator communicator)
			throws UnknownHostException {
		// Creación del adapter para el Cliente
		ObjectAdapter adapter = communicator.createObjectAdapter("Callback.Client");

		ObjectPrx proxy = adapter.add(new CallbackReceiverI(), Util.stringToIdentity("CallbackReceiver"));
		adapter.activate();

		// Obtener el proxy del receptor para recibir mensajes del servidor
		CallbackReceiverPrx receiver = CallbackReceiverPrx
				.uncheckedCast(proxy);

		// Interacción con el usuario
		displayMenu(sender, receiver, communicator);

		communicator.waitForShutdown();
	}

	// Método que muestra el menú principal y gestiona las opciones seleccionadas
	// por el usuario
	private static void displayMenu(CallbackSenderPrx sender, CallbackReceiverPrx receiver, Communicator communicator)
			throws UnknownHostException {
		boolean exit = false;
		while (!exit) {
			System.out
					.println("\n====================================================================================");
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
					sendMessageToServerAsync(sender, receiver); // Enviar un mensaje al servidor
					break;
				case "2":
					generateReport(); // Generar el informe de rendimiento
					break;
				case "3":
					exit = true; // Salir del programa
					communicator.shutdown(); // Cierra el comunicador
					break;
				default:
					System.out.println("Invalid option. Please try again.");
			}
		}
	}

	// Método que permite enviar un mensaje al servidor y recopila métricas de
	// rendimiento
	private static void sendMessageToServerAsync(CallbackSenderPrx sender, CallbackReceiverPrx receiver)
			throws UnknownHostException {
		// Obtiene el nombre de usuario y el hostname de la máquina local
		String username = System.getProperty("user.name").replace(" ", "").trim();
		String hostname = Inet4Address.getLocalHost().getHostName().trim();

		boolean exit = false;
		while (!exit) {
			String prefix = username + ":" + hostname + "=>"; // Prefijo para el mensaje
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
				sender.removeClient(hostname);
			} else {
				String messageIdentifier = generateMessageIdentifier();
				messages.put(messageIdentifier, new PerformanceMetricsForAMessage(input));
				// Envía el mensaje al servidor y recibe la respuesta
				sender.sendMessageAsync(messageIdentifier, prefix + input, System.currentTimeMillis(), receiver)
						.thenAccept(
								response -> {
									PerformanceMetricsForAMessage performanceMetrics = messages.get(messageIdentifier);
									System.out.println(
											"\n====================================================================================");
									// Calcula la latencia
									long latency = System.currentTimeMillis() - response.requestedTime;
									performanceMetrics.setLatency(latency);

									// Almacena el tiempo de procesamiento de la respuesta
									long processingTime = response.responseTime;
									performanceMetrics.setProcessingTime(processingTime);

									// Calcula el rendimiento de la red
									long netPerformance = latency - processingTime;
									performanceMetrics.setNetworkPerformance(netPerformance);

									// Almacena los valores de throughput y tasa de solicitudes no procesadas
									performanceMetrics.setThroughput(response.throughput);
									performanceMetrics.setUnprocessedRate(response.unprocessedRate);

									// Muestra la respuesta del servidor y las métricas correspondientes
									System.out.println("Server response: \n" + response.value + "\n");
									System.out.print("latency = " + latency + "ms, ");
									// Calcula el jitter si hay más de una medición de latencia
									if (messages.size() < 2) {
										performanceMetrics.setJitter(0.0);
									} else {
										Double jitter = calculateJitter();
										performanceMetrics.setJitter(jitter);
										System.out.print("jitter = " + jitter + "ms, ");
									}
									System.out.print("processing time = " + processingTime + "ms, ");
									System.out.print("network performance = " + netPerformance + "ms, ");
									System.out.println("throughput = " + response.throughput + " requests/sec");

									// Incrementa los contadores de solicitudes exitosas y totales
									successfulRequests++;
									totalRequests++; // Incrementa el contador de solicitudes totales
									System.out.println(
											"====================================================================================");
									System.out.print(prefix);
								})
						.exceptionally(ex -> {
							totalRequests++; // Incrementa el contador de solicitudes totales
							System.out.println(
									"\n====================================================================================");
							System.err.println("Error: \n" + ex.getMessage());
							double missingRate = calculateMissingRate(); // Calcula la tasa de solicitudes no recibidas
							System.out.println("Current missing rate = " + missingRate + "%");
							System.out.println(
									"====================================================================================");
							System.out.print(prefix);
							return null;
						});
			}
		}
	}

	private static String generateMessageIdentifier() {
		return UUID.randomUUID().toString();
	}

	// Método que calcula el jitter (variación de latencia) entre las solicitudes
	private static double calculateJitter() {
		double jitter = 0;
		Long previousLatency = null;
		int count = 0;

		for (PerformanceMetricsForAMessage metrics : messages.values()) {
			Long currentLatency = metrics.getLatency();

			if (currentLatency == null) {
				continue;
			}

			if (previousLatency != null) {
				jitter += Math.abs(currentLatency - previousLatency);
				count++;
			}

			previousLatency = currentLatency;
		}

		if (count == 0) {
			return 0;
		}

		return jitter / count; // División de tipo double para precisión
	}

	// Método que calcula la tasa de solicitudes no recibidas
	private static double calculateMissingRate() {
		// Calcula el porcentaje de solicitudes fallidas
		double missingRate = (double) (totalRequests - successfulRequests) /
				totalRequests * 100;
		return missingRate;
	}

	// Método para generar un informe de rendimiento con las métricas recogidas
	private static void generateReport() {
		System.out.println("Total Requests = " + totalRequests);
		System.out.println("Missing Rate (%) = " + calculateMissingRate() + "%");
		System.out.println("\n--- Performance Report ---");
		System.out.println(
				"| Sent Message                                      | Latency (ms) | Processing Time (ms) | Net Performance (ms) | Jitter (ms) | Unprocessed Rate (%) | Throughput (requests/sec) |");
		System.out.println(
				"|---------------------------------------------------|--------------|----------------------|----------------------|-------------|------------------|--------------------|");

		// Itera sobre todas las métricas almacenadas y las imprime en formato de tabla
		for (PerformanceMetricsForAMessage metrics : messages.values()) {
			if (metrics.areMetricsComplete()) {
				System.out.printf("| %-51s | %12d | %22d | %22d | %11.2f | %18.2f | %10.2f |\n",
						metrics.getMessage(),
						metrics.getLatency(),
						metrics.getProcessingTime(),
						metrics.getNetworkPerformance(),
						metrics.getJitter(),
						metrics.getUnprocessedRate(),
						metrics.getThroughput());
			} else {
				System.out.printf("| %-51s | %12s | %22s | %22s | %11s | %18s | %10s |\n",
						metrics.getMessage(), "NA", "NA", "NA", "NA", "NA", "NA");
			}
		}
	}

}
