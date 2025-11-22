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

    // Generic first names or mailbox names that should be replaced with "there"
    private static final Set<String> GENERIC_NAMES = new HashSet<>(Arrays.asList(
            "info", "contact", "hello", "sales", "support", "admin", "noreply", "marketing"
    ));

    // Generic email providers / company domains that should result in no company name
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

        // 1. Derive First Name and Last Name
        if (GENERIC_NAMES.contains(localPart)) {
            firstName = "there"; // Fallback for generic names
        } else {
            String[] nameParts = localPart.split("\\.");
            if (nameParts.length == 1) {
                firstName = capitalize(nameParts[0]);
            } else if (nameParts.length > 1) {
                firstName = capitalize(nameParts[0]);
                lastName = capitalize(nameParts[1]); // Taking first two parts
            }
        }

        // 2. Derive Company Name
        String[] domainSegments = domainPart.split("\\.");
        if (domainSegments.length > 1) {
            String potentialCompany = domainSegments[domainSegments.length - 2]; // e.g., 'example' from 'example.com' or 'example.co.uk'
            if (!GENERIC_DOMAINS.contains(potentialCompany)) {
                companyName = capitalize(potentialCompany);
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
