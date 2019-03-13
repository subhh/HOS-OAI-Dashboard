package de.hitec.oaidashboard.database;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import de.hitec.oaidashboard.database.datastructures2.HarvestingState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import de.hitec.oaidashboard.database.datastructures.License;
import de.hitec.oaidashboard.database.datastructures2.MetadataFormat;
import de.hitec.oaidashboard.database.datastructures.RecordSetMapper;
import de.hitec.oaidashboard.database.datastructures2.Repository;
import de.hitec.oaidashboard.database.datastructures.Set;
// HarvestingState could be confused with Thread.HarvestingState
//import de.hitec.oaidashboard.database.datastructures.HarvestingState;
import de.hitec.oaidashboard.database.datastructures.StateLicenseMapper;
import de.hitec.oaidashboard.database.datastructures.StateSetMapper;
import de.hitec.oaidashboard.parsers.JsonParser;
import de.hitec.oaidashboard.parsers.datastructures.Format;
import de.hitec.oaidashboard.parsers.datastructures.MethaIdStructure;
import de.hitec.oaidashboard.parsers.datastructures.MethaSet;

public class Harvester2 extends Thread {

    public Thread t;
    private Repository repo;
    private String metaIdPath;
    private String metaSyncPath;
    private String gitDirectory;
    private String exportDirectory;
    private SessionFactory factory;
    private HarvestingState state;
    public boolean reharvest;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    Harvester2(Repository repo, String mip, String msp,
              String gd, String ed, SessionFactory factory) {
        this.repo = repo;
        this.metaIdPath = mip;
        this.metaSyncPath = msp;
        this.gitDirectory = gd;
        this.exportDirectory = ed;
        this.factory = factory;
    }

    public void run() {
        MethaIdStructure instance = null;
        instance = getMetaIdAnswer();

        if (instance != null)
        {
            state =	new HarvestingState(
                    Timestamp.valueOf(LocalDateTime.now()), repo, "SUCCESS");
            //saveData(state);
            //System.out.println("Repo-date:" + instance.identify.earliestDatestamp.toString());
            // TODO: where to place this in new structure?
            if (repo.updateOnChange(instance.identify.repositoryName,
                    instance.identify.baseURL, instance.identify.adminEmail.get(0))) {
                updateData(repo);
            }
            //logger.info("Adding sets to current HarvestingState of repo: {}", repo.getHarvestingUrl());
            //saveSets(instance.sets);

            logger.info("Adding MetadataFormats to current HarvestingState for repo: {}", repo.getHarvestingUrl());
            for(Format format: instance.formats) {
                logger.info("FORMAT: {}", format.metadataPrefix);
            }
            addFormats(instance.formats, state);
            for(MetadataFormat format: state.getMetadataFormats()) {
                logger.info("format: {} id: ", format.getFormatPrefix(), format.getMetadataformat_id());
            }
            saveData(state);

            //logger.info("Starting Metha Sync for repo: {}", repo.getHarvestingUrl());
            //startMethaSync();
            //markSomeLicensesAsOpenOrClose();
            //computeStatistics();
        }
    }

    public void start() {
        System.out.println("Start Harvesting " + repo.getHarvestingUrl() );
        if (t == null) {
            t = new Thread (this);
            t.start();
        }
    }

    private Object saveData(Object input)
    {
        Session session = factory.openSession();
        Transaction tx = session.beginTransaction();
        Object id = null;

        try {
            id = (Object) session.save(input);
            tx.commit();
        } catch (HibernateException e) {
            if (tx!=null) tx.rollback();
            logger.info("HibernateException while harvesting repo: {}", repo.getHarvestingUrl(), e);
        } finally {
            session.close();
        }
        return id;
    }

