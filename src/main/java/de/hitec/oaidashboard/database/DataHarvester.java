package de.hitec.oaidashboard.database;

import de.hitec.oaidashboard.database.datastructures.Set;
import de.hitec.oaidashboard.database.datastructures2.HarvestingState;
import de.hitec.oaidashboard.database.datastructures2.Repository;
import de.hitec.oaidashboard.parsers.JsonParser;
import de.hitec.oaidashboard.parsers.datastructures.Format;
import de.hitec.oaidashboard.parsers.datastructures.MethaIdStructure;
import de.hitec.oaidashboard.parsers.datastructures.MethaSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DataHarvester extends Thread {

    private final String harvestingURL;
    private final String metaIdPath;
    private final String metaSyncPath;
    private final String gitDirectory;
    private final String exportDirectory;

    public boolean success = false;

    private List<Format> metadataFormats;
    private List<MethaSet> sets;

    public Thread t;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    DataHarvester(String harvestingURL, String mip, String msp,
                  String gd, String ed) {
        this.harvestingURL = harvestingURL;
        this.metaIdPath = mip;
        this.metaSyncPath = msp;
        this.gitDirectory = gd;
        this.exportDirectory = ed;
    }

    public void start() {
        logger.info("Start Harvesting with Harvesting-URL: {}", harvestingURL);
        if (t == null) {
            t = new Thread (this);
            t.start();
        }
    }

    public void run() {
        try {
            MethaIdStructure instance = getMetaIdAnswer();
            if(instance != null) {
                this.sets = instance.sets;
                logger.info("Got Sets from Harvesting-URL: {}", harvestingURL);
                for (MethaSet methaSet : instance.sets) {
                    logger.info("METHASET - setName: '{}', setSpec: '{}', from: '{}'", methaSet.setName, methaSet.setSpec, harvestingURL);
                }

                this.metadataFormats = instance.formats;
                logger.info("Got MetadataFormats from Harvesting-URL: {}", harvestingURL);
                for (Format format : instance.formats) {
                    logger.info("FORMAT- metadataPrefix: '{}', from: '{}'", format.metadataPrefix, harvestingURL);
                }

                //logger.info("Starting Metha Sync for repo: {}", repo.getHarvestingUrl());
                //startMethaSync();
                //markSomeLicensesAsOpenOrClose();
                //computeStatistics();

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

    public List<Format> getMetadataFormats() {
        return metadataFormats;
    }

    public List<MethaSet> getSets() {
        return sets;
    }

    public String getHarvestingURL() {
        return harvestingURL;
    }
}
