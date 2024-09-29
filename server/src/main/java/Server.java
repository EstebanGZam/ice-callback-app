import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public class Server {

	private static int totalRequests = 0;
	private static int resolvedRequests = 0;
	private static long processTime = 0;

	public static void main(String[] args) {
		// Lista para almacenar argumentos extra que se puedan pasar durante la ejecución del servidor
		java.util.List<String> extraArgs = new java.util.ArrayList<>();

		// Bloque try-with-resources para inicializar el comunicador Ice y garantizar su cierre
		try (Communicator communicator = Util.initialize(args, "server.cfg", extraArgs)) {

			// Si hay argumentos adicionales no reconocidos, imprime un error
			if (!extraArgs.isEmpty()) {
				System.err.println("too many arguments");
				// Muestra los argumentos no reconocidos
				for (String v : extraArgs) {
					System.out.println(v);
				}
			}

			// Crea un adaptador de objetos Ice, usando el nombre "Callback.Sender" definido en la configuración
			ObjectAdapter adapter = communicator.createObjectAdapter("Callback.Sender");

			// Añade un objeto de tipo CallbackSenderI al adaptador, asociándolo con la identidad "callbackSender"
			adapter.add(new CallbackSenderI(), Util.stringToIdentity("callbackSender"));

			// Activa el adaptador para empezar a aceptar solicitudes
			adapter.activate();

			// El servidor queda a la espera de que se le envíen solicitudes
			communicator.waitForShutdown();
		}
	}

	public static int getTotalRequests() {
		return totalRequests;
	}

	public static void setTotalRequests(int totalRequests) {
		Server.totalRequests = totalRequests;
	}

	public static int getResolvedRequests() {
		return resolvedRequests;
	}

	public static void setResolvedRequests(int resolvedRequests) {
		Server.resolvedRequests = resolvedRequests;
	}

	public static long getProcessTime() {
		return processTime;
	}

	public static void setProcessTime(long processTime) {
		Server.processTime = processTime;
	}

}
