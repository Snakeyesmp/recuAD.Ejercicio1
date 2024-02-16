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
		mongoConectarOnline();

		Scanner sc = new Scanner(System.in);
		System.out.println("Introduce el nombre de la capital");
		String nombreCapital = sc.nextLine();

		metodoAglutinador(nombreCapital);

		// migrarMongoDB(nombreCapital);// ESTE ES EL QUE HACE CHATGPT

		sc.close();
		mongoCerrarConexion(); // Cerrar conexion MongoDB
		sesion.close(); // Cerrar conexion Hibernate
	}

	// ------------------------------------------------------------
	// -------------------- MÉTODO AGLUTINADOR --------------------
	// ------------------------------------------------------------
	public static void metodoAglutinador(String nombreCapital) {
		Capitales capitalAinsertar = new Capitales();

		capitalAinsertar.setNombre(nombreCapital);

		// ----------- LA CAPITAL ESTÁ EN MYSQL -----------
		if (hibernateExisteCapital(nombreCapital)) {
			System.out.println("La capital '" + nombreCapital + "' ya está en MySQL");

			List<Poblaciones> listaPoblaciones = hibernateObtenerPoblaciones();
			for (Poblaciones poblacion : listaPoblaciones) {
				mongoComprobarOCrearPoblacion(poblacion);
			}

		}

		// ----------- LA CAPITAL NO ESTÁ EN MYSQL -----------
		else {

			if (hibernateInsertarCapital(capitalAinsertar)) {
				System.out.println("La capital '" + nombreCapital + "' se ha insertado en MySQL");

				mongoComprobarOCrearCapital(capitalAinsertar);

			} else {
				System.out.println("Ha habido un problema al insertar '" + nombreCapital + "' en MySQL");
			}

		}

	}

	// ------------------------------------------------------------
	// --------------------- HIBERNATE ----------------------------
	// ------------------------------------------------------------
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

	public static boolean hibernateExisteCapital(String nombreCapital) {

		List<Capitales> listaCapitales = hibernateObtenerCapitales();
		for (Capitales capitalActual : listaCapitales) {

			if (capitalActual.getNombre().equals(nombreCapital))
				return true;
		}
		return false;
	}

	// ------------------------------------------------------------
	// -------------------- CRUD MySQL ----------------------------
	// ------------------------------------------------------------

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

	public static Capitales hibernateLeerCapital(String nombreCapital) {
		sesion = sf.openSession();
		Capitales capital = sesion.createQuery("FROM Capitales WHERE nombre = :nombre", Capitales.class)
				.setParameter("nombre", nombreCapital)
				.uniqueResult();
		return capital;
	}

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
	// ----------------------- MONGODB ----------------------------
	// ------------------------------------------------------------

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

	public static boolean mongoComprobarOCrearCapital(Capitales capital) {

		MongoCollection<Document> capitalesCollection = database.getCollection("capitales");

		Document capitalExistente = capitalesCollection.find(Filters.eq("nombre", capital.getNombre())).first();

		if (capitalExistente != null) {
			System.out.println("La capital '" + capital.getNombre() + "' ya existe en MongoDB.");
			return true;
		} else {
			Document nuevaCapital = new Document().append("nombre", capital.getNombre());

			capitalesCollection.insertOne(nuevaCapital);

			System.out.println("La capital '" + capital.getNombre() + "' ha sido creada en MongoDB.");
			return false;
		}
	}

	public static boolean mongoComprobarOCrearPoblacion(Poblaciones poblacion) {
		MongoCollection<Document> poblacionesCollection = database.getCollection("poblaciones");
		MongoCollection<Document> capitalesCollection = database.getCollection("capitales");

		Document poblacionExistente = poblacionesCollection.find(Filters.eq("nombre", poblacion.getNombre())).first();

		if (poblacionExistente != null) {
			System.out.println("La población '" + poblacion.getNombre() + "' ya existe en MongoDB.");
			return true;
		} else {
			mongoComprobarOCrearCapital(poblacion.getCapitales());

			Document capitalMongoDB = capitalesCollection
					.find(Filters.eq("nombre", poblacion.getCapitales().getNombre())).first();
			ObjectId capitalId = capitalMongoDB.getObjectId("_id");

			Document capitalComprobada = capitalesCollection.find(Filters.eq("_id", capitalId)).first();

			if (!capitalComprobada.getString("nombre").equals(poblacion.getCapitales().getNombre())) {
				System.out.println("El ObjectId de la capital '" + poblacion.getCapitales().getNombre()
						+ "' no corresponde a la capital correcta en MongoDB para la población '"
						+ poblacion.getNombre() + "'.");

				Bson filter = Filters.eq("nombre", poblacion.getNombre());
				Bson updateOperation = Updates.set("capital", capitalId);
				poblacionesCollection.updateOne(filter, updateOperation);

				System.out.println(
						"La referencia errónea de la población '" + poblacion.getNombre() + "' ha sido corregida.");
			}

			Document nuevaPoblacion = new Document().append("nombre", poblacion.getNombre())
					.append("poblacion", poblacion.getHabitantes())
					.append("capital", capitalId);

			poblacionesCollection.insertOne(nuevaPoblacion);
			System.out.println("La población '" + poblacion.getNombre() + "' ha sido creada en MongoDB.");

			return false;
		}
	}

	public static void mongoComprobarOCrearPoblacionConIdNumerico(Poblaciones poblacion) {

		MongoCollection<Document> poblacionesCollection = database.getCollection("poblaciones");
		MongoCollection<Document> capitalesCollection = database.getCollection("capitales");

		Document poblacionMongoDB = poblacionesCollection
				.find(Filters.eq("nombre", poblacion.getNombre())).first();

		if (poblacionMongoDB == null) {
			Document capitalMongoDB = capitalesCollection
					.find(Filters.eq("nombre", poblacion.getCapitales().getNombre())).first();
			int capitalId = capitalMongoDB.getInteger("id");

			Document nuevaPoblacion = new Document().append("nombre", poblacion.getNombre())
					.append("poblacion", poblacion.getCodPoblacion())
					.append("capital", capitalId);
			poblacionesCollection.insertOne(nuevaPoblacion);
		}

	}

	public static void mongoComprobarOCrearPoblacionConObjetoEmbebido(Poblaciones poblacion) {
		MongoCollection<Document> poblacionesCollection = database.getCollection("poblaciones");
		MongoCollection<Document> capitalesCollection = database.getCollection("capitales");

		Document poblacionMongoDB = poblacionesCollection
				.find(Filters.eq("nombre", poblacion.getNombre())).first();

		if (poblacionMongoDB == null) {
			Document capitalMongoDB = capitalesCollection
					.find(Filters.eq("nombre", poblacion.getCapitales().getNombre())).first();

			Document nuevaPoblacion = new Document().append("nombre", poblacion.getNombre())
					.append("poblacion", poblacion.getCodPoblacion())
					.append("capital", capitalMongoDB);

			poblacionesCollection.insertOne(nuevaPoblacion);
		}
	}

	// ------------------------------------------------------------
	// ------------------- CRUD MONGODB ---------------------------
	// ------------------------------------------------------------
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

	// ------------------------------------------------------------
	// ------------------- CHATGPT MOMENTO ------------------------
	// ------------------------------------------------------------

	public static void migrarMongoDB(String nombreCapital) {
		Capitales capitalAInsertar = new Capitales();
		capitalAInsertar.setNombre(nombreCapital);

		// Si la capital no existe en MySQL, la insertamos
		if (!hibernateExisteCapital(nombreCapital)) {
			if (hibernateInsertarCapital(capitalAInsertar)) {
				System.out.println("La capital '" + nombreCapital + "' se ha insertado en MySQL");
			} else {
				System.out.println("Ha habido un problema al insertar '" + nombreCapital + "' en MySQL");
				return;
			}
		}

		// Si la capital no existe en MongoDB, la insertamos
		if (!mongoComprobarOCrearCapital(capitalAInsertar)) {
			System.out.println("La capital '" + nombreCapital + "' ha sido creada en MongoDB");
		} else {
			System.out.println("La capital '" + nombreCapital + "' ya existe en MongoDB");
		}

		// Si la capital existe en MySQL, obtenemos sus poblaciones
		List<Poblaciones> listaPoblaciones = hibernateObtenerPoblaciones();
		for (Poblaciones poblacion : listaPoblaciones) {
			// Comprobamos la existencia de cada población en MongoDB
			mongoComprobarOCrearPoblacion(poblacion);
		}

		// Si la capital existe en MongoDB, obtenemos sus poblaciones
		List<Document> poblacionesMongoDB = mongoObtenerPoblaciones();
		for (Document poblacionMongoDB : poblacionesMongoDB) {
			// Comprobamos la existencia de cada población en MySQL
			String nombrePoblacion = poblacionMongoDB.getString("nombre");
			Poblaciones poblacionMySQL = hibernateLeerPoblacion(nombrePoblacion);
			if (poblacionMySQL == null) {
				// Si la población no existe en MySQL, la creamos
				Poblaciones nuevaPoblacion = new Poblaciones();
				nuevaPoblacion.setNombre(nombrePoblacion);
				nuevaPoblacion.setHabitantes(poblacionMongoDB.getInteger("poblacion"));
				// Obtenemos la capital correspondiente y la asignamos a la población
				String nombreCapitalMongoDB = poblacionMongoDB.getString("capital");
				Capitales capitalPoblacion = hibernateLeerCapital(nombreCapitalMongoDB);
				nuevaPoblacion.setCapitales(capitalPoblacion);
				hibernateInsertarPoblacion(nuevaPoblacion);
				System.out.println("La población '" + nombrePoblacion + "' ha sido creada en MySQL");
			} else {
				// Si la población existe en MySQL, comprobamos su referencia a la capital
				String nombreCapitalPoblacion = poblacionMySQL.getCapitales().getNombre();
				if (!nombreCapitalPoblacion.equals(nombreCapital)) {
					// Si la referencia a la capital no es la adecuada, la modificamos
					poblacionMySQL.setCapitales(capitalAInsertar);
					hibernateActualizarPoblacion(poblacionMySQL);
					System.out.println(
							"La referencia de la población '" + nombrePoblacion + "' ha sido corregida en MySQL");
				}
			}
		}
	}

	public static Poblaciones hibernateLeerPoblacion(String nombrePoblacion) {
		try {
			sesion = sf.openSession();
			Poblaciones poblacion = sesion.createQuery("FROM Poblaciones WHERE nombre = :nombre", Poblaciones.class)
					.setParameter("nombre", nombrePoblacion)
					.uniqueResult();
			return poblacion;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean hibernateInsertarPoblacion(Poblaciones poblacion) {
		try {
			sesion = sf.openSession();
			Transaction tx = sesion.beginTransaction();
			sesion.persist(poblacion);
			tx.commit();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean hibernateActualizarPoblacion(Poblaciones poblacion) {
		try {
			sesion = sf.openSession();
			Transaction tx = sesion.beginTransaction();
			sesion.merge(poblacion);
			tx.commit();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}