package de.hitec.oaidashboard.database;

import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import de.hitec.oaidashboard.database.datastructures.License;
import de.hitec.oaidashboard.database.datastructures.MetadataFormat;
import de.hitec.oaidashboard.database.datastructures.Record;
import de.hitec.oaidashboard.database.datastructures.RecordSetMapper;
import de.hitec.oaidashboard.database.datastructures.Repository;
import de.hitec.oaidashboard.database.datastructures.Set;
// HarvestingState could be confused with Thread.HarvestingState
//import de.hitec.oaidashboard.database.datastructures.HarvestingState;
import de.hitec.oaidashboard.database.datastructures.StateFormatMapper;
import de.hitec.oaidashboard.database.datastructures.StateLicenseMapper;
import de.hitec.oaidashboard.database.datastructures.StateSetMapper;
import de.hitec.oaidashboard.parsers.JsonParser;
import de.hitec.oaidashboard.parsers.XmlParser;
import de.hitec.oaidashboard.parsers.datastructures.Format;
import de.hitec.oaidashboard.parsers.datastructures.HarvestedRecord;
import de.hitec.oaidashboard.parsers.datastructures.MethaIdStructure;
import de.hitec.oaidashboard.parsers.datastructures.MethaSet;

public class Harvester extends Thread {
	
