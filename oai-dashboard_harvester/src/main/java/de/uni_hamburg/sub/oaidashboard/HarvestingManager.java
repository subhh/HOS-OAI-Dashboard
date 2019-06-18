package de.uni_hamburg.sub.oaidashboard;

import de.uni_hamburg.sub.oaidashboard.aggregation.DataAggregator;
import de.uni_hamburg.sub.oaidashboard.commandline.CommandLineHandler;
import de.uni_hamburg.sub.oaidashboard.database.DataModelCreator;
import de.uni_hamburg.sub.oaidashboard.database.DatabaseManager;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.HarvestingStatus;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.LicenceCount;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.Repository;
import de.uni_hamburg.sub.oaidashboard.harvesting.DataHarvester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.*;

@org.hibernate.annotations.NamedNativeQueries(
	    @org.hibernate.annotations.NamedNativeQuery(name = "Update_LicenceType", 	    
	      query = "UPDATE LICENCECOUNT LC, LICENCE L SET LC.licence_type = L.licence_type "
	        	+ "WHERE LC.licence_type='UNKNOWN' and LC.licence_type != L.licence_type and LC.licence_name = L.licence_name",

	      resultClass = LicenceCount.class)
	)

public class HarvestingManager {

	private static String CONF_DIR = System.getProperty("user.home") + "/" + ".oai-dashboard/";
	private static final String CONF_FILENAME_PROPERTIES = "harvester.properties";

	private static String METHA_PATH = "/usr/sbin/";

	private static String METHA_ID = METHA_PATH + "metha-id";
	private static String METHA_SYNC = METHA_PATH + "metha-sync";

	// Here, metha-sync will place it's files (*.xml.gz)
	private static String EXPORT_DIRECTORY = CONF_DIR + "harvest";

	// Then, we will copy them here, and let git manage them.
	// Also, the metha-id answer will be stored here.
    // private static final String GIT_DIRECTORY = "/data";
	private static String GIT_PARENT_DIRECTORY = CONF_DIR + "oai_git";
	private static String LICENCE_FILE = CONF_DIR + "licences.json";

	// always set REHARVEST to false to use this.
	private static final boolean RESTORE_DB_FROM_GIT = false;

	// This flag is useless for production (must always be true),
	// but very useful for debugging, as harvesting may take a lot of time.
	private static boolean REHARVEST = true;

	private static boolean START_HARVEST = false;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

