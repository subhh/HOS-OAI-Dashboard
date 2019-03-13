package de.hitec.oaidashboard.database;

import de.hitec.oaidashboard.database.datastructures2.HarvestingState;
import de.hitec.oaidashboard.database.datastructures2.MetadataFormat;
import de.hitec.oaidashboard.database.datastructures2.OAISet;
import de.hitec.oaidashboard.database.datastructures2.Repository;
import de.hitec.oaidashboard.database.validation.DataModelValidator;
import de.hitec.oaidashboard.parsers.datastructures.Format;
import de.hitec.oaidashboard.parsers.datastructures.MethaSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
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
import java.util.List;

public class HarvestingDataModel {

    private final Repository repository;
    private final DataHarvester dataHarvester;
    private final SessionFactory factory;

    private HarvestingState state = null;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    public HarvestingDataModel(Repository repository, DataHarvester dataHarvester, SessionFactory factory) {

        this.repository = repository;
        this.dataHarvester = dataHarvester;
        this.factory = factory;

        initDataModel();
    }

    private void initDataModel() {

        List<MetadataFormat> metadataFormats = null; // metadataformats may not be empty -> do not instantiate empty list
        List<OAISet> oaiSets = new ArrayList<>(); // oaiSets may be empty

        // convert all Data (raw) to Java/Hibernate-DataModel
        try {
            logger.info("Getting MetadaFormats (JavaModel) from Database or creating new for repo: {}", repository.getHarvestingUrl());
            metadataFormats = getOrCreateFormats(dataHarvester.getMetadataFormats());

            logger.info("Getting OAISets (JavaModel) from Database or creating new for repo: {}", repository.getHarvestingUrl());
            oaiSets = getOrCreateOAISets(dataHarvester.getSets());

        } catch (DataModelException dataModelException) {
            dataModelException.printStackTrace();
            // TODO: create a FAILED empty state instead of SUCCESS
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: create a FAILED empty state instead of SUCCESS
        }

        // create state SUCCESS and connect all data
        state =	new HarvestingState(Timestamp.valueOf(LocalDateTime.now()), repository, "SUCCESS");

        if(metadataFormats != null) {
            logger.info("Adding MetadataFormats to current HarvestingState for repo: {}", repository.getHarvestingUrl());
            state.setMetadataFormats(metadataFormats);
            logger.info("Adding OAISets to current HarvestingState for repo: {}", repository.getHarvestingUrl());
            for(OAISet oaiSet: oaiSets) {
                DataModelValidator.isValidOAISet(oaiSet);
            }
            state.setOaiSets(oaiSets);
        }
    }

    private List<MetadataFormat> getOrCreateFormats(List<Format> metadataFormatsRaw) throws DataModelException {
        List<MetadataFormat> metadataFormats = new ArrayList<>();
        for (Format mFormat : metadataFormatsRaw) {
            MetadataFormat mf = getMFormatObject(mFormat.metadataPrefix, mFormat.schema, mFormat.metadataNamespace);
            if(mf != null) {
                logger.info("Got MetadataFormat from Database - name: {} id: {}", mf.getFormatPrefix(), mf.getMetadataformat_id());
            } else if (mf == null) {
                logger.info("Creating new MetadataFormat with prefix: {}", mFormat.metadataPrefix);
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
                logger.info("Got OAISet from Database - name: '{}', spec: '{}', id: {}", oaiSet.getName(), oaiSet.getSpec(), oaiSet.getId());
            } else if (oaiSet == null) {
                logger.info("Creating new OAISet with name: '{}' and spec: '{}'", set.setName, set.setSpec);
                oaiSet = new OAISet(set.setName, set.setSpec);
            }
            oaiSets.add(oaiSet);
        }
        return oaiSets;
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

    public void saveDataModel() {
        saveDataModelIntern(false);
    }

    private void saveDataModelIntern(boolean fallback) {
        if(state != null) {
            Session session = factory.openSession();
            Transaction tx = session.beginTransaction();
            Object id = null;

            try {
                session.save(state);
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                logger.info("Exception while creating DataModel for repo: {}", repository.getHarvestingUrl(), e);
                if(!fallback) {
                    createFailedState();
                    session.close();
                    saveDataModelIntern(true);
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
