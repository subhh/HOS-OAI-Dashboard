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

    public void loadRepositoriesFromJson(String repoFilePath, boolean fallback) {
        List<Map<String, String>> repositoriesData = new ArrayList<>();
        File  jsonFile = new File(repoFilePath);
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
            e.printStackTrace();
            if(fallback) {
                logger.info("Something went wrong with the provided repositories.json (expected here: {}), " +
                        "loading default repositories", jsonFile.getAbsolutePath());
                setUpDefaultRepositories();
            }
        }
        setUpRepositories(repositoriesData);
    }

    public void saveBasicRepoInfo(String name, String url) {
        Session session = factory.openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();
            Repository repo = new Repository(name, url);
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
}
