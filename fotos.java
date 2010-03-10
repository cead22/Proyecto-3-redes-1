import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.*;


/**
 * Clase para representar el cliente que solicita las fotos
 * @author Carlos Alvarez y Marion CArambula.
 */

public class fotos {

    /** Socket a traves de cual se 
     * hacen las consultas */
    Socket SocketCliente;
    /** Canal por el cual se envian
     *streams de datos */ 
    ObjectOutputStream salida;
    /** Canal por el cual se 
     * reciben streams de datos */ 
    ObjectInputStream entrada;
    /** Comando de consulta/transferencia
     * que se envia a la palicacion nodo */
    String mensaje;
    /** Puerto logico por el cual 
     * se establece la comunicacion */
    int puerto;
    /**  Maquina a la cual se conectara
     * para hacer las consultas */
    String maquina;
    /** Comando de consulta/transferencia
     * de fotos */
    String comando;
    
    /** Constructor
     * Crea una instancia de la clase fotos
     * @param port puerto por el cual se conecta a la base
     * de datos fotografica
     * @param maq maquina a la cual se conectara para hacer
     * las consultas
     */
    fotos(int port,String maq){
	puerto = port;
	maquina = maq;
    }


    /**
     * Provee una interfaz para realizar consultas 
     * sobre fotos, que se encuentran en una 'base de datos'
     * fotografica distribuida, y permite descargarlas.
     *
     */
    public void run()
    {
	String con = null;
	String [] cmd;
	BufferedReader br;
	Boolean b;
       	try{
	    SocketCliente = new Socket(maquina,puerto);
	    /*Se habilita el time out para esperar mensaje del servidor*/
	    SocketCliente.setSoTimeout(10);
	    mensaje = null;
	    salida = new ObjectOutputStream(SocketCliente.getOutputStream());
	    entrada = new ObjectInputStream(SocketCliente.getInputStream());	    
	    salida.flush();
	    con = (String)entrada.readObject();
	    
	    if (!con.equals("<exito/>")){
		System.err.println("El servidor corriendo en " + puerto + " no es nodo. Verifique el puerto");
		System.exit(-1);
	    }
	    /*Se deshabilita el time out*/
	    SocketCliente.setSoTimeout(0);
	    System.out.println("Conexion establecida con el servidor");
	    do {
		try{
		    
		    /* Se obtiene el comando a ejecutar */
		    br = new BufferedReader(new InputStreamReader(System.in));
		    mensaje = br.readLine();
		    /* Se verifica comando de salida */
		    if (mensaje.equalsIgnoreCase("q")) {
			mensaje = "<bye/>";
			sendMessage(mensaje);
		    }
		    /*Se verifica que el comando sea solicitud de foto */
		    else if (mensaje.matches("[\\s]*[dD][\\s]*[\\S]+[:][\\S]+")) {
			if (!mensaje.matches("[\\s]*[dD][\\s]*[\\S]+[:][\\S]+jpg")) {
			    System.err.println("La foto debe tener extension .jpg");
			    continue;
			}
			else{
			    System.out.println("Recibiendo foto");
			    cmd = mensaje.split(":");
			    recibir_archivo(mensaje, cmd[0].split(" ")[1],cmd[cmd.length-1]);
			}
		    }
		    else {
			sendMessage(mensaje);
			//System.out.println("aqui" +mensaje);
			String s = (String)entrada.readObject();
			if (s.equals("<alc/>")) {
			    System.out.println("Alcanzables: " + ((Vector)entrada.readObject()).size());
			}
			else {
			    System.out.println(s);
			}
		    }
		}
		catch (java.net.ConnectException c) {
		    System.err.println("No se pudo establecer conexion con " + maquina);
		}
		catch(Exception e){
		    System.err.println("ERROR: " + e.getMessage());
		    e.printStackTrace();
		}
	    } while (!mensaje.equalsIgnoreCase("<bye/>"));
	    
	    salida.close();
	    SocketCliente.close();
	}
	catch(java.net.UnknownHostException unknownHost){
	    System.err.println("Nombre de servidor invalido.");
	    System.exit(-1);
	}
	catch(IOException ioException){
	    System.err.println("No se pudo establecer conexion con el servidor");
	    System.exit(-1);
	}
	catch(ClassNotFoundException ioException){
	    ioException.printStackTrace();
	}
    }


