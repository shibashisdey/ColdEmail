package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.models.PlatformSetting;
import com.shibashis.coldmailer.v1.repositories.PlatformSettingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlatformPolicyService {

    public static final String MAX_CONTACTS_PER_CAMPAIGN = "MAX_CONTACTS_PER_CAMPAIGN";
    public static final String MAX_RETRIES = "MAX_RETRIES";
    public static final String DISPATCH_MIN_DELAY_MS = "DISPATCH_MIN_DELAY_MS";
    public static final String DISPATCH_MAX_DELAY_MS = "DISPATCH_MAX_DELAY_MS";
    public static final String TRACK_OPEN_ENABLED = "TRACK_OPEN_ENABLED";
    public static final String TRACK_RESUME_ENABLED = "TRACK_RESUME_ENABLED";
    private static final Set<String> ALLOWED_KEYS = Set.of(
            MAX_CONTACTS_PER_CAMPAIGN,
            MAX_RETRIES,
            DISPATCH_MIN_DELAY_MS,
            DISPATCH_MAX_DELAY_MS,
            TRACK_OPEN_ENABLED,
            TRACK_RESUME_ENABLED
    );

    private final PlatformSettingRepository platformSettingRepository;

    public PlatformPolicyService(PlatformSettingRepository platformSettingRepository) {
        this.platformSettingRepository = platformSettingRepository;
    }

    public long getLong(String key, long defaultValue) {
        return getSetting(key).map(PlatformSetting::getSettingValue)
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return getSetting(key).map(PlatformSetting::getSettingValue)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    @Transactional
    public PlatformSetting upsert(String key, String value) {
        if (!ALLOWED_KEYS.contains(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported setting key");
        }
        PlatformSetting setting = platformSettingRepository.findBySettingKey(key).orElseGet(PlatformSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        return platformSettingRepository.save(setting);
    }

    public Map<String, String> allSettings() {
        List<PlatformSetting> settings = platformSettingRepository.findAll();
        return settings.stream().collect(Collectors.toMap(PlatformSetting::getSettingKey, PlatformSetting::getSettingValue));
    }

    private Optional<PlatformSetting> getSetting(String key) {
        return platformSettingRepository.findBySettingKey(key);
    }
}
