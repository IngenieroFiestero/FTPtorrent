/*
* AUTOR: Andrés Tomás Campo, Samuel Garces Marin
* NIA: 669936,505428
* FICHERO: HandleFTPConnection.java
* TIEMPO: 3h
* DESCRIPCI'ON: Realiza todas las tareas 
* de comunicación a través de TCP con clientes y servidores.
*/

package src;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import src.FTPService.Response;
public class HandleFTPConnection extends Thread {
	/**
	 * TIPO: Funcion que va a realizar el Thread
	 * <ul>
	 * <li>RECIBIR: Recibir un archivo (Somos un cliente)
	 * <li>ENVIAR_ARCHIVO: Enviar un archivo (Somos el servidor)
	 * <li>RECIBIR_LISTA: Recibir la lista de archivos en el directorio (Somos
	 * el cliente)
	 * <li>ENVIAR_LISTA: Enviar la lista de archivos en el directorio (Somos el
	 * servidor)
	 */
	public static enum TIPO {
		RECIBIR, ENVIAR,ENVIAR_PARTE, ENVIAR_LISTA, RECIBIR_LISTA
	};
	public static final int PACKETSIZE = 1024;
	public static final int PART_SIZE = 1024*1024;
	private Socket socket;
	private String filename;
	private TIPO tipo;
	private int udpPort;
	private DatagramSocket udpSocket;
	private ServerSocket servidor;
	private int[] parte;
	private FileList files;
	private boolean[] completo;

