import java.rmi.*;
import java.util.Vector;
import java.io.Serializable;

/** Interfaz con los métodos que se
 * pueden llamar remotamente */

public interface InterfazRemota extends Remote {
    public SalidaDFS dfs_distribuido (String busqueda, Vector<String> visitados) throws RemoteException;
}