	private static void readConfigFromProperties() {
		File dir = new File(CONF_DIR);
		File file = new File(dir.toString() + "/" + CONF_FILENAME_PROPERTIES);
		if(dir.isDirectory()) {
			if(file.isFile()) {
				logger.info("loaded configuration from: " + file.toString());
				try {
					Properties prop = new Properties();
					prop.load(new FileInputStream(file.toString()));
					HarvestingManager.METHA_PATH = prop.getProperty("harvester.metha.path", METHA_PATH);
					HarvestingManager.METHA_ID = METHA_PATH + "metha-id";
					HarvestingManager.METHA_SYNC = METHA_PATH + "metha-sync";
					HarvestingManager.EXPORT_DIRECTORY = prop.getProperty("harvester.export.dir", EXPORT_DIRECTORY);
					HarvestingManager.GIT_PARENT_DIRECTORY = prop.getProperty("harvester.git.persistence.dir", GIT_PARENT_DIRECTORY);
					HarvestingManager.LICENCE_FILE = prop.getProperty("harvester.licences.file", LICENCE_FILE);
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

	public static void main(String[] args) {

		CommandLineHandler clHandler = new CommandLineHandler();
    	if(clHandler.parseCommandLineArguments(args)) {
    		// apply settings according to the command line arguments
			// always start with the command line directory!
			setConfDirFromCommandLine(clHandler);
			DatabaseManager dbMan = new DatabaseManager(CONF_DIR);
			boolean continue_operation = applyCommandLineSettings(clHandler, dbMan);

			if(continue_operation) {
				logger.info("using configuration directory '{}'", CONF_DIR);

				readConfigFromProperties();

				//dbMan.initializeRepositoriesFromJson();

				List<Repository> repositories = dbMan.getActiveReposFromDB();
				resetGitDirectory(); // TODO: evaluate if this must be called only when START_HARVEST is true
				initDirectories();
				LicenceManager.initManager(dbMan.getSessionFactory(), LICENCE_FILE);
				if (!REHARVEST) {
					harvestFromGit(repositories, dbMan);
				} else {
					if (START_HARVEST) {
						doHarvest(repositories, null, dbMan.getSessionFactory());
					}
				}
				LicenceManager.writeLicencesToFile();
				logger.info("Finished.");
			}
		}
	}

	private static void setConfDirFromCommandLine(CommandLineHandler clHandler) {
		if(clHandler.FLAG_CONF_DIR) {
			CONF_DIR = clHandler.SET_CONF_DIR;
			logger.info("Setting configuration directory to: {}", CONF_DIR);
		}
	}

	/**
	 * Reads an instance of CommandLineHandler for the flags that got set while parsing the command line arguments
	 * and applies all settings
	 * @param clHandler, instance of CommandLineHandler (that got used for parsing the command line arguments)
	 * @param dbMan, instance of DatabaseManager (some command line arguments issue operations on the database)
	 * @return continue_operation, if false, application should be stopped after this method call
	 */
	private static boolean applyCommandLineSettings(CommandLineHandler clHandler, DatabaseManager dbMan) {
		boolean continue_operation = true;
		if(clHandler.FLAG_INIT_DB) {
			logger.info("Initializing the database");
			dbMan.initializeDatabase();
		}
		if(clHandler.FLAG_RESET_DB) {
			logger.info("RESETTING DATABASE, ALL DATA WILL BE LOST");
			dbMan.resetDatabase();
		}
		if(clHandler.FLAG_REHARVEST) {
			logger.info("Setting REHARVEST to: {}", clHandler.FLAG_REHARVEST);
			REHARVEST = clHandler.FLAG_REHARVEST;
		}
		if(clHandler.FLAG_START_HARVEST) {
			logger.info("Starting harvesting run");
			START_HARVEST = true;
		} else {
			logger.info("### Not starting a harvesting run (not issued), see help for more information ###");
		}
		if(clHandler.FLAG_ONLY_UPDATE_LICENCES) {
			logger.info("Only updating licences according to licences.json (expected location according to current config: {})", LICENCE_FILE);
			LicenceManager.initManager(dbMan.getSessionFactory(), LICENCE_FILE);
			continue_operation = false;
		}
		if(clHandler.FLAG_LIST_REPOSITORIES) {
			logger.info("Only listing configured repositories (harvesting targets)");
			dbMan.listAllRepos();
			continue_operation = false;
		}
		return continue_operation;
	}

	private static void harvestFromGit(List<Repository> repositories, DatabaseManager dbMan) {
	    Hashtable<String, Set<Repository>> gitTags = queryGitTags(repositories);
		Timestamp stateTimestamp = null;
		// It is necessary to restore latest setting before following checkout,
		// as the checkout command does not 
		// restore files which git assumes to be unchanged
		for (Repository repo : repositories) {
			try {
				String methaUrlString = repo.getInitialDirectoryHash();
    	  		    		
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
					String methaUrlString = repo.getInitialDirectoryHash();
	    	  		    		
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
			doHarvest(repositories, createTimestampFromTag(tag), dbMan.getSessionFactory());
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
						String methaUrlString = repo.getInitialDirectoryHash();

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
				doHarvest(new ArrayList<Repository>(repos), stateTimestamp, dbMan.getSessionFactory());
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
    
	private static void doHarvest(List<Repository> repositories, Timestamp stateTimestamp, SessionFactory factory) {
		if (repositories != null) {

		    NextStepsCaller nextStepsCaller = (repository, dataHarvester) -> {
                // From here, everything needs to be Single-Threaded in relation to each
		    	// Repository:DataHarvester or Repository:HarvestingDataModel pair
                // Second Step: instantiation of model
		    	
                DataModelCreator dataModelCreator = new DataModelCreator(repository, dataHarvester, factory,
                		REHARVEST, stateTimestamp);

                if(!(dataModelCreator.getState().getStatus() == HarvestingStatus.FAILURE)) {

                	// Third Step: clean DataModel by custom specifications
					dataModelCreator.clean();

					// Fourth Step: data aggregation (counting records, licences etc., mapping licences and more)
                    DataAggregator dataAggregator = new DataAggregator(dataModelCreator);

					// Fifth Step: Validate against Hibernate/MySQL Schema
					dataModelCreator.validate();
				}

                // Sixth Step: Saving model to Database
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
					String methaUrlString = repo.getInitialDirectoryHash();
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
				.filter(f -> !f.getParent().toString().matches(GIT_PARENT_DIRECTORY)) // only delete files within repo dirs 
				.forEach(File::delete);
			}
		}
		catch (IOException ioex)
		{
			logger.error("Failed to clear harvesting directories! Will continue anyhow.");
		}		
	}

    private interface NextStepsCaller {
        void callNextStep(Repository repository, DataHarvester dataHarvester);
    }

	private static void harvestData(List<Repository> repositories, NextStepsCaller nextStepsCaller) {
        Map<DataHarvester, Repository> harvesterRepoMap = new HashMap<>();
        
		for(Repository repo : repositories) {
			DataHarvester dataHarvester = new DataHarvester(repo.getHarvesting_url(), repo.getInitialDirectoryHash(),
					METHA_ID, METHA_SYNC, GIT_PARENT_DIRECTORY, EXPORT_DIRECTORY, REHARVEST);
			harvesterRepoMap.put(dataHarvester, repo);
			dataHarvester.start();
		}
		logger.info("Waiting for DataHarvesters to finish ...");

		if(harvesterRepoMap.size() == 0) {
			logger.info("No Repositories configured for harvesting, see help for more information");
		}

		Set<DataHarvester> unfinishedHarvesters = new HashSet<>(harvesterRepoMap.keySet());
		while(unfinishedHarvesters.size() > 0) {
			logger.info("unfinishedHarvestersSize: " + unfinishedHarvesters.size());
		    for(DataHarvester dataHarvester: new HashSet<>(unfinishedHarvesters)) {
		        try {
                    if(dataHarvester.success || dataHarvester.stopped) {
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