    public void updateData(Object input){
        Session session = factory.openSession();
        Transaction tx = session.beginTransaction();

        try {
            session.update(input);
            tx.commit();
        } catch (HibernateException e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    public String getRepoUrl() {
        return repo.getHarvestingUrl();
    }

    private Set getSetObject(String setSpec) {
        Set set = null;
        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Set> criteria = builder.createQuery(Set.class);

            Root<Set> root = criteria.from(Set.class);
            criteria.select(root).where(builder.equal(root.get("spec"), setSpec));
            Query<Set> q = session.createQuery(criteria);
            if (!q.getResultList().isEmpty())
            {
                set = (Set) q.getResultList().get(0);
            }
            tx.commit();
        } catch (HibernateException e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return set;
    }

    private MetadataFormat getMFormatObject(String prefix, String schema, String ns) {
        MetadataFormat mf = null;
        Session session = factory.openSession();
        Transaction tx = null;
        logger.info("getMFormatObject for prefix: {}", prefix);
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
        } catch (HibernateException e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        if(mf != null) {
            logger.info("TEST: {}", mf.getFormatPrefix());
        }
        logger.info("PONG");
        return mf;
    }

    private License getLicenseObject(String rights) {
        License lic = null;
        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<License> criteria =
                    builder.createQuery(License.class);

            Root<License> root = criteria.from(License.class);
            criteria.select(root).where(builder.equal(root.get("name"), rights));
            Query<License> q = session.createQuery(criteria);
            if (!q.getResultList().isEmpty()) {
                lic = (License) q.getResultList().get(0);
            }
            tx.commit();
        } catch (HibernateException e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return lic;
    }

    private void saveSets(ArrayList<MethaSet> sets) {
        // TODO
    }

    private void addFormats(ArrayList<Format> formats, HarvestingState harvestingState) {
        logger.info("start adding formats");
        for (Format mFormat : formats) {
            logger.info("mFormat name: {}", mFormat.metadataPrefix);
            MetadataFormat mf = getMFormatObject(
                    mFormat.metadataPrefix, mFormat.schema, mFormat.metadataNamespace);
            if(mf != null) {
                logger.info("name: {} id: {}", mf.getFormatPrefix(), mf.getMetadataformat_id());
            }
            if (mf == null) {
                logger.info("CREATING NEW METADATAFORMAT WITH PREFIX: {}", mFormat.metadataPrefix);
                mf = new MetadataFormat(
                        mFormat.metadataPrefix, mFormat.schema, mFormat.metadataNamespace);
            }
            harvestingState.addMetadataFormat(mf);
        }
    }

    private MethaIdStructure getMetaIdAnswer() {
        MethaIdStructure instance = null;
        InputStream inStream = null;
        try
        {
            ProcessBuilder pb = new ProcessBuilder(metaIdPath,
                    repo.getHarvestingUrl());
            Process p = pb.start();
            inStream = p.getInputStream();
            String storeToFile = gitDirectory + "/MethaID-Answer-" +
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".json";
            JsonParser jParser = new JsonParser(inStream, storeToFile);
            instance = jParser.getJsonStructure();
            inStream.close();
            return instance;
        }
        catch (Throwable ex) {
            System.err.println("Failed to run Metadata-Harvestor." + ex);
            HarvestingState state =
                    new HarvestingState(
                            Timestamp.valueOf(LocalDateTime.now()), repo, "FAILED");
            saveData(state);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private void markSomeLicensesAsOpenOrClose() {
        // Set the type of licenses to 'open' or 'close' will be done via web interface
        // later on, but for testing purposes this should be fine.

        Session session = factory.openSession();
        Transaction tx = null;
        ArrayList<License> lics = null;

        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<License> criteria =
                    builder.createQuery(License.class);

            Root<License> root = criteria.from(License.class);

            criteria.select(root).where(builder.or(
                    builder.like(root.get("name"), "%creativecommons%"),
                    builder.like(root.get("name"), "%openAccess%")));

            Query<License> q = session.createQuery(criteria);
            if (!q.getResultList().isEmpty()) {
                lics = (ArrayList<License>) q.getResultList();
            }
            for (License lic : lics) {
                lic.setType("OPEN");
                session.update(lic);
            }
            criteria.select(root).where(
                    builder.like(root.get("name"), "%embargoedAccess%"));
            q = session.createQuery(criteria);
            if (!q.getResultList().isEmpty()) {
                lics = (ArrayList<License>) q.getResultList();
            }
            for (License lic : lics) {
                lic.setType("CLOSED");
                session.update(lic);
            }
            tx.commit();
        } catch (HibernateException e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    private void storeStateLicenseMapper()
    {
        // TODO
    }

    private void storeStateSetMapper()
    {
        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
            Root<RecordSetMapper> root = criteria.from(RecordSetMapper.class);
            criteria.multiselect(root.get("set"), builder.count(root));
            criteria.groupBy(root.get("set"));

            Query<Tuple> q = session.createQuery(criteria);
            ArrayList<Tuple> resultList = (ArrayList<Tuple>) q.getResultList();

            for(Tuple tuple : resultList) {
                if ((long) tuple.get(1) > 0) // default is 0, no update needed then
                {
                    CriteriaBuilder builder2 = session.getCriteriaBuilder();
                    CriteriaQuery<StateSetMapper> criteria2 =
                            builder2.createQuery(StateSetMapper.class);

                    Root<StateSetMapper> root2 = criteria2.from(StateSetMapper.class);
                    criteria2.select(root2).where(
                            builder2.equal(root2.get("state"), state),
                            builder2.equal(root2.get("set"), (Set) tuple.get(0)));
                    Query<StateSetMapper> q2 = session.createQuery(criteria2);
                    if (!q2.getResultList().isEmpty()) {
                        StateSetMapper ssm = (StateSetMapper) q2.getResultList().get(0);
                        ssm.setRecordCount((long) tuple.get(1));
                        session.update(ssm);
                    }
                }
            }
            tx.commit();
        } catch (HibernateException e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    private void computeStatistics() {
        storeStateLicenseMapper();
        storeStateSetMapper();
        computeStateRecordCount();
    }

    private void computeStateRecordCount() {

        // SELECT SUM(STATELICENSEMAPPER.record_count), LICENSE.license_type
        // FROM STATELICENSEMAPPER, LICENSE
        // WHERE LICENSE.license_id = STATELICENSEMAPPER.license_id
        // AND STATELICENSEMAPPER.state_id = <actual state_id>
        // GROUP BY LICENSE.license_type;

        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
            Root<StateLicenseMapper> root = criteria.from(StateLicenseMapper.class);
            Join<StateLicenseMapper, License> join = root.join("license", JoinType.INNER);

            criteria.multiselect(join.get("type"), builder.sum(root.get("recordCount"))).where(
                    builder.equal(root.get("state"), state));
            criteria.groupBy(join.get("type"));

            Query<Tuple> q = session.createQuery(criteria);
            ArrayList<Tuple> resultList = (ArrayList<Tuple>) q.getResultList();
            Long allRecords = 0l;
            for (Tuple tpl : resultList) {
                if (((String) tpl.get(0)).equals("OPEN"))
                {
                    state.setRecordCountOA((long) tpl.get(1));
                }
                allRecords += (long) tpl.get(1);
            }
            state.setRecordCount(allRecords);
            session.saveOrUpdate(state);
            tx.commit();
        } catch (HibernateException e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }


    private void startMethaSync() {
        // TODO
    }
}
