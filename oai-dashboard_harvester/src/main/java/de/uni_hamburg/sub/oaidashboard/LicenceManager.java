package de.uni_hamburg.sub.oaidashboard;

import de.uni_hamburg.sub.oaidashboard.database.datastructures.Licence;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.LicenceType;
import de.uni_hamburg.sub.oaidashboard.importexport.JsonParser;
import de.uni_hamburg.sub.oaidashboard.importexport.datastructures.JsonLicence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class LicenceManager {
	
	private static Hashtable<String, Set<Licence>> licences = new Hashtable<String, Set<Licence>> ();
	private static SessionFactory factory;
    private static Logger logger = LogManager.getLogger(Class.class.getName());
    private static JsonParser licenceJsonParser;
    private static Set<JsonLicence> jLicenceSet;
    private static Timestamp earliestChange = Timestamp.valueOf(LocalDateTime.now());
    
	public static void initManager(SessionFactory factory, String licenceFile) {
		LicenceManager.factory = factory;
		licenceJsonParser = new JsonParser(licenceFile);
		getLicences();
		jLicenceSet = licenceJsonParser.getLicences();
		updateLicences();
	}
	
	private static void updateLicences() {
		for (JsonLicence jlic : jLicenceSet) {
			if (licences.containsKey(jlic.licence_name)) {
				Set<Licence> dbLicences = licences.get(jlic.licence_name);

				// So far, there's only one entry of this name in licences:
				if (dbLicences.size() == 1)	{
					Licence dblic = dbLicences.iterator().next();
					
					// type has changed, without timestamp restrictions
					if (jlic.validFrom.equals(dblic.getValidFrom()) && (jlic.validUntil == dblic.getValidUntil() || jlic.validUntil.equals(dblic.getValidUntil()))) {
						if (dblic.getType() != jlic.licence_type) {
							if (dblic.getValidFrom().before(earliestChange)) {
								earliestChange = dblic.getValidFrom();
							}
							dblic.setType(jlic.licence_type);
							updateLicence(dblic);
							updateLicenceCount(dblic);
						} // else: no changes
					}
					// licence-validUntil timestamp has been set, we expect to have another JSON-Entry
					// of the same name, different type and timerange after this one (see next else-clause). 
					else if (jlic.validFrom.equals(dblic.getValidFrom()) && jlic.licence_type.equals(dblic.getType()) ){
						dblic.setValidUntil(jlic.validUntil);
						updateLicence(dblic);
					}
					// Caution: This will only work for exactly one additional licencetype/date combo (hopefully the only case) 
					// This is "the second entry" (see above), the licence of this type is expected to be actually valid.
					else if (dblic.getType() != jlic.licence_type && jlic.validFrom.after(dblic.getValidFrom())) {
						if (dblic.getValidFrom().before(earliestChange)) {
							earliestChange = dblic.getValidFrom();
						}
						Licence lic = new Licence(jlic.licence_name);
						lic.setType(jlic.licence_type);
						lic.setValidFrom(jlic.validFrom);
						lic.setValidUntil(jlic.validUntil);
						storeNewLicence(lic);
						updateLicenceCount(lic);						
					}
				}
				else { // there are already two or more entries
					
					// this is supposed to work comparable as above:
					// Situation in db:
					// 1: licname a, lictype x, valid_form time1, valid_until time2
					// 2: licname a, lictype y, valid_from time2+1, valid_until null
					// should be changed into:
					// 1: (no changes)
					// 2: licname a, lictype y, valid_from time2+1, valid_until time3
					// 3: licname a, lictype z, valid_from time3+1, valid_until null
					
					// here change of (2):
					boolean found = false;
					for (Licence lic : dbLicences) {
						if (jlic.validFrom.equals(lic.getValidFrom()) && jlic.licence_type.equals(lic.getType())) {
							if (!(jlic.validUntil == lic.getValidUntil() || jlic.validUntil.equals(lic.getValidUntil()))) {
								lic.setValidUntil(jlic.validUntil);
								updateLicence(lic);
							}
							found = true;
						}
					};
					// add (3):
					if (!found) {
						if (jlic.validFrom.before(earliestChange)) {
							earliestChange = jlic.validFrom;
						}
						Licence lic = new Licence(jlic.licence_name);
						lic.setType(jlic.licence_type);
						lic.setValidFrom(jlic.validFrom);
						lic.setValidUntil(jlic.validUntil);
						storeNewLicence(lic);
						updateLicenceCount(lic);												
					}				
				}
			}
			else { // completely new licence
				Licence lic = new Licence(jlic.licence_name);
				lic.setType(jlic.licence_type);
				lic.setValidFrom(jlic.validFrom);
				lic.setValidUntil(jlic.validUntil);
				storeNewLicence(lic);
				licences.put(lic.getName(), new HashSet<> (Arrays.asList(lic)));
			}	
		}
//		if (earliestChange.before(Timestamp.valueOf(LocalDateTime.now().minusHours(10)))) {
		if (earliestChange.after(Timestamp.valueOf(LocalDateTime.now().minusHours(10)))) {
			updateRecCountOA();
		}
	}

	public static void writeLicencesToFile() {
		licenceJsonParser.setLicences(licences);
	}
	
	public static LicenceType getType(String name, Timestamp date) {		
		Set<Licence> licenceset = null;
		LicenceType type = null;
		
		if (licences.containsKey(name)) {
			licenceset = licences.get(name);
			if (licenceset.size() == 1)
			{
				type = licenceset.iterator().next().getType();
			}
			else {
				for (Licence lic : licenceset) {
					if (lic.getValidFrom().before(date) && 
							(lic.getValidUntil() == null || lic.getValidUntil().after(date))) {
						type = lic.getType();
					}
				}
			}
		} else {
			Licence lic = new Licence(name);
			storeNewLicence(lic);
			type = lic.getType();
			licences.put(name, new HashSet<Licence> (Arrays.asList(lic)));
		}
		return type;
	}
		
	private static void storeNewLicence(Licence lic) {
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();
		try {
			session.save(lic);
			tx.commit();
		} catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace(); 
		} finally {
			session.close(); 
		}
	}
	
	private static void updateLicence(Licence lic) {
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();
		try {
			session.update(lic);
			tx.commit();
		} catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace(); 
		} finally {
			session.close(); 
		}
	}

	private static void getLicences() {
		Set<Licence> licenceset = getLicenceSet();
		for (Licence licence : licenceset) {
			if (licences.containsKey(licence.getName())) {
				licences.get(licence.getName()).add(licence);
			} else {
				licences.put(licence.getName(), new HashSet<Licence> (Arrays.asList(licence)));
			}
		}
	}
	
	private static Set<Licence> getLicenceSet() {
		Session session = factory.openSession();
		Transaction tx = null;
		Set<Licence> licenceset = new HashSet<Licence> ();
		try {
			tx = session.beginTransaction();
			
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Licence> criteria = 
					builder.createQuery(Licence.class);

			Root<Licence> root = criteria.from(Licence.class);
			criteria.select(root);
			Query<Licence> q = session.createQuery(criteria);
			if (!q.getResultList().isEmpty()) {
				
				licenceset = new HashSet<Licence> (q.getResultList());				
			}
			tx.commit();
		} catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace(); 
		} finally {
			session.close();
		}
		return licenceset;
	}
	
	private static void updateRecCountOA() {
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();

		// As JPA per v2.1 does not allow update queries, the following is formed as native query 
		Query query = session.createNativeQuery("UPDATE HARVESTINGSTATE hs INNER JOIN ("
				+ "SELECT SUM(lc.record_count) oa_count, lc.state_id FROM LICENCECOUNT lc WHERE "
				+ "lc.licence_type='OPEN' and lc.state_id>=(SELECT MIN(hs.state_id) FROM HARVESTINGSTATE hs "
				+ "WHERE hs.timestamp >= :earlyTS ) GROUP by lc.state_id) lcrc on "
				+ "lcrc.state_id = hs.state_id SET hs.record_count_oa = lcrc.oa_count");
		query.setParameter("earlyTS", earliestChange);
		try {
			int rowsAffected = query.executeUpdate();
			logger.info("Updated " + rowsAffected + " row(s) in HARVESTINGSTATE with"
					+ " a change of the openAccess recordCount");
		}
		catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		} finally {
			session.close();
		}
	}

	private static void updateLicenceCount(Licence jlic) {
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();

		// As JPA per v2.1 does not allow update queries, the following is formed as native query 
		Query query = session.createNativeQuery("UPDATE LICENCECOUNT lc INNER JOIN HARVESTINGSTATE hs on lc.state_id = hs.state_id"
				+ " SET lc.licence_type = :licType WHERE lc.licence_name = :licName "
				+ "AND hs.timestamp >= :validFrom");
		query.setParameter("licType", jlic.getType().toString());
		query.setParameter("licName", jlic.getName());
		query.setParameter("validFrom", jlic.getValidFrom());
		
		try {
			int rowsAffected = query.executeUpdate();
			logger.info("Updated " + rowsAffected + " row(s) in LicenceCount with Licence " +
				"changed to " + jlic.getType());
		}
		catch (HibernateException e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		} finally {
			session.close();
		}
	}

}
