package org.cru.migration.support;

import org.apache.commons.validator.routines.EmailValidator;

/**
 * author@lee.braddock
 */
public class EmailHelper {
    public static String getEmailDomain(String email) {
        try {
            if (EmailValidator.getInstance().isValid(email)) {
                return email.split("@")[1];
            }

            return null;

        } catch (Exception e) {

            return null;
        }
    }
}