	/**
	 * Constructores que utilizan un socket recibido desde un SocketServer desde
	 * la clase UDPFTPServer, un DatagramSocket que es el mismo utilizado por
	 * UDPFTPServer para poder enviar la informacion sobre el puerto al cliente
	 * asi como enviar un Transfer OK o Error informando sobre el estado del
	 * envio. Tambien es necesario un udpPort indicando el puerto de destino de
	 * los paquetes UDP y un valor TIPO definiendo el tipo de funcion que realiza
	 * el Thread.
	 * 
	 * @param socket
	 *            Socket TCP para envio o recepcion de informacion.
	 * @param udpSocket
	 *            Socket UDP para el envio de estados.
	 * @param udpPort
	 *            Puerto UDP del cliente/servidor remoto
	 * @param tipo
	 *            Tipo de conexion
	 */
	public HandleFTPConnection(DatagramSocket udpSocket, InetAddress host, int udpPort) {
		//Envio de lista
		try {
			this.servidor = new ServerSocket(0);
			FTPService.sendUDPresponse(udpSocket, Response.PORT.name() + " " + servidor.getLocalPort(), host, udpPort);
			this.socket = servidor.accept();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.tipo = TIPO.ENVIAR_LISTA;
		this.udpPort = udpPort;
		this.udpSocket = udpSocket;
	}
	public HandleFTPConnection(Socket sock,DatagramSocket udpSocket,int udpPort, FileList files){
		//Recepcion de lista
		this.tipo = TIPO.RECIBIR_LISTA;
		this.socket = sock;
		this.udpSocket = udpSocket;
		this.udpPort = udpPort;
		this.servidor = null;
		this.files = files;
	}
	public HandleFTPConnection(Socket sock,DatagramSocket udpSocket,int udpPort,String filename, boolean[] completo){
		//Recibir archivo da igual que sea parte al final se guarda como fichero1.part3-5 y luego ya se juntaran todos
		this.parte = new int[]{-1,-1};
		try {
			parte = FTPService.requestedPart(filename);
		} catch (FTPException e) {
			e.printStackTrace();
		}
		this.tipo = TIPO.RECIBIR;
		this.socket = sock;
		this.udpSocket = udpSocket;
		this.udpPort = udpPort;
		this.servidor = null;
		this.filename = filename;
		this.completo = completo;
	}

	public HandleFTPConnection(DatagramSocket udpSocket,InetAddress host, int udpPort, String filename) {
		//Envio de archivo
		parte = new int[]{-1,-1};
		try {
			parte = FTPService.requestedPart(filename);
		} catch (FTPException e) {
			e.printStackTrace();
		}
		if(parte[0] == -1|| parte[1] == -1){
			this.tipo = TIPO.ENVIAR;
		}else{
			this.tipo = TIPO.ENVIAR_PARTE;
		}
		try {
			this.servidor = new ServerSocket(0);
			FTPService.sendUDPresponse(udpSocket, Response.PORT.name() + " " + servidor.getLocalPort(), host, udpPort);
			this.socket = servidor.accept();
		} catch (IOException e) {
		}
		
		this.udpPort = udpPort;
		this.udpSocket = udpSocket;
		try {
			this.filename = FTPService.requestedFileName(filename);
		} catch (FTPException e) {
			this.filename = filename;
		}
		this.files = new FileList(FTPService.generateList());
	}

	/**
	 * Metodo run del thread. Puede funcionar de 4 modos diferentes en funcin de
	 * lo que queramos realizar:
	 * <ul>
	 * <li>RECIBIR: Recibir un archivo (Somos un cliente)
	 * <li>ENVIAR_ARCHIVO: Enviar un archivo (Somos el servidor)
	 * <li>RECIBIR_LISTA: Recibir la lista de archivos en el directorio (Somos
	 * el cliente)
	 * <li>ENVIAR_LISTA: Enviar la lista de archivos en el directorio (Somos el
	 * servidor)
	 */
	@Override
	public void run() {
		switch (tipo) {
		case ENVIAR:
			enviarArchivoEntero();
			break;
		case ENVIAR_PARTE:
			enviarArchivoPartido();
			break;
		case ENVIAR_LISTA:
			enviarLista();
			break;
		case RECIBIR:
			recibirArchivoEntero();
			break;
		case RECIBIR_LISTA:
			recibirLista();
			break;
		}
		try{
			if(servidor != null){
				servidor.close();
			}
		}catch(IOException e){}
	}
	private void recibirArchivoEntero(){
		boolean error = false;
		try {
			InputStream is = socket.getInputStream();
			FileOutputStream fos = new FileOutputStream(filename);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			byte[] buffer = new byte[PACKETSIZE];
			int count;
			// Mientras haya algo que recibir
			while ((count = is.read(buffer)) != -1) {
				bos.write(buffer, 0, count);
			}
			try {
				//Cerrar todo
				is.close();
				bos.close();
				fos.close();
				socket.close();
			} catch (IOException e) {
				//No se pudo cerrar
				error = true;
			}
		} catch (FileNotFoundException e) {
			//No se encontro el fichero
			System.out.println("File " + filename + " not found");
			error = true;
		} catch (IOException e) {
			completo[0] = true;
			error = true;
		}
		byte[] buffer = new byte[FTPService.SIZEMAX];
		DatagramPacket recepcion = new DatagramPacket(buffer, buffer.length);
		try {
			// Esperar confirmacion de envio
			udpSocket.receive(recepcion);
			String datos = new String(recepcion.getData(), 0, recepcion.getLength());
			Response resp = FTPService.responseFromString(datos);
			switch (resp) {
			case OK:
				break;
			case SERVERROR:
				error = true;
				completo[0] = true;
				break;
			default:
				break;
			}
		} catch (IOException e) {
			error = true;
		} catch (FTPException e) {
			error = true;
		}
		if(error){
			completo[0]= true;
		}
	}
	private void recibirLista(){
		String dat = "";
		try {
			InputStreamReader inputstreamreader = new InputStreamReader(socket.getInputStream());
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
			String lineread = "";
			while ((lineread = bufferedreader.readLine()) != null) {
				dat = dat + lineread + "\n";
			}
			try {
				socket.close();
				bufferedreader.close();
			} catch (IOException e) {
				System.out.println(e);
			}
		} catch (IOException e) {
			System.out.println(e);
		}
		// Esperar a la recepcion del OK
		byte[] buffer = new byte[FTPService.SIZEMAX];
		DatagramPacket recepcion = new DatagramPacket(buffer, buffer.length);
		try {
			// Esperar confirmacion de envio
			udpSocket.receive(recepcion);
			String datos = new String(recepcion.getData(), 0, recepcion.getLength());
			Response resp = FTPService.responseFromString(datos);
			switch (resp) {
			case OK:
				//Procesar la variable dat que contiene la lista y generar el FileList para trabajar mas facilmente
				Scanner scanner = new Scanner(dat);
				while(scanner.hasNextLine()){
					String cadena = scanner.nextLine();
					if(!cadena.equals("")){
						String[] fileOrLength = cadena.split(" ");
						this.files.addFile(fileOrLength[0],Long.parseLong(fileOrLength[1]));
					}
				}
				scanner.close();
				break;
			case SERVERROR:
				break;
			default:
				break;
			}
		} catch (IOException e) {
		} catch (FTPException e) {
		}
	}
	private void enviarArchivoPartido(){
		boolean error = false;
		try {
			FileInputStream is = new FileInputStream(filename);
			
			long restantes = PART_SIZE*(parte[0]);//Bytes a saltarse
			long bytesLeer = PART_SIZE;
			//Usar un while con el skip porque el propio metodo dice que no asegura si saltara Todos
			while(restantes > 0){
				//Saltarse x bytes para leer
				restantes = restantes - is.skip(restantes);
			}
			OutputStream os = socket.getOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(os);
			byte[] buffer = new byte[PACKETSIZE];
			int count;
			// Mientras haya algo que leer
			while ((count = is.read(buffer)) > 0 && bytesLeer > 0) {
				bytesLeer = bytesLeer - count;
				if(bytesLeer < 0){
					bos.write(buffer,0,(int) (count + bytesLeer));//Escribir cierta cantidad
				}else{
					bos.write(buffer, 0, count);
				}
			}
			try {
				is.close();
				bos.close();
				socket.close();
				FTPService.sendUDPresponse(udpSocket, Response.OK.name(), socket.getInetAddress(), udpPort);
			} catch (IOException e) {
				error = true;
			}
		} catch (FileNotFoundException e) {
			error = true;
			System.out.println("File " + filename + " not found");
		} catch (IOException e) {
			error = true;
			System.out.println(e);
		} finally {
			if (error){
				boolean conseguido = false;
				while(conseguido){
					try {
						FTPService.sendUDPresponse(udpSocket, Response.SERVERROR.name(), socket.getInetAddress(),
								udpPort);
						conseguido = true;
					} catch (IOException e) {}
				}
				System.out.println("Error Envio");
			}else{
				System.out.println("Archivo enviado");
			}
		}
	}
	private void enviarArchivoEntero(){
		boolean error = false;
		try {
			FileInputStream is = new FileInputStream(filename);
			OutputStream os = socket.getOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(os);
			byte[] buffer = new byte[PACKETSIZE];
			int count;
			// Mientras haya algo que leer
			while ((count = is.read(buffer)) > 0) {
				bos.write(buffer, 0, count);
			}
			try {
				is.close();
				bos.close();
				socket.close();
				FTPService.sendUDPresponse(udpSocket, Response.OK.name(), socket.getInetAddress(), udpPort);
			} catch (IOException e) {
				error = true;
			}
		} catch (FileNotFoundException e) {
			error = true;
			System.out.println("File " + filename + " not found");
		} catch (IOException e) {
			error = true;
			System.out.println(e);
		} finally {
			if (error)
				try {
					FTPService.sendUDPresponse(udpSocket, Response.SERVERROR.name(), socket.getInetAddress(),
							udpPort);
				} catch (IOException e) {
				}
		}
	}
	private void enviarLista(){
		boolean error = false;
		try {
			PrintWriter printwriter = new PrintWriter(socket.getOutputStream(), true);
			String files = FTPService.generateList();
			printwriter.println(files);
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println(e);
				error = true;
			}
			try {
				FTPService.sendUDPresponse(udpSocket, Response.OK.name(), socket.getInetAddress(), udpPort);
			} catch (IOException e) {
				error = true;
				System.out.println(e);
			}
		} catch (IOException e) {
			System.out.println(e);
			error = true;
		} finally {
			if (error)
				try {
					FTPService.sendUDPresponse(udpSocket, Response.SERVERROR.name(), socket.getInetAddress(),
							udpPort);
				} catch (IOException e) {
				}
		}
	}
}
