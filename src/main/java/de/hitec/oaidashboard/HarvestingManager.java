package de.hitec.oaidashboard;

import de.hitec.oaidashboard.aggregation.DataAggregator;
import de.hitec.oaidashboard.database.DataModelCreator;
import de.hitec.oaidashboard.database.datastructures.*;
import de.hitec.oaidashboard.harvesting.DataHarvester;
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

public class HarvestingManager {

	private static final String SCRIPT_FILE = "exportSchemaScript.sql";
	private static final String METHA_ID_PATH = "/usr/sbin/metha-id";
	private static final String METHA_SYNC_PATH = "/usr/sbin/metha-sync";
	
	// Here, metha-sync will place it's files (*.xml.gz) 
	private static final String EXPORT_DIRECTORY = "/tmp/harvest";
	
	// Then, we will copy them here, and let git manage them.
	// Also, the metha-id answer will be stored here. 
    // private static final String GIT_DIRECTORY = "/data";
	private static final String GIT_DIRECTORY = "/tmp/oai_git";
	private static final boolean RESET_DATABASE = true;
	// This flag is useless for production (must always be true),
	// but very useful for debugging, as harvesting may take a lot of time.
	private static final boolean REHARVEST = false;
	private static SessionFactory factory;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    private static SchemaExport getSchemaExport() {
		SchemaExport export = new SchemaExport();
		// Script file.		
		File outputFile = new File(SCRIPT_FILE);
		if (outputFile.exists()) { outputFile.delete(); }
		String outputFilePath = outputFile.getAbsolutePath();
		logger.info("Export file: {}", outputFilePath);
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

	public static ArrayList<Repository> getActiveReposFromDB() {
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

	public static void main(String[] args) {
		initDatabase();

		List<Repository> repositories = getActiveReposFromDB();
		if (repositories != null) {

		    NextStepsCaller nextStepsCaller = (repository, dataHarvester) -> {
                // From here, everything needs to be Single-Threaded in relation to each Repository:DataHarvester or Repository:HarvestingDataModel pair
                // Second Step: instatiation of model
                DataModelCreator dataModelCreator = new DataModelCreator(repository, dataHarvester, factory);

                if(!(dataModelCreator.getState().getStatus() == HarvestingStatus.FAILURE)) {
                    // Third Step: data aggregation (counting records, licences etc., mapping licences and more)
                    DataAggregator dataAggregator = new DataAggregator(dataModelCreator);

                    // Fourth Step: Validate against Hibernate/MySQL Schema + Custom Validation
                    dataModelCreator.validate();
                }

                // Fifth Step: Saving model to Database
                dataModelCreator.saveDataModel();
            };

			// First Step: MultiThreaded collection of data (Json, XML etc.)
            harvestData(repositories, nextStepsCaller);
		} else {
		    logger.info("No target repositories found in Database, doing nothing.");
        }
		logger.info("Finished.");
	}

	private interface NextStepsCaller {
        void callNextStep(Repository repository, DataHarvester dataHarvester);
    }

	public static void main2(String[] args) throws IOException {
		initDatabase();
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();
		Object id = null;

		try {
			HarvestingState test = session.get(HarvestingState.class, new Long(3));

			for(LicenceCount licenceCount: test.getLicenceCounts()) {
				logger.info("LicenceCount licence_name: {}, record_count: {}", licenceCount.getLicence_name(), licenceCount.getRecord_count());
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null) tx.rollback();
			logger.info("Exception while creating DataModel for repo",  e);
		} finally {
			session.close();
		}
	}

	private static void harvestData(List<Repository> repositories, NextStepsCaller nextStepsCaller) {
        Map<DataHarvester, Repository> harvesterRepoMap = new HashMap<>();

		for(Repository repo : repositories) {
			DataHarvester dataHarvester = new DataHarvester(repo.getHarvesting_url(), METHA_ID_PATH, METHA_SYNC_PATH,
					GIT_DIRECTORY, EXPORT_DIRECTORY, REHARVEST);
			harvesterRepoMap.put(dataHarvester, repo);
			dataHarvester.start();
		}
		logger.info("Waiting for DataHarvesters to finish ...");

		Set<DataHarvester> unfinishedHarvesters = new HashSet<>(harvesterRepoMap.keySet());
		while(unfinishedHarvesters.size() > 0) {
		    for(DataHarvester dataHarvester: new HashSet<>(unfinishedHarvesters)) {
		        try {
                    if(dataHarvester.success || dataHarvester.t.isInterrupted()) {
                        dataHarvester.t.join();
                        unfinishedHarvesters.remove(dataHarvester);
                        nextStepsCaller.callNextStep(harvesterRepoMap.get(dataHarvester), dataHarvester);
                    }
                    Thread.sleep(100);
                } catch(Exception e) {
                    logger.error("An error occurred while harvesting data from Harvesting-URL: {}", dataHarvester.getHarvestingURL(), e);
                    unfinishedHarvesters.remove(dataHarvester);
                    // TODO: save failed state to database
                }
            }
        }
	}
}


