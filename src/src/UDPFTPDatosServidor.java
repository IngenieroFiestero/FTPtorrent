/*
* AUTOR: Andrés Tomás Campo , Samuel Garces Marin
* NIA: 505428, 669936
* FICHERO: UDPFTPDatosServidor.java
* TIEMPO: 1 h
* DESCRIPCI'ON: Contiene la información en memoria de un servidor. 
* También puede contener la lista de ficheros que este servidor posee.
*/
package src;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Clase que permite almacenar servidores UDPFTP asi como los archivos que poseen
 * @author Samuel Garces Marin
 *
 */
public class UDPFTPDatosServidor {
	private String name;
	private int port;
	private InetAddress ip;
	private long bw; //Medido en bytes/s
	private FileList fileList = new FileList();
	private long retardo;
	
	/**
	 * Constructor de la clase principal que utiliza una cadena para generar los Datos del Servidor. Ejemplo de cadena:
	 * Servidor1 127.0.0.1 5000 8000000
	 * @param txt Cadena desde la que leer
	 * @throws UnknownHostException
	 */
	public UDPFTPDatosServidor(String txt) throws UnknownHostException{
		String[] datos = txt.split(" ");
		this.name = datos[0];
		this.ip = InetAddress.getByName(datos[1]);
		this.port = Integer.parseInt(datos[2]);
		this.bw = Long.parseLong(datos[3]);
		this.retardo = 0;
	}
	/**
	 * Nombre del Servidor
	 * @return String: Nombre
	 */
	public String getName(){
		return this.name;
	}
	/**
	 * Devuelve el puerto en el que escucha el servidor remoto
	 * @return int: Puerto de escucha
	 */
	public int getPort(){
		return this.port;
	}
	/**
	 * Devuelve la direccion remota en la que escucha el servidor
	 * @return InetAddress: Direccion remota
	 */
	public InetAddress getAddress(){
		return ip;
	}
	/**
	 * Devuelve el ancho de banda del que dispone el servidor remoto.
	 * @return long: Ancho de banda
	 */
	public long getBW(){
		return this.bw;
	}
	/**
	 * Convierte los datos del servidor en una cadena para poder por ejemplo almacenarlo en un archivo de texto.
	 */
	public String toString(){
		return name + " "+ ip + " " + port + " " + bw;
	}
	/**
	 * Permite añadir archivos a la lista de la que dispone dicho servidor
	 * @param fileList Lista de nombres de archivos
	 * @param lengths Longitudes de los archivos
	 */
	public void addFiles(String[] fileList,long[] lengths){
		this.fileList.addFileListFromServer(fileList, lengths);
	}
	/**
	 * Permite sustituir la lista actual de ficheros por una nueva
	 * @param files Lista de ficheros
	 */
	public void addFiles(FileList files){
		this.fileList = files;
	}
	public void setRetardo(long ret){
		retardo = ret;
	}
	public long getRetardo(){
		return retardo;
	}
	/**
	 * Devuelve true si existe el fichero en la lista de ficheros y false si no existe.
	 * @param file Nombre del fichero
	 * @return
	 */
	public boolean existsFile(String file){
		return this.fileList.exists(new UDPFTPFile(file,0));
	}
	/**
	 * Obtiene los datos de un fichero a traves de su nombre
	 * @param file Nombre del fichero
	 * @return UDPFTPFile Fichero
	 */
	public UDPFTPFile getFile(String file){
		UDPFTPFile[] files = fileList.getFiles();
		for (int i = 0; i < files.length; i++) {
			if(files[i].getFileName().equals(file)){
				return files[i];
			}
		}
		return null;
		
		
	}
}

