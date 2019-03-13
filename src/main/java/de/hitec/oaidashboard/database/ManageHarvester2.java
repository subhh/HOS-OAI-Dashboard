package de.hitec.oaidashboard.database;

import de.hitec.oaidashboard.database.datastructures2.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ManageHarvester2 {

	private static final String SCRIPT_FILE = "exportSchemaScript.sql";
	private static final String METHA_ID_PATH = "/usr/sbin/metha-id";
	private static final String METHA_SYNC_PATH = "/usr/sbin/metha-sync";
	
	// Here, metha-sync will place it's files (*.xml.gz) 
	private static final String EXPORT_DIRECTORY = "/tmp/harvest";
	
	// Then, we will copy them here, and let git manage them.
	// Also, the metha-id answer will be stored here. 
//	private static final String GIT_DIRECTORY = "/data";
	private static final String GIT_DIRECTORY = "/tmp/oai_git";
	private static final boolean RESET_DATABASE = true;
	// This flag is useless for production (must always be true),
	// but very useful for debugging, as harvesting may take a lot of time.
	private static final boolean REHARVEST = true;
	private static SessionFactory factory;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    private static SchemaExport getSchemaExport() {
		SchemaExport export = new SchemaExport();
		// Script file.		
		File outputFile = new File(SCRIPT_FILE);
		if (outputFile.exists()) { outputFile.delete(); }
		String outputFilePath = outputFile.getAbsolutePath();
		System.out.println("Export file: " + outputFilePath);
		export.setDelimiter(";");
		export.setOutputFile(outputFilePath);
		// No Stop if Error
		export.setHaltOnError(false);
		return export;
	}

	private static void saveBasicRepoInfo(String name, String url) {
		Session session = factory.openSession();
		Transaction tx = null;

		try {
			tx = session.beginTransaction();
			Repository repo = new Repository(name, url);
			session.save(repo); 
			tx.commit();
		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
			e.printStackTrace(); 
		} finally {
			session.close(); 
		}
	}

	private static ArrayList<Repository> getActiveReposFromDB() {
		ArrayList<Repository> repositories = null;
		Session session = factory.openSession();
		Transaction tx = null;
			
		try {
			tx = session.beginTransaction();
			
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Repository> criteria = builder.createQuery(Repository.class);

			Root<Repository> root = criteria.from(Repository.class);
			criteria.select(root).where(builder.equal(root.get("state"), "ACTIVE"));
			Query<Repository> q = session.createQuery(criteria);		
			repositories = (ArrayList<Repository>) q.getResultList();

			tx.commit();
		} catch (HibernateException e) {
			if (tx != null) {
                tx.rollback();
            }
			e.printStackTrace(); 
		} finally {
			session.close(); 
		}
		return repositories;
	}

	public static void dropDataBase(SchemaExport export, Metadata metadata) {
		// TargetType.DATABASE - Execute on Database
		// TargetType.SCRIPT - Write Script file.
		// TargetType.STDOUT - Write log to Console.
		EnumSet<TargetType> targetTypes = EnumSet.of(
				TargetType.DATABASE, TargetType.SCRIPT, TargetType.STDOUT);
		export.drop(targetTypes, metadata);
	}

	public static void createDataBase(SchemaExport export, Metadata metadata) {
		// TargetType.DATABASE - Execute on Databse
		// TargetType.SCRIPT - Write Script file.
		// TargetType.STDOUT - Write log to Console.
		EnumSet<TargetType> targetTypes = EnumSet.of(
				TargetType.DATABASE, TargetType.SCRIPT, TargetType.STDOUT);
		SchemaExport.Action action = SchemaExport.Action.CREATE;
		export.execute(targetTypes, action, metadata);
		System.out.println("Export OK");
	}

	private static void resetDatabase(SchemaExport export, Metadata metadata) {
		logger.info("Dropping all tables of database...");
		dropDataBase(export, metadata);
		logger.info("Creating all tables  Database...");
		createDataBase(export, metadata);
		logger.info("Setting up default repositories...");
		setUpDefaultRepositories();
	}

	private static void initDatabase() {
		// read config and create necessary objects
		String configFileName = "hibernate.cfg.xml";
		ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.configure(configFileName).build();
		Metadata metadata = new MetadataSources(serviceRegistry)
				.getMetadataBuilder().build();
		SchemaExport export = getSchemaExport();

		// init buildSessionFactory
		try {
			factory = metadata.buildSessionFactory();
		} catch (Throwable ex) {
			System.err.println("Failed to create sessionFactory object." + ex);
			throw new ExceptionInInitializerError(ex);
		}

		// reset database when appropriate setting is true
		if (RESET_DATABASE) {
			resetDatabase(export, metadata);
		}
	}

	private static void setUpDefaultRepositories() {
		saveBasicRepoInfo("tub.dok", "http://tubdok.tub.tuhh.de/oai/request");
		saveBasicRepoInfo("Elektronische Dissertationen Universit&auml;t Hamburg, GERMANY", "http://ediss.sub.uni-hamburg.de/oai2/oai2.php");
		saveBasicRepoInfo("OPuS \\u00e2\\u0080\\u0093 Volltextserver der HCU", "http://edoc.sub.uni-hamburg.de/hcu/oai2/oai2.php");
		saveBasicRepoInfo("Beispiel-Volltextrepository", "http://edoc.sub.uni-hamburg.de/hsu/oai2/oai2.php");
		saveBasicRepoInfo("HAW OPUS","http://edoc.sub.uni-hamburg.de/haw/oai2/oai2.php");
	}

	public static void main(String[] args) throws IOException {
		initDatabase();

		List<Repository> repositories = getActiveReposFromDB();
		if (repositories != null) {

			// First step: MultiThreaded collection of data (Json, XML etc.)
			Map<Repository, DataHarvester> repoHarvesterMap = harvestData(repositories);

			// Second step: SingleThreaded instantiation of Model and saving to Database
			instantiateAndSaveModels(repoHarvesterMap);

			logger.info("Finished.");
		}
	}

	private static Map<Repository, DataHarvester> harvestData(List<Repository> repositories) {
    	Map<Repository, DataHarvester> repoHarvesterMap = new HashMap<>();

		for(Repository repo : repositories) {
			DataHarvester dataHarvester = new DataHarvester(repo.getHarvestingUrl(), METHA_ID_PATH, METHA_SYNC_PATH,
					GIT_DIRECTORY, EXPORT_DIRECTORY);
			//harv.reharvest = REHARVEST;
			repoHarvesterMap.put(repo, dataHarvester);
			dataHarvester.start();
		}
		logger.info("Waiting for DataHarvesters to finish ...");
		for (DataHarvester dataHarvester : repoHarvesterMap.values()) {
			try	{
				dataHarvester.t.join();
				// TODO: if no success: generate failed state and save to DB
			}

			catch (Exception ex) {
				logger.error("An error occurred while harvesting data from Harvesting-URL: {}", dataHarvester.getHarvestingURL(), ex);
			}
		}
		return repoHarvesterMap;
	}

	private static void instantiateAndSaveModels(Map<Repository, DataHarvester> repoHarvesterMap) {
    	for(Map.Entry entry: repoHarvesterMap.entrySet()) {
    		Repository repository = (Repository) entry.getKey();
    		DataHarvester dataHarvester = (DataHarvester) entry.getValue();
    		HarvestingDataModel harvestingDataModel = new HarvestingDataModel(repository, dataHarvester, factory);

			/**
			 * IMPORTANT: the saving operation should always be done directly after instantiating a new HarvestingDataModel-Object
			 * or before creating a new one.
			 * If you create multiple HarvestingDataModels without saving in between, you can easily create inconsistencies in the Database,
			 * for example doublets of MetadataFormats, Sets, Records etc.
			 */
    		harvestingDataModel.saveDataModel();
		}
	}
}


