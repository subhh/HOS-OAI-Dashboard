package de.hitec.oaidashboard.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.query.Query;
import org.hibernate.HibernateException; 
import org.hibernate.Session; 
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import de.hitec.oaidashboard.database.datastructures.Repository;

public class ManageHarvester {

	private static final String SCRIPT_FILE = "exportSchemaScript.sql";
	private static final String METHA_ID_PATH = "/home/user/go/bin/metha-id";
	private static final String METHA_SYNC_PATH = "/home/user/go/bin/metha-sync";
	
	// Here, metha-sync will place it's files (*.xml.gz) 
	private static final String EXPORT_DIRECTORY = "/tmp/harvest";
	
	// Then, we will copy them here, and let git manage them.
	// Also, the metha-id answer will be stored here. 
//	private static final String GIT_DIRECTORY = "/data";
	private static final String GIT_DIRECTORY = "/tmp/oai_git";
	private static final boolean RESET_DATABASE = false;
	// This flag is useless for production (must always be true),
	// but very useful for debugging, as harvesting may take a lot of time.
	private static final boolean REHARVEST = true;
	private static SessionFactory factory;
	
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

	private void saveBasicRepoInfo(String name, String url)
	{
		Session session = factory.openSession();
		Transaction tx = null;

		try {
			tx = session.beginTransaction();
			Repository repo = new Repository(name, url);
			session.save(repo); 
			tx.commit();
		} catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace(); 
		} finally {
			session.close(); 
		}
	}

	private ArrayList<Repository> getActiveReposFromDB() {
		ArrayList<Repository> repositories = null;
		Session session = factory.openSession();
		Transaction tx = null;
			
		try {
			tx = session.beginTransaction();
			
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Repository> criteria = 
					builder.createQuery(Repository.class);

			Root<Repository> root = criteria.from(Repository.class);
			criteria.select(root).where(builder.equal(root.get("state"), "ACTIVE"));
			Query<Repository> q = session.createQuery(criteria);		
			repositories = (ArrayList<Repository>) q.getResultList();

			tx.commit();
		} catch (HibernateException e) {
			if (tx!=null) tx.rollback();
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

	public static void main(String[] args) throws IOException {

		ManageHarvester MH = new ManageHarvester();
		String configFileName = "hibernate.cfg.xml";
		ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
	               .configure(configFileName).build();
		Metadata metadata = new MetadataSources(serviceRegistry).getMetadataBuilder().build();
		SchemaExport export = getSchemaExport();
		
		try {
			factory = metadata.buildSessionFactory();
		} catch (Throwable ex) { 
			System.err.println("Failed to create sessionFactory object." + ex);
			throw new ExceptionInInitializerError(ex); 
		}

		if (RESET_DATABASE)
		{
			System.out.println("Drop Database...");
	    	dropDataBase(export, metadata);
			System.out.println("Create Database...");
			createDataBase(export, metadata);
			
			MH.saveBasicRepoInfo("tub.dok", "http://tubdok.tub.tuhh.de/oai/request");
			MH.saveBasicRepoInfo("Elektronische Dissertationen Universit&auml;t Hamburg, GERMANY",
					"http://ediss.sub.uni-hamburg.de/oai2/oai2.php");
			MH.saveBasicRepoInfo("OPuS \\u00e2\\u0080\\u0093 Volltextserver der HCU",
					"http://edoc.sub.uni-hamburg.de/hcu/oai2/oai2.php");
			MH.saveBasicRepoInfo("Beispiel-Volltextrepository",
					"http://edoc.sub.uni-hamburg.de/hsu/oai2/oai2.php");
			MH.saveBasicRepoInfo("HAW OPUS",
					"http://edoc.sub.uni-hamburg.de/haw/oai2/oai2.php");
		}
		
		ArrayList<Repository> repositories;
		repositories = MH.getActiveReposFromDB();
		if (repositories != null)
		{
			ArrayList<Harvester> harvesters = new ArrayList<Harvester>();
			for(Repository repo : repositories)
			{
				Harvester harv = new Harvester(repo, METHA_ID_PATH, METHA_SYNC_PATH,
						GIT_DIRECTORY, EXPORT_DIRECTORY, factory);
				harv.reharvest = REHARVEST;
				harvesters.add(harv);
				harv.start();
			}
			System.out.println("Waiting for Harvesters to finish ...");
			for (Harvester harv : harvesters)
			{
				try
				{
					harv.t.join();
				}
				catch (Exception ex)
				{
					System.out.println("An error occurred while harvesting " + harv.getRepoUrl() + " " + ex);
				}
			}
			System.out.println("Finished.");
		}
	}
}


