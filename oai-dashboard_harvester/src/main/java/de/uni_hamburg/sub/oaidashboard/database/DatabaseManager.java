package de.uni_hamburg.sub.oaidashboard.database;

import de.uni_hamburg.sub.oaidashboard.database.datastructures.Repository;
import de.uni_hamburg.sub.oaidashboard.repositories.RepositoryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class DatabaseManager {

    private final String CONF_DIR;
    private final String CONF_FILENAME_HIBERNATE = "hibernate.cfg.xml";
    private final String SCRIPT_FILE = "/tmp/exportSchemaScript.sql";

    private Metadata metadata;
    private SessionFactory factory;

    private static Logger logger = LogManager.getLogger(DatabaseManager.class.getName());

    public DatabaseManager(final String CONF_DIR) {
        this.CONF_DIR = CONF_DIR;

        // read config and create necessary objects
        ServiceRegistry serviceRegistry;
        File configFile = loadHibernateConfigFromDirectory();
        if(configFile != null) {
            serviceRegistry = new StandardServiceRegistryBuilder()
                    .configure(configFile).build();
        } else {
            serviceRegistry = new StandardServiceRegistryBuilder()
                    .configure(CONF_FILENAME_HIBERNATE).build();
        }
        metadata = new MetadataSources(serviceRegistry)
                .getMetadataBuilder().build();

        // build SessionFactory
        try {
            factory = metadata.buildSessionFactory();
        } catch (Throwable ex) {
            logger.info("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private File loadHibernateConfigFromDirectory() {
        File configFile = null;
        File dir = new File(CONF_DIR);
        File file = new File(dir.toString() + "/" + CONF_FILENAME_HIBERNATE);
        if(dir.isDirectory()) {
            if(file.isFile()) {
                configFile = file;
            } else {
                logger.info("hibernate configuration file does not exist: " + file.toString());
            }
        } else {
            logger.info("configuration directory does not exist: " + dir.toString());
        }
        if(configFile != null) {
            logger.info("loaded hibernate configuration from: " + configFile.toString());
        } else {
            logger.info("failed to load hibernate configuration from: " + file.toString() + " reverting to default configuration file (classpath)");
        }
        return configFile;
    }

    private SchemaExport getSchemaExport() {
        SchemaExport export = new SchemaExport();
        File outputFile = new File(SCRIPT_FILE);
        if (outputFile.exists()) {
            outputFile.delete();
        }
        String outputFilePath = outputFile.getAbsolutePath();
        logger.info("Export file: {}", outputFilePath);
        export.setDelimiter(";");
        export.setOutputFile(outputFilePath);
        // No Stop if Error
        export.setHaltOnError(false);
        return export;
    }

    private void dropDataBase() {
        logger.info("starting deletion of database");
        // TargetType.DATABASE - Execute on Database
        // TargetType.SCRIPT - Write Script file.
        // TargetType.STDOUT - Write log to Console.
        EnumSet<TargetType> targetTypes = EnumSet.of(
                TargetType.DATABASE, TargetType.SCRIPT, TargetType.STDOUT);
        getSchemaExport().drop(targetTypes, metadata);
        logger.info("finished deletion of database");
    }

    public void resetDatabase() {
        logger.info("starting reset of database");
        dropDataBase();
        initializeDatabase();
        logger.info("finished reset of database");
    }

    /**
     * Creates all tables derived from the data model defined by hibernate configuration and class annotations
     */
    public void initializeDatabase() {
        logger.info("starting initialization of database");
        // TargetType.DATABASE - Execute on Databse
        // TargetType.SCRIPT - Write Script file.
        // TargetType.STDOUT - Write log to Console.
        EnumSet<TargetType> targetTypes = EnumSet.of(
                TargetType.DATABASE, TargetType.SCRIPT, TargetType.STDOUT);
        getSchemaExport().execute(targetTypes, SchemaExport.Action.CREATE, metadata);
        logger.info("finished initialization of database");
    }

    public void initializeRepositoriesFromJson() {
        logger.info("Starting setting up repositories from json...");
        RepositoryManager repoManager = new RepositoryManager(factory);
        repoManager.loadRepositoriesFromJson(CONF_DIR + "/repositories.json", true);
        logger.info("Finished setting up repositories from json...");
    }

    public void addRepositoryFromJson(String repo_json_filename) {
        logger.info("Starting loading repository from json...");
        RepositoryManager repoManager = new RepositoryManager(factory);
        repoManager.loadRepositoryFromJson(repo_json_filename);
        logger.info("Finished loading repository from json...");
    }

    public void updateRepositoryByIdFromJson(int repo_id, String repo_json_filename) {
        logger.info("Starting updating repository by ID from json...");
        List<Repository> repositories = getAllReposFromDB();
        Repository target_repo = null;
        for(Repository repository : repositories) {
            if(repository.getId() == repo_id) {
                target_repo = repository;
                break;
            }
        }
        if(target_repo == null) {
            logger.info("found no repository with provided ID: {}", repo_id);
        } else {
            logger.info("repository before update: {}", target_repo);
            RepositoryManager repoManager = new RepositoryManager(factory);
            Repository transient_repo = repoManager.createTransientRepositoryFromJson(repo_json_filename);
            target_repo.setName(transient_repo.getName());
            target_repo.setHarvesting_url(transient_repo.getHarvesting_url());
            target_repo.setLand(transient_repo.getLand());
            target_repo.setBundesland(transient_repo.getBundesland());
            target_repo.setGeodaten(transient_repo.getGeodaten());
            target_repo.setTechnische_plattform(transient_repo.getTechnische_plattform());
            target_repo.setKontakt(transient_repo.getKontakt());
            target_repo.updateHash();
            logger.info("repository after update: {}", target_repo);
            repoManager.updateRepository(target_repo);
        }
        logger.info("Finished updating repository by ID from json...");
    }

    public void listAllRepos() {
        List<Repository> repositories = getAllReposFromDB();

        for(Repository repository : repositories) {
            System.out.println(repository.toString());
        }
    }

    private List<Repository> getAllReposFromDB() {
        List<Repository> repositories = new ArrayList<>();
        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Repository> criteria = builder.createQuery(Repository.class);

            Root<Repository> root = criteria.from(Repository.class);
            Query<Repository> q = session.createQuery(criteria);
            repositories = q.getResultList();

            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
        return repositories;
    }

    public List<Repository> getActiveReposFromDB() {
        List<Repository> repositories = null;
        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Repository> criteria = builder.createQuery(Repository.class);

            Root<Repository> root = criteria.from(Repository.class);
            criteria.select(root).where(builder.equal(root.get("state"), "ACTIVE"));
            Query<Repository> q = session.createQuery(criteria);
            repositories = q.getResultList();

            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
        return repositories;
    }

    public SessionFactory getSessionFactory() {
        return this.factory;
    }

    private void updateRepositoryStateById(int repo_id, String state) {
        List<Repository> repositories = getAllReposFromDB();
        Repository target_repo = null;
        for(Repository repository : repositories) {
            if(repository.getId() == repo_id) {
                target_repo = repository;
                break;
            }
        }
        if(target_repo == null) {
            logger.info("found no repository with provided ID: {}", repo_id);
        } else {
            target_repo.setState(state);
            new RepositoryManager(factory).updateRepository(target_repo);
        }
    }

    public void activateRepositoryById(int repo_id) {
        logger.info("Start activating repository by ID ...");
        updateRepositoryStateById(repo_id, "ACTIVE");
        logger.info("Finished activating repository by ID...");
    }

    public void disableRepositoryById(int repo_id) {
        logger.info("Start disabling repository by ID ...");
        updateRepositoryStateById(repo_id, "DISABLED");
        logger.info("Finished disabling repository by ID...");
    }
}
