# FTPtorrent
## Diseño e Implementación de un  Sistema Distribuido utilizando la  arquitectura Master - Worker
El presente trabajo tenía como objetivos programar un Sistema Distribuido utilizando la arquitectura Máster-Worker. Nuestro cliente interactúa con varios servidores en los que se encuentra el fichero. Al paralelizar el proceso conseguimos una mayor velocidad de descarga frente a la descarga con un solo servidor. Excepcionalmente, nos encontramos con el caso en el que un servidor tiene la velocidad máxima que soporta el cliente, de manera que usando únicamente este servidor alcanzamos el mínimo tiempo de descarga sin necesidad de usar otros servidores. 
Los objetivos del trabajo eran dos, en primer lugar conseguir el menor tiempo de descarga gracias a un algoritmo de balanceo de carga; en segundo lugar conseguir una tolerancia a fallos que permita reponerse al sistema frente al error en el envío de un fichero.  
En cuanto al balanceo de carga, a lo largo del desarrollo del trabajo se han planteado cuatro posibles soluciones en la que cada solución supone una mejora frente a la anterior, para finalmente llegar a la solución final. 
La tolerancia de fallos se basa en devolver una parte fallida al balanceador de carga para que este la vuelva a enviar a otro servidor, que será el más rápido. 
### Solucion 1: Reparto proporcional 
En primer lugar para el balanceo de carga realizamos una división proporcional. Dependiendo del ancho de banda de cada servidor le mandábamos más o menos carga a 
este. Por ejemplo, para el caso de 4 servidores heterogéneos (1, 2, 4, 8): 
Al servidor 1 le mandamos 1/15 del total de la carga 
Al servidor 2 le mandamos 2/15 del total de la carga 
Al servidor 3 le mandamos 4/15 del total de la carga 
Al servidor 4 le mandamos 8/15 del total de la carga 
El problema de este algoritmo es que el número de partes total no es siempre divisible por las fracciones de ancho de banda. Cuando repartimos partes se da el caso que los servidores lentos mandan partes cuando realmente no deberían mandar ninguna. 

### Solución planteada 2: Reparto proporcional con tiempo real 
En la segunda solución dividimos dicho fichero dependiendo del tiempo real de transmisión, ya que los anchos de banda teóricos de los servidores no son iguales a los tiempos de transmisión reales. Una vez tenemos estos tiempos los usamos para realizar el mismo algoritmo que en la solución 1. Esta vez tenemos en cuenta los valores reales de transmisión donde el retardo y el tiempo de descarga están presentes. 
 
### Solución planteada 3: Reparto con vectores de decisión 
En la tercera solución mejoramos el algoritmo de repartición solucionando parcialmente el problema del primer intento. Una vez tenemos el número de partes necesarias para descargar un fichero aplicamos el siguiente algoritmo: 
Primero ordenamos los servidores por tiempos de transmisión. Una vez tenemos esta ordenación comparamos los tiempos de transmisión de cada servidor con el servidor más lento obteniendo un vector [a b c d], con tantas componentes como servidores tenemos. Tras esto, hayamos otro segundo vector [e f g h] en el cual sus componentes representan las relaciones existentes entre servidores adyacentes. Es decir, la componente e representa la relación entre el servidor más rápido y el segundo más rápido; la componente f representa la relación entre el segundo más rápido respecto al tercero. 
Por ejemplo, con los servidores heterogéneos [1 ,2 ,4 ,8] obtenemos los vectores      [8, 4 ,2 ,1] y [2 ,2 ,2 ,1]. Una vez tenemos estos vectores aplicamos la repartición usando ambas relaciones. 
El problema de esta solución consiste en que sigue siendo necesario redondear en algún momento por lo que se siguen enviando partes en algunos casos a servidores lentos que no se deben enviar. 

### Solución planteada 4: Algoritmo Simplex 
La última solución consiste en la repartición de partes natural por servidores. Los servidores van pidiendo partes conforme terminan una transmisión, por lo que los servidores más rápidos serán los que más partes envían. Esta repartición natural se ejecuta hasta que quede un número de K partes por repartir. Llega un punto en el que ya no es necesario mandarle archivos a los servidores más lentos debido a que el número de partes restantes es pequeño. Cuando llegamos a este valor K restringimos el número de partes a enviar a los servidores más lentos. La variable K la conseguimos sumando las relaciones de los tiempos de descarga de todos los servidores respecto al más lento. Al sumar todos los valores de esta relación obtenemos el número de partes a partir del cual ya no será necesario pedirle un paquete al servidor más lento y procedemos a aplicar el algoritmo.   
Descripción del algoritmo con un ejemplo: 
Número de partes del fichero ejemplo: 100 
Tiempos de transmisión teóricos de los servidores respecto al más lento: [8 4 2 1]; (el servidor uno transmite ocho partes mientras el servidor cuatro ha transmitido una; el servidor dos transmite cuatro partes mientras el servidor cuatro ha transmitido una única parte y así sucesivamente.) 
En primer lugar obtenemos el parámetro K haciendo el módulo entre el número de partes total del archivo a enviar y la suma de los tiempos teóricos de transmisión de los paquetes respecto al más lento. En este caso sería: 100 % (1+2+4+8) = 10. 
En segundo lugar comienza la descarga del fichero de manera natural, enviándole partes a cada servidor hasta que el número de partes sea igual a K. Los servidores más rápidos descargarán más partes que los lentos ya que irán terminando antes por lo que solicitarán más partes.  
Una vez el número de partes restantes es igual a K, calculamos el tiempo que tardaría el servidor más rápido en procesarlas todas de manera secuencial. Siguiendo con el ejemplo numérico, con K=10, el vector de partes por servidor a repartir sería este inicialmente: 
[10, 0 ,0 ,0] 
En segundo lugar calculamos el tiempo total al pasarle una parte al segundo servidor más rápido y lo comparamos con el tiempo almacenado en la iteración anterior. Si el tiempo obtenido es menor el vector de partes por servidor pasa a ser: 
[9, 1, 0, 0] 
De esta forma iremos aumentando el número de partes a enviar al segundo servidor más rápido hasta que se deje de cumplir la condición de que el último tiempo obtenido es menor que el tiempo de la iteración anterior. Este proceso se repite con todos los servidores restantes. 
En el caso de que el número de partes del archivo sea menor que K, aplicamos la división de partes directamente con los tiempos de transmisión teóricos, aplicando el algoritmo descrito arriba con la única diferencia que no tenemos en cuenta los tiempos de transmisión reales, ya que no hemos podido mandar paquetes a todos los servidores. 
### Validación Experimental 
Practica 4 | file1.rar (segundos) |	file2.mp4 (segundos) 
-----------|----------------------|---------------------
Servidor 100 Mbps: |	0.8 |	7.1 
Servidores 10 Mbps: |	8 |	69 
Servidor: 8Mbps |	10 |	86 
Servidor: 6Mbps |	20,2 |	173 
Servidor: 4Mbps |	40,5 |	346 
Servidor: 2Mbps |	81 |	693 

TP6 |	file1.rar (segundos) |	file2.mp4 (segundos) 
---|----------------------|-----------------------
Servidor 100 Mbps: |	0.87 |	7.21 
Servidores 10 Mbps: |	2.5 |	17.8 
Servidores heterogéneos: |	6.2 |	48.8 

![Grafica de velocidades](https://github.com/IngenieroFiestero/FTPtorrent/blob/master/grafico.jpg)

