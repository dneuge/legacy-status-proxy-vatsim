package de.energiequant.vatsim.compatibility.legacyproxy.attribution;

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

    private static void requireData(String what, Object o) {
        if (o == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }

        requireDataOrNull(what, o);
    }

    private static void requireDataOrNull(String what, Object o) {
        if (o instanceof String) {
            String s = (String) o;
            if (s.trim().isEmpty()) {
                throw new IllegalArgumentException(what + " must not be blank");
            }
        } else if (o instanceof Collection) {
            Collection<?> c = (Collection<?>) o;
            if (c.isEmpty()) {
                throw new IllegalArgumentException(what + " must not be empty");
            }
        }
    }
}
