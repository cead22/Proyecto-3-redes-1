import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.*;


/**
 * Clase vacia para representar el cliente que solicita las fotos
 * @author Carlos Alvarez y Marion CArambula.
 */

public class fotos {
    
    /** Crea una instancia de la clase fotos */
    fotos(){}

    /** 
     * Verifica una cadena de caracteres que representa un   
     * comando y ejecuta la accion pertinente.
     * @param comando String que indica la accion a tomar.
     * @param interfaz Interfaz con comandos que se pueden ejecutar
     * remotamente
     */
    private void verificar_comando(String comando, InterfazRemota interfaz) {
	String cmd[] = comando.split("[\\s]+");
	String aux[];
	String servidor;
	String archivo;

	if (cmd[0].equalsIgnoreCase("C") && cmd.length == 3){
	    if (cmd[1].equalsIgnoreCase("-t") || cmd[1].equalsIgnoreCase("-k")){
		try {
		    SalidaDFS salida = interfaz.dfs_distribuido(cmd[1] + " " + cmd[2], new Vector<String>());
		    System.out.println(salida.resultado);
		}
		catch (java.rmi.RemoteException r) {
		    System.err.println("No se pudo establecer conexion con el servidor");
		}
		finally {
		    return;
		}
	    }
	    /* Comando invalido */
	    else System.out.println("Codigo Invalido");
	}
	/* Solicitar una foto */
	else if (cmd[0].equalsIgnoreCase("D") && cmd.length == 2 && cmd[1].matches("[\\S]+[:][\\S]+jpg")){
	    aux = cmd[1].split(":");
	    servidor = aux[0];
	    archivo = aux[1];
	    try {
		FileOutputStream fos = new FileOutputStream("./" + archivo);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		byte [] foto = interfaz.archivo_a_bytes(archivo);
		if (foto == null) {
		    System.out.println("Foto no encontrada o error al abrirla en el servidor");
		}
		else {
		    bos.write(foto,0,foto.length);
		}
	    }
	    catch (java.rmi.RemoteException r) {
		System.err.println("No se pudo establecer conexion con el servidor");
	    }
	    catch(IOException i) {
		System.err.println("Error al escribir el archivo");
	    }
	    finally {
		return;
	    }
	}
	/* obterner numero de alcanzables */
	else if (cmd[0].equalsIgnoreCase("A") && cmd.length == 1){
	    try {
		Vector<String> alc = interfaz.alcanzables(new Vector<String>());
		System.out.println("Alcanzables: " + alc.size());
	    }
	    catch (java.rmi.RemoteException r) {
		System.err.println("No se pudo establecer conexion con el servidor");
	    }
	    finally {
		return;
	    }
	}
	/* Salir */
	else if (cmd[0].equalsIgnoreCase("Q") && cmd.length == 1){
	    System.out.println("Chao");
	    System.exit(0);
	}
       	else {
	    System.out.println("Comando Invalido");
	    return;
	}
    }

    /**
     * Provee un medio para realizar consultas 
     * sobre fotos, que se encuentran en una 'base de datos'
     * fotografica distribuida, y permite descargarlas.
     */
    public void run(InterfazRemota interfaz) {
	String con = null;
	String [] cmd;
	BufferedReader br;
	Boolean b;
	String mensaje;
	
	mensaje = null;
	do {
	    /* Se obtiene el comando a ejecutar de la entrada estandar */
	    try {
		br = new BufferedReader(new InputStreamReader(System.in));
		mensaje = br.readLine();
		verificar_comando(mensaje, interfaz);
	    }
	    catch (IOException e) {
		System.err.println("Error al leer de la entrada estandar");
		//System.exit(-1);
	    }
	} while (true);
    }
	
 
    /** Revisa los parametros de la llamada y lee de la consola
     * los comandos a enviar a la aplicacion nodos */
    public static void main(String args[]){
	int puerto = 0;
	String maq = null;
	InterfazRemota interfaz = null;

	/* Revision de llamada */
	if (args.length != 4) {
	    System.out.println("Uso: fotos -s <servidor> -p <puerto>");
	    System.exit(-1);
	}

	if (args[0].equals("-s")  && args[2].equals("-p")) {
	    try {
		puerto = Integer.valueOf(args[3]);
	    }
	    catch(NumberFormatException e){
		System.out.println("El puerto debe ser un numero entero entre 1025 y 65535");
	    }
	    maq = args[1];
	}
	else if (args[0].equals("-p") && args[2].equals("-s")) {
	    try {
		puerto = Integer.valueOf(args[1]);
	    }
	    catch(NumberFormatException e){
		System.out.println("El puerto debe ser un numero entero entre 1025 y 65535");
	    }
	}
	else {
	    System.out.println("Uso: edolab -f <maquinas> -p <puertoRemote>\n");
	    System.exit(-1);
	}

	/* Obtener interfaz con metodos remotos */
	try {
	    System.out.println("//" + maq + ":" + puerto + "/fotop2p");
	    interfaz = (InterfazRemota)java.rmi.Naming.lookup("//" + maq + ":" + puerto + "/fotop2p");
	}
	catch (NotBoundException e) {
	    System.err.println("No existe el servicio solicitado en " + maq + ":" + puerto);
	}
	catch (MalformedURLException m) {
	    System.err.println("No se pudo establecer conexion con el servidor: URL incorrecta");
	}
	catch (java.rmi.RemoteException r) {
	    System.err.println("No se pudo establecer conexion con el servidor");
	}
      
	fotos cliente = new fotos();
	cliente.run(interfaz);
    }
}