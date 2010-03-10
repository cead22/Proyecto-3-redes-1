all: nodo.class fotos.class explorador.class
 
nodo.class : nodo.java explorador.java
	javac -classpath nanoxml-lite-2.2.3.jar:. nodo.java
 
fotos.class : fotos.java 
	javac -classpath nanoxml-lite-2.2.3.jar:. fotos.java
 
explorador.class : explorador.java
	javac -classpath nanoxml-lite-2.2.3.jar:. explorador.java