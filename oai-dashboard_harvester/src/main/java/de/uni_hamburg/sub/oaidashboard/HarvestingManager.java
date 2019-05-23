package de.uni_hamburg.sub.oaidashboard;

import de.uni_hamburg.sub.oaidashboard.aggregation.DataAggregator;
import de.uni_hamburg.sub.oaidashboard.database.DataModelCreator;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.HarvestingState;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.HarvestingStatus;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.Licence;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.LicenceCount;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.LicenceType;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.Repository;
import de.uni_hamburg.sub.oaidashboard.harvesting.DataHarvester;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;

@org.hibernate.annotations.NamedNativeQueries(
	    @org.hibernate.annotations.NamedNativeQuery(name = "Update_LicenceType", 	    
	      query = "UPDATE LICENCECOUNT LC, LICENCE L SET LC.licence_type = L.licence_type "
	        	+ "WHERE LC.licence_type='UNKNOWN' and LC.licence_type != L.licence_type and LC.licence_name = L.licence_name",
//	      query = "select * from deptemployee emp where name=:name",
	      resultClass = LicenceCount.class)
	)

public class HarvestingManager {

	private static final String CONF_DIR = ".oai-dashboard";
	private static final String CONF_FILENAME_PROPERTIES = "harvester.properties";
	private static final String CONF_FILENAME_HIBERNATE = "hibernate.cfg.xml";

	private static final String SCRIPT_FILE = "exportSchemaScript.sql";
	private static String METHA_PATH = "/usr/sbin/";

	private static String METHA_ID = METHA_PATH + "metha-id";
	private static String METHA_SYNC = METHA_PATH + "metha-sync";

	// Here, metha-sync will place it's files (*.xml.gz)
	private static String EXPORT_DIRECTORY = "/tmp/harvest";

	// Then, we will copy them here, and let git manage them.
	// Also, the metha-id answer will be stored here.
    // private static final String GIT_DIRECTORY = "/data";
	private static String GIT_PARENT_DIRECTORY = "/tmp/oai_git";
	private static boolean RESET_DATABASE = false;

	// If the schema of the datadase should change, it's
	// necessary to delete the database first based on the old schema.
	private static final boolean DELETE_ONLY_DATABASE = false;

	// always set REHARVEST to false to use this.
	private static final boolean RESTORE_DB_FROM_GIT = false;

