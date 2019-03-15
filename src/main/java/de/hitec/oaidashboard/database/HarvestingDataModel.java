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

public class HarvestingDataModel {

    private final Repository repository;
    private final DataHarvester dataHarvester;
    private final SessionFactory factory;

    private HarvestingState state = null;

    private List<Record> records = null;

    private static Logger logger = LogManager.getRootLogger();

    public HarvestingDataModel(Repository repository, DataHarvester dataHarvester, SessionFactory factory) {

        this.repository = repository;
        this.dataHarvester = dataHarvester;
        this.factory = factory;

        initDataModel();
    }

    private void initDataModel() {

        List<MetadataFormat> metadataFormats = null; // metadataformats may not be empty -> do not instantiate empty list
        List<OAISet> oaiSets = new ArrayList<>(); // oaiSets may be empty
        //List<Record> records = null; // records may not be empty -> do not instantiate empty list
        List<Licence> licences = null; // licences may be not be empty -> do not instantiate empty list

        // convert all Data (raw) to Java/Hibernate-DataModel
        try {
            logger.info("Getting MetadaFormats (JavaModel) from Database or creating new for repo: {}", repository.getHarvestingUrl());
            metadataFormats = getOrCreateFormats(dataHarvester.getMetadataFormats());

            logger.info("Getting OAISets (JavaModel) from Database or creating new for repo: {}", repository.getHarvestingUrl());
            oaiSets = getOrCreateOAISets(dataHarvester.getSets());

            logger.info("Getting Records (JavaModel) from Database or creating new for repo: {}", repository.getHarvestingUrl());
            records = createRecords(dataHarvester.getRecords());

            logger.info("Getting Licences (JavaModel) from Database or creating new for repo: {}", repository.getHarvestingUrl());
            licences = getOrCreateLicences(dataHarvester.getRecords());

        } catch (DataModelException dataModelException) {
            dataModelException.printStackTrace();
            // TODO: create a FAILED empty state instead of SUCCESS
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: create a FAILED empty state instead of SUCCESS
        }

        // create state SUCCESS and connect all data
        state =	new HarvestingState(Timestamp.valueOf(LocalDateTime.now()), repository, "SUCCESS");

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

            //state.setOaiSets(oaiSets);
        }
    }

    private void mapStateToSets(HarvestingState state, List<OAISet> oaiSets) {
        Set<StateSetMapper> stateSetMappers = new HashSet<>();
        for(OAISet oaiSet: oaiSets) {
            StateSetMapper stateSetMapper = new StateSetMapper(state, oaiSet); // reuse of StateSetMapper from Database makes no sense
            stateSetMapper.setRecordCount(10);
            stateSetMappers.add(stateSetMapper);
        }
        state.setStateSetMappers(stateSetMappers);
    }

    private void mapRecordsToState(HarvestingState state, List<Record> records) {
        for(Record record: records) {
            record.setState(state);
        }
    }

    private void mapRecordsToSets(List<Record> records, List<OAISet> oaiSets) {
        for(Record record: records) {
            List<OAISet> mappedSets = new ArrayList<>();
            List<String> set_specs = record.getSet_specs();
            for(OAISet oaiSet: oaiSets) {
                for(String set_spec: set_specs) {
                    if (set_spec.equals(oaiSet.getSpec())) {
                        mappedSets.add(oaiSet);
                    }
                }
            }
            record.setOaiSets(mappedSets);
        }
    }

    private void mapRecordsToLicences(List<Record> records, List<Licence> licences) {
        for(Record record: records) {
            Licence mappedLicence = null;
            String licence_str = record.getLicence_str();
            logger.debug("record: '{}', licence_str: '{}'", record.getIdentifier(), licence_str);
            for(Licence licence: licences) {
                if(licence_str.equals(licence.getName())){
                    mappedLicence = licence;
                    break;
                }
            }
            if(mappedLicence != null) {
                record.setLicense(mappedLicence);
            } else {
                // TODO: no licence found, how can that happen and what shall we do then?
            }
        }
    }

    private List<MetadataFormat> getOrCreateFormats(List<Format> metadataFormatsRaw) throws DataModelException {
        List<MetadataFormat> metadataFormats = new ArrayList<>();
        for (Format mFormat : metadataFormatsRaw) {
            MetadataFormat mf = getMFormatObject(mFormat.metadataPrefix, mFormat.schema, mFormat.metadataNamespace);
            if(mf != null) {
                logger.debug("Got MetadataFormat from Database - name: {} id: {}", mf.getFormatPrefix(), mf.getMetadataformat_id());
            } else if (mf == null) {
                logger.debug("Creating new MetadataFormat with prefix: {}", mFormat.metadataPrefix);
                mf = new MetadataFormat(mFormat.metadataPrefix, mFormat.schema, mFormat.metadataNamespace);
            }
            metadataFormats.add(mf);
        }
        return metadataFormats;
    }

    private List<OAISet> getOrCreateOAISets(List<MethaSet> setsRaw) throws DataModelException {
        List<OAISet> oaiSets = new ArrayList<>();
        for (MethaSet set : setsRaw) {
            OAISet oaiSet = getSetObject(set.setName, set.setSpec);
            if(oaiSet != null) {
                logger.debug("Got OAISet from Database - name: '{}', spec: '{}', id: {}", oaiSet.getName(), oaiSet.getSpec(), oaiSet.getId());
            } else if (oaiSet == null) {
                logger.debug("Creating new OAISet with name: '{}' and spec: '{}'", set.setName, set.setSpec);
                oaiSet = new OAISet(set.setName, set.setSpec);
            }
            oaiSets.add(oaiSet);
        }
        return oaiSets;
    }

    private List<Licence> getOrCreateLicences(List<HarvestedRecord> recordsRaw) throws DataModelException {
        List<Licence> licences = new ArrayList<>();
        for(HarvestedRecord recordRaw: recordsRaw) {
            String licence_str = recordRaw.rights;
            Licence licence = getLicenceObject(licence_str);
            if(licence != null) {
                logger.debug("Got Licence from Database - name: '{}', id: {}", licence.getName(), licence.getId());
            } else if(licence == null) {
                logger.debug("Creating new Licence with name: '{}'", licence_str);
                licence = new Licence(licence_str);
            }
            licences.add(licence);
        }
        return licences;
    }

    // TODO: currently we always need to create new records, there seems to be no way to prevent this, no matter the solution (with a mapping table, there will be a number of new mappings equal the number of records, each time (harvestRun)
