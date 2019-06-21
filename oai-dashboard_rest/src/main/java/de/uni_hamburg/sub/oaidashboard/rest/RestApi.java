package de.uni_hamburg.sub.oaidashboard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_hamburg.sub.oaidashboard.ConfigurationManager;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/api")
@Api(value="/", description="Test" )
public class RestApi {

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    @GET
    @Path("/GetCombinedStateAtTimePoint/{timepoint}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Get a summarized 'All'-State for a specific timepoint, where all values from all harvested repos/states of the timepoint are added up")
    public String getCombinedStateAtTimePoint(@ApiParam(value="The date (year, month, day) from which the harvested information " +
            "shall be retrieved; Format: YYYY-MM-DD)", required=true)
                                              @PathParam("timepoint") String timepoint) throws IOException {
        logger.info("REST-API called /GetCombinedStateAtTimepoint/{timepoint} " +
                "with params timepoint: '{}'", timepoint);

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date safe_timepoint = null;
        try {
            safe_timepoint = sdf.parse(timepoint);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        List<HarvestingState> stateList = getAllStatesFromDB(safe_timepoint);

        applyMappings(stateList);

        String json_state = "{}"; // by default return minimal valid json data
        if(stateList.size() > 0) {
            HarvestingState combinedState = createCombinedState(stateList);
            ObjectMapper objectMapper = new ObjectMapper();
            json_state = objectMapper.writeValueAsString(combinedState);
        }
        return json_state;
    }

    private HarvestingState createCombinedState(List<HarvestingState> stateList) {
        HarvestingState combinedState = new HarvestingState();

        int record_count_all = 0;
        int record_count_oa_all = 0;

        for(HarvestingState state: stateList) {
            record_count_all += state.getRecord_count();
            record_count_oa_all += state.getRecord_count_oa();
        }

        Set<LicenceCount> licenceCounts_all = getMergedLicenceCounts(stateList);
        Set<SetCount> setCounts_all = getMergedSetCounts(stateList);
        Set<MetadataFormat> metadataFormats_all = getMergedMetadataFormats(stateList);
        Set<DCFormatCount> dcFormatCounts_all = getMergedDCFormatCounts(stateList);
        Set<DCTypeCount> dcTypeCounts_all = getMergedDCTypeCounts(stateList);

        combinedState.setRecord_count(record_count_all);
        combinedState.setTimestamp(new Timestamp((new Date()).getTime()));
        combinedState.setRecord_count_oa(record_count_oa_all);
        Repository repository = new Repository();
        repository.setName("all");
        combinedState.setRepository(repository);
        combinedState.setStatus(HarvestingStatus.SUCCESS);
        combinedState.setLicenceCounts(licenceCounts_all);
        combinedState.setSetCounts(setCounts_all);
        combinedState.setMetadataFormats(metadataFormats_all);
        combinedState.setDCFormatCounts(dcFormatCounts_all);
        combinedState.setDCTypeCounts(dcTypeCounts_all);

        return combinedState;
    }

    private Set<LicenceCount> getMergedLicenceCounts(List<HarvestingState> stateList) {
        Set<LicenceCount> licenceCounts = new HashSet<>();
        Function<HarvestingState, Set<Object>> targetFunc = state -> new HashSet<>(state.getLicenceCounts());
        Function<Object, String> identityFunc = object -> ((LicenceCount) object).getLicence_name();

        for(Map.Entry<String, List<Object>> licenceGroupEntry : groupTargetSetOfStateById(stateList, targetFunc, identityFunc).entrySet()) {
            int record_count_all = 0;
            LicenceType licenceType = LicenceType.UNKNOWN;
            for(Object identicalLicenceCountObject : licenceGroupEntry.getValue()) {
                LicenceCount identicalLicenceCount = (LicenceCount) identicalLicenceCountObject;
                record_count_all += identicalLicenceCount.getRecord_count();
                licenceType = identicalLicenceCount.getLicence_type();
            }
            LicenceCount mergedLicenceCount = new LicenceCount();
            mergedLicenceCount.setLicence_name(licenceGroupEntry.getKey());
            mergedLicenceCount.setRecord_count(record_count_all);
            mergedLicenceCount.setLicence_type(licenceType);
            mergedLicenceCount.setId(-1);
            licenceCounts.add(mergedLicenceCount);
        }
        return licenceCounts;
    }

    private Set<SetCount> getMergedSetCounts(List<HarvestingState> stateList) {
        Set<SetCount> setCounts = new HashSet<>();
        Function<HarvestingState, Set<Object>> targetFunc = state -> new HashSet<>(state.getSetCounts());
        Function<Object, String> identityFunc = object -> ((SetCount) object).getSet_spec();

        for(Map.Entry<String, List<Object>> countGroupEntry : groupTargetSetOfStateById(stateList, targetFunc, identityFunc).entrySet()) {
            int record_count_all = 0;
            Set<String> set_names = new HashSet<>();
            for(Object identicalSetCountObject : countGroupEntry.getValue()) {
                SetCount identicalSetCount = (SetCount) identicalSetCountObject;
                record_count_all += identicalSetCount.getRecord_count();
                set_names.add(identicalSetCount.getSet_name());
            }
            SetCount mergedSetCount = new SetCount();
            mergedSetCount.setSet_name(String.join("; ", set_names));
            mergedSetCount.setRecord_count(record_count_all);
            mergedSetCount.setSet_spec(countGroupEntry.getKey());
            mergedSetCount.setId(-1);
            setCounts.add(mergedSetCount);
        }
        return setCounts;
    }

    private Set<MetadataFormat> getMergedMetadataFormats(List<HarvestingState> stateList) {
        Set<MetadataFormat> metadataFormats = new HashSet<>();
        Function<HarvestingState, Set<Object>> targetFunc = state -> new HashSet<>(state.getMetadataFormats());
        Function<Object, String> identityFunc = object -> ((MetadataFormat) object).getNamespace();

        for(Map.Entry<String, List<Object>> countGroupEntry : groupTargetSetOfStateById(stateList, targetFunc, identityFunc).entrySet()) {
            Set<String> prefixes = new HashSet<>();
            Set<String> format_schemas = new HashSet<>();
            for(Object identicalMetadataFormatObject : countGroupEntry.getValue()) {
                MetadataFormat identicalMetadataFormat = (MetadataFormat) identicalMetadataFormatObject;
                prefixes.add(identicalMetadataFormat.getPrefix());
                format_schemas.add(identicalMetadataFormat.getFormat_schema());
            }
            MetadataFormat mergedMetadataFormat = new MetadataFormat();
            mergedMetadataFormat.setPrefix(String.join("; ", prefixes));
            mergedMetadataFormat.setFormat_schema(String.join("; ", format_schemas));
            mergedMetadataFormat.setNamespace(countGroupEntry.getKey());
            mergedMetadataFormat.setId(-1);
            metadataFormats.add(mergedMetadataFormat);
        }
        return metadataFormats;
    }

    private Set<DCFormatCount> getMergedDCFormatCounts(List<HarvestingState> stateList) {
        Set<DCFormatCount> dcFormatCounts = new HashSet<>();
        Function<HarvestingState, Set<Object>> targetFunc = state -> new HashSet<>(state.getDCFormatCounts());
        Function<Object, String> identityFunc = object -> ((DCFormatCount) object).getDc_Format();

        for(Map.Entry<String, List<Object>> countGroupEntry : groupTargetSetOfStateById(stateList, targetFunc, identityFunc).entrySet()) {
            int record_count_all = 0;
            for(Object identicalDCFormatCountObject : countGroupEntry.getValue()) {
                DCFormatCount identicalDCFormatCount = (DCFormatCount) identicalDCFormatCountObject;
                record_count_all += identicalDCFormatCount.getRecord_count();
            }
            DCFormatCount mergedDCFormatCount = new DCFormatCount();
            mergedDCFormatCount.setRecord_count(record_count_all);
            mergedDCFormatCount.setDc_Format(countGroupEntry.getKey());
            mergedDCFormatCount.setId(-1);
            dcFormatCounts.add(mergedDCFormatCount);
        }
        return dcFormatCounts;
    }

    private Set<DCTypeCount> getMergedDCTypeCounts(List<HarvestingState> stateList) {
        Set<DCTypeCount> dcTypeCounts = new HashSet<>();
        Function<HarvestingState, Set<Object>> targetFunc = state -> new HashSet<>(state.getDCTypeCounts());
        Function<Object, String> identityFunc = object -> ((DCTypeCount) object).getDc_Type();

        for(Map.Entry<String, List<Object>> countGroupEntry : groupTargetSetOfStateById(stateList, targetFunc, identityFunc).entrySet()) {
            int record_count_all = 0;
            for(Object identicalDCTypeCountObject : countGroupEntry.getValue()) {
                DCTypeCount identicalDCTypeCount = (DCTypeCount) identicalDCTypeCountObject; //
                record_count_all += identicalDCTypeCount.getRecord_count();
            }
            DCTypeCount mergedDCTypeCount = new DCTypeCount();
            mergedDCTypeCount.setRecord_count(record_count_all);
            mergedDCTypeCount.setDc_Type(countGroupEntry.getKey());
            mergedDCTypeCount.setId(-1);
            dcTypeCounts.add(mergedDCTypeCount);
        }
        return dcTypeCounts;
    }

    private Set<DCTypeCount> getMergedDCTypeCounts(HarvestingState state) {
        Set<DCTypeCount> dcTypeCounts = new HashSet<>();
        Function<Object, String> identityFunc = object -> ((DCTypeCount) object).getDc_Type();

        Map<String, List<DCTypeCount>> grouping = new HashMap<>();
        for (DCTypeCount dcTypeCount : state.getDCTypeCounts()) {
            String dc_type = dcTypeCount.getDc_Type();
            if (!grouping.containsKey(dc_type)) {
                grouping.put(dc_type, new ArrayList<>());
            }
            grouping.get(dc_type).add(dcTypeCount);
        }

        for(Map.Entry<String, List<DCTypeCount>> groupEntry : grouping.entrySet()) {
            if(groupEntry.getValue().size() > 1) {
                int record_count_all = 0;
                for (DCTypeCount identicalDCTypeCount : groupEntry.getValue()) {
                    record_count_all += identicalDCTypeCount.getRecord_count();
                }
                DCTypeCount mergedDCTypeCount = new DCTypeCount();
                mergedDCTypeCount.setRecord_count(record_count_all);
                mergedDCTypeCount.setDc_Type(groupEntry.getKey());
                mergedDCTypeCount.setId(-1);
                dcTypeCounts.add(mergedDCTypeCount);
            } else {
                if(groupEntry.getValue().size() == 1) {
                    dcTypeCounts.add(groupEntry.getValue().get(0));
                }
            }
        }
        return dcTypeCounts;
    }

    private Set<LicenceCount> getMergedLicenceCounts(HarvestingState state) {
        Set<LicenceCount> licenceCounts = new HashSet<>();

        Map<String, List<LicenceCount>> grouping = new HashMap<>();
        for (LicenceCount licenceCount : state.getLicenceCounts()) {
            String licence_name = licenceCount.getLicence_name();
            if (!grouping.containsKey(licence_name)) {
                grouping.put(licence_name, new ArrayList<>());
            }
            grouping.get(licence_name).add(licenceCount);
        }

        for(Map.Entry<String, List<LicenceCount>> groupEntry : grouping.entrySet()) {
            if(groupEntry.getValue().size() > 1) {
                int record_count_all = 0;
                Set<LicenceType> licence_types = new HashSet<>();
                for (LicenceCount identicalLicenceCount : groupEntry.getValue()) {
                    record_count_all += identicalLicenceCount.getRecord_count();
                    licence_types.add(identicalLicenceCount.getLicence_type());
                }
                LicenceCount mergedLicenceCount = new LicenceCount();
                mergedLicenceCount.setRecord_count(record_count_all);
                mergedLicenceCount.setLicence_name(groupEntry.getKey());
                mergedLicenceCount.setId(-1);

                // handle licence types
                if(licence_types.size() == 1) {
                    mergedLicenceCount.setLicence_type(licence_types.iterator().next());
                } else {
                    logger.info("Found conflicting Licence Types when merging LicenceCounts to: {}",
                            groupEntry.getKey());
                    logger.info("Using Licence Type 'UNKNOWN'");
                    mergedLicenceCount.setLicence_type(LicenceType.UNKNOWN);
                }

                licenceCounts.add(mergedLicenceCount);
            } else {
                if(groupEntry.getValue().size() == 1) {
                    licenceCounts.add(groupEntry.getValue().get(0));
                }
            }
        }
        return licenceCounts;
    }

    private Map<String, List<Object>> groupTargetSetOfStateById(List<HarvestingState> stateList,
                                                                Function<HarvestingState, Set<Object>> tFunc,
                                                                Function<Object, String> idFunc) {
        Map<String, List<Object>> grouping = new HashMap<>();
        for(HarvestingState state : stateList) {
            for (Object object : tFunc.apply(state)) {
                String id = idFunc.apply(object);
                if (!grouping.containsKey(id)) {
                    grouping.put(id, new ArrayList<>());
                }
                grouping.get(id).add(object);
            }
        }
        return grouping;
    }

    @GET
    @Path("/ListRepos")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="List all repositories")
    public String listRepos() throws IOException {
        logger.info("REST-API called /ListRepos");

        List<Repository> repositories = getReposFromDB();

        ObjectMapper objectMapper = new ObjectMapper();
        String json_repos = objectMapper.writeValueAsString(repositories);
        return json_repos;
    }

