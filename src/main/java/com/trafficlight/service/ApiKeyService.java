package com.trafficlight.service;

import com.trafficlight.model.ApiKey;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ApiKeyService {

    private final Map<String, ApiKey> keysById = new ConcurrentHashMap<>();
    private final Map<String, String> idByKey = new ConcurrentHashMap<>();

    @ConfigProperty(name = "traffic.keys.default-admin", defaultValue = "admin-key-2026")
    String defaultAdminKey;

    @ConfigProperty(name = "traffic.keys.default-operator", defaultValue = "operator-key-2026")
    String defaultOperatorKey;

    @ConfigProperty(name = "traffic.keys.default-emergency", defaultValue = "dispatch-key-2026")
    String defaultEmergencyKey;

    @PostConstruct
    void init() {
        registerKey(new ApiKey(
            "key-admin-01",
            defaultAdminKey,
            "City Admin Center",
            "ADMIN",
            true,
            Instant.now()
        ));

        registerKey(new ApiKey(
            "key-operator-01",
            defaultOperatorKey,
            "Traffic Operator #1",
            "OPERATOR",
            true,
            Instant.now()
        ));

        registerKey(new ApiKey(
            "key-emergency-01",
            defaultEmergencyKey,
            "Emergency Services Dispatch",
            "EMERGENCY",
            true,
            Instant.now()
        ));
    }

    private void registerKey(ApiKey apiKey) {
        keysById.put(apiKey.id(), apiKey);
        idByKey.put(apiKey.key(), apiKey.id());
    }

    public Optional<ApiKey> validate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String id = idByKey.get(rawKey.trim());
        if (id == null) {
            return Optional.empty();
        }
        ApiKey key = keysById.get(id);
        if (key != null && key.enabled()) {
            return Optional.of(key);
        }
        return Optional.empty();
    }

    public List<ApiKey> listAll() {
        List<ApiKey> list = new ArrayList<>(keysById.values());
        list.sort(Comparator.comparing(ApiKey::createdAt));
        return list;
    }

    public ApiKey createKey(String name, String role) {
        String id = "key-" + UUID.randomUUID().toString().substring(0, 8);
        String rawKey = "tlk_" + UUID.randomUUID().toString().replace("-", "");
        String assignedRole = (role == null || role.isBlank()) ? "OPERATOR" : role.toUpperCase().trim();
        String assignedName = (name == null || name.isBlank()) ? "Unnamed Client" : name.trim();

        ApiKey newKey = new ApiKey(id, rawKey, assignedName, assignedRole, true, Instant.now());
        registerKey(newKey);
        return newKey;
    }

    public boolean revokeKey(String id) {
        ApiKey existing = keysById.get(id);
        if (existing == null) {
            return false;
        }
        ApiKey revoked = new ApiKey(
            existing.id(),
            existing.key(),
            existing.name(),
            existing.role(),
            false,
            existing.createdAt()
        );
        keysById.put(id, revoked);
        return true;
    }

    public boolean deleteKey(String id) {
        ApiKey removed = keysById.remove(id);
        if (removed != null) {
            idByKey.remove(removed.key());
            return true;
        }
        return false;
    }
}