	public Thread t;
	private Repository repo;
	private String metaIdPath;
	private String metaSyncPath;
	private String gitDirectory;
	private String exportDirectory;
	private SessionFactory factory;
	private de.hitec.oaidashboard.database.datastructures.State state;
	public boolean reharvest;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    Harvester(Repository repo, String mip, String msp,
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
			state =	new de.hitec.oaidashboard.database.datastructures.State(
					Timestamp.valueOf(LocalDateTime.now()), repo, "SUCCESS");
            saveData(state);
			//System.out.println("Repo-date:" + instance.identify.earliestDatestamp.toString());
			if (repo.updateOnChange(instance.identify.repositoryName,
					instance.identify.baseURL, instance.identify.adminEmail.get(0)))
			{
				updateData(repo);
			}
			logger.info("Attempting to save sets for repo: {}", repo.getHarvestingUrl());
			saveSets(instance.sets);
            logger.info("Attempting to save formats for repo: {}", repo.getHarvestingUrl());
			saveFormats(instance.formats);
            logger.info("Starting Metha Sync for repo: {}", repo.getHarvestingUrl());
			startMethaSync();
			markSomeLicensesAsOpenOrClose();
			computeStatistics();
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
			
		try {
			tx = session.beginTransaction();
			
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<MetadataFormat> criteria = 
					builder.createQuery(MetadataFormat.class);

			Root<MetadataFormat> root = criteria.from(MetadataFormat.class);
			criteria.select(root).where(builder.equal(root.get("formatPrefix"), prefix));
/*					builder.and(builder.equal(root.get("formatPrefix"), prefix),
			                    builder.equal(root.get("schema"), schema),
			                    builder.equal(root.get("namespace"), ns))); */
			Query<MetadataFormat> q = session.createQuery(criteria);
			if (!q.getResultList().isEmpty()) {
				mf = (MetadataFormat) q.getResultList().get(0);
			}
			tx.commit();
		} catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace(); 
		} finally {
			session.close();
		}
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
		for (MethaSet mSet : sets)
		{
			Set set = getSetObject(mSet.setSpec);
			Object status = null; // returns id if save succeeds
			if (set == null)
			{
                logger.info("Instantiating new Set with name: '{}' and spec: '{}' for repo: {}", mSet.setName, mSet.setSpec, repo.getHarvestingUrl());
				set = new Set(mSet.setName, mSet.setSpec);

				status = saveData(set);
				if (status != null)
				{
					StateSetMapper ssm = new StateSetMapper(state, set);
					saveData(ssm);
				}
			}
			else
			{
				StateSetMapper ssm = new StateSetMapper(state, set);
				saveData(ssm);
			}
		}		
	}

	private void saveFormats(ArrayList<Format> formats) {
		for (Format mFormat : formats) {
			MetadataFormat mf = getMFormatObject(
					mFormat.metadataPrefix, mFormat.schema, mFormat.metadataNamespace);
			Object status = null; // returns id if save succeeds
			if (mf == null)
			{
				mf = new MetadataFormat(
						mFormat.metadataPrefix, mFormat.schema, mFormat.metadataNamespace);
				status = saveData(mf);
			}
			if (status != null)
			{
				StateFormatMapper sfm = new StateFormatMapper(state, mf);
				saveData(sfm);
			}
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
			de.hitec.oaidashboard.database.datastructures.State state = 
					new de.hitec.oaidashboard.database.datastructures.State(
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
		// "SELECT COUNT(*), license_id  FROM RECORD GROUP BY license_id"

		Session session = factory.openSession();
		Transaction tx = null;
			
		try {
			tx = session.beginTransaction();

			CriteriaBuilder builder = session.getCriteriaBuilder();
	        CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
	        Root<Record> root = criteria.from(Record.class);
	        criteria.multiselect(root.get("license"), builder.count(root)).where(
	        		builder.equal(root.get("state"), state));
	        criteria.groupBy(root.get("license"));
	        
	        Query<Tuple> q = session.createQuery(criteria);
	        ArrayList<Tuple> resultList = (ArrayList<Tuple>) q.getResultList();
	        		
	        for(Tuple tuple : resultList) {
	        	StateLicenseMapper slm = new StateLicenseMapper(
	        			state, (License) tuple.get(0), (long) tuple.get(1));
	        	session.save(slm);
	        }
			tx.commit();
		} catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace(); 
		} finally {
			session.close();
		}
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
		ArrayList<HarvestedRecord> records = null;
		try {
			Calendar calendar = null;

            // metha-id generates a directory name by concatenating the requested 'set' string and '#'
            // and the requested format string (as default) 'oai_dc' and '#' together with the given url.
            // Then, the whole string is base64-encoded.
            // This directory name can be retrieved with the following call:
            // metha-sync -dir -base-dir <exportDirectory> <repo.getHarvestingUrl()>.
            // but the direct computation is most probably better performing:

            String urlString = "#oai_dc#" + repo.getHarvestingUrl();

            File dir = new File(exportDirectory + (String) File.separator + 
            		Base64.getUrlEncoder().withoutPadding().encodeToString(urlString.getBytes("UTF-8")));

            
			ProcessBuilder pb = new ProcessBuilder(metaSyncPath, 
            		"-no-intervals", "-base-dir", exportDirectory,
            		repo.getHarvestingUrl());
            
			calendar = Calendar.getInstance();
            state.setstartTime(new java.sql.Timestamp(
            		calendar.getTime().getTime()));
            if (reharvest) {
            	
                for (File filepath: dir.listFiles()) {
                	if (filepath.getName().endsWith(".xml.gz")) {
                		filepath.delete();
                	}
                }
                Process p = pb.start();
            	p.waitFor();
	            
	            if (p.getErrorStream().available() > 0) {
	            	System.err.println(IOUtils.toString(p.getErrorStream(), "UTF-8"));
	            }
            }
			calendar = Calendar.getInstance();
	        state.setendTime(new java.sql.Timestamp(
            		calendar.getTime().getTime()));
           
            XmlParser xmlparser = new XmlParser();
            
            if (dir.listFiles().length == 0) 
            {
            	state.setStatus("FAILED");            	
            }
            updateData(state);
            
            Calendar earliestDate = Calendar.getInstance();
            Calendar latestDate = javax.xml.bind.DatatypeConverter.parseDateTime("1000-01-01T12:00:00Z");
            boolean dateChanged = false;
            for (File filepath: dir.listFiles()) {
            	if (filepath.getName().endsWith(".xml.gz")) {
            		records = xmlparser.getRecords(
            				FileSystems.getDefault().getPath(filepath.getCanonicalPath()), 
            				FileSystems.getDefault().getPath(gitDirectory));
            		for (HarvestedRecord hRecord : records)
            		{
            			Calendar recordDate = javax.xml.bind.DatatypeConverter.parseDateTime(hRecord.dateStamp);
            			if (recordDate.after(latestDate))
            			{
            				latestDate = recordDate;
            				dateChanged = true;
            			}
            			if (recordDate.before(earliestDate))
            			{
            				earliestDate = recordDate;            				
            				dateChanged = true;
            			}            			
            			// store License first, if not already stored
            			License lic = getLicenseObject(hRecord.rights);
            			Object status = null; // returns id if save succeeds
            			if (lic == null)
            			{
            				lic = new License(hRecord.rights);
            				status = saveData(lic);
            			}
            			Record storeRecord = new Record(lic, state);
            			status = saveData(storeRecord);
            			if (status != null)
            			{
            				for (String recordSpec : hRecord.specList)
            				{
            					Set set = getSetObject(recordSpec);
            					// if the setSpec associated with a record can be matched
            					// with a set from ListSets, store the setId in the mapper table,
            					// otherwise, store the setSpec string.
            					if (set != null) {
            						RecordSetMapper rsmap = new RecordSetMapper(storeRecord, set);
            						saveData(rsmap);
            					}
            					else {
            						RecordSetMapper rsmap = new RecordSetMapper(storeRecord, recordSpec);
            						saveData(rsmap);
            					}            			
            				}
            			}
            		}
            	}
            }
            if (dateChanged) {
	            state.setEarliestRecordTimestamp(new java.sql.Timestamp(earliestDate.getTime().getTime()));
	            state.setLatestRecordTimestamp(new java.sql.Timestamp(latestDate.getTime().getTime()));
	            updateData(state);
            }
		}
		catch (Throwable ex)
		{
			System.err.println("Failed to run Record-Harvester." + ex);
		}
	}
}
