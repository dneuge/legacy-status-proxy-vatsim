package de.energiequant.vatsim.compatibility.legacyproxy.attribution;

import static de.energiequant.vatsim.compatibility.legacyproxy.utils.ArgumentChecks.requireData;
import static de.energiequant.vatsim.compatibility.legacyproxy.utils.ArgumentChecks.requireDataOrNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Project {
    private final String name;
    private final String version;
    private final Optional<String> url;
    private final Set<License> licenses;

    Project(String name, String version, String url, Collection<License> licenses) {
        requireData("name", name);
        requireData("version", version);
        requireDataOrNull("url", url);
        requireData("licenses", licenses);

        this.name = name;
        this.version = version;
        this.url = Optional.ofNullable(url);
        this.licenses = Collections.unmodifiableSet(new HashSet<>(licenses));
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Optional<String> getUrl() {
        return url;
    }

    public Set<License> getLicenses() {
        return licenses;
    }
}
