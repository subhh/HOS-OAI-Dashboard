package de.uni_hamburg.sub.oaidashboard;

import de.uni_hamburg.sub.oaidashboard.database.datastructures.Licence;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.LicenceCount;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.LicenceType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.sql.Timestamp;
import java.util.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class LicenceManager {
	
	private static Hashtable<String, Set<Licence>> licences = new Hashtable<String, Set<Licence>> ();
	private static SessionFactory factory;
    private static Logger logger = LogManager.getLogger(Class.class.getName());
    
	public static void initManager(SessionFactory factory) {
		LicenceManager.factory = factory;
		getLicences();
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
			setLicenceType(lic);
			storeNewLicence(lic);
			type = lic.getType();
			licences.put(name, new HashSet<Licence> (Arrays.asList(lic)));
		}
		return type;
	}
	
    private static void setLicenceType(Licence lic) {
        logger.info("Marking Licences (LicenceCount) as OPEN, CLOSED or UNKNOWN");
            if(lic.getName().toLowerCase().contains("creativecommons")) {
                logger.debug("Marked Licence with with licence_name: '{}', as OPEN", lic.getName());
                lic.setType(LicenceType.OPEN);
            } else if(lic.getName().toLowerCase().contains("openaccess")) {
                logger.debug("Marked Licence with with licence_name: '{}', as OPEN", lic.getName());
                lic.setType(LicenceType.OPEN);
            } else if(lic.getName().toLowerCase().contains("embargoedaccess")) {
                logger.debug("Marked Licence with with licence_name: '{}', as CLOSED", lic.getName());
                lic.setType(LicenceType.CLOSED);
            } else {
                logger.debug("Marked Licence with with licence_name: '{}', as UNKNOWN", lic.getName());
                lic.setType(LicenceType.UNKNOWN);
            }
    }
	
	private static void storeNewLicence(Licence lic) {
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();
		try {
			session.save(lic);
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
}
