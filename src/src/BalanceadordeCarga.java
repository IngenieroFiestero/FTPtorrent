/*
* AUTOR: Andres Tomas Campo, Samuel Garces Marin
* NIA: 505428, 669936
* FICHERO: BalanceadordeCarga.java
* TIEMPO: 20 h
* DESCRIPCI'ON: Algoritmo de balanceo de carga
*/
package src;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BalanceadordeCarga {
	private long[] tiempo;// Ultimos tiempos de descarga de los ficheros
	private long[] nTiempo;//Veces que se ha actualizado la variable tiempo (Para calcular medias de tiempo)
	private Vector<Integer> partes;// Partes que quedan por descargar
	private long kMinimo = 0;// Numero minimo a partir del cual aplicar el algoritmo 2
	private long partesRestantes = 0;// numero de partes que quedan(para no llamar a partes.size() todo el rato)
	private boolean algoritmo2 = false;// Estamos usando el algoritmo2?
	private long[] partesServidor;// Numero de partes que puede coger cada hilo aplicando el algorithmo 2
	private long[] ultimaParte;// Ultima parte asignada a cada servidor
	private long[] servidorMasRapido;// Indica cual es el thread mas rapido para asignacion con prioridades
	private Logger log = UDPFTPClient.getLogger();// Debugear y mostrar como trabaja el balanceador de carga
	/*
	 * Ultima parte que fue asignada al servidor por ahora no se usa pero podria
	 * usarse si queremos que un thread no siga pidiendo partes al servidor
	 */

	/**
	 * Constructor del Balanceador de Carga
	 * 
	 * @param partes
	 *            Vector con la lista de partes, esta lista va disminuyendo de
	 *            longitud a medida que se van pidiendo partes
	 */
	
	public BalanceadordeCarga(Vector<Integer> partes, int nServidores) {
		tiempo = new long[nServidores];
		nTiempo = new long[nServidores];
		this.partes = partes;
		this.partesRestantes = this.partes.size();
		this.partesServidor = new long[nServidores];
		this.ultimaParte = new long[nServidores];
		this.servidorMasRapido = new long[nServidores];
		for (int i = 0; i < nServidores; i++) {
			servidorMasRapido[i] = i;
			nTiempo[i]=-1;
		}
	}

	public BalanceadordeCarga(Vector<Integer> partes, int nServidores, long kMinimo,long[] tiempos) {
		this(partes, nServidores);
		this.kMinimo = kMinimo;
		this.tiempo = tiempos.clone();
	}
	public synchronized long getNextPart(int servidor, long tiempo) {
		this.nTiempo[servidor]++;
		// Primero informamos
		this.tiempo[servidor] = (this.tiempo[servidor]*this.nTiempo[servidor] + tiempo)/(this.nTiempo[servidor]+1);
		log.log(Level.INFO, "Gestor " + servidor + " - Parte: " + ultimaParte[servidor] + " - Tiempo: " + tiempo + " -Media: " + this.tiempo[servidor] + " - Partes Pedidas: " + this.nTiempo[servidor]);
		if (kMinimo == 0) {
			// Algoritmo sin balanceo
		} else {
			// Comprobar si somos el mas rapido
			if (this.tiempo[(int) servidorMasRapido[0]] > tiempo) {
				// Reordenar lista
				servidorMasRapido = generarListaRapidos(this.tiempo.clone());
			}
			// Comprobar que algoritmo aplicar
			if (partesRestantes <= kMinimo && !algoritmo2) {
				algoritmo2 = true;
				// La primera vez que ejecutamos el segundo algoritmo asignamos
				// el maximo de partes que puede pedir cada servidor
				// Esta asignacion se hace en el array partesServidor
				log.log(Level.INFO, "Faltan " + partesRestantes + " partes y el minimo k=" + kMinimo);
				actualizarPartes();
			} 
		}
		// Obtener parte pero por prioridades
		long ret = -1;
		if (partesRestantes <= 0) {
			// Si no quedan partes devolvemos un -1
			ultimaParte[servidor] = -1;
			ret = -1;
		} else if (algoritmo2 && partesServidor[servidor] <= 0) {
			// Si estamoss usando el segundo algoritmo y no tenemos permiso para
			// coger partes
			ultimaParte[servidor] = -1;
			ret = -1;
		} else {
			// Obtenemos la primera parte de la lista y la eliminamos tras
			// enviarla
			ret = this.partes.firstElement().intValue();
			this.partes.remove(0);
			partesRestantes--;
			ultimaParte[servidor] = ret;
			if (algoritmo2) {
				// Si estamos en el segundo algoritmo disminuimos la cantidad de
				// partes para ese servidor
				partesServidor[servidor]--;
			}
		}
		return ret;
	}

	public synchronized void devolverParte(int servidor, long tiempo) {
		this.partes.addElement((int) ultimaParte[servidor]);
		this.tiempo[servidor] = (this.tiempo[servidor]*this.nTiempo[servidor] + tiempo)/(this.nTiempo[servidor]+1);
		partesRestantes++;
		// Le asignamos la parte al mas rapido si estamos en el algoritmo 2
		partesServidor[(int) servidorMasRapido[0]]++;
		ultimaParte[servidor] = -1;
	}
	private void actualizarPartes(){
		MedidorDeTiempo mt = new MedidorDeTiempo();
		mt.tic();
		//Ordenacion de tiempos de mas rapido a mas lento
		long [] duplicadoTiempo = tiempo.clone();// Ultimos tiempos de descarga de los ficheros
		float [] decision = new float[duplicadoTiempo.length];//No se usa
		long[] rapidos = generarListaRapidos(duplicadoTiempo);//Indice de los rapidos respecto al vector tiempos y las partes de servidor
		float sumaTotal = 0;
		for (int i = 0; i < rapidos.length; i++) {
			log.log(Level.INFO, "Servidor " + i+ " tiempo: " + tiempo[i]);
		}
		for(int i=0;i<duplicadoTiempo.length; i++){
			if(duplicadoTiempo[i] > 0 && duplicadoTiempo[0] > 0){
				decision[i] = duplicadoTiempo[i]/duplicadoTiempo[0];
				// Aqui en decision tenemos almacenado la relacion entre cada tiempo respecto al tiempo mas pequeño
				// por tanto cuando queden K partes accederemos a este metodo y el se encargará de decidir cuantas partes va para
				// cada servidor
				
				sumaTotal = sumaTotal + decision[i];
			}
		}
		//Algoritmo Simplex de optimizacion
		partesServidor[(int) rapidos[0]] = kMinimo;
		long tiempoTotal = partesServidor[(int) rapidos[0]]*duplicadoTiempo[0];//Tiempo de descarga desde el mas rapido
		boolean optimo =false;
		while(!optimo){
			boolean encontrado = false;
			for (int j = 1; j <partesServidor.length && !encontrado; j++) {
				if( j < partesServidor.length && j > 0 && partesServidor[(int) rapidos[j-1]] > 0  ){
					partesServidor[(int) rapidos[j-1]]--;
					partesServidor[(int) rapidos[j]]++;
					//Calculo ahora del tiempo en esta interaccion
					long tiempoAux = 0;
					for (int j2 = 0; j2 < partesServidor.length; j2++) {
						tiempoAux = Math.max(tiempoAux, partesServidor[(int) rapidos[j2]]*duplicadoTiempo[j2]);
					}
					if(tiempoAux <= tiempoTotal){
						encontrado=true;
						tiempoTotal = tiempoAux;
					}else{
						partesServidor[(int) rapidos[j-1]]++;
						partesServidor[(int) rapidos[j]]--;
					}
				}
			}
			if(!encontrado){
				optimo = true;
			}
		}
		for (int i = 0; i < rapidos.length; i++) {
			log.log(Level.INFO, "Asignacion Servidor " + i+ " - partes: " + partesServidor[i] + " tiempo: " + tiempo[i]);
		}
		mt.toc();
		log.log(Level.INFO, " Tiempo Optimizacion: " + mt.getLast()/1000000 + " milisegundos");
	}
	public static long[] generarListaRapidos(long lista[]) {
		long[] ret = new long[lista.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = i;
		}
		// Usamos un bucle anidado
		for (int i = 0; i < (lista.length - 1); i++) {
			for (int j = i + 1; j < lista.length; j++) {
				if (lista[i] > lista[j]) {
					// Intercambiamos valores
					long variableauxiliar = lista[i];
					long servAux = ret[i];
					lista[i] = lista[j];
					ret[i] = ret[j];
					lista[j] = variableauxiliar;
					ret[j] = servAux;
				}
			}
		}
		return ret;
	}

	public static void ordenaLista(long[] duplicadoTiempo) {
		// Usamos un bucle anidado
		for (int i = 0; i < (duplicadoTiempo.length - 1); i++) {
			for (int j = i + 1; j < duplicadoTiempo.length; j++) {
				if (duplicadoTiempo[i] > duplicadoTiempo[j]) {
					// Intercambiamos valores
					long variableauxiliar = duplicadoTiempo[i];
					duplicadoTiempo[i] = duplicadoTiempo[j];
					duplicadoTiempo[j] = variableauxiliar;
				}

			}
		}
	}
	public static int encontrarKMinimo(long[] bw,int partes){
		long[] bwAux = bw.clone();
		ordenaLista(bwAux);
		float[] pesos = new float[bwAux.length];
		float suma = 0;
		for (int i = 0; i < bwAux.length; i++) {
			pesos[i]=(int) (bwAux[i]/bwAux[0]);
			suma = suma + pesos[i];
		}
		if(partes < suma){
			return partes;
		}else{
			return (int) (partes%((int)suma));
		}
	}

}
