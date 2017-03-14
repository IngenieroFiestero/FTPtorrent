/*
* AUTOR: Andrés Tomás Campo, Samuel Garces Marin
* NIA: 669936 ,505428
* FICHERO: MedidorDeTiempo.java
* TIEMPO: 1h
* DESCRIPCI'ON: Clase sencilla creada para medir diferencias de tiempo.
*/
package src;

public class MedidorDeTiempo {
	private long difTiempos ;
	public MedidorDeTiempo() {
		difTiempos=0;
	}

	public void tic() {
		difTiempos=System.nanoTime();
	}

	public void toc() {
		difTiempos =System.nanoTime()- difTiempos;

	}

	public Long getLast() {
		return difTiempos;
	}
}