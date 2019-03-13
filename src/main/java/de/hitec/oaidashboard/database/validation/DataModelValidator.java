package de.hitec.oaidashboard.database.validation;

import de.hitec.oaidashboard.database.datastructures2.OAISet;
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

    public static boolean isValidOAISet(OAISet oaiSet) {
        return validateOAISetAgainstHibernate(oaiSet);
    }

    private static boolean validateOAISetAgainstHibernate(OAISet oaiSet) {
        boolean isValid = false;

        Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
        Set<ConstraintViolation<OAISet>> constraintViolations = validator.validate(oaiSet);
        if(constraintViolations.size() == 0){
            isValid = true;
        } else {
            logger.info("Invalid OAISet found with name: '{}' and spec: '{}'", oaiSet.getName(), oaiSet.getSpec());
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
