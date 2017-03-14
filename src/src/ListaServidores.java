/*
* AUTOR: Andrés Tomás, Samuel Garces 
* NIA: 669936,505428
* FICHERO: ListaServidores.java
* TIEMPO: 2 horas
* DESCRIPCION: Contiene la lista de servidores obtenida a través del archivo “list.txt”. Una vez obtenida la lista de 
* ficheros se almacena también aquí y nos permite obtener la lista final de ficheros.
*/

package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

/**
 * Permite almacenar la lista de los servidores que poseemos
 * @author admin
 *
 */
public class ListaServidores {
	private Vector<UDPFTPDatosServidor> lista = new Vector<UDPFTPDatosServidor>();
	
	public ListaServidores(String file) throws FTPException{
		File fichero = new File(file);
		Scanner s = null;
		try {
			s = new Scanner(fichero);
			while (s.hasNextLine()) {
				//La siguiente linea puede tirar error FTPException si ya hay un servidor con el mismo nombre
				addServidor(new UDPFTPDatosServidor(s.nextLine()));
			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}catch(IOException ex){
			ex.printStackTrace();
		}finally {
			try {
				if (s != null)
					s.close();
			} catch (Exception ex2) {
				ex2.printStackTrace();
			}
		}
	}
	public ListaServidores(UDPFTPDatosServidor[] files) throws FTPException{
		for (int i = 0; i < files.length; i++) {
			addServidor(files[i]);
		}
	}
	public void addServidor(UDPFTPDatosServidor server) throws FTPException{
		String name = server.getName();
		for (int i = 0; i < lista.size(); i++) {
			if(lista.get(i).getName().equals(name)){
				throw new FTPException("There are alredy a server with that name");
			}
		}
		lista.add(server);
	}
	public void addServidores(UDPFTPDatosServidor[] servers){
		for (int i = 0; i < servers.length; i++) {
			try {
				addServidor(servers[i]);
			} catch (FTPException e) {}
		}
	}
	public void removeServidor(UDPFTPDatosServidor server) throws FTPException{
		lista.remove(server);
	}
	public UDPFTPDatosServidor getServidor(String name){
		for (int i = 0; i < lista.size(); i++) {
			if(lista.get(i).getName().equals(name)){
				return lista.get(i);
			}
		}
		return null;
	}
	public UDPFTPDatosServidor[] getServidores(){
		return lista.toArray(new UDPFTPDatosServidor[lista.size()]);
	}
	public void addFiles(FileList files,String server){
		UDPFTPDatosServidor sv = getServidor(server);
		sv.addFiles(files);
	}
	public UDPFTPDatosServidor[] getServersFile(String file){
		Vector<UDPFTPDatosServidor> files = new Vector<UDPFTPDatosServidor>();
		for (int i = 0; i < lista.size(); i++) {
			if(lista.get(i).existsFile(file)){
				files.addElement(lista.get(i));
			}
		}
		return files.toArray(new UDPFTPDatosServidor[files.size()]);
	}
	public String toString(){
		String ret = "";
		for (int i = 0; i < lista.size()-1; i++) {
			ret = ret +lista.get(i).toString() +"\n";
		}
		ret =ret + lista.get(lista.size()-1).toString();
		return ret;
	}
	
}
