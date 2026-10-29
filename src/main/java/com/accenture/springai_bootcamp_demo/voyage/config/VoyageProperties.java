package com.accenture.springai_bootcamp_demo.voyage.config;

import com.accenture.springai_bootcamp_demo.voyage.entity.Location;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The crew every new voyage starts with
 */
@ConfigurationProperties(prefix = "voyage")
public record VoyageProperties(List<CrewProfile> crew) {
    public VoyageProperties {
        crew = crew == null ? List.of() : List.copyOf(crew);
    }

    public record CrewProfile(String name, String role, String persona, Location location) {
    }
}
