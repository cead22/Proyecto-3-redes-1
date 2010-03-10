import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;
import java.net.*;

public class MetodosRemotos
    extends UnicastRemoteObject {
	    //implements InterfazRemota {


    /** Canal por el cual se 
     * envian streams de datos */
    private ObjectOutputStream out;
    /** Canal por el cual se 
     * reciben streams de datos */
    private ObjectInputStream in;
    /** directorio donde se encuentran
     * las fotos */
    String directorio;
    /** computadores alcanzables desde este */
    public Vector<String> nodos_vecinos;
    /** Puerto logico por el cual 
     * se establece la comunicacion */
    private int puerto;


    public MetodosRemotos(ObjectOutputStream os, ObjectInputStream is,
			  String dir, Vector<String> vecinos,
			  int p) throws RemoteException {
	out = os;
	in = is;
	directorio = dir;
	nodos_vecinos = vecinos;
	puerto = p;
    }

    /** 
     * Lleva a cabo un dfs para obtener informacion sobre los nodos vecinos
     * @param busqueda Indica que tipo de busqueda se esta realizado: por titulo o por palabras claves.
     * @param visitados Representa el vector de nodos que ya fueron visitados previamente.
     */
    public Vector<String> dfs_distribuido (String busqueda, Vector<String> visitados){
	String resultado = "";
	File archivo = new File(directorio);
	String[] archivos_xml = archivo.list(new explorador(".xml"));
	Socket sock = null;
	ObjectOutputStream salida = null;
	ObjectInputStream entrada = null;
	String foto;
	
	/* busqueda local */
	for (int i = 0; i < archivos_xml.length; i++) {
	    if (!(foto = nodo.match(directorio + "/" +archivos_xml[i], busqueda)).equals("<no/>")){
		resultado = resultado + "\n===\n+ Archivo: " + archivos_xml[i].substring(0,archivos_xml[i].length()-4) + "\n" + foto + "\n";
	    }
	    
	}
	
	/* marcar como visitado */
	visitados.add(nodo.mi_ip());
	
	/* busqueda remota */
	try {
	    for (int i = 0; i < nodos_vecinos.size(); i++) {
		if (!visitados.contains(nodos_vecinos.elementAt(i))){
		    try{
			sock = new Socket(nodos_vecinos.elementAt(i),puerto);
		    }
		    catch (Exception e){
			System.err.println("No se pudo establecer conexion con: " + nodos_vecinos.elementAt(i));
			// evitar que sea visitado
			visitados.add(nodos_vecinos.elementAt(i));
			// continuo con las demas conexiones
			continue;
		    }
		    salida = new ObjectOutputStream(sock.getOutputStream());
		    entrada = new ObjectInputStream(sock.getInputStream());
		    System.out.println("A: "+nodo.recibir(entrada));
		    /* Se envia un comando B que indica que la comunicacion ahora es llevada a cabo entre nodos*/
		    nodo.enviar(salida,"B " + busqueda);
		    nodo.enviar(salida,visitados);
		    
		    /* Se obtiene el resultado de los demas nodos y se concatena con el resultado actual */
		    resultado = resultado + (String)nodo.recibir(entrada);
		    visitados = (Vector <String>)nodo.recibir(entrada);
		    /* Se corta la comunicacion con los nodos que ya se visitaron*/
		    nodo.enviar(salida,"<bye/>");
		    /*Se cierran los descriptores correspondientes*/
		    salida.close();
		    entrada.close();
		    sock.close();
		}
	    }
	    
	}
	catch (Exception e){
	    System.err.println(e.getMessage());
	    e.printStackTrace();
	} finally {
	    /* Se envia el resultado obtenido sobre las fotos */
	    if (resultado == "")
		resultado = "No hay ninguna foto que haga match con la busqueda establecida";
	    nodo.enviar(out,resultado);
	    /* Se devuelve el vector de visitados */
	    return visitados;
	} 
    }
    
    /*
    public SalidaDFS dfs_distribuido (String busqueda, Vector<String> visitados)  throws RemoteException {
	String resultado = "";
	File archivo = new File(directorio);
	String[] archivos_xml = archivo.list(new explorador(".xml"));
	//Socket sock = null;
	//	ObjectOutputStream salida = null;
	//ObjectInputStream entrada = null;
	String foto;
	SalidaDFS salida = null;
	// busqueda local //
	for (int i = 0; i < archivos_xml.length; i++) {
	    if (!(foto = nodo.match(directorio + "/" +archivos_xml[i], busqueda)).equals("<no/>")){
		resultado = resultado + "\n===\n+ Archivo: " + archivos_xml[i].substring(0,archivos_xml[i].length()-4) + "\n" + foto + "\n";
	    }
	 
	}

	// marcar como visitado //
	visitados.add(nodo.mi_ip());

	// busqueda remota //
	try {
	    for (int i = 0; i < nodos_vecinos.size(); i++) {
		if (!visitados.contains(nodos_vecinos.elementAt(i))){

		    //visitados.add(nodos_vecinos.elementAt(i));
		    		    		    		    
		    // Se obtiene el resultado de los demas nodos y se concatena con el resultado actual //
		    salida = dfs_distribuido(busqueda,visitados);
		    // Se corta la comunicacion con los nodos que ya se visitaron//
		}
	    }
	}
	catch (Exception e){
	    System.err.println(e.getMessage());
	    e.printStackTrace();
	} finally {
	    // Se envia el resultado obtenido sobre las fotos //
	    if (resultado == "")
		resultado = "No hay ninguna foto que haga match con la busqueda establecida";
	    // Se devuelve el vector de visitados //
	    return new SalidaDFS(salida.resultado,salida.visitados);
	} 
    }
    */
}