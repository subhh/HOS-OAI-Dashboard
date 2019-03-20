package de.uni_hamburg.sub.oaidashboard.database.validation;

import de.uni_hamburg.sub.oaidashboard.database.datastructures.LicenceCount;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.SetCount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * ContraintValidation using JSR 341
 * See: http://docs.jboss.org/hibernate/validator/5.0/reference/en-US/pdf/hibernate_validator_reference.pdf
 * TODO: update all datastructures that are used by hibernate to JSR 341!
 */

public class DataModelValidator {

    private static Logger logger = LogManager.getLogger(Class.class.getName());

    public static boolean isValidLicenceCount(LicenceCount licenceCount) {
        return validateLicenceCountAgainstHibernate(licenceCount);
    }

    public static boolean isValidSetCount(SetCount setCount) {
        return validateSetCountAgainstHibernate(setCount);
    }

    private static boolean validateLicenceCountAgainstHibernate(LicenceCount licenceCount) {
        boolean isValid = false;
        Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
        Set<ConstraintViolation<LicenceCount>> constraintViolations = validator.validate(licenceCount);
        if(constraintViolations.size() == 0){
            isValid = true;
        } else {
            logger.info("Invalid LicenceCount found with licence_name: '{}' " +
                    ", licence_type: '{}' and record_count: {}",
                    licenceCount.getLicence_name(), licenceCount.getLicence_type(), licenceCount.getRecord_count());
            for(ConstraintViolation<?> violation: constraintViolations) {
                logger.info("ContraintViolation - propertyPath: '{}', message: '{}', invalidValue: '{}'",
                        violation.getPropertyPath(),
                        violation.getMessage(),
                        violation.getInvalidValue());
            }
        }
        return isValid;
    }

    private static boolean validateSetCountAgainstHibernate(SetCount setCount) {
        boolean isValid = false;
        Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
        Set<ConstraintViolation<SetCount>> constraintViolations = validator.validate(setCount);
        if(constraintViolations.size() == 0){
            isValid = true;
        } else {
            logger.info("Invalid SetCount found with set_name: '{}', set_spec: '{}'" +
                            " and record_count: {}",
                    setCount.getSet_name(), setCount.getSet_spec(), setCount.getRecord_count());
            for(ConstraintViolation<?> violation: constraintViolations) {
                logger.info("ContraintViolation - propertyPath: '{}', message: '{}', invalidValue: '{}'",
                        violation.getPropertyPath(),
                        violation.getMessage(),
                        violation.getInvalidValue());
            }
        }
        return isValid;
    }
}
