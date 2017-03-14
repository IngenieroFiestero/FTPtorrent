/*
* AUTOR: Andres Tomas Campo, Samuel Garces Marin
* NIA: 505428, 669936
* FICHERO: BalanceadordeCarga.java
* TIEMPO: 10 h
* DESCRIPCI'ON: Se encarga de las descargas en serie,
*  así como del control de errores. Si hay algún error durante la 
*  descarga vuelve a pedirla hasta 3 veces y si no consigue descargarla
*   la devuelve al balanceador de carga para que la distribuya nuevamente.
*/
package src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import src.FTPService.Command;
import src.FTPService.Response;

public class GestorServidores extends Thread {
	/**
	 * Numero maximo de peticiones que hara el gestor de servidores al servidor
	 * remoto antes de finalizar
	 */
	public static final int MAX_INTENTOS_FALLIDOS = 1;
	UDPFTPDatosServidor _servidor;
	BalanceadordeCarga _bc;
	int _id;
	int paquetSize = 1024 * 1024;
	UDPFTPFile _file;
	Logger log;
	public static Logger debugger = UDPFTPClient.configureLogger("DebugTime.log", "DebugTime");

	public GestorServidores(UDPFTPDatosServidor servidor, BalanceadordeCarga bc, int id, UDPFTPFile file) {
		_servidor = servidor;
		_bc = bc;
		_id = id;
		_file = file;
		log = UDPFTPClient.getLogger();
	}

	public void run() {
		int parte = new Long(_bc.getNextPart(_id, HandleFTPConnection.PART_SIZE * 8 / _servidor.getBW() * 1000000000))
				.intValue();
		int intentosFallidos = 0;
		if (parte >= 0) {
			MedidorDeTiempo reloj = new MedidorDeTiempo();
			DatagramSocket udpSocket = null;
			try {
				udpSocket = new DatagramSocket();
			} catch (SocketException e) {
			}
			int downloadPort = 0;
			byte[] bufer = new byte[1024];
			DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
			long numPartes = (_file.getFileLength() / paquetSize);
			long resto = (_file.getFileLength() % paquetSize);
			if (resto != 0) {
				numPartes = numPartes + 1;
			}
			String filename = _file.getFileName() + ".part" + parte + "-" + (numPartes - 1);
			while (parte >= 0) {
				boolean[] completo = new boolean[1];
				completo[0] = false;
				try {
					log.log(Level.INFO, "Descargando parte " + parte + " desde el servidor " + _servidor.getName()
							+ " intento numero: " + intentosFallidos);
					reloj.tic();
					FTPService.sendUDPcommand(udpSocket, Command.GET.name() + " " + filename, _servidor.getAddress(),
							_servidor.getPort());
					udpSocket.receive(peticion);
					String datos = new String(peticion.getData(), 0, peticion.getLength());
					Response response = FTPService.responseFromString(datos.toString());
					switch (response) {
					case PORT:
						downloadPort = FTPService.portFromResponse(datos.toString());
						break;
					default:
						System.out.println("Ha habido algún error G. servidores");
						break;
					}
					HandleFTPConnection hfc = new HandleFTPConnection(new Socket(_servidor.getAddress(), downloadPort),
							udpSocket, _servidor.getPort(), filename, completo);
					hfc.start();
					try {
						hfc.join();
					} catch (InterruptedException e) {
						completo[0] = true;
					}
					reloj.toc();
					if (!completo[0]) {
						debugger.log(Level.INFO, _id + " " + parte + " " + reloj.getLast());// Guardar
																							// en
																							// un
																							// fichero
																							// los
																							// tiempos
																							// de
																							// descarga
																							// de
																							// cada
																							// parte
						parte = new Long(_bc.getNextPart(_id, reloj.getLast())).intValue();
						intentosFallidos = 0;
						System.out.print(HandleFTPClient.pb.render(1));
					} else {
						System.out.print(HandleFTPClient.pb.render(0));
						intentosFallidos++;
						if (intentosFallidos <= MAX_INTENTOS_FALLIDOS) {
							// Llamar al balanceador y devolverle la parte para
							// que otro la descargue
							_bc.devolverParte(_id, reloj.getLast());
							log.log(Level.WARNING,
									"Error descargando parte " + parte + " desde el servidor " + _servidor.getName());
						}
					}
					filename = _file.getFileName() + ".part" + parte + "-" + (numPartes - 1);
				} catch (FTPException ftpe) {
				} catch (IOException e) {
				}
			}
		}
	}
}
