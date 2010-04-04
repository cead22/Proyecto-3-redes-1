import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import nanoxml.*;
import java.rmi.*;

/**
 * Clase para representar los nodos conectados a la red.
 * @author Carlos Alvarez y Marion CArambula.
 */
public class nodo extends java.rmi.server.UnicastRemoteObject implements InterfazRemota {

    /** Comando recibido de 
     * consulta/transferencia */
    private String mensaje;
    /** Puerto logico por el cual 
     * se establece la comunicacion */
    private static int puerto;
    /** directorio donde se encuentran
     * las fotos */
    private static String directorio;
    /** computadores alcanzables desde este */
    private static Vector<String> nodos_vecinos;
    /** traza sobre la que se escribira las operaciones realizadas*/
    private BufferedWriter traza;

   /** 
    * Crea un nodo a partir de un puerto y un directorio.
    * @param p puerto a traves el cual se hara la conexion.
    * @param dir directorio donde.
    */
    public nodo(int p, String dir, String t) throws RemoteException {
	directorio = dir;
	puerto = p;
	try {
	    traza = new BufferedWriter(new FileWriter(t,true));
	}
	catch (Exception e){
	    System.err.println("Error con la traza");
	}
    }
	
    /** 
     * Obtiene las vecinos de un nodo y los almacena en
     * un vector.
     * @param traza Archivo donde se encuentran especificados los vecinos de un nodo.
     * @throws EOFException, FileNotFoundException
     * @return Vector de visitados
     */ 
    public static Vector<String> Vecinos (String maquinas) {
	File archivo = null;
	FileReader fr = null;
	BufferedReader br = null;
	Vector <String> ln  = new Vector <String>();
	try {
	    archivo = new File (maquinas);
	    fr = new FileReader (archivo);
	    br = new BufferedReader(fr);
	    
	    // Lectura del fichero
	    String linea;
	    /*Se obtienen los nodos vecinos, se ignora si dentro del archivo localhost es un vecino */
	    while((linea = br.readLine()) != null && !linea.equals("127.0.0.1") && !linea.equals("localhost")){
		linea = InetAddress.getByName(linea).getHostAddress();
		ln.addElement(linea);
	    }
	}
	catch(EOFException e) {
	    return ln;
	}
	catch(FileNotFoundException e){
	    System.err.println("El archivo "+ maquinas+" no existe.");
	}
	catch (Exception e){
	    e.printStackTrace();
	}
	finally{
	    try{
		/*Se cierra el archivo */
		if(null != fr){   
		    fr.close();     
		}                  
	    }catch (Exception e2){ 
		e2.printStackTrace();
	    }
	}
	/* Se retorna el vector de vecinos */
	return ln;
    }
    
    /** 
     * Indica la forma correcta de hacer la llamada al programa.
     * @param s String a imprimir por pantalla.
     */
    public static void uso(String s){
	System.out.println(s);
	System.out.println("Uso: nodo -p <puerto> -f <maquinas> -l <archivoTrazas> -d <directorio>");
	System.exit(-1);
    }
    
    /** 
     * Indica la forma correcta de hacer la llamada al programa.
     */
    public static void uso(){
	System.out.println("Uso: nodo -p <puerto> -f <maquinas> -l <archivoTrazas> -d <directorio>");
	System.exit(-1);
    }