    @GET
    @Path("/GetStateAtTimePoint/{repo_id}/{timepoint}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="Get (Harvesting-)State at specific timepoint by repository ID")
    public String getStateAtTimePoint(@ApiParam(value="The ID of the repository (see ListRepos); Format: Number", required=true)
                                          @PathParam("repo_id") String repo_id,
                                      @ApiParam(value="The date (year, month, day) from which the harvested information " +
                                              "shall be retrieved; Format: YYYY-MM-DD", required=true)
                                      @PathParam("timepoint") String timepoint) throws IOException {
        logger.info("REST-API called /GetStateAtTimePoint/{repo_id}/{timepoint} " +
                "with params repo_id: '{}', timepoint: '{}'", repo_id, timepoint);

        int safe_repo_id = Integer.parseInt(repo_id);

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date safe_timepoint = null;
        try {
            safe_timepoint = sdf.parse(timepoint);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        HarvestingState state = getStateFromDB(safe_repo_id, safe_timepoint);

        applyMappings(state);

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
    @ApiOperation(value="Get States for a specific time range by repository ID")
    public String getStatesAtTimeRange(@ApiParam(value="The ID of the repository (see ListRepos); Format: Number", required=true)
                                           @PathParam("repo_id") String repo_id,
                                       @ApiParam(value="The starting date (year, month, day) from which the harvested information " +
                                               "shall be retrieved; Format: YYYY-MM-DD", required=true)
                                       @PathParam("timepoint_from") String timepoint_from,
                                       @ApiParam(value="The ending date (year, month, day) from which the harvested information " +
                                               "shall be retrieved; Format: YYYY-MM-DD", required=true)
                                       @PathParam("timepoint_to") String timepoint_to) throws IOException {
        logger.info("REST-API called /GetStateAtTimePoint/{repo_id}/{timepoint_from}/{timepoint_to} " +
                "with params repo_id: '{}', timepoint_from: '{}', timepoint_to: '{}'",
                repo_id, timepoint_from, timepoint_to);

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

        applyMappings(stateList);

        ObjectMapper objectMapper = new ObjectMapper();
        String json_state = objectMapper.writeValueAsString(stateList);
        return json_state;
    }

    private void applyMappings(HarvestingState state) {
        if(state != null) {
            List<HarvestingState> stateList = new ArrayList<>();
            stateList.add(state);
            applyMappings(stateList);
        }
    }

    private void applyMappings(List<HarvestingState> stateList) {
        ConfigurationManager configMan = new ConfigurationManager();
        Map<String, String> mappings_dc_type = configMan.getReverseDCTypeMappings();
        Map<String, String> mappings_licences = configMan.getReverseLicenceMappings();

        for(HarvestingState state : stateList) {
            // apply dc type mapping
            for(DCTypeCount dcTypeCount : state.getDCTypeCounts()) {
                if(mappings_dc_type.containsKey(dcTypeCount.getDc_Type())) {
                    //logger.info("Mapping {} to {}", dcTypeCount.getDc_Type(), mappings.get(dcTypeCount.getDc_Type()));
                    dcTypeCount.setDc_Type(mappings_dc_type.get(dcTypeCount.getDc_Type()));
                }
            }
            Set<DCTypeCount> mergedDCTypeCounts = getMergedDCTypeCounts(state);
            state.setDCTypeCounts(mergedDCTypeCounts);

            // apply licences mapping
            for(LicenceCount licenceCount : state.getLicenceCounts()) {
                if(mappings_licences.containsKey(licenceCount.getLicence_name())) {
                    //logger.info("Mapping {} to {}", licenceCount.getLicence_name(), mappings_licences.get(licenceCount.getLicence_name()));
                    licenceCount.setLicence_name(mappings_licences.get(licenceCount.getLicence_name()));
                }
            }
            Set<LicenceCount> mergedLicenceCounts = getMergedLicenceCounts(state);
            state.setLicenceCounts(mergedLicenceCounts);
        }
    }

    private List<HarvestingState> getAllStatesFromDB(Date timepoint) {
        SessionFactory factory = getSessionFactory();
        Session session = factory.openSession();
        List<HarvestingState> stateList = new ArrayList<>();

        try {
            stateList = session.createNamedQuery("get_all_states_at_timepoint", HarvestingState.class)
                    .setParameter("timepoint", timepoint)
                    .getResultList();
            for(HarvestingState state: stateList) {
                state.fixLazyInitialization();
            }
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            session.close();
            factory.close();
        }

        return filterStates(stateList);
    }

    /**
     * Makes sure, that for each Date (year/month/day) and Repository only the most recent HarvestingState gets returned
     * @param stateList
     * @return a stateList that for each Date (year/month/day) and Repository only has the most recent HarvestingState
     */
    private List<HarvestingState> filterStates(List<HarvestingState> stateList) {
        List<HarvestingState> filteredStates = new ArrayList<>();

        // first: group by repository id
        Map<Integer, Set<HarvestingState>> groupedById = stateList.stream()
                .collect(Collectors.groupingBy(s -> s.getRepository().getId(), Collectors.toSet()));
        //logger.info("Grouped by Repository ID: {}", groupedById);

        // second: group by day/month/year -> date
        Function<HarvestingState, Date> dateFunc =
                state -> DateUtils.truncate(new Date(state.getTimestamp().getTime()), Calendar.DATE);
        for(Map.Entry<Integer, Set<HarvestingState>> idGroup : groupedById.entrySet()) {
            Map<Date, Set<HarvestingState>> groupedByDate = idGroup.getValue().stream()
                    .collect(Collectors.groupingBy(s -> dateFunc.apply(s), Collectors.toSet()));
            //logger.info("Grouped by Date: {}", groupedByDate);

            // third: take only the most recent state of each date group
            for(Map.Entry<Date, Set<HarvestingState>> dateGroup : groupedByDate.entrySet()) {
                HarvestingState latest = Collections.max(dateGroup.getValue(), Comparator.comparing(HarvestingState::getTimestamp));
                filteredStates.add(latest);
            }
        }

        return filteredStates;
    }

    private HarvestingState getStateFromDB(int repo_id, Date timepoint) {
        HarvestingState state = null;
        List<HarvestingState> stateList = getStatesFromDB(repo_id, timepoint, timepoint);
        if(stateList.size() > 0) {
            // because of the method 'filterStates', the stateList in this case should never be bigger then 1
            state = stateList.get(0);
        }
        return state;
    }

    private List<HarvestingState> getStatesFromDB(int repo_id, Date timepoint_from, Date timepoint_to) {
        SessionFactory factory = getSessionFactory();
        Session session = factory.openSession();
        List<HarvestingState> stateList = new ArrayList<>();

        try {
            stateList = session.createNamedQuery("get_states_at_timerange", HarvestingState.class)
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
            factory.close();
        }
        return filterStates(stateList);
    }

    private List<Repository> getReposFromDB() {
        SessionFactory factory = getSessionFactory();
        Session session = factory.openSession();
        List<Repository> repositories = null;

        try {
            repositories = session.createNamedQuery("get_active_repositories", Repository.class)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            session.close();
            factory.close();
        }
        return repositories;
    }

    private static SessionFactory getSessionFactory() {
        return new Configuration().configure().buildSessionFactory();
    }
}