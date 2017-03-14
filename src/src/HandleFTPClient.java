/*
* AUTOR: Andrés Tomás Campo , Samuel Garces Marin
* NIA: 669936,505428
* FICHERO: HandleFTPClient.java
* TIEMPO: 10h
* DESCRIPCION: Funciones básicas usadas por el cliente como 
* decir HELLO, pedir la lista o pedir un archivo.
*/
package src;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;
import java.util.logging.Level;
import src.FTPService.Command;
import src.FTPService.Response;

public class HandleFTPClient {
	private ListaServidores lista;
	private FileList files;
	public static ProgressBar pb;

	public HandleFTPClient(ListaServidores lista, FileList files) {
		this.lista = lista;
		this.files = files;
	}

	public void getFile(String file) {
		UDPFTPDatosServidor[] servers = lista.getServersFile(file);
		UDPFTPFile myFile = servers[0].getFile(file);
		long[] bwTeorica = new long[servers.length];
		long bwClient = 100000;
		long bwTotal = 0;
		long[] tiempos = new long[servers.length];
		// Calculo del numero de partes del archivo
		long numPartsFile = myFile.getFileLength() / HandleFTPConnection.PART_SIZE;
		long resto = myFile.getFileLength() % HandleFTPConnection.PART_SIZE;
		if (resto != 0) {
			numPartsFile = numPartsFile + 1;
		}
		pb = new ProgressBar(numPartsFile, 1);
		BalanceadordeCarga bc = null;

		for (int i = 0; i < servers.length; i++) {
			long bw = servers[i].getBW();
			tiempos[i] = HandleFTPConnection.PART_SIZE * 8 * 10000000 / bw;
			bwTeorica[i] = bw;
			bwTotal = bwTotal + bw;
		}
		// 3 CASOS ENUNCIADO
		// En caso de que la velocidad de todos sea igual a la del cliente
		if (((bwClient == bwTeorica[0]) || (bwClient == bwTeorica[1]) || (bwClient == bwTeorica[3])
				|| (bwClient == bwTeorica[3]))) {
			System.out.println("Descargando todo desde uno: ");
			MedidorDeTiempo reloj = new MedidorDeTiempo();
			DatagramSocket udpSocket = null;
			try {
				udpSocket = new DatagramSocket();
			} catch (SocketException e) {
			}
			int downloadPort = 0;
			byte[] bufer = new byte[1024];
			DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
			String filename = myFile.getFileName();
			boolean[] completo = new boolean[1];
			completo[0] = false;
			try {
				reloj.tic();
				FTPService.sendUDPcommand(udpSocket, Command.GET.name() + " " + filename, servers[0].getAddress(),
						servers[0].getPort());
				udpSocket.receive(peticion);
				String datos = new String(peticion.getData(), 0, peticion.getLength());
				Response response = FTPService.responseFromString(datos.toString());
				switch (response) {
				case PORT:
					downloadPort = FTPService.portFromResponse(datos.toString());
					break;
				default:
					break;
				}
				HandleFTPConnection hfc = new HandleFTPConnection(new Socket(servers[0].getAddress(), downloadPort),
						udpSocket, servers[0].getPort(), filename, completo);
				hfc.start();
				try {
					hfc.join();
				} catch (InterruptedException e) {
					completo[0] = true;
				}
				reloj.toc();
				// Si no ha habido error pedimos nueva parte y renderizamos el
				// progreso
				if (!completo[0]) {
					System.out.println();
					System.out.println("Transfer OK");
					System.out.println("Tiempo requerido: " + reloj.getLast());
				} else {
					System.out.println();
					System.out.println("ERROR");
				}
			} catch (FTPException ftpe) {
			} catch (IOException e) {
			}

		} else {
			MedidorDeTiempo mt = new MedidorDeTiempo();
			long sumaRelaciones = BalanceadordeCarga.encontrarKMinimo(tiempos.clone(), (int) numPartsFile);
			UDPFTPClient.LOG.log(Level.INFO, "kMinima: " + sumaRelaciones);
			// Crear el vector con todas las partes
			Vector<Integer> partes = new Vector<Integer>();
			for (int i = 0; i < numPartsFile; i++) {
				partes.add(new Integer(i));
			}
			bc = new BalanceadordeCarga(partes, servers.length, sumaRelaciones, tiempos);
			// Estas velocidades teoricas no las usamos realmente porque
			// trabajamos con las reales
			System.out.println("Descargando: ");
			GestorServidores[] gs = new GestorServidores[servers.length];
			mt.tic();
			for (int i = 0; i < gs.length; i++) {
				gs[i] = new GestorServidores(servers[i], bc, i, myFile);
				gs[i].start();
			}
			for (int i = 0; i < gs.length; i++) {
				try {
					gs[i].join();
				} catch (InterruptedException e) {
				}
			}
			mt.toc();

			// Cuando acabe el Gestor de Conexion con el servidor de trabajar
			// significa que ha recibido todos los ficheros
			// Ahora hay que Juntarlos
			System.out.println();
			System.out.println("Juntando ficheros...");
			boolean error = juntarPartes(myFile);
			if (error) {
				System.out.println();
				System.out.println("ERROR");
			} else {
				System.out.println();
				System.out.println("Transfer OK");
				System.out.println("Tiempo Transcurrido: " + ((float) mt.getLast() / 1000000000) + " segundos");
			}
		}
	}

