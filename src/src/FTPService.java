/*
* AUTOR: Andrés Tomás, Samuel Garces Marin
* NIA: 669936,505428
* FICHERO: FTPService.java
* TIEMPO: 1 hora
* DESCRIPCION: Clase para la obtencion de Comandos, Respuestas y otra informacion
*  de Strings. Se añaden pequeñas mejoras 
*  con respecto a la practica 4 como por ejemplo identificar ella parte del archivo.
*/
package src;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class FTPService {
	public static final int TIMEOUT = 500;
	public static final int SIZEMAX = 255; // Maximum size of datagram
	public static final int SERVERPORT = 5000; // default server port
	public static final String COMMAND_SEPARATOR = " ";
	public static final String FILE_PART_NAME = ".part";
	//private String[] names;

	public static  enum Command {
		HELLO, LIST, GET, QUIT, ERROR
	};

	public static enum Response {
		WCOME, OK, PORT, SERVERROR, BYE, UNKNOWN
	};

	FTPService() {
	}
	/**
	 * Obtiene el comando desde un String
	 * @param textcommand Cadena que contiene el comando
	 * @return El comando
	 * @throws FTPException
	 */
	public static Command commandFromString(String textcommand) throws FTPException {
		try{
			String[] cadenas = textcommand.split(" ");
			return Command.valueOf(cadenas[0]);
		}catch(Exception e){
			throw new FTPException("No such Command");
		}
	}
	/**
	 * Obtiene la respuesta desde una cadena
	 * @param textresponse La cadena que contiene la respuesta. Ej: WCOME Hola
	 * @return
	 * @throws FTPException
	 */
	public static Response responseFromString(String textresponse) throws FTPException {
		try{
			String[] cadenas = textresponse.split(" ");
			return Response.valueOf(cadenas[0]);
		}catch(Exception e){
			throw new FTPException("No such Response");
		}
	}
	/**
	 * Nombre del archivo solicitado
	 * @param textcommand Cadena de texto
	 * @return Nombre del fichero
	 * @throws FTPException
	 */
	public static String requestedFile(String textcommand) throws FTPException {
		return findCommand(Command.GET.name(),textcommand);
	}
	public static String requestedFileName(String fileText) throws FTPException {
		String[] fileNames = fileText.split(".part");
		return fileNames[0];
		
	}
	/**
	 * Parte solicitada del fichero
	 * @param txt Cadena que contiene el fichero. Ej: fichero1.part1-4
	 * @return int[2] que contiene como primer elemento la parte pedida y como segunda el total de partes
	 * @throws FTPException
	 */
	public static int[] requestedPart(String txt) throws FTPException{
		String filename = txt;
		int[] part = new int[]{-1,-1};
		for (int i = 0; i < filename.length() && part[0] < 0; i++) {
			boolean valid = true;
			for (int j = 0; j < FILE_PART_NAME.length() && valid; j++) {
				if(i+j < filename.length()){
					//No nos salimos del vector
					if(filename.charAt(i+j) != FILE_PART_NAME.charAt(j)){
						//Si en algun momento no coinciden salimos del bucle for
						valid = false;
					}
				}
			}
			if(valid == true){
				//Generamos una subcadena que se encuentre justo despues de la palabra "-part"
				String subcadena = filename.substring(i + FILE_PART_NAME.length());
				//Buscamos el numero
				boolean validpart = true;
				String parteInicial = "";
				String parteFinal = "";
				boolean inicial = true;
				for (int j = 0; j < subcadena.length() && validpart; j++) {
					if(subcadena.charAt(j) >= '0' && subcadena.charAt(j) <= '9'){
						//Es un numero
						if(inicial){
							parteInicial = parteInicial + subcadena.charAt(j);
						}else{
							parteFinal = parteFinal + subcadena.charAt(j);
						}
					}else if(subcadena.charAt(j) == '-'){
						//Separador de parte inicial de final
						inicial = false;
					}else{
						validpart = false;
						//Ya no hay mas numeros y salimos del bucle
					}
				}
				if(parteInicial.equals("") && parteFinal.equals("")){
					//Esta la palabra reservada "-part" pero no hay numero seguido
					throw new FTPException("Not valid File Part");
				}else if(parteFinal.equals("")){
					part =  new int[]{Integer.parseInt(parteInicial),-1};
				}else{
					part =  new int[]{Integer.parseInt(parteInicial),Integer.parseInt(parteFinal)};
				}
			}
		}
		return part;
		
	}
	/**
	 * Devuelve el puerto al cual conectarse desde un String
	 * @param textresponse Cadena que contiene el puerto
	 * @return puerto
	 * @throws NumberFormatException
	 * @throws FTPException
	 */
	public static int portFromResponse(String textresponse) throws NumberFormatException, FTPException {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(findResponse(Response.PORT.name(),textresponse));
		return scan.nextInt();
	}
	/**
	 * Enviar un paquete UDP con una respuesta
	 * @param socket UDPSocket
	 * @param response Texto a enviar
	 * @param hostServidor Direccion remota a la que mandar el paquete
	 * @param puertoServidor Puerto remoto al que conectarse
	 * @throws IOException
	 */
	public static void sendUDPresponse(DatagramSocket socket, String response,InetAddress hostServidor,int puertoServidor) throws IOException {
		socket.send(new DatagramPacket(response.getBytes(),response.getBytes().length,hostServidor,puertoServidor));
	}
	/**
	 * Enviar un paquete UDP con un comando
	 * @param socket UDPSocket
	 * @param command Texto a enviar
	 * @param hostServidor Direccion remota a la que mandar el paquete
	 * @param puertoServidor Puerto remoto al que conectarse
	 * @throws IOException
	 */
	public static void sendUDPcommand(DatagramSocket socket,String command,InetAddress hostServidor,int puertoServidor) throws IOException {
		socket.send(new DatagramPacket(command.getBytes(), 0, command.getBytes().length, hostServidor, puertoServidor));
	}
	/**
	 * Encuentra un comando en la cadena de forma que pueda leer aunque la primera palabra no sea un comando pero si alguna de las siguientes
	 * @param com Comando a buscar
	 * @param txt Texto que contiene el comando
	 * @return Cadena nueva
	 * @throws FTPException En caso de no encontrar el comando
	 */
	private static String findCommand(String com,String txt) throws FTPException{
		String[] cadenas;
		String datos = "";
		int pos = -1;
		if (txt.contains(COMMAND_SEPARATOR) && txt.contains(com)) {
			cadenas = txt.split(COMMAND_SEPARATOR);
			for (int i = 0; i < cadenas.length; i++) {
				if (cadenas[i].equals(com) && i <= cadenas.length - 1) {
					pos = i;
				}
				if(pos >= 0 && i > pos){
					datos = datos +cadenas[i] + " ";
				}
			}
		}
		if(pos <0){
			throw new FTPException("Comando " +com+" no valido en " + txt);
		}else{
			return datos;
		}
	}
	/**
	 * Encuentra una respuesta en la cadena de forma que pueda leer aunque la primera palabra no sea una respuesta pero si alguna de las siguientes.
	 * Y crea una nueva subcadena que devuelve.
	 * @param com Comando a buscar
	 * @param txt Texto que contiene el comando
	 * @return Cadena nueva
	 * @throws FTPException En caso de no encontrar el comando
	 */
	private static String findResponse(String com,String txt) throws FTPException{
		String[] cadenas;
		String datos = "";
		int pos = -1;
		if (txt.contains(COMMAND_SEPARATOR) && txt.contains(com)) {
			cadenas = txt.split(COMMAND_SEPARATOR);
			for (int i = 0; i < cadenas.length; i++) {
				if (cadenas[i].equals(com) && i <= cadenas.length - 1) {
					pos = i;
				}
				if(pos >= 0 && i > pos){
					datos = datos +cadenas[i] + " ";
				}
			}
		}
		if(pos <0){
			throw new FTPException("Respuesta " +com+" no valida");
		}else{
			return datos;
		}
	}
	/**
	 * Devuelve la Lista de ficheros en el directorio
	 * @return Lista de ficheros en el directorio
	 */
	public static File[] getFileList(){
		String currentPath = System.getProperty("user.dir");
		File folder = new File(currentPath);
		return folder.listFiles();
	}
	/**
	 * Genera una cadena que contiene la Lista de Ficheros
	 * @return Lista de ficheros
	 */
	public static String generateList(){
		File[] files = getFileList();
		String ret = "";
		for (int i = 0; i < files.length -1; i++) {
			ret = ret +files[i].getName() + " " + files[i].length() + "\n";
		}
		ret = ret +files[files.length-1].getName() + " " + files[files.length-1].length();
		return ret;
	}
	/**
	 * Obtiene las longitudes de un fichero
	 * @param name Nombre del fichero
	 * @return Longitud del fichero en bytes
	 */
	public static long getFileParts(String name){
		File[] files = getFileList();
		for (int i = 0; i < files.length; i++) {
			if(files[i].getName().equals(name)){
				return files[i].length();
			}
		}
		return -1;
	}

}
