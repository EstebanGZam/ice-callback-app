import Demo.CallbackReceiverPrx;
import Demo.CallbackSenderPrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
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
	private static final List<Double> unprocessedRates = new ArrayList<>();
	private static final List<Double> throughput = new ArrayList<>();

	// Variables para contar solicitudes exitosas y totales
	private static int successfulRequests = 0;
	private static int totalRequests = 0;

	public static void main(String[] args) {
		List<String> extraArgs = new ArrayList<>();
		// Inicializa el comunicador ICE y obtiene el proxy del servicio
		try (Communicator communicator = Util.initialize(args, "client.cfg", extraArgs)) {
			// Verifica y establece el proxy del servicio remoto
			CallbackSenderPrx service = CallbackSenderPrx
					.checkedCast(communicator.propertyToProxy("CallbackSender.Proxy"))
					.ice_twoway().ice_timeout(-1).ice_secure(false);
			System.out.println(service);
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
		System.out.println("Adapter: " + Arrays.toString(adapter.getEndpoints()));

		ObjectPrx proxy = adapter.add(new CallbackReceiverI(), Util.stringToIdentity("CallbackReceiver"));
		adapter.activate();

		// Obtener el proxy del receptor para recibir mensajes del servidor
		CallbackReceiverPrx receiver = CallbackReceiverPrx
				.uncheckedCast(proxy);
		System.out.println(receiver);

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
				sentMessages.add(input); // Agrega el mensaje enviado a la lista

				// Envía el mensaje al servidor y recibe la respuesta
				sender.sendMessageAsync(prefix + input, receiver).thenAccept(
						response -> {
							System.out.println(
									"\n====================================================================================");
							// Registra el tiempo de inicio del envío
							long start = System.currentTimeMillis();
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
							unprocessedRates.add(response.unprocessedRate);

							// Muestra la respuesta del servidor y las métricas correspondientes
							System.out.println("Server response: \n" + response.value + "\n");
							System.out.print("latency = " + latency + "ms, ");
							// Calcula el jitter si hay más de una medición de latencia
							if (latencies.size() > 1) {
								long jitter = calculateJitter();
								System.out.print("jitter = " + jitter + "ms, ");
							}
							System.out.print("processing time = " + processingTime + "ms, ");
							System.out.println("network performance = " + netPerformance + "ms");

							// Incrementa los contadores de solicitudes exitosas y totales
							successfulRequests++;
							totalRequests++;
							System.out.println(
									"====================================================================================");
							System.out.print(prefix);
						})
						.exceptionally(ex -> {
							System.err.println("Error sending message: \n" + ex.getMessage());
							return null;
						});
			}
		}
	}

	// Método que calcula el jitter (variación de latencia) entre las solicitudes
	private static long calculateJitter() {
		long jitter = 0;
		for (int i = 1; i < latencies.size(); i++) {
			// Suma las diferencias absolutas entre latencias consecutivas
			jitter += Math.abs(latencies.get(i) - latencies.get(i - 1));
		}
		jitter /= (latencies.size() - 1); // Calcula el jitter promedio
		jitters.add(jitter); // Almacena el valor de jitter
		return jitter;
	}

	// Método que calcula la tasa de solicitudes no recibidas
	// private static double calculateMissingRate() {
	// // // Calcula el porcentaje de solicitudes fallidas
	// // double missingRate = (double) (totalRequests - successfulRequests) /
	// // totalRequests * 100;
	// // missingRates.add(missingRate); // Almacena el valor de la tasa de
	// solicitudes
	// // fallidas
	// // return missingRate;
	// //// System.out.println("Missing Rate: " + missingRate + " %");
	// // }
	// return 0.0;
	// }

	// Método para generar un informe de rendimiento con las métricas recogidas
	private static void generateReport() {
		System.out.println("\n--- Performance Report ---");
		System.out.println(
				"| Sent Message                                      | Latency (ms) | Processing Time (ms) | Net Performance (ms) | Jitter (ms) | Missing Rate (%) | Unprocessed Rate (%) | Throughput |");
		System.out.println(
				"|---------------------------------------------------|--------------|----------------------|----------------------|-------------|------------------|--------------------|------------|");

		// Itera sobre todas las métricas almacenadas y las imprime en formato de tabla
		for (int i = 0; i < latencies.size(); i++) {
			System.out.printf("| %-51s | %12d | %22d | %22d | %11d | %16.2f | %18.2f | %10.2f |\n",
					sentMessages.get(i), latencies.get(i), processingTimes.get(i), networkPerformance.get(i),
					(i == 0 ? 0 : jitters.get(i - 1)), missingRates.get(i), unprocessedRates.get(i), throughput.get(i));
		}
	}

}
