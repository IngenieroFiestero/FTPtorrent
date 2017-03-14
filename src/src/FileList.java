/*
* AUTOR: Andres Tomas Campo, Samuel Garces Marin
* NIA: 505428, 669936
* FICHERO: FileList.java
* TIEMPO: 1h
* DESCRIPCI'ON: Lista de ficheros junto a su tamaño
*/
package src;

import java.util.Scanner;
import java.util.Vector;

public class FileList {
	private Vector<UDPFTPFile> fileList = new Vector<UDPFTPFile>();

	public FileList() {
	}
	public FileList(String files) {
		Scanner scanner = new Scanner(files);
		while(scanner.hasNextLine()){
			String cadena = scanner.nextLine();
			if(!cadena.equals("")){
				String[] fileOrLength = cadena.split(" ");
				this.addFile(fileOrLength[0],Long.parseLong(fileOrLength[1]));
			}
		}
		scanner.close();
	}

	public void addFileListFromServer(String[] fileList, long[] lengths) {
		for (int i = 0; i < fileList.length; i++) {
			UDPFTPFile file = new UDPFTPFile(fileList[i], lengths[i]);
			if (!this.fileList.contains(file)) {
				// Si no existe añadimos el fichero a la lista
				this.fileList.add(file);
			} else {
				// Existe el fichero
			}
		}
	}
	public void addFile(String fileName, long length) {
		UDPFTPFile file = new UDPFTPFile(fileName, length);
		if (!exists(file)) {
			// Si no existe añadimos el fichero a la lista
			this.fileList.add(file);
		}
	}
	public void addFile(UDPFTPFile file) {
		if (!exists(file)) {
			// Si no existe añadimos el fichero a la lista
			this.fileList.add(file);
		}
	}
	public boolean exists(UDPFTPFile file){
		for (int i = 0; i < fileList.size(); i++) {
			if(file.getFileName().equals(fileList.get(i).getFileName())){
				return true;
			}
		}
		return false;
	}
	public UDPFTPFile[] getFiles(){
		return fileList.toArray(new UDPFTPFile[fileList.size()]);
	}
	public UDPFTPFile getFile(String filename){
		for (int i = 0; i < fileList.size(); i++) {
			if(filename.equals(fileList.get(i).getFileName())){
				return fileList.get(i);
			}
		}
		return null;
	}
	public String toString(){
		String ret = "";
		for (int i = 0; i < fileList.size()-1; i++) {
			ret = ret + fileList.get(i).getFileName() + " " + fileList.get(i).getFileLength() + "\n";
		}
		if(fileList.size()-1 >0){
			ret = ret + fileList.get(fileList.size()-1).getFileName() + " " + fileList.get(fileList.size()-1).getFileLength();
		}
		return ret;
	}

}
