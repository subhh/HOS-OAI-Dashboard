package de.uni_hamburg.sub.oaidashboard.aggregation;

import de.uni_hamburg.sub.oaidashboard.database.DataModelCreator;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.*;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.HarvestedRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;


public class DataAggregator {

    private final DataModelCreator dataModel;
    private static Logger logger = LogManager.getLogger(Class.class.getName());

    public DataAggregator(DataModelCreator dataModelCreator) {
        this.dataModel = dataModelCreator;

        countEntities();
        countOARecords();
    }

    private void countEntities() {
        countRecords();
        countRecordsForLicences();
        countRecordsForSets();
        countRecordsForDCFormats();
        countRecordsForDCTypes();
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

    private void countRecordsForDCTypes() {
        Map<String, Long> countMap = countStrings(dataModel.getHarvestedRecords().stream().
                map(harvestedRecord -> harvestedRecord.typeList).
                flatMap(Collection::stream).
                collect(Collectors.toList()));

        for (DCTypeCount typeCount: dataModel.getState().getDCTypeCounts()) {
            if (countMap.containsKey(typeCount.getDc_Type())){
                typeCount.setRecord_count(toIntExact(countMap.get(typeCount.getDc_Type())));
            }
        }
    }

    private void countRecordsForLicences() {
        Map<String, Long> countMap = countStrings(dataModel.getHarvestedRecords().stream().
                map(harvestedRecord -> harvestedRecord.rightsList.get(0)).
                collect(Collectors.toList()));

        for (LicenceCount licenceCount : dataModel.getState().getLicenceCounts()) {
            if (countMap.containsKey(licenceCount.getLicence_name())) {
                licenceCount.setRecord_count(toIntExact(countMap.get(licenceCount.getLicence_name())));
            }
        }
    }

    private void countRecordsForDCFormats() {
        Map<String, Long> countMap = countStrings(dataModel.getHarvestedRecords().stream().
                map(harvestedRecord -> harvestedRecord.dc_format).
                collect(Collectors.toList()));
        for (DCFormatCount dcFormatCount : dataModel.getState().getDCFormatCounts()) {
            if(countMap.containsKey(dcFormatCount.getDc_Format())) {
                dcFormatCount.setRecord_count(toIntExact(countMap.get(dcFormatCount.getDc_Format())));
            }
        }
    }

    private Map<String, Long> countStrings(List<String> strings) {
        return strings.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }


    private void countOARecords() {
        Set<LicenceCount> licenceCounts = dataModel.getState().getLicenceCounts();
        Map<Enum, Integer> licenceTypeCount = licenceCounts.stream().collect(
                Collectors.groupingBy(LicenceCount::getLicence_type, Collectors.summingInt(LicenceCount::getRecord_count)));
        logger.debug(licenceTypeCount);
        long oaCount = licenceTypeCount.getOrDefault(LicenceType.OPEN, 0);
        logger.info("Found {} Licences that are OPEN", oaCount);
        dataModel.getState().setRecord_count_oa(oaCount);
    }
}
