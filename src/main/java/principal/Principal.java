package principal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
// HIBERNATE
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import jakarta.persistence.TypedQuery;
//MIS CLASES HIBERNATE
import clasesHibernate.Capitales;
import clasesHibernate.Poblaciones;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.FindIterable;
// MONGO
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public class Principal {

	// HIBERNATE
	private static final Configuration cfg = new Configuration().configure();
	private static final SessionFactory sf = cfg.buildSessionFactory();
	private static Session sesion;

	// MONGODB

	private static MongoDatabase database;
	private static MongoClient mongoClient;

	public static void main(String[] args) {

		/*
		 * PARA LEER LOS DATOS DE MYSQL System.out.println("----- Capitales ----- ");
		 * List<Capitales> listaCapitales = hibernateObtenerCapitales(); for (Capitales
		 * capital : listaCapitales) { System.out.println(capital.getNombre()); }
		 * 
		 * System.out.println("----- Poblaciones -----"); List<Poblaciones>
		 * listaPoblaciones = hibernateObtenerPoblaciones(); for (Poblaciones poblacion
		 * : listaPoblaciones) { System.out.println(poblacion.getNombre());
		 * 
		 * }
		 */

		mongoConectarOnline();

		menu();

		mongoCerrarConexion(); // Cerrar conexion MongoDB
		sesion.close(); // Cerrar conexion Hibernate

	}

	public static void menu() {

		Scanner sc = new Scanner(System.in);
		System.out.println("Introduce el nombre de la capital");
		String nombreCapital = sc.nextLine();

		metodoAglutinador(nombreCapital);

		sc.close();

	}

	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// -------------------- MÉTODO AGLUTINADOR --------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	public static void metodoAglutinador(String nombreCapital) {
		Capitales capitalAinsertar = new Capitales();

		capitalAinsertar.setNombre(nombreCapital);

		// ----------- LA CAPITAL ESTÁ EN MYSQL -----------
		if (hibernateExisteCapital(nombreCapital)) {
			System.out.println("La capital '" + nombreCapital + "' ya está en MySQL");

			// ------- BUSCAR POBLACIONES EN MySQL --------
			List<Poblaciones> listaPoblaciones = hibernateObtenerPoblaciones();

			for (Poblaciones poblacion : listaPoblaciones) {
				mongoComprobarOCrearPoblacion(poblacion);
			}

		}

		// ----------- LA CAPITAL NO ESTÁ EN MYSQL -----------
		else {

			if (hibernateInsertarCapital(capitalAinsertar)) {
				System.out.println("La capital '" + nombreCapital + "' se ha insertado en MySQL");
				// Comprobar si la capital existe en MongoDB y si no, crearla
				mongoComprobarOCrearCapital(capitalAinsertar);

			} else {
				System.out.println("Ha habido un problema al insertar '" + nombreCapital + "' en MySQL");
			}

		}

	}
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// -------------------- HIBERNATE ----------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------

	// Obtener todas las capitales, se puede usar para saber si están repetidas
	public static List<Capitales> hibernateObtenerCapitales() {
		try {
			sesion = sf.openSession();

			String hql = "FROM Capitales";
			TypedQuery<Capitales> consulta = sesion.createQuery(hql, Capitales.class);
			List<Capitales> capitales = consulta.getResultList();
			return capitales;
		} catch (Exception e) {
			return null;
		}
	}

	// Obtener todas las poblaciones, se puede usar para saber si están repetidas
	public static List<Poblaciones> hibernateObtenerPoblaciones() {
		try {
			sesion = sf.openSession();

			String hql = "FROM Poblaciones";
			TypedQuery<Poblaciones> consulta = sesion.createQuery(hql, Poblaciones.class);
			List<Poblaciones> poblaciones = consulta.getResultList();
			return poblaciones;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Le pasas el nombre de la capital, comprueba en la bbdd ese campo
	 * 
	 * @param nombreCapital
	 * @return true si existe, false si no
	 */
	public static boolean hibernateExisteCapital(String nombreCapital) {

		List<Capitales> listaCapitales = hibernateObtenerCapitales();

		for (Capitales capitalActual : listaCapitales) {

			if (capitalActual.getNombre().equals(nombreCapital))
				return true;

		}
		return false;

	}

	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// -------------------- CRUD MySQL ----------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------

	/**
	 * 
	 * Le pasas un objeto de capital y la inserta en hibernate
	 */
	public static boolean hibernateInsertarCapital(Capitales capital) {
		try {
			sesion = sf.openSession();
			Transaction tx = sesion.beginTransaction();

			sesion.persist(capital);

			tx.commit();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// Leer una capital por su nombre
	public static Capitales hibernateLeerCapital(String nombreCapital) {
		sesion = sf.openSession();
		Capitales capital = sesion.createQuery("FROM Capitales WHERE nombre = :nombre", Capitales.class)
				.setParameter("nombre", nombreCapital)
				.uniqueResult();
		sesion.close();
		return capital;
	}

	// Actualizar una capital
	public static boolean hibernateActualizarCapital(String nombreActual, String nombreNuevo) {
		try {
			sesion = sf.openSession();
			Transaction tx = sesion.beginTransaction();
			Capitales capital = sesion.createQuery("FROM Capitales WHERE nombre = :nombre", Capitales.class)
					.setParameter("nombre", nombreActual)
					.uniqueResult();
			if (capital != null) {
				capital.setNombre(nombreNuevo);
				sesion.merge(capital);
				tx.commit();
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// Eliminar una capital
	public static boolean hibernateEliminarCapital(String nombreCapital) {
		try {
			sesion = sf.openSession();
			Transaction tx = sesion.beginTransaction();
			Capitales capital = sesion.createQuery("FROM Capitales WHERE nombre = :nombre", Capitales.class)
					.setParameter("nombre", nombreCapital)
					.uniqueResult();
			if (capital != null) {
				sesion.remove(capital);
				tx.commit();
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// --------------------- MONGODB ------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------

	/**
	 * Conectar a la BBDD de
	 * https://cloud.mongodb.com/v2/65aea9f73f5c6d633cf14665#/clusters/detail/Cluster0
	 * 
	 * @return
	 */
	public static boolean mongoConectarOnline() {
		String connectionString = "mongodb+srv://mariomunozpequeno:4rtlus9pq7UjxKNO@cluster0.xu4apmq.mongodb.net/?retryWrites=true&w=majority";
		ServerApi serverApi = ServerApi.builder().version(ServerApiVersion.V1).build();
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(connectionString)).serverApi(serverApi).build();
		// Create a new client and connect to the server
		mongoClient = MongoClients.create(settings);
		try {
			// Send a ping to confirm a successful connection
			database = mongoClient.getDatabase("provinciasmongo");
			database.runCommand(new Document("ping", 1));
			System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Conectarse a la BBDD local de MongoDB
	 * 
	 * @return
	 */
	public static boolean mongoConectarLocal() {
		try {
			mongoClient = MongoClients.create();
			database = mongoClient.getDatabase("provinciasMongo");
			System.out.println("Te has conectado a MongoDB");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public static void mongoCerrarConexion() {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	public static List<Document> mongoObtenerCapitales() {
		ArrayList<Document> listaDocumentos = new ArrayList<Document>();

		FindIterable<Document> it = database.getCollection("capitales").find();

		Iterator<Document> iter = it.iterator();
		Document documento;

		while (iter.hasNext()) {
			documento = iter.next();
			listaDocumentos.add(documento);
			// System.out.println("Nombre: " + documento.getString("nombre"));
		}

		return listaDocumentos;
	}

	public static List<Document> mongoObtenerPoblaciones() {
		ArrayList<Document> listaDocumentos = new ArrayList<Document>();

		FindIterable<Document> it = database.getCollection("poblaciones").find();

		Iterator<Document> iter = it.iterator();
		Document documento;

		while (iter.hasNext()) {
			documento = iter.next();
			listaDocumentos.add(documento);
			// System.out.println("Nombre: " + documento.getString("nombre"));
		}

		return listaDocumentos;
	}

	/**
	 * Comprueba si el parámetro está en la tabla Capitales de MongoDB, si no está
	 * lo crea
	 * 
	 * @param capital
	 * @return
	 */
	public static boolean mongoComprobarOCrearCapital(Capitales capital) {
		// Obtener la colección "capitales" de la base de datos
		MongoCollection<Document> capitalesCollection = database.getCollection("capitales");

		// Buscar en la colección "capitales" un documento que tenga el mismo nombre que
		// la capital
		Document capitalExistente = capitalesCollection.find(Filters.eq("nombre", capital.getNombre())).first();

		// Si se encuentra un documento existente
		if (capitalExistente != null) {
			// Imprimir un mensaje informando que la capital ya existe en MongoDB
			System.out.println("La capital '" + capital.getNombre() + "' ya existe en MongoDB.");
			return true;
		}
		// Si no se encuentra un documento existente
		else {
			// Crear un nuevo documento con el nombre de la capital
			Document nuevaCapital = new Document().append("nombre", capital.getNombre());

			// Insertar el nuevo documento en la colección "capitales"
			capitalesCollection.insertOne(nuevaCapital);

			// Imprimir un mensaje informando que la capital ha sido creada en MongoDB
			System.out.println("La capital '" + capital.getNombre() + "' ha sido creada en MongoDB.");
			return false;
		}
	}

	/**
	 * Comprueba si el parámetro está en la tabla Poblaciones de MongoDB, si no está
	 * lo crea
	 * 
	 * @param nombrePoblacion
	 * @return
	 */
	public static boolean mongoComprobarOCrearPoblacion(Poblaciones poblacion) {
		MongoCollection<Document> poblacionesCollection = database.getCollection("poblaciones");
		MongoCollection<Document> capitalesCollection = database.getCollection("capitales");

		Document poblacionExistente = poblacionesCollection.find(Filters.eq("nombre", poblacion.getNombre())).first();

		if (poblacionExistente != null) {
			System.out.println("La población '" + poblacion.getNombre() + "' ya existe en MongoDB.");
			return true;
		} else {
			// Comprobar si la capital existe en MongoDB y si no, crearla
			mongoComprobarOCrearCapital(poblacion.getCapitales());

			// Buscar la capital en MongoDB y obtener su ObjectID
			Document capitalMongoDB = capitalesCollection
					.find(Filters.eq("nombre", poblacion.getCapitales().getNombre())).first();
			ObjectId capitalId = capitalMongoDB.getObjectId("_id");

			// Buscar la capital en MongoDB utilizando el ObjectId obtenido
			Document capitalComprobada = capitalesCollection.find(Filters.eq("_id", capitalId)).first();

			// Comprobar que el nombre de la capital obtenida coincide con el nombre de la
			// capital de la población
			if (!capitalComprobada.getString("nombre").equals(poblacion.getCapitales().getNombre())) {
				System.out.println("El ObjectId de la capital '" + poblacion.getCapitales().getNombre()
						+ "' no corresponde a la capital correcta en MongoDB para la población '"
						+ poblacion.getNombre() + "'.");

				// Corregir la referencia errónea
				Bson filter = Filters.eq("nombre", poblacion.getNombre());
				Bson updateOperation = Updates.set("capital", capitalId);
				poblacionesCollection.updateOne(filter, updateOperation);

				System.out.println(
						"La referencia errónea de la población '" + poblacion.getNombre() + "' ha sido corregida.");
			}

			Document nuevaPoblacion = new Document().append("nombre", poblacion.getNombre())
					.append("poblacion", poblacion.getCodPoblacion())
					.append("capital", capitalId); // Usar el ObjectID de la capital

			poblacionesCollection.insertOne(nuevaPoblacion);
			System.out.println("La población '" + poblacion.getNombre() + "' ha sido creada en MongoDB.");

			return false;
		}
	}

	public static void mongoComprobarOCrearPoblacionConIdNumerico(Poblaciones poblacion) {

		MongoCollection<Document> poblacionesCollection = database.getCollection("poblaciones");
		MongoCollection<Document> capitalesCollection = database.getCollection("capitales");

		// Buscar la población en MongoDB
		Document poblacionMongoDB = poblacionesCollection
				.find(Filters.eq("nombre", poblacion.getNombre())).first();

		// Si la población no existe, crearla
		if (poblacionMongoDB == null) {
			// Buscar la capital en MongoDB y obtener su ID numérico
			Document capitalMongoDB = capitalesCollection
					.find(Filters.eq("nombre", poblacion.getCapitales().getNombre())).first();
			int capitalId = capitalMongoDB.getInteger("id");

			Document nuevaPoblacion = new Document().append("nombre", poblacion.getNombre())
					.append("poblacion", poblacion.getCodPoblacion())
					.append("capital", capitalId); // Usar el ID numérico de la capital

			poblacionesCollection.insertOne(nuevaPoblacion);
		}

	}

	public static void mongoComprobarOCrearPoblacionConObjetoEmbebido(Poblaciones poblacion) {
		MongoCollection<Document> poblacionesCollection = database.getCollection("poblaciones");
		MongoCollection<Document> capitalesCollection = database.getCollection("capitales");

		// Buscar la población en MongoDB
		Document poblacionMongoDB = poblacionesCollection
				.find(Filters.eq("nombre", poblacion.getNombre())).first();

		// Si la población no existe, crearla
		if (poblacionMongoDB == null) {
			// Buscar la capital en MongoDB
			Document capitalMongoDB = capitalesCollection
					.find(Filters.eq("nombre", poblacion.getCapitales().getNombre())).first();

			Document nuevaPoblacion = new Document().append("nombre", poblacion.getNombre())
					.append("poblacion", poblacion.getCodPoblacion())
					.append("capital", capitalMongoDB); // Usar la capital como un subdocumento

			poblacionesCollection.insertOne(nuevaPoblacion);
		}
	}

	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------- CRUD MONGODB ---------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// ------------------------------------------------------------

	// ------------------- Está hecho con pronvincias pero se puede cambiar a lo que haga falta ------------------
	public Document leerProvincia(String nombre) {
		MongoCollection<Document> provinciasCollection = database.getCollection("provincias");
		Document provincia = provinciasCollection.find(Filters.eq("nombre", nombre)).first();
		return provincia;
	}

	public void borrarProvincia(String nombre) {
		MongoCollection<Document> provinciasCollection = database.getCollection("provincias");
		provinciasCollection.deleteOne(Filters.eq("nombre", nombre));
	}

	public void modificarProvincia(String nombre, String nuevoNombre) {
		MongoCollection<Document> provinciasCollection = database.getCollection("provincias");
		provinciasCollection.updateOne(Filters.eq("nombre", nombre), Updates.set("nombre", nuevoNombre));
	}

	public void crearProvincia(String nombre) {
		MongoCollection<Document> provinciasCollection = database.getCollection("provincias");
		Document nuevaProvincia = new Document("nombre", nombre);
		provinciasCollection.insertOne(nuevaProvincia);
	}


}
