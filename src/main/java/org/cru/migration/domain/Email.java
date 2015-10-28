package org.cru.migration.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.validator.routines.EmailValidator;

/**
 * author@lee.braddock
 */
public class Email {

    private String email;

    public Email(final String email) {
        this.email = email;
    }

    public String getDomain() {
        try {
            if (EmailValidator.getInstance().isValid(email)) {
                return email.split("@")[1];
            }

            return null;

        } catch (Exception e) {

            return null;
        }
    }

    public String getCountryCode() {
        try {
            if (EmailValidator.getInstance().isValid(email)) {
                String domain = email.split("@")[1];

                Iterable<String> domainIterable = Splitter.on(".").omitEmptyStrings().trimResults().split(domain);

                String countryCode = Iterables.getLast(domainIterable);

                return Iterables.size(domainIterable) == 3 ? countryCode : null;
            }

            return null;

        } catch (Exception e) {

            return null;
        }
    }

    @Override
    public String toString() {
        return email;
    }
}