/*    private List<Record> getOrCreateRecords(List<HarvestedRecord> recordsRaw) throws DataModelException {
        List<Record> records = new ArrayList<>();
        for (HarvestedRecord recordRaw : recordsRaw) {
            Record record = getRecordObject(recordRaw.identifier);
            if(record != null) {
                logger.info("Got Record from Database - identifier: '{}'", record.getIdentifier());
            } else if (record == null) {
                logger.info("Creating new Record with identifier: '{}'", recordRaw.identifier);
                record = new Record(recordRaw.identifier);
            }
            record.setSet_specs(recordRaw.specList); // set spec list from raw record, not managed by Hibernate
            records.add(record);
        }
        return records;
    }*/

    private List<Record> createRecords(List<HarvestedRecord> recordsRaw) throws DataModelException {
        List<Record> records = new ArrayList<>();
        for (HarvestedRecord recordRaw : recordsRaw) {
            logger.debug("Creating new Record with identifier: '{}'", recordRaw.identifier);
            Record record = new Record(recordRaw.identifier);
            record.setSet_specs(recordRaw.specList); // set spec list from raw record, not managed by Hibernate
            record.setLicence_str(recordRaw.rights); // licence_str from raw record, not managed by Hibernate
            records.add(record);
        }
        return records;
    }


    private OAISet getSetObject(String name, String spec) throws DataModelException {
        OAISet oaiSet = null;
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<OAISet> criteria = builder.createQuery(OAISet.class);

            Root<OAISet> root = criteria.from(OAISet.class);
            criteria.select(root).where(builder.equal(root.get("spec"), spec));

            Query<OAISet> q = session.createQuery(criteria);
            if (!q.getResultList().isEmpty()) {
                oaiSet = (OAISet) q.uniqueResult();
            }
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
            throw new DataModelException(e.getMessage());
        } finally {
            session.close();
        }
        return oaiSet;
    }

    private MetadataFormat getMFormatObject(String prefix, String schema, String ns) throws DataModelException {
        MetadataFormat mf = null;
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<MetadataFormat> criteria = builder.createQuery(MetadataFormat.class);

            Root<MetadataFormat> root = criteria.from(MetadataFormat.class);
            criteria.select(root).where(builder.equal(root.get("formatPrefix"), prefix));

            Query<MetadataFormat> q = session.createQuery(criteria);
            if (!q.getResultList().isEmpty()) {
                mf = (MetadataFormat) q.uniqueResult();
            }
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
            throw new DataModelException(e.getMessage());
        } finally {
            session.close();
        }
        return mf;
    }

	private Licence getLicenceObject(String rights) throws DataModelException {
		Licence lic = null;
		Session session = factory.openSession();
		Transaction tx = null;

		try {
			tx = session.beginTransaction();

			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Licence> criteria =
					builder.createQuery(Licence.class);

			Root<Licence> root = criteria.from(Licence.class);
			criteria.select(root).where(builder.equal(root.get("name"), rights));
			Query<Licence> q = session.createQuery(criteria);
			if (!q.getResultList().isEmpty()) {
				lic = (Licence) q.getResultList().get(0);
			}
			tx.commit();
		} catch (Exception e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
			throw new DataModelException(e.getMessage());
		} finally {
			session.close();
		}
		return lic;
	}

    private Record getRecordObject(String identifier) throws DataModelException {
        Record mf = null;
        Session session = factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Record> criteria = builder.createQuery(Record.class);

            Root<Record> root = criteria.from(Record.class);
            criteria.select(root).where(builder.equal(root.get("identifier"), identifier));

            Query<Record> q = session.createQuery(criteria);
            if (!q.getResultList().isEmpty()) {
                mf = (Record) q.uniqueResult();
            }
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
            throw new DataModelException(e.getMessage());
        } finally {
            session.close();
        }
        return mf;
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

                // TODO: do not save records (or anything other than a failed state) when 'fallback' is triggered
                // TODO: change 'fallback' to a special method
                for(Record record: records) {
                    session.save(record);
                }
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.info("Exception while creating DataModel for repo: {}", repository.getHarvestingUrl(), e);
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
}
