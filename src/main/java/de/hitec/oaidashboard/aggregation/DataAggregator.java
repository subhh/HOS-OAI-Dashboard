package de.hitec.oaidashboard.aggregation;

import de.hitec.oaidashboard.database.HarvestingDataModel;
import de.hitec.oaidashboard.database.datastructures2.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataAggregator {

    private final HarvestingDataModel dataModel;
    private static Logger logger = LogManager.getLogger(Class.class.getName());

    public DataAggregator(HarvestingDataModel harvestingDataModel) {
        this.dataModel = harvestingDataModel;

        countEntities();
    }

    private void countEntities() {
        countRecords();
        countRecordsForLicences();
        //countRecordsForSets();
    }

    private void countRecords() {
        HarvestingState state = dataModel.getState();
        List<Record> records = dataModel.getRecords();
        state.setRecordCount(records.size());
    }


    private void countRecordsForLicences() {
        HarvestingState state = dataModel.getState();
        List<Record> records = dataModel.getRecords();
        Map<Licence, Integer> licenceIDtoRecordcountMap = new HashMap<>();
        for(Record record: records) {
            Licence licenceFromRecord = record.getLicence();
            int count = licenceIDtoRecordcountMap.containsKey(licenceFromRecord) ? licenceIDtoRecordcountMap.get(licenceFromRecord) : 0;
            licenceIDtoRecordcountMap.put(licenceFromRecord, count + 1);
        }
        for(StateLicenceMapper stateLicenceMapper: state.getStateLicenceMappers()) {
            if(licenceIDtoRecordcountMap.containsKey(stateLicenceMapper.getLicence())) {
                stateLicenceMapper.setRecordCount(licenceIDtoRecordcountMap.get(stateLicenceMapper.getLicence()));
            }
        }
    }

    private void countRecordsForSets() {
        HarvestingState state = dataModel.getState();
        List<Record> records = dataModel.getRecords();

        Map<OAISet, Integer> oaiSetToRecordcountMap = new HashMap<>();
        for(Record record: records) {
            for(OAISet oaiSet: record.getOaiSets()) {
                int count = oaiSetToRecordcountMap.containsKey(oaiSet) ? oaiSetToRecordcountMap.get(oaiSet) : 0;
                oaiSetToRecordcountMap.put(oaiSet, count + 1);
            }
        }
        for(StateSetMapper stateSetMapper: state.getStateSetMappers()) {
            if(oaiSetToRecordcountMap.containsKey(stateSetMapper.getSet())) {
                stateSetMapper.setRecordCount(oaiSetToRecordcountMap.get(stateSetMapper.getSet()));
            }
        }
    }
}
