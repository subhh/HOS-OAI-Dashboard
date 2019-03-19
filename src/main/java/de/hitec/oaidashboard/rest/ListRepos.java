package de.hitec.oaidashboard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hitec.oaidashboard.database.datastructures.HarvestingState;
import de.hitec.oaidashboard.database.datastructures.Repository;
import org.apache.commons.lang3.time.DateUtils;
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
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Path("/api")
public class ListRepos {

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

        System.out.println(timepoint);
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date save_timepoint = null;
        try {
            save_timepoint = sdf.parse(timepoint);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        HarvestingState state = getStateFromDB(save_repo_id, save_timepoint);
        ObjectMapper objectMapper = new ObjectMapper();
        String json_state = objectMapper.writeValueAsString(state);
        return json_state;
    }

    private HarvestingState getStateFromDB(int repo_id, Date timepoint) {
        Session session = getSessionFactory().openSession();
        HarvestingState state = null;
        Date next_day = DateUtils.addDays(timepoint, 1);
        System.out.println(timepoint.toString());
        System.out.println(next_day.toString());
        try {
            state = session.createNamedQuery("get_state_at_timepoint", HarvestingState.class)
                    .setParameter("repo_id", repo_id)
                    .setParameter("timepoint1", timepoint)
                    .setParameter("timepoint2", next_day)
                    .getResultList().get(0);
            state.fixLazyInitialization();
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            session.close();
        }
        return state;
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