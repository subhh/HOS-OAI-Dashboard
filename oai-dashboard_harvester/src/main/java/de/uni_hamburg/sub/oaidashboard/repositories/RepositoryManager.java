package de.uni_hamburg.sub.oaidashboard.repositories;

import de.uni_hamburg.sub.oaidashboard.database.datastructures.Repository;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositoryManager {

    private static Logger logger = LogManager.getLogger(RepositoryManager.class.getName());
    private final SessionFactory factory;

    public RepositoryManager(SessionFactory factory) {
        this.factory = factory;
    }

    public Repository createTransientRepositoryFromJson(String repoFilePath) {
        Repository repo = null;
        File jsonFile = new File(repoFilePath);
        try {
            String jsonContent = FileUtils.readFileToString(jsonFile, "utf-8");
            JSONObject jsonObject = new JSONObject(jsonContent);
            // mandatory
            String name = jsonObject.getString("name");
            String url = jsonObject.getString("url");
            // not mandatory
            String land = jsonObject.has("land") ? jsonObject.getString("land") : "";
            String bundesland = jsonObject.has("bundesland") ? jsonObject.getString("bundesland") : "";
            String geodaten = jsonObject.has("geodaten") ? jsonObject.getString("geodaten") : "";
            String technische_plattform = jsonObject.has("technische_plattform") ? jsonObject.getString("technische_plattform") : "";
            String kontakt = jsonObject.has("kontakt") ? jsonObject.getString("kontakt") : "";

            repo = new Repository(name, url);
            repo.setLand(land);
            repo.setBundesland(bundesland);
            repo.setGeodaten(geodaten);
            repo.setTechnische_plattform(technische_plattform);
            repo.setKontakt(kontakt);

        } catch (Exception e) {
            logger.error("An error occurred while loading a repository from json file: {}", repoFilePath, e);
        }
        return repo;
    }

    public void loadRepositoryFromJson(String repoFilePath) {
        saveRepository(createTransientRepositoryFromJson(repoFilePath));
    }

    public void loadRepositoriesFromJson(String repoFilePath, boolean fallback) {
        List<Map<String, String>> repositoriesData = new ArrayList<>();
        File jsonFile = new File(repoFilePath);
        try {
            String jsonContent = FileUtils.readFileToString(jsonFile, "utf-8");
            JSONArray jsonRepos = new JSONObject(jsonContent).getJSONArray("repositories");
            for (int i = 0; i < jsonRepos.length(); i++) {
                Map repositoryData = new HashMap<>();
                repositoryData.put("name", jsonRepos.getJSONObject(i).getString("name"));
                repositoryData.put("url", jsonRepos.getJSONObject(i).getString("url"));
                repositoriesData.add(repositoryData);
            }
        } catch (Exception e) {
            logger.error(e);
            if(fallback) {
                logger.info("Something went wrong with the provided repositories.json (expected here: {}), " +
                        "loading default repositories", jsonFile.getAbsolutePath());
                setUpDefaultRepositories();
            }
        }
        setUpRepositories(repositoriesData);
    }

    private void saveRepository(Repository repo) {
        logger.info("attempting to persist repository into database: {}", repo.toString());

        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();
            session.save(repo);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    private void saveBasicRepoInfo(String name, String url) {
        Repository repo = new Repository(name, url);
        saveRepository(repo);
    }

    private void setUpRepositories(List<Map<String, String>> repositoriesData) {
        for(Map<String, String> repositoryData : repositoriesData) {
            String name = repositoryData.get("name");
            String url = repositoryData.get("url");
            logger.info("repo name: {}, url: {}", name, url);
            saveBasicRepoInfo(name, url);
        }
    }

    private void setUpDefaultRepositories() {
        saveBasicRepoInfo("tub.dok", "http://tubdok.tub.tuhh.de/oai/request");
        saveBasicRepoInfo("Elektronische Dissertationen Universit&auml;t Hamburg, GERMANY", "http://ediss.sub.uni-hamburg.de/oai2/oai2.php");
        saveBasicRepoInfo("OPuS \\u00e2\\u0080\\u0093 Volltextserver der HCU", "http://edoc.sub.uni-hamburg.de/hcu/oai2/oai2.php");
        saveBasicRepoInfo("Beispiel-Volltextrepository", "http://edoc.sub.uni-hamburg.de/hsu/oai2/oai2.php");
        saveBasicRepoInfo("HAW OPUS","http://edoc.sub.uni-hamburg.de/haw/oai2/oai2.php");
    }

    public void updateRepository(Repository repo) {
        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();
            session.update(repo);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
    }
}
