package de.hitec.oaidashboard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hitec.oaidashboard.database.datastructures.HarvestingState;
import de.hitec.oaidashboard.database.datastructures.Repository;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Path("/api")
public class RestApi {

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    @GET
    @Path("/ListRepos")
    @Produces(MediaType.APPLICATION_JSON)
    public String listRepos() throws IOException {
        List<Repository> repositories = getReposFromDB();

        ObjectMapper objectMapper = new ObjectMapper();
        String json_repos = objectMapper.writeValueAsString(repositories);
        return json_repos;
    }

    @GET
    @Path("/GetStateAtTimePoint/{repo_id}/{timepoint}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getStateAtTimePoint(@PathParam("repo_id") String repo_id,
                                      @PathParam("timepoint") String timepoint) throws IOException {

        int save_repo_id = Integer.parseInt(repo_id);

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date save_timepoint = null;
        try {
            save_timepoint = sdf.parse(timepoint);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        HarvestingState state = getStateFromDB(save_repo_id, save_timepoint);
        ObjectMapper objectMapper = new ObjectMapper();
        String json_state = "{}"; // by default return minimal valid json data
        if(state != null) {
            json_state = objectMapper.writeValueAsString(state);
        }
        return json_state;
    }

    @GET
    @Path("/GetStatesAtTimeRange/{repo_id}/{timepoint_from}/{timepoint_to}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getStatesAtTimeRange(@PathParam("repo_id") String repo_id,
                                       @PathParam("timepoint_from") String timepoint_from,
                                       @PathParam("timepoint_to") String timepoint_to) throws IOException {
        int save_repo_id = Integer.parseInt(repo_id);

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date save_timepoint_from = null;
        Date save_timepoint_to = null;
        try {
            save_timepoint_from = sdf.parse(timepoint_from);
            save_timepoint_to = sdf.parse(timepoint_to);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        List<HarvestingState> stateList = getStatesFromDB(save_repo_id, save_timepoint_from, save_timepoint_to);
        ObjectMapper objectMapper = new ObjectMapper();
        String json_state = objectMapper.writeValueAsString(stateList);
        return json_state;
    }

    private HarvestingState getStateFromDB(int repo_id, Date timepoint) {
        Date next_day = DateUtils.addDays(timepoint, 1);
        HarvestingState state = null;
        List<HarvestingState> stateList = getStatesFromDB(repo_id, timepoint, next_day);
        if(stateList.size() > 0) {
            state = stateList.get(0);
        }
        return state;
    }

    private List<HarvestingState> getStatesFromDB(int repo_id, Date timepoint_from, Date timepoint_to) {
        Session session = getSessionFactory().openSession();
        List<HarvestingState> stateList = new ArrayList<>();

        try {
            stateList = session.createNamedQuery("get_state_at_timepoint", HarvestingState.class)
                    .setParameter("repo_id", repo_id)
                    .setParameter("timepoint_from", timepoint_from)
                    .setParameter("timepoint_to", timepoint_to)
                    .getResultList();
            for(HarvestingState state: stateList) {
                state.fixLazyInitialization();
            }
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
        return stateList;
    }

    private List<Repository> getReposFromDB() {

        Session session = getSessionFactory().openSession();
        List<Repository> repositories = null;

        try {
            repositories = session.createNamedQuery("get_active_repositories", Repository.class)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
        return repositories;
    }

    private static SessionFactory getSessionFactory() {
        return new Configuration().configure().buildSessionFactory();
    }
}