	// This flag is useless for production (must always be true),
	// but very useful for debugging, as harvesting may take a lot of time.
	private static boolean REHARVEST = false;
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
    	if (!DELETE_ONLY_DATABASE)
    	{
    		logger.info("Creating all tables  Database...");
    		createDataBase(export, metadata);
    		logger.info("Setting up default repositories...");
    		setUpDefaultRepositories();
    	}
	}

	private static File loadHibernateConfigFromDirectory() {
		File configFile = null;
		File dir = new File(System.getProperty("user.home") + "/" + CONF_DIR);
		File file = new File(dir.toString() + "/" + CONF_FILENAME_HIBERNATE);
		if(dir.isDirectory()) {
			if(file.isFile()) {
				configFile = file;
			} else {
				logger.info("hibernate configuration file does not exist: " + file.toString());
			}
		} else {
			logger.info("configuration directory does not exist: " + dir.toString());
		}
		if(configFile != null) {
			logger.info("loaded hibernate configuration from: " + configFile.toString());
		} else {
			logger.info("failed to load hibernate configuration from: " + file.toString() + " reverting to default configuration file (classpath)");
		}
		return configFile;
	}

	private static void readConfigFromProperties() {
		File dir = new File(System.getProperty("user.home") + "/" + CONF_DIR);
		File file = new File(dir.toString() + "/" + CONF_FILENAME_PROPERTIES);
		if(dir.isDirectory()) {
			if(file.isFile()) {
				logger.info("loaded configuration from: " + file.toString());
				try {
					Properties prop = new Properties();
					prop.load(new FileInputStream(file.toString()));
					HarvestingManager.METHA_PATH = prop.getProperty("harvester.metha.path");
					HarvestingManager.METHA_ID = METHA_PATH + "metha-id";
					HarvestingManager.METHA_SYNC = METHA_PATH + "metha-sync";
					HarvestingManager.EXPORT_DIRECTORY = prop.getProperty("harvester.export.dir");
					HarvestingManager.GIT_PARENT_DIRECTORY = prop.getProperty("harvester.git.persistence.dir");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				logger.info("configuration file does not exist: " + file.toString() + " using default configuration");
			}
		} else {
			logger.info("configuration directory does not exist: " + dir.toString() + " using default configuration");
		}
	}

	private static void initDatabase() {
		// read config and create necessary objects
		ServiceRegistry serviceRegistry;
		File configFile = loadHibernateConfigFromDirectory();
		if(configFile != null) {
			serviceRegistry = new StandardServiceRegistryBuilder()
					.configure(configFile).build();
		} else {
			serviceRegistry = new StandardServiceRegistryBuilder()
					.configure(CONF_FILENAME_HIBERNATE).build();
		}
		Metadata metadata = new MetadataSources(serviceRegistry)
				.getMetadataBuilder().build();
		SchemaExport export = getSchemaExport();

		// buildSessionFactory
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
    	parseCommandLine(args);
    	readConfigFromProperties();
		initDatabase();
		if (DELETE_ONLY_DATABASE) {
			return;
	    }
		List<Repository> repositories = getActiveReposFromDB();
		resetGitDirectory();
		initDirectories();
    	LicenceManager.initManager(factory);
		updateUnkownLicences();
		if (!REHARVEST) {			
			harvestFromGit(repositories);
		} else {
			doHarvest(repositories, null);
		}
		logger.info("Finished.");
	}


	private static void updateUnkownLicences() {
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();
		
		Query query = session.createNativeQuery(
				"UPDATE LICENCECOUNT LC, LICENCE L SET LC.licence_type = L.licence_type "
	        	+ "WHERE LC.licence_type='UNKNOWN' and LC.licence_type != L.licence_type "
				+ "and LC.licence_name = L.licence_name");

		try {
			int rowsAffected = query.executeUpdate();
			logger.info("Updated " + rowsAffected + " row(s) in LicenceCount with UNKNOWN Licence " +
					"that is of open/closed type meanwhile.");
		}
		catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		} finally {
			session.close();
		}
	}

	private static void harvestFromGit(List<Repository> repositories) {
	    Hashtable<String, Set<Repository>> gitTags = queryGitTags(repositories);
		Timestamp stateTimestamp = null;
		// It is necessary to restore latest setting before following checkout,
		// as the checkout command does not 
		// restore files which git assumes to be unchanged
		for (Repository repo : repositories) {
			try {
				String methaUrlString = (String) File.separator 
		    			+ Base64.getUrlEncoder().withoutPadding().encodeToString(
		    					("#oai_dc#" + repo.getHarvesting_url()).getBytes("UTF-8"));
    	  		    		
	        	ProcessBuilder pb = new ProcessBuilder("git", "checkout", "--", "*");        	
	    		pb.directory(new File(GIT_PARENT_DIRECTORY + methaUrlString));
	    		Process p = pb.start();
	    		p.waitFor();
			}
			catch (IOException e) {
				System.err.println("Caught IOException: " + e.getMessage());
			}
			catch (InterruptedException e) {
				System.err.println("Caught InterruptedException: " + e.getMessage());
			}
		}
		if (gitTags.size() == 1 && gitTags.containsKey("-- *")) {
			// shortcut meanly meant for debugging. Checkout latest commit
			String tag = null;
			for (Repository repo : repositories) {
				try {
					String methaUrlString = (String) File.separator 
			    			+ Base64.getUrlEncoder().withoutPadding().encodeToString(
			    					("#oai_dc#" + repo.getHarvesting_url()).getBytes("UTF-8"));
	    	  		    		
		        	ProcessBuilder pb = new ProcessBuilder("git", "checkout", "master");        	
		    		pb.directory(new File(GIT_PARENT_DIRECTORY + methaUrlString));
		    		Process p = pb.start();
		    		p.waitFor();
		    		// now get the date from latest commit tag
		        	pb = new ProcessBuilder("git", "-P", "tag");        	
		    		pb.directory(new File(GIT_PARENT_DIRECTORY + methaUrlString));
		    		p = pb.start();
		    		p.waitFor();
		    		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			        String line = null;
		            while((line = br.readLine()) != null) {
		            	// A tag usually is an isodate like "2019-04-16_09-40-06.888849".
		            	// The latest tag can be found in the last line, so always overwrite previous tag
		            	tag = line.trim();
		                logger.info(line);
		            }
				}
				catch (IOException e) {
					System.err.println("Caught IOException: " + e.getMessage());
				}
				catch (InterruptedException e) {
					System.err.println("Caught InterruptedException: " + e.getMessage());
				}
			}
			doHarvest(repositories, createTimestampFromTag(tag));
		} else {
			Set<String> keySet = gitTags.keySet();
			for (String key : keySet) {				
				Set<Repository> repos = gitTags.get(key);
				for (Repository repo : repos) {
					try {
						// the 'tag' we stored in the hashtable is a shortcut, we have to find
						// the corresponding complete tag (with minutes, seconds, ...).
						// If more than one tag matches, take only the last one.
						BufferedReader br;
						String methaUrlString = (String) File.separator 
				    			+ Base64.getUrlEncoder().withoutPadding().encodeToString(
				    					("#oai_dc#" + repo.getHarvesting_url()).getBytes("UTF-8"));

			        	ProcessBuilder pb = new ProcessBuilder("git", "-P", "tag", "-l", key+"*" );        	
			    		pb.directory(new File(GIT_PARENT_DIRECTORY + methaUrlString));
			    		Process p = pb.start();
			    		p.waitFor();
			            br = new BufferedReader(new InputStreamReader(p.getInputStream()));

				        String line = null;
				        String tag = null;
			            while((line = br.readLine()) != null) {
			            	// A tag usually is an isodate like "2019-04-16_09-40-06.888849".
			            	// The key used above is only the first part, like "2019-04-16_09"+'*'
			            	// i.e. the date, including the hour, and we want to find the whole tag
			            	tag = line.trim();
			                logger.info(line);
			            }
			            // Within 'queryGitTags' we just queried for tags, found one and stored it's 
			            // abbreviation. Now a tag corresponding to the abbrev. should still be there.
		            	assert (tag != null);
		            	pb = new ProcessBuilder("git", "checkout", tag);        	
		            	pb.directory(new File(GIT_PARENT_DIRECTORY + methaUrlString));
		            	p = pb.start();		
		            	p.waitFor();
		        		stateTimestamp = createTimestampFromTag(tag);
					}
					catch (IOException e) {
						System.err.println("Caught IOException: " + e.getMessage());
					}
					catch (InterruptedException e) {
						System.err.println("Caught InterruptedException: " + e.getMessage());
					}
				}
				doHarvest(new ArrayList<Repository>(repos), stateTimestamp);
			}
		}	
	}

    private static Timestamp createTimestampFromTag(String tag) {
    	// splits tag "YYYY-MM-DD_hh-mm-ss.xxxxx" into "YYYY-MM-DD" and "hh-mm-ss.xxxxx"
		String[] tagParts = tag.split("_");
		// changes "hh-mm-ss.xxxxx" into "hh:mm:ss.xxxxx"
		// and concatenates both parts to "YYYY-MM-DD hh:mm:ss.xxxxx"
		return Timestamp.valueOf(tagParts[0] + " " + tagParts[1].replace("-", ":"));
    }
    
	private static void doHarvest(List<Repository> repositories, Timestamp stateTimestamp) {
		if (repositories != null) {

		    NextStepsCaller nextStepsCaller = (repository, dataHarvester) -> {
                // From here, everything needs to be Single-Threaded in relation to each
		    	// Repository:DataHarvester or Repository:HarvestingDataModel pair
                // Second Step: instantiation of model
		    	
                DataModelCreator dataModelCreator = new DataModelCreator(repository, dataHarvester, factory,
                		REHARVEST, stateTimestamp);

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
	}

	private static Hashtable queryGitTags(List<Repository> repositories) {
    	// restores the complete DB from git repository, if flag RESTORE_DB_FROM_GIT 
    	// is true, otherwise take only datasets from the latest harvest run.  
    	Hashtable<String, Set<Repository>> gitTags = new Hashtable<String, Set<Repository>>();
    	
		for(Repository repo : repositories) {

			if (RESTORE_DB_FROM_GIT) {  
				BufferedReader br;
				try {
					String methaUrlString = (String) File.separator 
			    			+ Base64.getUrlEncoder().withoutPadding().encodeToString(
			    					("#oai_dc#" + repo.getHarvesting_url()).getBytes("UTF-8"));

		        	ProcessBuilder pb = new ProcessBuilder("git", "-P", "tag");        	
		    		pb.directory(new File(GIT_PARENT_DIRECTORY + methaUrlString));
		    		Process p = pb.start();
		    		p.waitFor();
		            br = new BufferedReader(new InputStreamReader(p.getInputStream()));

			        String line = null;
		            while((line = br.readLine()) != null) {
		            	// a tag usually is an isodate like "2019-04-16_09-40-06.888849"
		            	// we are only interested in the first part, like "2019-04-16_09"
		            	// i.e. the date, including the hour.
		            	String tag = line.trim().substring(0, 13);
		            	Set<Repository> tempRepoSet;
		            	if (gitTags.containsKey(tag)) {
		            		tempRepoSet = gitTags.get(tag);
		            		tempRepoSet.add(repo);            		            		
		            	} else {
		            		tempRepoSet = new HashSet<Repository>(Arrays.asList(repo));
		            	}
		            	gitTags.put(tag, tempRepoSet);
		                logger.info(line);
		            }            
				}
				catch (IOException e) {
					System.err.println("Caught IOException: " + e.getMessage());
				}
				catch (InterruptedException e) {
					System.err.println("Caught InterruptedException: " + e.getMessage());
				}
            }
			else {
				// this is supposed to work as debugging solution
				gitTags.put("-- *", new HashSet<Repository>(Arrays.asList(repo)));
			}
    	}
    	return gitTags;		
	}

	private static void initDirectories() {
    	
    	String[] dirs = {GIT_PARENT_DIRECTORY, EXPORT_DIRECTORY};
    	for (String dir : dirs)
    	{
        	File d = new File(dir);
        	if ( !d.exists() )
        	{
        		d.mkdir();
        	}
    	}
    }

	private static void resetGitDirectory() {
		// The harvested records are supposed to be collected in a git repository.
		// Each harvested state should be stored, and under normal circumstances 
		// the harvest process will overwrite all existing files within the git directory,
		// which is the desired behavior. However, in order to ensure that i.e. in
		// case of a failed harvesting run older files will not interfere with currently
		// harvested files, it is necessary to delete all older files in the
		// git directory before starting a new harvest run.
		
		try
		{
			if (Files.exists(FileSystems.getDefault().getPath(GIT_PARENT_DIRECTORY)))
			{
				Files.walk(FileSystems.getDefault().getPath(GIT_PARENT_DIRECTORY))
				.sorted(Comparator.reverseOrder())
				.filter(p -> !p.toString().contains(".git"))
				.map(Path::toFile)
				.forEach(File::delete);
			}
		}
		catch (IOException ioex)
		{
			logger.error("Failed to clear harvesting directories! Will continue anyhow.");
		}		
	}

	private static void parseCommandLine(String[] args)
	{
        if(args.length > 0) {
            logger.info("Parsing command line arguments");

            CommandLine commandLine;
            // TODO: remove this argument because it is dangerous in a production environment
            Option reset_database_option = Option.builder("RESET")
                    .required(false)
                    .hasArg()
                    .desc("Reset the database")
                    .build();
            // TODO: remove this argument because it is dangerous in a production environment
            Option reharvest_option = Option.builder("REHARVEST")
                    .required(false)
                    .hasArg()
                    .desc("Reharvest")
                    .build();
            Options options = new Options();
            options.addOption(reset_database_option);
            options.addOption(reharvest_option);
            CommandLineParser parser = new DefaultParser();
            try {
                commandLine = parser.parse(options, args);
                if(commandLine.hasOption("RESET")) {
                    boolean reset_option_argument = Boolean.parseBoolean(commandLine.getOptionValue("RESET"));
                    logger.info("Setting RESET_DATABASE to: {}", reset_option_argument);
                    RESET_DATABASE = reset_option_argument;
                }
                if(commandLine.hasOption("REHARVEST")) {
                    boolean reharvest_option_argument = Boolean.parseBoolean(commandLine.getOptionValue("REHARVEST"));
                    logger.info("Setting REHARVEST to: {}", reharvest_option_argument);
                    REHARVEST = reharvest_option_argument;
                }
            } catch (Exception e) {
                logger.info("Error parsing command line options", e);
            }
        }
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
			DataHarvester dataHarvester = new DataHarvester(repo.getHarvesting_url(), METHA_ID, METHA_SYNC,
					GIT_PARENT_DIRECTORY, EXPORT_DIRECTORY, REHARVEST);
			harvesterRepoMap.put(dataHarvester, repo);
			dataHarvester.start();
		}
		logger.info("Waiting for DataHarvesters to finish ...");

		Set<DataHarvester> unfinishedHarvesters = new HashSet<>(harvesterRepoMap.keySet());
		while(unfinishedHarvesters.size() > 0) {
			logger.info("unfinishedHarvestersSize: " + unfinishedHarvesters.size());
		    for(DataHarvester dataHarvester: new HashSet<>(unfinishedHarvesters)) {
		        try {
                    if(dataHarvester.success || dataHarvester.t.isInterrupted()) {
                        dataHarvester.t.join();
                        unfinishedHarvesters.remove(dataHarvester);
                        nextStepsCaller.callNextStep(harvesterRepoMap.get(dataHarvester), dataHarvester);
                    }
                    Thread.sleep(5000);
                } catch(Exception e) {
                    logger.error("An error occurred while harvesting data from Harvesting-URL: {}", dataHarvester.getHarvestingURL(), e);
                    unfinishedHarvesters.remove(dataHarvester);
                    // TODO: save failed state to database
                }
            }
        }
	}
}


