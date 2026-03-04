package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.dto.ProspectData;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProspectDerivationService {

    private static final String GENERIC_LABEL = "Generic";

    // Generic mailbox names that should not be treated as real people names
    private static final Set<String> GENERIC_NAMES = new HashSet<>(Arrays.asList(
            "info", "contact", "hello", "sales", "support", "admin", "noreply", "marketing"
    ));

    // Generic email providers / mailbox domains that should not be treated as real company names
    private static final Set<String> GENERIC_DOMAINS = new HashSet<>(Arrays.asList(
            "gmail", "yahoo", "outlook", "hotmail", "aol", "icloud", "zoho", "protonmail", "mail"
    ));

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.*)@(.*)$");

    public ProspectData deriveFromEmail(String email) {
        String firstName = null;
        String lastName = null;
        String companyName = null;

        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (!matcher.matches()) {
            return new ProspectData(null, null, null); // Invalid email format
        }

        String localPart = matcher.group(1).toLowerCase(Locale.ROOT);
        String domainPart = matcher.group(2).toLowerCase(Locale.ROOT);

        // 1. Derive First Name and Last Name (email-first)
        String normalizedLocalPart = localPart.replace("-", ".").replace("_", ".");
        if (GENERIC_NAMES.contains(normalizedLocalPart)) {
            firstName = GENERIC_LABEL;
        } else {
            String[] nameParts = normalizedLocalPart.split("\\.");
            if (nameParts.length == 1) {
                firstName = capitalize(nameParts[0]);
            } else if (nameParts.length > 1) {
                firstName = capitalize(nameParts[0]);
                lastName = capitalize(nameParts[1]);
            }
        }

        // 2. Derive Company Name
        String[] domainSegments = domainPart.split("\\.");
        if (domainSegments.length > 1) {
            String potentialCompany = domainSegments[domainSegments.length - 2];
            if (!GENERIC_DOMAINS.contains(potentialCompany)) {
                companyName = capitalize(potentialCompany);
            } else {
                companyName = GENERIC_LABEL;
            }
        }

        return new ProspectData(firstName, lastName, companyName);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}
