package principal;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQMetaData;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import net.xqj.exist.ExistXQDataSource;

public class ModeloXQJ {

    private static final String URI = "xmldb:exist://localhost:8080";
    private static final String USUARIO = "admin";
    private static final String CONTRASENA = "admin";

    private static MongoDatabase db;
    private static MongoClient mongoClient;

    private static SessionFactory sf;
    private static Session sesion;

    public ModeloXQJ() {
    }

    public ModeloXQJ(MongoDatabase db, MongoClient mongoClient, Session sesionXqj,
            SessionFactory sfXqj) {
        this.db = db;
        this.mongoClient = mongoClient;
        this.sesion = sesionXqj;
        this.sf = sfXqj;
    }

    public static XQConnection getCon() {
        try {
            XQDataSource xqds = new ExistXQDataSource();
            // .newInstance deprecated
            // XQDataSource xqds = (XQDataSource)
            // Class.forName("net.xqj.exist.ExistXQDataSource").newInstance();
            xqds.setProperty("serverName", "localhost");
            xqds.setProperty("port", "8080");
            xqds.setProperty("user", USUARIO);
            xqds.setProperty("password", CONTRASENA);
            XQConnection cnn = xqds.getConnection();
            return (XQConnection) cnn;
        } catch (XQException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static void metaInformacion() {

        try {
            XQMetaData xqmd = getCon().getMetaData();
            System.out.println("nombre de usuario: " + xqmd.getUserName() +
                    "\nSoportar transacciones: " + xqmd.isTransactionSupported() +
                    "\nSoportar XQuery: " + xqmd.isXQueryXSupported());
        } catch (XQException e) {
            e.printStackTrace();
        }

    }

    public static void probarConexion() {
        try {
            XQConnection conn = getCon();
            if (conn != null) {
                XQMetaData metaData = conn.getMetaData();
                System.out.println("Conexión exitosa. Usuario: " + metaData.getUserName());
            } else {
                System.out.println("No se pudo establecer la conexión.");
            }
        } catch (XQException e) {
            System.out.println("Error al probar la conexión: " + e.getMessage());
        }
    }

    private static Collection conectarExistDB() throws XMLDBException {
        // Registrar el controlador de la base de datos
        String driver = "org.exist.xmldb.DatabaseImpl";
        try {
            Class<?> cl = Class.forName(driver);
            Database database = (Database) cl.getDeclaredConstructor().newInstance();
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }

        // Establecer la conexión
        Collection coleccion = DatabaseManager.getCollection(URI + "/exist/xmlrpc/db", USUARIO, CONTRASENA);
        return coleccion;
    }

    public static void migrarDesdeMongoDB() {
        // Establecer conexión a MongoDB
        MongoCollection<Document> poblacionesCollection = db.getCollection("poblaciones");

        // Establecer conexión a eXistDB
        Collection existCollection = null;
        try {
            existCollection = conectarExistDB();
            if (existCollection != null) {
                // Iterar sobre los documentos en MongoDB
                MongoCursor<Document> cursor = poblacionesCollection.find().iterator();
                while (cursor.hasNext()) {
                    Document poblacionDoc = cursor.next();
                    String nombre = poblacionDoc.getString("nombre");
                    int poblacion = poblacionDoc.getInteger("poblacion");
                    String capitalId = poblacionDoc.getString("capital");

                    // Construir el documento XML para almacenar en eXistDB
                    String xmlDocumento = String.format(
                            "<poblacion><nombre>%s</nombre><habitantes>%d</habitantes><capital>%s</capital></poblacion>",
                            nombre, poblacion, capitalId);

                    // Almacenar el documento en eXistDB
                    Resource documentoExist = existCollection.createResource(null, "XMLResource");
                    documentoExist.setContent(xmlDocumento);
                    existCollection.storeResource(documentoExist);
                }
                System.out.println("Datos migrados con éxito de MongoDB a eXistDB.");
            }
        } catch (XMLDBException e) {
            e.printStackTrace();
        }
    }

    private static Collection getCol(String URICol) {
        try {
            Class cl = Class.forName("org.exist.xmldb.DatabaseImpl");
            Database database = (Database) cl.getDeclaredConstructor().newInstance();
            DatabaseManager.registerDatabase(database);
            return DatabaseManager.getCollection("xmldb:exist://localhost:8080/exist/xmlrpc" + URICol,
                    USUARIO, CONTRASENA);

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException | XMLDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void crearColeccion(String URICol, String nameCol) {

        try {
            // con el metodo get con cogemos lacoleccion del padre
            Collection colPadre = getCol(URICol);
            if (colPadre == null) {
                System.out.println("La coleccion padre no existe");
            } else {
                // a partir de la ruta padre creamos en ella un collection a partir de ella y el
                // nombre pasado como parametro
                CollectionManagementService serv = (CollectionManagementService) colPadre
                        .getService("CollectionManagementService", "1.0");
                serv.createCollection(nameCol);
            }

        } catch (XMLDBException e) {
            e.printStackTrace();
        }

    }

    public static void borrarColeccion(String URICol, String nameCol) {

        try {
            // con el metodo get con cogemos lacoleccion del padre
            Collection colPadre = getCol(URICol);
            if (colPadre == null) {
                System.out.println("La coleccion padre no existe");
            } else {
                CollectionManagementService serv = (CollectionManagementService) colPadre
                        .getService("CollectionManagementService", "1.0");
                serv.removeCollection(nameCol);
            }
        } catch (XMLDBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    public static void exportarMongoDBToExistDB() {
        try {
            // Establecer conexión con eXistDB
            XQConnection connection = getCon();
            XQExpression expression = connection.createExpression();

            // Obtener la colección "poblaciones" de MongoDB
            MongoCollection<Document> poblacionesCollection = db.getCollection("poblaciones");
            MongoCollection<Document> capitalesCollection = db.getCollection("capitales"); // Añadido
            FindIterable<Document> poblacionesIt = poblacionesCollection.find();
            Iterator<Document> poblacionesIter = poblacionesIt.iterator();

            while (poblacionesIter.hasNext()) {
                Document poblacionDoc = poblacionesIter.next();
                String nombre = poblacionDoc.getString("nombre");
                int poblacion = poblacionDoc.getInteger("poblacion");
                ObjectId capitalIdObj = poblacionDoc.getObjectId("capital"); // Obtener el ObjectId

                // Obtener el nombre de la capital a partir del ObjectId
                Document capitalDoc = capitalesCollection.find(Filters.eq("_id", capitalIdObj)).first(); // Modificado
                String capitalNombre = capitalDoc != null ? capitalDoc.getString("nombre") : null;

                // Imprimir los valores para depurar
                System.out.println("Nombre: " + nombre);
                System.out.println("Población: " + poblacion);
                System.out.println("Nombre de la capital: " + capitalNombre);

                // Verificar si los valores necesarios no son nulos
                if (nombre != null && poblacion != 0 && capitalNombre != null) {
                    // Construir la expresión XQuery para insertar la población en eXistDB
                    String query = String.format(
                            "update insert "
                                    + "<poblacion>"
                                    + "    <nombre>%s</nombre>"
                                    + "    <habitantes>%d</habitantes>"
                                    + "    <capital>%s</capital>"
                                    + "</poblacion> into doc(\"/db/provincias/localidades.xml\")/localidades",
                            nombre, poblacion, capitalNombre);

                    // Ejecutar la expresión XQuery
                    expression.executeCommand(query);

                    // Imprimir un mensaje para indicar que la población se ha migrado correctamente
                    System.out.println("Población migrada correctamente a eXistDB: " + nombre);
                } else {
                    System.out.println("Alguno de los valores necesarios para añadir la población es nulo.");
                }
            }

            // Cerrar la conexión
            connection.close();
        } catch (XQException e) {
            e.printStackTrace();
        }
    }

    public static void crearDocumentoLocalidades() {
        try {
            // Establecer conexión con eXistDB
            XQConnection connection = getCon();
            XQExpression expression = connection.createExpression();

            // Construir la expresión XQuery para crear el documento
            String query = "xquery version \"3.1\";\n"
                    + "let $doc := <localidades/>\n"
                    + "return\n"
                    + "    xmldb:store(\"/db/provincias\", \"localidades.xml\", $doc)";

            // Ejecutar la expresión XQuery
            expression.executeCommand(query);

            // Imprimir un mensaje para indicar que el documento se ha creado correctamente
            System.out.println("Documento localidades.xml creado correctamente en eXistDB.");

            // Cerrar la conexión
            connection.close();
        } catch (XQException e) {
            e.printStackTrace();
        }
    }

}
