package de.energiequant.vatsim.compatibility.legacyproxy.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtils.class);

    private static final char PATH_SEPARATOR = '/';
    private static final char PACKAGE_SEPARATOR = '.';

    private ResourceUtils() {
        // utility class, hide constructor
    }

    public static Optional<String> getRelativeResourceContentAsString(Class<?> rootClass, String relativeFilePath, Charset characterSet) {
        StringBuilder sb = new StringBuilder();

        if (relativeFilePath.charAt(0) != PATH_SEPARATOR) {
            relativeFilePath = "/" + relativeFilePath;
        }

        String resourcePath = rootClass.getPackage().getName().replace(PACKAGE_SEPARATOR, PATH_SEPARATOR)
            + relativeFilePath;

        try (
            InputStream is = rootClass.getClassLoader().getResourceAsStream(resourcePath);
            InputStreamReader isr = new InputStreamReader(is, characterSet)
        ) {
            char[] buffer = new char[4096];
            while (isr.ready()) {
                int read = isr.read(buffer);
                if (read > 0) {
                    sb.append(buffer, 0, read);
                }
            }
        } catch (IOException ex) {
            LOGGER.warn(
                "failed to load resource content for class {}, relative file path {}, charset {}",
                rootClass, relativeFilePath, characterSet, ex
            );
            return Optional.empty();
        }

        return Optional.of(sb.toString());
    }

    public static Optional<String> getAbsoluteResourceContentAsString(Class<?> rootClass, String resourcePath, Charset characterSet) {
        StringBuilder sb = new StringBuilder();

        try (
            InputStream is = rootClass.getClassLoader().getResourceAsStream(resourcePath);
            InputStreamReader isr = new InputStreamReader(is, characterSet)
        ) {
            char[] buffer = new char[4096];
            while (isr.ready()) {
                int read = isr.read(buffer);
                if (read > 0) {
                    sb.append(buffer, 0, read);
                }
            }
        } catch (IOException ex) {
            LOGGER.warn(
                "failed to load resource content for class {}, absolute resource path {}, charset {}",
                rootClass, resourcePath, characterSet, ex
            );
            return Optional.empty();
        }

        return Optional.of(sb.toString());
    }
}