	/**
	 * Envia un HELLO a todos los servidores y cuando le responden los almacena
	 * en la nueva lista y sino los
	 */

	/**
	 * Metodo que mide la velocidad real de los servidores
	 *
	 */

	public void sayHello() {
		UDPFTPDatosServidor[] servers = lista.getServidores();
		Vector<UDPFTPDatosServidor> availableServers = new Vector<UDPFTPDatosServidor>();
		DatagramSocket udpSocket = null;
		try {
			udpSocket = new DatagramSocket(0);
		} catch (SocketException e2) {
		}
		byte[] bufer = new byte[1024];
		DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
		try {
			udpSocket.setSoTimeout(100);
		} catch (SocketException e1) {
		}
		MedidorDeTiempo mt = new MedidorDeTiempo();
		for (int i = 0; i < servers.length; i++) {
			boolean respuesta = false;
			for (int j = 0; j < 3 && !respuesta; j++) {
				try {
					mt.tic();
					FTPService.sendUDPcommand(udpSocket, Command.HELLO.name(), servers[i].getAddress(),
							servers[i].getPort());
					udpSocket.receive(peticion);
					mt.toc();
					String datos = new String(peticion.getData(), 0, peticion.getLength());
					Response response = FTPService.responseFromString(datos.toString());
					switch (response) {
					case WCOME:
						availableServers.addElement(servers[i]);
						respuesta = true;
						servers[i].setRetardo(mt.getLast());
						break;
					default:
						break;
					}
				} catch (IOException | FTPException e) {
				}
			}
			if (!respuesta) {
				// Si no ha contestado uno lo eliminamos
				try {
					lista.removeServidor(servers[i]);
				} catch (FTPException e) {
				}
			}
		}
		// Reconstruir la lista con los servidores que han contestado al hello
		lista.addServidores(availableServers.toArray(new UDPFTPDatosServidor[availableServers.size()]));
		System.out.println("Servidores que han respondido: " + lista.getServidores().length);
	}

