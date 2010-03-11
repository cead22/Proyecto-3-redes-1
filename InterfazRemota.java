import java.io.Serializable;

/** Interfaz con los métodos que se
 * pueden llamar remotamente */

public interface InterfazRemota extends java.rmi.Remote {
    public SalidaDFS dfs_distribuido (String busqueda, java.util.Vector<String> visitados) throws java.rmi.RemoteException;
    public java.util.Vector<String> alcanzables (java.util.Vector<String> visitados) throws java.rmi.RemoteException;
    public byte[] archivo_a_bytes(String archivo) throws java.rmi.RemoteException;	
}