    /** 
     * Obtiene el ip publico de la maquina donde se esta ejecutando el programa
     * @return Ip Publico
     * @throws  Exception
     */
    public static String mi_ip () {
	Enumeration e1;
	Enumeration e2;
	NetworkInterface ni;
	String ip;
	try {
	    /*Se exploran las interfaces disponibles para obtener el ip publico*/
	    e1 = NetworkInterface.getNetworkInterfaces();
	    while(e1.hasMoreElements()) {
		ni = (NetworkInterface) e1.nextElement();
		e2 = ni.getInetAddresses();
		while (e2.hasMoreElements()){
		    ip = ((InetAddress) e2.nextElement()).getHostAddress();
		    if (ip.matches("[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}") && !ip.equals("127.0.0.1")) {
			return ip;
		    }
		}
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
	return "conexion nula";
    }
    
    /** 
     * Lleva a cabo un dfs para obtener informacion sobre los nodos alcanzables
     * @param visitados Representa el vector de nodos que ya fueron visitados previamente.
     */
    public Vector<String> alcanzables (Vector<String> visitados){
	InterfazRemota ir = null;
	try{
	    traza.write("Consulta  A recibida desde " + mi_ip() + "\n");
	    traza.flush();
	}
	catch(Exception e){
	    System.err.println("Error al escribir en la traza");
	}
	
	/* marcar como visitado */
	visitados.add(mi_ip());
	
	/* busqueda remota */
	for (int i = 0; i < nodos_vecinos.size(); i++) {
	    if (!visitados.contains(nodos_vecinos.elementAt(i))){
		try {
		    /* Creacion de una interfaz para establecer conexion con los vecinos */
		    ir = (InterfazRemota)java.rmi.Naming.lookup("//" + nodos_vecinos.elementAt(i) + ":" + puerto + "/fotop2p");
		    visitados = ir.alcanzables(visitados);
		}
		catch (NotBoundException e) {
		    System.err.println("No esta corriendo el servidor en " + nodos_vecinos.elementAt(i) + ":" + puerto);
		    visitados.add(nodos_vecinos.elementAt(i));
		}
		catch (MalformedURLException m) {
		    System.err.println("No se pudo establecer conexion con el servidor: URL incorrecta");
		}
		catch (RemoteException r) {
		    System.err.println("No se pudo establecer conexion con el servidor en " + nodos_vecinos.elementAt(i));
		    visitados.add(nodos_vecinos.elementAt(i));
		    System.out.println(r.getMessage());
		}
	    }
	}
	return visitados;
    }
    

	
	
    /** 
     * Lleva a cabo un dfs para obtener informacion sobre los nodos vecinos
     * @param busqueda Indica que tipo de busqueda se esta realizado: por titulo o por palabras claves.
     * @param visitados Representa el vector de nodos que ya fueron visitados previamente.
     */
    public SalidaDFS dfs_distribuido (String busqueda, Vector<String> visitados) throws RemoteException {
	String resultado = "";
	File archivo = new File(directorio);
	String[] archivos_xml = archivo.list(new explorador(".xml"));
	String foto;
	InterfazRemota ir;
	SalidaDFS sal;
	
	try{
	    traza.write("Consulta" + busqueda + "recibida desde " + mi_ip() + "\n");
	    traza.flush();
	}
	catch(Exception e){
	    System.err.println("Error al escribir en la traza");
	}
	
	if (archivos_xml == null) {
	    System.err.println("Directorio " + directorio + " no existe");
	}
	else {
	
	// busqueda local //
	    for (int i = 0; i < archivos_xml.length; i++) {
		if (!(foto = match(directorio + "/" +archivos_xml[i], busqueda)).equals("<no/>")){
		    resultado = resultado + "\n===\n+ Archivo: " + archivos_xml[i].substring(0,archivos_xml[i].length()-4) + "\n" + foto + "\n";
		}
		
	    }
	}

	// marcar como visitado //
	visitados.add(mi_ip());

	// busqueda remota //
	for (int i = 0; i < nodos_vecinos.size(); i++) {
	    if (!visitados.contains(nodos_vecinos.elementAt(i))){
		try {
		    System.out.println("//" + nodos_vecinos.elementAt(i) + ":" + puerto + "/fotop2p");
		    ir = (InterfazRemota)java.rmi.Naming.lookup("//" + nodos_vecinos.elementAt(i) + ":" + puerto + "/fotop2p");
		    sal = ir.dfs_distribuido(busqueda, visitados);
		    resultado = resultado + sal.resultado;
		    visitados = sal.visitados;
		}
		catch (NotBoundException e) {
		    System.err.println("No existe el servicio solicitado en " + nodos_vecinos.elementAt(i) + ":" + puerto);
		}
		catch (MalformedURLException m) {
		    System.err.println("No se pudo establecer conexion con el servidor: URL incorrecta");
		}
		catch (RemoteException r) {
		    System.err.println("No se pudo establecer conexion con el servidor en " + nodos_vecinos.elementAt(i));
		}
	    }
	}	   
	return new SalidaDFS(resultado,visitados);
    }

    
    /** 
     * Hace una busqueda de una string dado en un archivo xml.
     * @param archivo Archivo donde se ejecutara la busqueda.
     * @param busqueda String a buscar en el archivo.
     * @return No en caso que no se haya conseguido el string en el archivo
     * en cualquier otro caso un string con ciertas caracteristicas.
     */
    public static String match (String archivo, String busqueda) {

	XMLElement xml = new XMLElement();
	FileReader reader = null;
	Vector children = null;
	Vector sub_children = null;
	String nombre_elem;
	String contenido_elem;
	String tipo_busqueda = (busqueda.split("[\\s]+"))[0];
	String cadena = (busqueda.split("[\\s]+"))[1];
	String atributo = null;
	String titulo;
	String autor;
	String descripcion;
	String servidor;
	Pattern patron = Pattern.compile(cadena,Pattern.CASE_INSENSITIVE);
	Matcher aux;
	
	try {
	    reader = new FileReader(archivo);
	    xml.parseFromReader(reader);
	    children = xml.getChildren();
	}
	catch (IOException e) {
	    System.err.println(e.getMessage());
	}

	if (tipo_busqueda.equalsIgnoreCase("-t")){	
	    for (int i = 0; i < children.size(); i++){
		/* Se obtiene el nombre del tag */
		nombre_elem = ((XMLElement)children.elementAt(i)).getName();
		/* Verificacion que el tag sea titulo */		
		if (nombre_elem.equals("titulo")) {
		    /* Se obtiene el contenido del tag titulo */
		    contenido_elem = ((XMLElement)children.elementAt(i)).getContent();
		    /* Se verifica si hay un substring con la cadena dada */
		    aux = patron.matcher(contenido_elem);
		    if (aux.find()){
			titulo = "- Titulo: " + contenido_elem + "\n";
			autor = "- Autor:\n\t" + ((XMLElement)children.elementAt(i+2)).getAttribute("name") + "\n";
			descripcion = "- Descripcion:" + ((XMLElement)children.elementAt(i+3)).getContent() + "\n";
			servidor = "- Servidor: \n\t" + mi_ip() + "\n===";
			return titulo + autor + descripcion + servidor;
		    }
		    return "<no/>";
		}
	    }
	}
	else {
	    for (int i = 0; i < children.size(); i++){
		/* Se obtiene el nombre del tag */
		nombre_elem = ((XMLElement)children.elementAt(i)).getName();
		/* Se verifica que el tag sea palabrasClave */
		if (nombre_elem.equals("palabrasClave")){
		    /* Para c/entrada */
		    sub_children = ((XMLElement)children.elementAt(i)).getChildren();
		    for (int j = 0; j < sub_children.size(); j++){
			contenido_elem = (String)((XMLElement)sub_children.elementAt(j)).getAttribute("palabra");
			aux = patron.matcher(contenido_elem);
			if (aux.find()){
			    titulo = "- Titulo: " + ((XMLElement)children.elementAt(i-4)) + "\n";
			    autor = "- Autor:\n\t" + ((XMLElement)children.elementAt(i-2)).getAttribute("name") + "\n";
			    descripcion = "- Descripcion:" + ((XMLElement)children.elementAt(i-1)).getContent() + "\n";
			    servidor = "- Servidor: \n\t" + mi_ip() + "\n===";
			    return titulo + autor + descripcion + servidor;
			}
		    }
		    return "<no/>";
		}
	    }
	} 
	return "<no/>"; // no necesario si el xml esta bien hecho
    }


    /** 
     * Envia una foto que ha sido solicitada
     * @param archivo Archivo que se desea enviar.
     * @return un arreglo de bytes que representa la foto
     */
    public byte[] archivo_a_bytes(String archivo) {
	try{
	    traza.write("Peticion de la foto " + archivo + " recibida desde " + mi_ip() + "\n");
	    traza.flush();
	}
	catch(Exception e){
	    System.err.println("Error al escribir en la traza");
	}
	try {
	    File foto = new File (directorio + "/" + archivo);
	    byte [] bytes_foto  = new byte [(int)foto.length()];
	    int tam = bytes_foto.length;
	    FileInputStream fi = new FileInputStream(foto);
	    BufferedInputStream bi = new BufferedInputStream(fi);

	    bi.read(bytes_foto,0,bytes_foto.length);
	    return  bytes_foto;
	}
	catch (FileNotFoundException f) {
	    return null;
	}
	catch (IOException e) {
	    return null;
	}
    }


    public static void main(String args[]) throws Exception {
	int port = 0;
	int argc = args.length;
	String maquinas = null;
	String traza = null;
	String dir = null;
	boolean check[] = {false,false,false,false};
	String dir_Act = System.getProperty("user.dir");
	
	// Revision de parametros de  llamada
	for (int i = 0; i < argc - 1; i += 2){
	    if (args[i].equals("-p")){
		try {
		    puerto = Integer.valueOf(args[i+1]);
		}
		catch(NumberFormatException e) {
		    uso("El puerto debe ser un entero entre 1025 y 65536");
		}
		if (!(puerto > 1024 && puerto < 65536)) {
		    uso("El puerto debe ser un entero entre 1025 y 65536");
		}
		check[0] = true;
	    }
	    else if (args[i].equals("-f")){
		maquinas = args[i+1];
		check[1] = true;
	    }
	    else if (args[i].equals("-l")){
		traza = args[i+1];
		check[2] = true;
	    }
	    else if (args[i].equals("-d")){
		directorio = args[i+1];
		check[3] = true;
	    }    
	    else uso();
	}
		
	if (!(check[0] && check[1] && check[2] && check[3])) uso();
	nodos_vecinos = Vecinos(maquinas);
	InterfazRemota ir = new nodo(puerto,directorio,traza);
	Naming.rebind("//" + java.net.InetAddress.getLocalHost().getHostAddress() +
                             ":" + puerto + "/fotop2p", ir);
    }
}

