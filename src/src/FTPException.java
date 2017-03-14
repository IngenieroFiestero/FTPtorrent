/*
* AUTOR: Andrés Tomás Campo , Samuel Garces Marin
* NIA: 669936, 505428
* FICHERO: FTPException.java
* TIEMPO:1h
* DESCRIPCI'ON: Excepcion personalizada lanzada por FTPService en casos en los que un comando o respuesta no sea válido
*/
package src;
public class FTPException extends Exception {

	private static final long serialVersionUID = 1L;

	public FTPException(String msg) {
		super(msg);
	}

	public FTPException(String msg, Exception e) {
		super(msg, e);
	}

	public FTPException(Exception e) {
		super(e);
	}
}
