/*
* AUTOR: Andrés Tomás Campo , Samuel Garces Marin
* NIA: 505428, 669936
* FICHERO: UDPFTPDatosServidor.java
* TIEMPO: 2 h
* DESCRIPCI'ON: Clase principal de cliente. Lee por pantalla cadenas, las procesa y obtiene el
*  Comando y finalmente llama a HandleFTPClient para realizar las acciones.
*/


package src;

import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import src.FTPService.Command;

/**
 * AUTOR: Samuel Garces Marin NIA: 669936 FICHERO: UDPFTPClient.java TIEMPO: 6h
 * DESCRIPCION: Cliente que se conecta a un servidor para obtener ficheros.
 */
public class UDPFTPClient {
	public static Logger LOG = configureLogger("udpftpclient-log.log", "UDPFTPClient");// Public
																						// para
																						// que
																						// otros
																						// lo
																						// usen
	public static final String separador = "-----------------------";
	public static final int BUFFER_LENGTH = 1000;
	public static final String LIST_FILE = "list.txt";
	private static FileHandler fh;

	public static void main(String args[]) {
		LOG.info("Logger funcionando");
		ListaServidores list = null;
		System.out.println(separador + " Cliente " + separador);
		// Obtener la lista de servidores
		try {
			list = new ListaServidores(LIST_FILE);
		} catch (FTPException e1) {
		}
		FileList fileList = new FileList();
		System.out.println("Servidores en la lista:");
		System.out.println(list.toString());
		LOG.info("Lista de servidores:\n" + list.toString());
		HandleFTPClient hClient = new HandleFTPClient(list, fileList);
		hClient.sayHello();
		System.out.println("Servidores activos:");
		System.out.println(list.toString());
		hClient.list();
		System.out.println("Lista de ficheros: ");
		System.out.println(fileList.toString());
		LOG.info("Lista de ficheros:\n" + fileList.toString());
		boolean funcionar = true;
		while (funcionar) {
			boolean salir = false;
			@SuppressWarnings("resource")
			Scanner scanner = new Scanner(System.in);
			String comando = "";
			Command com = null;
			while (salir == false) {
				try {
					comando = scanner.nextLine();
					LOG.info(comando);
					try {
						com = FTPService.commandFromString(comando);
						salir = true;
					} catch (FTPException ftpe) {
						salir = false;
					}
				} catch (Exception e) {
					System.out.println("Valor no valido.");
				}
			}
			switch (com) {
			case HELLO:
				System.out.println("Lista de servidores:");
				System.out.println(list.toString());
				break;
			case GET:
				try {
					String[] split = FTPService.requestedFile(comando).split(" ");
					hClient.getFile(split[0]);
				} catch (FTPException e) {
					System.out.println("Error: " + e);
				}
				break;
			case LIST:
				hClient.list();
				System.out.println("Lista de ficheros: ");
				System.out.println(fileList.toString());
				break;
			case QUIT:
				System.out.println("Bye...");
				funcionar = false;
				break;
			default:
				System.out.println("No encontrado");
				break;
			}
		}

	}

	public static Logger getLogger() {
		return LOG;
	}

	public static Logger configureLogger(String filename, String name) {
		Logger log = null;
		try {
			log = Logger.getLogger(name);
			// Sistema de login para debugear y ver commo trabaja el sistema de
			// balanceo de carga
			fh = new FileHandler(filename);
			log.addHandler(fh);
			log.setUseParentHandlers(false);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (Exception e) {

		}
		return log;
	}
}