	/**
	 * Envia un LIST a cada uno de los servidores disponibles
	 */
	public void list() {
		UDPFTPDatosServidor[] servers = lista.getServidores();
		DatagramSocket udpSocket = null;
		try {
			udpSocket = new DatagramSocket(0);
			udpSocket.setSoTimeout(100);
		} catch (SocketException e) {
		}
		byte[] bufer = new byte[1024];
		DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
		int[] puertos = new int[servers.length];
		Vector<UDPFTPDatosServidor> serversAux = new Vector<UDPFTPDatosServidor>();
		for (int i = 0; i < servers.length; i++) {
			boolean respuesta = false;
			for (int j = 0; j < 3 && !respuesta; j++) {
				// Enviar un paquete list a cada servidor pero con varios
				// intentos por si se retrasa el envio del puerto
				try {
					FTPService.sendUDPcommand(udpSocket, Command.LIST.name(), servers[i].getAddress(),
							servers[i].getPort());
					udpSocket.receive(peticion);
					String datos = new String(peticion.getData(), 0, peticion.getLength());
					Response response = FTPService.responseFromString(datos.toString());
					switch (response) {
					case PORT:
						// Si nos responde no hacemos mas intentos y obtenemos
						// el puerto al que conectarnos
						respuesta = true;
						puertos[i] = FTPService.portFromResponse(datos.toString());
						serversAux.addElement(servers[i]);
						break;
					default:
						break;
					}
				} catch (IOException | FTPException e) {
				}
			}
		}
		servers = serversAux.toArray(new UDPFTPDatosServidor[serversAux.size()]);
		// Pedir todas las listas de ficheros
		FileList[] files = new FileList[servers.length];
		HandleFTPConnection[] hc = new HandleFTPConnection[servers.length];
		for (int i = 0; i < servers.length; i++) {
			try {
				files[i] = new FileList();
				hc[i] = new HandleFTPConnection(new Socket(servers[i].getAddress(), puertos[i]), udpSocket,
						servers[i].getPort(), files[i]);
				hc[i].start();
			} catch (IOException e) {
			}
		}
		// Generar una LinkedHashSet para que no se repitan valores al añadir
		// ficheros
		// Esperar a que todo acabe
		for (int i = 0; i < servers.length; i++) {
			try {
				hc[i].join();
				// Asignamos la lista de ficheros del servidor al servidor
				servers[i].addFiles(files[i]);
				// Obtenemos un array de ficheros que recorreremos para añadir
				// uno por uno a la LinkedHashSet
				UDPFTPFile[] filesFromThread = files[i].getFiles();
				for (int j = 0; j < filesFromThread.length; j++) {
					if (!filesFromThread.equals(null)) {
						this.files.addFile(filesFromThread[j]);
					}
				}
			} catch (InterruptedException e) {
			}
		}
	}

	private boolean juntarPartes(UDPFTPFile myFile) {
		long numPartsFile = myFile.getFileLength() / HandleFTPConnection.PART_SIZE;
		long resto = myFile.getFileLength() % HandleFTPConnection.PART_SIZE;
		if (resto != 0) {
			numPartsFile = numPartsFile + 1;
		}
		pb = new ProgressBar(numPartsFile, 1);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(myFile.getFileName());
		} catch (FileNotFoundException e1) {
		}
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		FileInputStream is = null;
		boolean error = false;
		byte[] buffer = new byte[HandleFTPConnection.PACKETSIZE];
		for (int i = 0; i < numPartsFile; i++) {
			String filename = myFile.getFileName() + ".part" + i + "-" + (numPartsFile - 1);
			try {
				is = new FileInputStream(filename);
				int count;
				// Mientras haya algo que recibir
				while ((count = is.read(buffer)) != -1) {
					bos.write(buffer, 0, count);
				}
				try {
					// Cerrar todo
					is.close();
				} catch (IOException e) {
					// No se pudo cerrar
					error = true;
				}
			} catch (FileNotFoundException fne) {
				error = true;
			} catch (IOException e) {
				error = true;
			}
			System.out.print(pb.render(1));
		}
		try {
			// Cerrar todo
			is.close();
			bos.close();
			fos.close();
		} catch (IOException e) {
			// No se pudo cerrar
			error = true;
		}
		return error;
	}

}
