/*
* AUTOR: Andrés Tomás Campo, Samuel Garces Marin
* NIA: 669936 ,505428
* FICHERO: ProgressBar.java
* TIEMPO: 1h
* DESCRIPCI'ON: Nos permite mostrar por pantalla
*  una barra de progreso con porcentaje de descarga.
*/
package src;
public class ProgressBar {
	public static final int ROTACIONAL = 0;// Va cambiando el simbolo [|,/,-,\]
	public static final int PORCENTAJE = 1;
	public static final String ANIM = "|/-\\";
	public static final long ANIM_TIME = 100;
	public static final String SIMBOLO = "=";
	private String barra;
	private int animacion;
	private static final int MAX = 20;//Maximo de barras
	private float max;
	private int type;
	private float level;
	private int point;
	private long time;
	
	public ProgressBar(float max,int type){
		barra = "";
		this.max = max;
		this.type = type;
		this.level = 0;
		this.animacion = 0;
		this.point = 0;
		this.time = System.currentTimeMillis();
	}
	public synchronized String render(float step){
		this.level = this.level + step;
		animacion = (int) ((100/this.max*level)%ANIM.length());
		long thistime = System.currentTimeMillis();
		String puntos = "";
		if(thistime - time >= ANIM_TIME){
			this.time = thistime;
			point = (point + 1)%4;
		}
		for (int i = 0; i < point; i++) {
			puntos = puntos + ". ";
		}
		puntos = puntos + "    ";//Añadir espacios para evitar que salgan cosas demas en la pantalla
		switch(type){
		case ROTACIONAL:
			if(this.level >= this.max){
				return "- 100% |"+barra+"|\r";
			}else{
				int porcentaje = (int) (100/this.max*level);
				return ANIM.charAt(animacion) + " " +porcentaje +"% " + puntos +" \r";
			}
		case PORCENTAJE:
			if(this.level >= this.max){
				return "100% |"+barra+"|\r";
			}else{
				barra = "";
				for (int i=0; i < MAX; i++) {
					if(i <MAX/this.max*level){
						barra = barra + SIMBOLO;
					}else{
						barra = barra + " ";
					}
				}
				int porcentaje = (int) (100/this.max*level);
				return " " +porcentaje +"% |"+barra+"| "+ puntos + " \r";
			}
		}
		return barra;
	}
	//Todavia no usar porque no se puede limpiar bien
	public String render(float step,String texto){
		return render(step) + " - " + texto;
	}
	
}