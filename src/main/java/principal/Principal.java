package principal;

import java.util.List;
import java.util.Scanner;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import clasesHibernate.Capitales;
import clasesHibernate.Poblaciones;
import jakarta.persistence.TypedQuery;

public class Principal {

	// HIBERNATE
	private static final Configuration cfg = new Configuration().configure();
	private static final SessionFactory sf = cfg.buildSessionFactory();
	private static Session sesion;

	public static void main(String[] args) {
		
		/* PARA LEER LOS DATOS DE MYSQL
		System.out.println("----- Capitales ----- ");
		List<Capitales> listaCapitales = hibernateObtenerCapitales();
		for (Capitales capital : listaCapitales) {
			System.out.println(capital.getNombre());
		}
		
		System.out.println("----- Poblaciones -----");
		List<Poblaciones> listaPoblaciones = hibernateObtenerPoblaciones();
		for (Poblaciones poblacion : listaPoblaciones) {
			System.out.println(poblacion.getNombre());

		}
		*/

		menu();

	}

	public static void menu() {

		Scanner sc = new Scanner(System.in);
		System.out.println("Introduce el nombre de la capital");
		String nombreCapital = sc.nextLine();
		
		metodoAglutinador(nombreCapital);
		
	}

	public static void metodoAglutinador(String nombreCapital) {
		Capitales capitalAinsertar = new Capitales();

		capitalAinsertar.setNombre(nombreCapital);

		// ----------- LA CAPITAL ESTÁ EN MYSQL -----------
		if(hibernateExisteCapital(nombreCapital)) {
			System.out.println("La capital '" + nombreCapital + "' ya está en MySQL");
		}
		
		// ----------- LA CAPITAL NO ESTÁ EN MYSQL -----------
		else {
			
			if(hibernateInsertarCapital(capitalAinsertar)) {
				System.out.println("La capital '" + nombreCapital + "' se ha insertado en MySQL");
			}else {
				System.out.println("Ha habido un problema al insertar '" + nombreCapital + "' en MySQL");
			}
			
		}
		
	}
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	// -------------------- HIBERNATE ----------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------
	//------------------------------------------------------------

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
		} finally {
			if (sesion != null) {
				sesion.close();
			}
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
		} finally {
			if (sesion != null) {
				sesion.close();
			}
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
			
			if(capitalActual.getNombre().equals(nombreCapital))
				return true;
			
		}
		return false;
		
	}
	
	
	
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
		} finally {
			if (sesion != null) {
				sesion.close();
			}
		}
	}

	/**
	 * 
	 * Le pasas un objeto de capital y la inserta en hibernate
	 */
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
		} finally {
			if (sesion != null) {
				sesion.close();
			}
		}
	}

	/**
	 * Método que realiza una consulta simple a la base de datos y luego imprime los
	 * resultados.
	 * 
	 * public static void hibernateImprimirCapitales() { try { // Abrir sesion
	 * sesion = sf.openSession();
	 * 
	 * String hql = "FROM Capitales"; TypedQuery<Capitales> consulta =
	 * sesion.createQuery(hql, Capitales.class); List<Capitales> capitales =
	 * consulta.getResultList();
	 * 
	 * for (Capitales capital : capitales) {
	 * System.out.println(capital.getNombre()); }
	 * 
	 * } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
	 * finally { if (sesion != null) { sesion.close(); } } }
	 */

}
