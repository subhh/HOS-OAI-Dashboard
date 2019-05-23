package de.uni_hamburg.sub.oaidashboard.database;

import de.uni_hamburg.sub.oaidashboard.LicenceManager;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.*;
import de.uni_hamburg.sub.oaidashboard.database.validation.DataModelValidator;
import de.uni_hamburg.sub.oaidashboard.harvesting.DataHarvester;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.Format;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.HarvestedRecord;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.MethaSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DataModelCreator {

    private final Repository repository;
    private final DataHarvester dataHarvester;
    private final SessionFactory factory;
    public Timestamp stateTimestamp = null;

    private HarvestingState state = null;

    private List<HarvestedRecord> harvestedRecords = null; // records may not be empty -> do not instantiate empty list

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    public DataModelCreator(Repository repository, DataHarvester dataHarvester, 
    		SessionFactory factory, boolean REHARVEST, Timestamp stateTimestamp) {

        this.repository = repository;
        this.dataHarvester = dataHarvester;
        this.factory = factory;

        this.harvestedRecords = dataHarvester.getRecords();

        initDataModel(REHARVEST, stateTimestamp);
    }

    public void addCommitGit(String tag, String gitDirectory)
    {
    	try {
    		ProcessBuilder pb = new ProcessBuilder();
    		pb.command("git", "add", ".");
    		pb.directory(new File(gitDirectory));
    		Process addGit = pb.start();
    		addGit.waitFor();
    		pb.command("git", "commit", "-m", tag);
    		Process commitGit = pb.start();
    		commitGit.waitFor();
    		// git tags must not contain spaces or colons:
    		tag = tag.replace(" ", "_");
    		tag = tag.replace(":", "-");
    		pb.command("git", "tag", tag);
    		Process tagGit = pb.start();
    		tagGit.waitFor();
    		logger.info("Stored harvested records into git: {}");
    	}
    	catch (Exception e) {
    		logger.error("Error while storing harvested records into git: {}", e);
    	}
    }

    private void initDataModel(boolean REHARVEST, Timestamp stateTimestamp) {
    	
    	if (stateTimestamp == null) {
    		stateTimestamp = Timestamp.valueOf(LocalDateTime.now());
    	}

        this.state = new HarvestingState(stateTimestamp, repository, HarvestingStatus.SUCCESS);
    	
        if (REHARVEST) { // Save harvested records to git:        	
        	addCommitGit(stateTimestamp.toString(), dataHarvester.getGitDirectory());
        }
        try {
            // convert all Data (raw) to Java/Hibernate-DataModel
            Set<LicenceCount> licenceCounts = createLicenceCounts(dataHarvester.getRecords(), state, stateTimestamp);
            Set<SetCount> setCounts = createSetCounts(dataHarvester.getSets(), state);
            Set<MetadataFormat> metadataFormats = createMetadataFormats(dataHarvester.getMetadataFormats(), state);
            Set<DCFormatCount> dcFormatCounts = createDCFormatCounts(dataHarvester.getRecords(), state);
            Set<DCTypeCount> dcTypeCounts = createDCTypeCounts(dataHarvester.getRecords(), state);

            // metadataFormats and licenceCounts should always be there for any given repository
            if((metadataFormats.size() > 0) && (licenceCounts.size() > 0)) {
                state.setLicenceCounts(licenceCounts);
                state.setSetCounts(setCounts);
                state.setMetadataFormats(metadataFormats);
                state.setDCFormatCounts(dcFormatCounts);
                state.setDCTypeCounts(dcTypeCounts);
            } else {
                if(metadataFormats.size() == 0) { setStateToFailed("GOT 0 METADATAFORMATS"); }
                if(licenceCounts.size() == 0) { setStateToFailed("GOT 0 LICENCES"); }
            }

        } catch (Exception e) {
            e.printStackTrace();
            setStateToFailed("UNHANDLED EXCEPTION: " + e.getMessage());
        }
    }

    private Set<LicenceCount> createLicenceCounts(List<HarvestedRecord> recordsRaw, 
    		HarvestingState state, Timestamp stateTimestamp) {
        logger.info("Creating LicenceCounts (JavaModel) for repo: {}", repository.getHarvesting_url());

        Set<String> licences_name = recordsRaw.stream().
                map(harvestedRecord -> harvestedRecord.rightsList.get(0)).
                collect(Collectors.toSet());

        Set<LicenceCount> licenceCounts = licences_name.stream().
                map(licence_name -> new LicenceCount(licence_name, state, LicenceManager.getType(licence_name, stateTimestamp))).
                peek(licenceCount -> logger.debug("Creating new LicenceCount with name: '{}'", licenceCount.getLicence_name())).
                collect(Collectors.toSet());

        logger.debug("Created {} LicenceCounts!", licenceCounts.size());
        return licenceCounts;
    }

    private Set<SetCount> createSetCounts(List<MethaSet> setsRaw, HarvestingState state) {
        logger.info("Creating SetCounts (JavaModel) for repo: {}", repository.getHarvesting_url());
        Set<SetCount> setCounts = new HashSet<>();
        for(MethaSet setRaw: setsRaw) {
            logger.debug("Creating new SetCount with set_name: '{}' and set_spec: '{}'", setRaw.setName, setRaw.setSpec);
            setCounts.add(new SetCount(setRaw.setName, setRaw.setSpec, state));
        }
        logger.debug("Created {} SetCounts!", setCounts.size());
        return setCounts;
    }

    private Set<DCFormatCount> createDCFormatCounts(List<HarvestedRecord> recordsRaw, HarvestingState state) {
        logger.info("Creating DCFormatCounts (JavaModel) for repo: {}", repository.getHarvesting_url());

        Set<String> formats_raw = recordsRaw.stream().
                map(harvestedRecord -> harvestedRecord.dc_format).
                collect(Collectors.toSet());

        Set<DCFormatCount> formatCounts = formats_raw.stream().
                map(format_raw -> new DCFormatCount(format_raw, state)).
                peek(formatCount -> logger.debug("Creating new DCFormatCount with dc_format: '{}'", formatCount.getDc_Format())).
                collect(Collectors.toSet());

        logger.debug("Created {} DCFormatCounts!", formatCounts.size());
        return formatCounts;
    }

    private Set<DCTypeCount> createDCTypeCounts(List<HarvestedRecord> recordsRaw, HarvestingState state) {
        logger.info("Creating DCTypeCounts (JavaModel) for repo: {}", repository.getHarvesting_url());

        Set<String> types_raw = recordsRaw.stream().
                map(harvestedRecord -> harvestedRecord.typeList).
                flatMap(Collection::stream).
                collect(Collectors.toSet());
        //logger.info("set of types: {}", types_raw);

        Set<DCTypeCount> typeCounts = types_raw.stream().
                map(type_raw -> new DCTypeCount(type_raw, state)).
                peek(typeCount -> logger.debug("Creating new DCTypeCount with dc_type: '{}'", typeCount.getDc_Type())).
                collect(Collectors.toSet());

        logger.debug("Created {} DCTypeCounts!", typeCounts.size());
        return typeCounts;
    }

    private Set<MetadataFormat> createMetadataFormats(List<Format> formatsRaw, HarvestingState state) {
        logger.info("Creating MetadaFormats (JavaModel) for repo: {}", repository.getHarvesting_url());
        Set<MetadataFormat> metadataFormats = new HashSet<>();
        for(Format formatRaw: formatsRaw) {
            logger.debug("Creating new MetadataFormat with prefix: '{}', schema: {} and " +
                    "namespace: '{}'", formatRaw.metadataPrefix, formatRaw.schema, formatRaw.metadataNamespace);
            metadataFormats.add(new MetadataFormat(formatRaw.metadataPrefix, formatRaw.schema, formatRaw.metadataNamespace, state));
        }
        logger.debug("Created {} MetadataFormats!", metadataFormats.size());
        return metadataFormats;
    }

    public void saveDataModel() {
        saveDataModel(false);
    }

    private void saveDataModel(boolean fallback) {
        if(state != null) {
            logger.info("Attempting to save HarvestingState into database for repo: {}", repository.getHarvesting_url());
            Session session = factory.openSession();
            Transaction tx = session.beginTransaction();

            try {
                setStartAndEndtimeOfState();
                session.save(state);
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.info("Exception while creating DataModel for repo: {}", repository.getHarvesting_url(), e);
                if(!fallback) {
                    setStateToFailed("UNKNOWN EXCEPTION");
                    session.close();
                    saveDataModel(true);
                } else {
                    /**
                     * A catastrophic failure must be happening, possible causes:
                     * database down, DataModel completely inconsistent, some other unforeseen error
                     * TODO: what to do here? If this happens, how to inform the users of the failure if we (possibly) can not write into the database?
                     */
                    logger.error("ERROR, NO STATE COULD BE SAVED TO THE DATABASE");
                }
            } finally {
                session.close();
            }
        } else {
            this.state = new HarvestingState(Timestamp.valueOf(LocalDateTime.now()), repository, HarvestingStatus.FAILURE);
            setStateToFailed("UNKNOWN ERROR");
            saveDataModel();
        }
    }

    private void setStateToFailed(String error_message) {
        logger.error("Something happened, error_message: '{}', " +
                "setting current HarvestingState to 'FAILURE' for repo: {}",
                error_message,
                repository.getHarvesting_url());

        state.setLicenceCounts(null);
        state.setMetadataFormats(null);
        state.setSetCounts(null);
        state.setDCFormatCounts(null);
        state.setDCTypeCounts(null);
        state.setEarliest_record_timestamp(null);
        state.setLatest_record_timestamp(null);
        state.setRecord_count(0);
        state.setRecord_count_oa(0);
        state.setRecord_count_fulltext(0);
        state.setError_message(error_message);
        state.setStatus(HarvestingStatus.FAILURE);
    }

    private void setStartAndEndtimeOfState() {
        state.setstartTime(dataHarvester.getStartTime());
        state.setEnd_time(new Timestamp(Calendar.getInstance().getTime().getTime()));
    }

    public HarvestingState getState() {
        return this.state;
    }

    public List<HarvestedRecord> getHarvestedRecords() {
        return this.harvestedRecords;
    }

    public void validate() {
        logger.info("Validating DataModel against Hibernate annotations: {}", repository.getHarvesting_url());
        for(LicenceCount licenceCount: state.getLicenceCounts()) {
            DataModelValidator.isValidLicenceCount(licenceCount);
        }
        for(SetCount setCount: state.getSetCounts()) {
            DataModelValidator.isValidSetCount(setCount);
        }
    }
}
