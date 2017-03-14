/*
* AUTOR: Andres Tomas Campo, Samuel Garces Marin
* NIA: 505428, 669936
* FICHERO: UDPFPTFile.java
* TIEMPO: 1h
* DESCRIPCI'ON:  Clase sencilla para guardar el nombre y la longitud de un fichero
*/
package src;

/**
 * Clase sencilla para guardar el nombre y la longitud de un fichero
 * @author Samuel Garces
 * @author Andres Tomas
 *
 */
public class UDPFTPFile {
	private String filename;
	private Long length;//Medido en bytes
	public UDPFTPFile(String file, long length){
		this.filename = file;
		this.length = length;
	}
	/**
	 * Obtener el nombre del fichero
	 * @return
	 */
	public String getFileName(){
		return this.filename;
	}
	/**
	 * Obtener la longitud del fichero en bytes
	 * @return
	 */
	public long getFileLength(){
		return this.length;
	}
}
