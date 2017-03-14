/*
* AUTOR: Andres Tomas Campo, Samuel Garces Marin
* NIA: 505428, 669936
* FICHERO: UDPFTPServer.java
* TIEMPO: 2 h
* DESCRIPCI'ON: Clase principal del servidor. Se pone a la espera de paquetes UDP con comandos desde un cliente 
* y envía archivos enteros o partidos.
*/

package src;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import src.FTPService.*;
public class UDPFTPServer {
	public static final int BUFFER_LENGTH = 1000;
	public static final String SEPARADOR = "-----------------------";
	public static final String WELCOME_TXT = "Bienvenido a mi servidor FTP";
	public static File folder;
	public static int port;

	public static void main(String args[]) {
		try {
			String currentPath = System.getProperty("user.dir");
			port = args.length > 0 ? Integer.parseInt(args[0]) : FTPService.SERVERPORT;
			DatagramSocket socketUDP = new DatagramSocket(port);
			byte[] bufer = new byte[BUFFER_LENGTH];
			System.out.println(SEPARADOR + " Servidor de Descarga " + SEPARADOR);
			System.out.println("Parametros: ");
			System.out.println("Puerto: " + port);
			System.out.println("Directorio: " + currentPath);
			System.out.println(SEPARADOR + "\nLista de ficheros: ");
			System.out.println(FTPService.generateList());
			System.out.println(SEPARADOR);
			while (true) {
				//Nos llega una peticion
				DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
				socketUDP.receive(peticion);
				String datos = new String(peticion.getData(), 0, peticion.getLength());
				System.out.println("Handling UDP Client at " + peticion.getAddress().toString() + ":"
						+ peticion.getPort() + " " + datos);
				try {
					//Buscar el comando
					Command comand = FTPService.commandFromString(datos.toString());
					switch (comand) {
					case HELLO:
						//Comando Hello respondemos con un welcome
						FTPService.sendUDPresponse(socketUDP, Response.WCOME.name() + " " + WELCOME_TXT, peticion.getAddress(),peticion.getPort());
						break;
					case LIST:
						HandleFTPConnection hFtp = new HandleFTPConnection(socketUDP,peticion.getAddress(),peticion.getPort());
						hFtp.start();
						break;
					case GET:
						HandleFTPConnection hFtp2 = new HandleFTPConnection(socketUDP,peticion.getAddress(),peticion.getPort(),FTPService.requestedFile(datos.toString()));
						hFtp2.start();
						break;
					case QUIT:
						FTPService.sendUDPresponse(socketUDP, Response.BYE.name() + " Hasta la vista", peticion.getAddress(), peticion.getPort());
						break;
					case ERROR:
						FTPService.sendUDPresponse(socketUDP, Response.SERVERROR.name(), peticion.getAddress(), peticion.getPort());
						break;
					default:
						FTPService.sendUDPresponse(socketUDP, Response.UNKNOWN.name(), peticion.getAddress(), peticion.getPort());
						break;
					}
				} catch (FTPException e) {
					System.err.println(e);
				} catch (IOException e) {
					System.out.println("Error de conexion");
				}
			}
		} catch (IOException e) {
		}
	}
	/**
	 * Metodo simple para imprimir por obtener un String con la lista de archivos.
	 * Puesto que en la clase Servidor solo se utiliza una vez al arrancar 
	 *  para mostrar por pantalla los archivos en el directorio, 
	 * no dar�? ning�? tipo de bloqueos ya que a�? no est�? a la espera de clientes
	 * y el resto de usos se har�? desde HandleFTPConnection en paralelo.
	 * @return
	 */
	public static String getFiles(){
		String ret = "";
		File[] listOfFiles = folder.listFiles();
		for (File file : listOfFiles) {
			if (file.isFile()) {
				ret = ret + file.getName()+"\n";
			}
		}
		return ret;
	}
}
