package de.uni_hamburg.sub.oaidashboard.harvesting;

import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.Format;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.HarvestedRecord;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.MethaIdStructure;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.MethaSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class DataHarvester extends Thread {

    private final String harvestingURL;
    private final String metaIdPath;
    private final String metaSyncPath;
    private final String gitParentDirectory;
    private final String exportDirectory;
    private final String methaUrlString;
    private String gitDirectory;

    private boolean reharvest;

    public boolean success = false;
    public volatile boolean stopped = false;

    private List<Format> metadataFormats;
    private List<MethaSet> sets;
    private List<HarvestedRecord> records;

    private Timestamp startTime;

    public Thread t;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    public DataHarvester(String harvestingURL, String mUS, String mip, String msp,
                  String gpd, String ed, boolean reharvest) {
        this.harvestingURL = harvestingURL;
        this.methaUrlString = mUS;
        this.metaIdPath = mip;
        this.metaSyncPath = msp;
        this.gitParentDirectory = gpd;
        this.exportDirectory = ed;
        this.reharvest = reharvest;
    }

    public void start() {
        logger.info("Start Harvesting with Harvesting-URL: {}", harvestingURL);
        if (t == null) {
            t = new Thread (this);
            t.start();
        }
    }

    public void run() {
        // Set StartTime (will be used as StartTime for HarvestingState)
        this.startTime = new Timestamp(Calendar.getInstance().getTime().getTime());

        try {
            gitDirectory = new File(gitParentDirectory + File.separator + methaUrlString).getCanonicalPath();
            ensureGit();

            MethaIdStructure instance = getMetaIdAnswer();
        	if(instance != null) {
        		this.sets = instance.sets;
        		logger.info("Got {} Sets from Harvesting-URL or corresponding file: {}", sets.size(), harvestingURL);

        		this.metadataFormats = instance.formats;
        		logger.info("Got {} MetadataFormats from Harvesting-URL or corresponding file: {}",
        				metadataFormats.size(), harvestingURL);

        		logger.info("Starting Metha-Sync for repo: {}", harvestingURL);
        		records = startMethaSync();
        		logger.info("Got {} records for repo: {}" , records.size(), harvestingURL);

        		success = true;
        	} else {
        		success = false;
        	}
        } catch (Exception e) {
            success = false;
            logger.error("Error while harvesting data from URL: {}", harvestingURL, e);
            stopped = true;
        }
    }

        
    private void ensureGit() throws Exception
    {
    	File gitDirFile = new File(gitDirectory);
        if ( !gitDirFile.exists() )
    	{
        	gitDirFile.mkdir();
    	}
        
        // clear git directory -> already done in HarvestingManager->resetGitDirectory
//        for (File filepath: gitDirFile.listFiles()) {
//        	if (filepath.isFile()) {
//        		filepath.delete();
//        	}
//        }

        if (new File(gitDirectory + File.separator + ".git").exists())
    	{
    		return;
    	}
    	
        ProcessBuilder pb = new ProcessBuilder();
    	pb.command("git", "init");
    	pb.directory(gitDirFile);
    	Process initGit = pb.start();
    	initGit.waitFor();
    }
    
    private MethaIdStructure getMetaIdAnswer() throws Exception {
        MethaIdStructure instance = null;
        InputStream inStream = null;
        try {
            String storeToFile = gitDirectory + File.separator + "MethaID-Answer.json";
        	if (reharvest) {
	            ProcessBuilder pb = new ProcessBuilder(metaIdPath, harvestingURL);
                logger.info("Calling process: '{} {}'", metaIdPath, harvestingURL);
	            Process p = pb.start();
                BufferedReader brError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String lineError = null;
                while((lineError = brError.readLine()) != null ) {
                    if(lineError != null) {
                        logger.info(lineError);
                    }
                }
	            inStream = p.getInputStream();
        	}
        	else
        	{
        		inStream = new FileInputStream(storeToFile);
        		storeToFile = ""; // marker for the JsonParser so that the stream won't be stored  
        	}
            JsonParser jParser = new JsonParser(inStream, storeToFile);
            instance = jParser.getJsonStructure();
            inStream.close();
        }
        catch (Exception e) {
        	if (reharvest) {
        		logger.error("Error while harvesting data from URL: {}", harvestingURL, e);
        	}
        	else {
        		logger.error("Error while harvesting data from file: {}", 
        				gitDirectory + File.separator + "MethaID-Answer.json", e);
        	}
            throw new Exception(e);
        }
        return instance;
    }

    private List<HarvestedRecord> startMethaSync() {
        List<HarvestedRecord> records = new ArrayList<>();
        try {
            // metha-sync generates a directory name by concatenating the requested 'set' string and '#'
            // and the requested format string (as default) 'oai_dc' and '#' together with the given url.
            // Then, the whole string is base64-encoded.
            // This directory name could be retrieved with the following call:
            // metha-sync -dir -base-dir <exportDirectory> <repo.getHarvesting_url()>.
            // but the direct computation is most probably better performing:

            File dirIn = new File(exportDirectory + methaUrlString);
            File dirOut = new File(gitDirectory);

            // Fallback solution if metha-sync had changed above described computation: 
            if(!dirIn.exists()) {
            	ProcessBuilder pb = new ProcessBuilder(metaSyncPath,
                        "-dir", "-base-dir", exportDirectory, harvestingURL);
                Process p = pb.start();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                dirIn = new File(br.readLine());
            }
            if (reharvest) {
            	ProcessBuilder pb = new ProcessBuilder(metaSyncPath,
                    "-no-intervals", "-base-dir", exportDirectory, harvestingURL);

                logger.info(dirIn);
                if(dirIn.exists()) {
                    for (File filepath: dirIn.listFiles()) {
                    	if (filepath.isFile()) {
                    		filepath.delete();
                        }
                    }
                    dirIn.delete();
                }

                logger.info("Calling process: '{} {} {} {} {}'", metaSyncPath, "-no-intervals", "-base-dir", exportDirectory, harvestingURL);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = null;
                while((line = br.readLine()) != null) {
                    logger.info(line);
                }
                p.waitFor();
            }
            else
            {
            	dirIn = dirOut;
            	dirOut = null;
            }
            XmlParser xmlparser = new XmlParser();

            // TODO: if no files are havested, is that automatically a failed state?
            //if (dir.listFiles().length == 0) {
            //    state.setStatus("FAILED");
            //}
                        
            for (File filepath: dirIn.listFiles()) {
                if (filepath.getName().endsWith(".xml.gz") || filepath.getName().endsWith(".xml")) {
                    records.addAll(xmlparser.getRecords(
                            FileSystems.getDefault().getPath(filepath.getCanonicalPath()),
                            dirOut != null ? FileSystems.getDefault().getPath(dirOut.getCanonicalPath()) : null));
                }
            }
        }
        catch (Exception e) {
            // TODO: create and save failed datamodel (in HarvestingManager...)
            logger.info("Failed to run Record-Harvester.", e);
        }
        return records;
    }

    public List<Format> getMetadataFormats() {
        return metadataFormats;
    }

    public List<MethaSet> getSets() {
        return sets;
    }

    public List<HarvestedRecord> getRecords() {
        return records;
    }

    public String getHarvestingURL() {
        return harvestingURL;
    }

    public String getGitDirectory() {
    	return gitDirectory;
    }
    
    public Timestamp getStartTime() {
        return startTime;
    }

    private static class ProcessReadTask implements Callable<List<String>> {

        private InputStream inputStream;

        public ProcessReadTask(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public List<String> call() {
            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.toList());
        }
    }
}
