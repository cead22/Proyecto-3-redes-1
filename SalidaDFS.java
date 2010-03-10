import java.util.Vector;
/** Clase que guarda la salida de un def_distribuido */
public class SalidaDFS implements java.io.Serializable {
    /** Contiene datos de los archivos que hicieron
     * match con la busqueda */
    public String resultado;
    /** Contiene las direcciones ip de las maquinas
     * que fueron visitadas */
    public Vector<String> visitados;
    
    public SalidaDFS () {
	resultado = null;
	visitados = null;
    }
    
    public SalidaDFS (String r, Vector<String> v) {
	resultado = r;
	visitados = v;
    }
}