package de.hitec.oaidashboard.database;

import de.hitec.oaidashboard.database.datastructures2.*;
import de.hitec.oaidashboard.database.validation.DataModelValidator;
import de.hitec.oaidashboard.parsers.datastructures.Format;
import de.hitec.oaidashboard.parsers.datastructures.HarvestedRecord;
import de.hitec.oaidashboard.parsers.datastructures.MethaSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HarvestingDataModel {

    private final Repository repository;
    private final DataHarvester dataHarvester;
    private final SessionFactory factory;

    private HarvestingState state = null;

    private List<HarvestedRecord> harvestedRecords = null; // records may not be empty -> do not instantiate empty list
    Set<SetCount> setCounts = new HashSet<>(); // SetCounts may be empty

    private static Logger logger = LogManager.getRootLogger();

    public HarvestingDataModel(Repository repository, DataHarvester dataHarvester, SessionFactory factory) {

        this.repository = repository;
        this.dataHarvester = dataHarvester;
        this.factory = factory;

        this.harvestedRecords = dataHarvester.getRecords();

        initDataModel();
    }

    private void initDataModel() {

        Set<MetadataFormat> metadataFormats = null; // metadataformats may not be empty -> do not instantiate empty list
        Set<LicenceCount> licenceCounts = null; // licenceCounts may be not be empty -> do not instantiate empty list

        // create state SUCCESS and connect all data
        state =	new HarvestingState(Timestamp.valueOf(LocalDateTime.now()), repository, "SUCCESS");

        // convert all Data (raw) to Java/Hibernate-DataModel
        try {

            logger.info("Creating LicenceCounts (JavaModel) for repo: {}", repository.getHarvestingUrl());
            licenceCounts = createLicenceCounts(dataHarvester.getRecords(), state);

            logger.info("Creating SetCounts (JavaModel) for repo: {}", repository.getHarvestingUrl());
            setCounts = createSetCounts(dataHarvester.getSets(), state);

            logger.info("Creating MetadaFormats (JavaModel) for repo: {}", repository.getHarvestingUrl());
            metadataFormats = createMetadataFormats(dataHarvester.getMetadataFormats(), state);

        } catch (DataModelException dataModelException) {
            dataModelException.printStackTrace();
            // TODO: create a FAILED empty state instead of SUCCESS
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: create a FAILED empty state instead of SUCCESS
        }

        //logger.info("Mapping Licences to current HarvestingState for repo: {}", repository.getHarvestingUrl());

        state.setLicenceCounts(licenceCounts);
        state.setSetCounts(setCounts);
        state.setMetadataFormats(metadataFormats);

/*
        if(metadataFormats != null && records != null) {
            logger.info("Adding MetadataFormats to current HarvestingState for repo: {}", repository.getHarvestingUrl());
            state.setMetadataFormats(metadataFormats);

            logger.info("Adding OAISets to current HarvestingState for repo: {}", repository.getHarvestingUrl());
            for(OAISet oaiSet: oaiSets) {
                DataModelValidator.isValidOAISet(oaiSet);
            }
            mapStateToSets(state, oaiSets);

            logger.info("Adding Records to current HarvestingState for repo: {}", repository.getHarvestingUrl());
            mapRecordsToState(state, records);

            logger.info("Mapping Records and Sets for current HarvestingState for repo: {}", repository.getHarvestingUrl());
            mapRecordsToSets(records, oaiSets);

            logger.info("Mapping Records and Licences to current HarvestingState for repo: {}", repository.getHarvestingUrl());
            mapRecordsToLicences(records, licences);

            logger.info("Mapping Licences to State for current HarvestingState for repo: {}", repository.getHarvestingUrl());
            mapStateToLicences(state, licences);
        }
*/
    }

    private Set<LicenceCount> createLicenceCounts(List<HarvestedRecord> recordsRaw, HarvestingState state) throws DataModelException {
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
        Set<SetCount> setCounts = new HashSet<>();
        for(MethaSet setRaw: setsRaw) {
            logger.debug("Creating new SetCount with set_name: '{}' and set_spec: '{}'", setRaw.setName, setRaw.setSpec);
            setCounts.add(new SetCount(setRaw.setName, setRaw.setSpec, state));
        }
        logger.debug("Created {} SetCounts!", setCounts.size());
        return setCounts;
    }

    private Set<MetadataFormat> createMetadataFormats(List<Format> formatsRaw, HarvestingState state) {
        Set<MetadataFormat> metadataFormats = new HashSet<>();
        for(Format formatRaw: formatsRaw) {
            logger.info("Creating new MetadataFormat with prefix: '{}', schema: {} and " +
                    "namespace: '{}'", formatRaw.metadataPrefix, formatRaw.schema, formatRaw.metadataNamespace);
            metadataFormats.add(new MetadataFormat(formatRaw.metadataPrefix, formatRaw.schema, formatRaw.metadataNamespace, state));
        }
        logger.info("Created {} MetadataFormats!", metadataFormats.size());
        return metadataFormats;
    }

    public void saveDataModel() {
        saveDataModel(false);
    }

    private void saveDataModel(boolean fallback) {
        if(state != null) {
            logger.info("Attempting to save HarvestingState into database for repo: {}", repository.getHarvestingUrl());
            Session session = factory.openSession();
            Transaction tx = session.beginTransaction();
            Object id = null;

            try {
                session.save(state);
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.info("Exception while creating DataModel for repo: {}", repository.getHarvestingUrl(), e);
                // TODO: Rework fallback-logic!
                if(!fallback) {
                    createFailedState();
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
            // TODO: create a FAILED empty state instead of SUCCESS and try to save it
        }
    }

    private void createFailedState() {
        state =	new HarvestingState(Timestamp.valueOf(LocalDateTime.now()), repository, "FAILURE");
    }

    public HarvestingState getState() {
        return this.state;
    }

    public List<HarvestedRecord> getHarvestedRecords() {
        return this.harvestedRecords;
    }

    public Set<SetCount> getSetCounts() {
        return this.setCounts;
    }

    public void validate() {
        logger.info("Validating DataModel against Hibernate annotations: {}", repository.getHarvestingUrl());
        for(LicenceCount licenceCount: state.getLicenceCounts()) {
            DataModelValidator.isValidLicenceCount(licenceCount);
        }
        for(SetCount setCount: state.getSetCounts()) {
            DataModelValidator.isValidSetCount(setCount);
        }
    }
}
