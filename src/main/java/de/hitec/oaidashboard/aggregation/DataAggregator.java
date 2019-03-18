package de.hitec.oaidashboard.aggregation;

import de.hitec.oaidashboard.database.HarvestingDataModel;
import de.hitec.oaidashboard.database.datastructures2.*;
import de.hitec.oaidashboard.parsers.datastructures.HarvestedRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.toIntExact;

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
        countRecordsForSets();
    }

    private void countRecords() {
        HarvestingState state = dataModel.getState();
        List<HarvestedRecord> harvestedRecords = dataModel.getHarvestedRecords();
        state.setRecord_count(harvestedRecords.size());
    }

    private void countRecordsForSets() {
        Map<String, Long> countMap = countStrings(dataModel.getHarvestedRecords().stream().
                map(harvestedRecord -> harvestedRecord.specList).
                flatMap(Collection::stream).
                collect(Collectors.toList()));

        for (SetCount setCount: dataModel.getState().getSetCounts()) {
            if (countMap.containsKey(setCount.getSet_spec())) {
                setCount.setRecord_count(toIntExact(countMap.get(setCount.getSet_spec())));
            }
        }
    }

    private void countRecordsForLicences() {
        Map<String, Long> countMap = countStrings(dataModel.getHarvestedRecords().stream().
                map(harvestedRecord -> harvestedRecord.rights).
                collect(Collectors.toList()));

        for (LicenceCount licenceCount : dataModel.getState().getLicenceCounts()) {
            if (countMap.containsKey(licenceCount.getLicence_name())) {
                licenceCount.setRecord_count(toIntExact(countMap.get(licenceCount.getLicence_name())));
            }
        }
    }

    private Map<String, Long> countStrings(List<String> strings) {
        //Map<String, Integer> countMap = new HashMap<>();

        return strings.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        /*for(String s: strings) {
            int count = countMap.containsKey(s) ? countMap.get(s) : 0;
            countMap.put(s, count + 1);
        }
        return countMap;*/
    }
}