    /** Convierte un arreglo de 4 bytes a un entero
     * @param b el arreglo de bytes
     */
    public int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
    }

    /** Funcion que recibe una foto que le envia una aplicacion
     * nodo y la guarda en el directorio actual
     * @param mensaje comando a enviar a nodo para que transfiera
     * el archivo
     * @param nodo direccion ip de la maquina que enviara la foto
     * @param archivo nombre de la foto
     */
    private void recibir_archivo(String mensaje, String nodo, String archivo) throws Exception {
	Socket sock = null;
	int filesize = 0;
	int bytesRead;
	int current = 0;
	ObjectInputStream is;
	ObjectOutputStream os;
	FileOutputStream fos = new FileOutputStream("./" + archivo);
	BufferedOutputStream bos = new BufferedOutputStream(fos);
	byte [] tamano = new byte[4]; 
	byte [] mybytearray;
	String dir_cliente = SocketCliente.getInetAddress().getHostAddress();
	String dir_nodo = InetAddress.getByName(nodo).getHostAddress();

	/* Se requiere nueva conexion */
	if (!dir_cliente.equals(dir_nodo)){
	    sock = new Socket(nodo,puerto);
	    is = new ObjectInputStream(sock.getInputStream());
	    os = new ObjectOutputStream(sock.getOutputStream());
	    os.writeObject(mensaje);
	    os.flush();
	    System.out.println((String)is.readObject());	
	    is = new ObjectInputStream(sock.getInputStream());
	    current = 0;
	    bytesRead = 0;
	    while(bytesRead > -1 && current < 4){
		bytesRead = is.read(tamano, current, 4 - current);
		if(bytesRead >= 0) current += bytesRead;
	    } 
	    
	    mybytearray = new byte [byteArrayToInt(tamano)];
	    
	    if (mybytearray.length == 0) {
		System.out.println("Foto no encontrada");
		os.writeObject("<bye/>");
		os.flush();
		return;
	    }
	    current = 0;
	    bytesRead = 0;
	    while(bytesRead > -1 && current < byteArrayToInt(tamano))
		{
		    bytesRead =	is.read(mybytearray, current, (mybytearray.length-current));
		    
		    if(bytesRead >= 0) current += bytesRead;
		}
	    bos.write(mybytearray, 0 , current);
	    bos.flush();
	    bos.close();
	    
	    os.writeObject("<bye/>");
	    os.flush();
	    System.out.println("Foto Recibida");
	    try{
		os.close();
		is.close();
		sock.close();
	    }
	    catch (IOException io){
		System.err.println("Error al cerrar stream de E/S y/o Socket");
	    }
	  
	}
	/* No se requiere hacer nueva conexion */
	else {
	    is = entrada;
	    os = salida;
	    os.writeObject(mensaje);
	    os.flush();
	    is = new ObjectInputStream(SocketCliente.getInputStream());
	    current = 0;
	    bytesRead = 0;
	    while(bytesRead > -1 && current < 4){
		bytesRead = is.read(tamano, current, 4 - current);
		if(bytesRead >= 0) current += bytesRead;
	    } 
	    
	    mybytearray = new byte [byteArrayToInt(tamano)];
	    
	    if (mybytearray.length == 0) {
		System.out.println("Foto no encontrada");
		os.writeObject("<recibido/>");
		os.flush();
		return;
	    }
	    current = 0;
	    bytesRead = 0;
	    while(bytesRead > -1 && current < byteArrayToInt(tamano))
		{
		    bytesRead =	is.read(mybytearray, current, (mybytearray.length-current));
		    
		    if(bytesRead >= 0) current += bytesRead;
		}
	    bos.write(mybytearray, 0 , current);
	    bos.flush();
	    bos.close();
	    
	    os.writeObject("<recibido/>");
	    os.flush();
	    System.out.println("Foto Recibida");
	    
	}
	
    }


    /**
     * Envia un mensaje determinado a traves de un canal de salida
     * especificado.
     * @param mensaje mensaje a enviar.
     * @throws IOException
     */
    public void sendMessage(String msg){
	try{
	    salida.writeObject(msg);
	    salida.flush();
	}
	catch(IOException ioException){
	    ioException.printStackTrace();
	}
    }

    
    /** Revisa los parametros de la llamada y lee de la consola
     * los comandos a enviar a la aplicacion nodos
     */
    public static void main(String args[]){
	int puerto = 0;
	String maq = null;
	
	/* Revision de llamada */
	if (args.length != 4) {
	    System.out.println("Uso: fotos -s <servidor> -p <puerto>");
	    System.exit(-1);
	}

	if (args[0].equals("-s")  && args[2].equals("-p")) {
	    try {
		puerto = Integer.valueOf(args[3]);
	    }catch(NumberFormatException e){
		System.out.println("El puerto debe ser un numero entero entre 1025 y 65535");
	    }
	    maq = args[1];
	}
	else if (args[0].equals("-p") && args[2].equals("-s")) {
	    try {
		puerto = Integer.valueOf(args[1]);
	    }catch(NumberFormatException e){
		System.out.println("El puerto debe ser un numero entero entre 1025 y 65535");
	    }maq = args[3];
	}
	else {
	    System.out.println("Uso: edolab -f <maquinas> -p <puertoRemote>\n");
	    System.exit(-1);
	}

	// prueba rmi

	try {
	    System.out.println("//" + maq + ":" + puerto + "/fotop2p");
	    InterfazRemota ir = (InterfazRemota)java.rmi.Naming.lookup("//" + maq + ":" + puerto + "/fotop2p");
	    System.err.println(ir);
	    SalidaDFS sal = ir.dfs_distribuido("-t y",new Vector<String>());
	    System.out.println(sal.resultado);
	}
	catch (NotBoundException e) {
	    System.err.println("No existe el servicio solicitado en " + maq + ":" + puerto);
	}
	catch (MalformedURLException m) {
	    System.err.println("URL incorrecta");
	}
	catch (RemoteException r) {
	    System.err.println("No se pudo establecer conexion con el servidor");
	    r.printStackTrace();
	}
	// fin prueba 

	//fotos cliente = new fotos(puerto,maq);
	//cliente.run();
	
    }
}