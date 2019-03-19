package de.hitec.oaidashboard.database;

import de.hitec.oaidashboard.database.datastructures.*;
import de.hitec.oaidashboard.database.validation.DataModelValidator;
import de.hitec.oaidashboard.harvesting.DataHarvester;
import de.hitec.oaidashboard.harvesting.datastructures.Format;
import de.hitec.oaidashboard.harvesting.datastructures.HarvestedRecord;
import de.hitec.oaidashboard.harvesting.datastructures.MethaSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataModelCreator {

    private final Repository repository;
    private final DataHarvester dataHarvester;
    private final SessionFactory factory;

    private HarvestingState state = null;

    private List<HarvestedRecord> harvestedRecords = null; // records may not be empty -> do not instantiate empty list

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    public DataModelCreator(Repository repository, DataHarvester dataHarvester, SessionFactory factory) {

        this.repository = repository;
        this.dataHarvester = dataHarvester;
        this.factory = factory;

        this.harvestedRecords = dataHarvester.getRecords();

        initDataModel();
    }

    private void initDataModel() {

        this.state = new HarvestingState(Timestamp.valueOf(LocalDateTime.now()), repository, HarvestingStatus.SUCCESS);

        try {
            // convert all Data (raw) to Java/Hibernate-DataModel
            Set<LicenceCount> licenceCounts = createLicenceCounts(dataHarvester.getRecords(), state);
            Set<SetCount> setCounts = createSetCounts(dataHarvester.getSets(), state);
            Set<MetadataFormat> metadataFormats = createMetadataFormats(dataHarvester.getMetadataFormats(), state);

            // metadataFormats and licenceCounts should always be there for any given repository
            if((metadataFormats.size() > 0) && (licenceCounts.size() > 0)) {
                state.setLicenceCounts(licenceCounts);
                state.setSetCounts(setCounts);
                state.setMetadataFormats(metadataFormats);
            } else {
                if(metadataFormats.size() == 0) { setStateToFailed("GOT 0 METADATAFORMATS"); }
                if(licenceCounts.size() == 0) { setStateToFailed("GOT 0 LICENCES"); }
            }

        } catch (Exception e) {
            e.printStackTrace();
            setStateToFailed("UNHANDLED EXCEPTION: " + e.getMessage());
        }
    }

    private Set<LicenceCount> createLicenceCounts(List<HarvestedRecord> recordsRaw, HarvestingState state) {
        logger.info("Creating LicenceCounts (JavaModel) for repo: {}", repository.getHarvesting_url());

        Set<String> licences_raw = recordsRaw.stream().
                map(harvestedRecord -> harvestedRecord.rights).
                collect(Collectors.toSet());

        Set<LicenceCount> licenceCounts = licences_raw.stream().
                map(licence_raw -> new LicenceCount(licence_raw, state)).
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