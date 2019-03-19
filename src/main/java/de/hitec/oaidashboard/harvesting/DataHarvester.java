package de.hitec.oaidashboard.harvesting;

import de.hitec.oaidashboard.harvesting.datastructures.Format;
import de.hitec.oaidashboard.harvesting.datastructures.HarvestedRecord;
import de.hitec.oaidashboard.harvesting.datastructures.MethaIdStructure;
import de.hitec.oaidashboard.harvesting.datastructures.MethaSet;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

public class DataHarvester extends Thread {

    private final String harvestingURL;
    private final String metaIdPath;
    private final String metaSyncPath;
    private final String gitDirectory;
    private final String exportDirectory;

    private boolean reharvest;

    public boolean success = false;

    private List<Format> metadataFormats;
    private List<MethaSet> sets;
    private List<HarvestedRecord> records;

    private Timestamp startTime;

    public Thread t;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    public DataHarvester(String harvestingURL, String mip, String msp,
                  String gd, String ed, boolean reharvest) {
        this.harvestingURL = harvestingURL;
        this.metaIdPath = mip;
        this.metaSyncPath = msp;
        this.gitDirectory = gd;
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
            MethaIdStructure instance = getMetaIdAnswer();
            if(instance != null) {
                this.sets = instance.sets;
                logger.info("Got {} Sets from Harvesting-URL: {}", sets.size(), harvestingURL);

                this.metadataFormats = instance.formats;
                logger.info("Got {} MetadataFormats from Harvesting-URL: {}", metadataFormats.size(), harvestingURL);

                logger.info("Starting Metha-Sync for repo: {}", harvestingURL);
                records = startMethaSync();
                logger.info("Got {} records for repo: {}" , records.size(), harvestingURL);
                HarvestedRecord rec = new HarvestedRecord();

                success = true;
            } else {
                success = false;
            }
        } catch (Exception e) {
            success = false;
            logger.error("Error while harvesting data from URL: {}", harvestingURL, e);
        }
    }

    private MethaIdStructure getMetaIdAnswer() {
        MethaIdStructure instance = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(metaIdPath, harvestingURL);
            Process p = pb.start();
            InputStream inStream = p.getInputStream();
            String storeToFile = gitDirectory + "/MethaID-Answer-" +
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".json";
            JsonParser jParser = new JsonParser(inStream, storeToFile);
            instance = jParser.getJsonStructure();
            inStream.close();
        }
        catch (Exception e) {
            logger.error("Error while harvesting data from URL: {}", harvestingURL, e);
            throw new ExceptionInInitializerError(e);
        }
        return instance;
    }

    private List<HarvestedRecord> startMethaSync() {
        List<HarvestedRecord> records = new ArrayList<>();
        try {
            // metha-id generates a directory name by concatenating the requested 'set' string and '#'
            // and the requested format string (as default) 'oai_dc' and '#' together with the given url.
            // Then, the whole string is base64-encoded.
            // This directory name can be retrieved with the following call:
            // metha-sync -dir -base-dir <exportDirectory> <repo.getHarvesting_url()>.
            // but the direct computation is most probably better performing:

            String urlString = "#oai_dc#" + harvestingURL;

            File dir = new File(exportDirectory + (String) File.separator +
                    Base64.getUrlEncoder().withoutPadding().encodeToString(urlString.getBytes("UTF-8")));


            ProcessBuilder pb = new ProcessBuilder(metaSyncPath,
                    "-no-intervals", "-base-dir", exportDirectory, harvestingURL);

            if (reharvest) {
                logger.info(dir);
                if(dir.exists()) {
                    for (File filepath: dir.listFiles()) {
                        if (filepath.getName().endsWith(".xml.gz")) {
                            filepath.delete();
                        }
                    }
                }
                Process p = pb.start();
                p.waitFor();

                if (p.getErrorStream().available() > 0) {
                    logger.info(IOUtils.toString(p.getErrorStream(), "UTF-8"));
                }
            }

            XmlParser xmlparser = new XmlParser();

            // TODO: if no files are havested, is that automatically a failed state?
            //if (dir.listFiles().length == 0) {
            //    state.setStatus("FAILED");
            //}
            
            for (File filepath: dir.listFiles()) {
                if (filepath.getName().endsWith(".xml.gz")) {
                    records.addAll(xmlparser.getRecords(
                            FileSystems.getDefault().getPath(filepath.getCanonicalPath()),
                            FileSystems.getDefault().getPath(gitDirectory)));
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

    public Timestamp getStartTime() {
        return startTime;
    }
}
