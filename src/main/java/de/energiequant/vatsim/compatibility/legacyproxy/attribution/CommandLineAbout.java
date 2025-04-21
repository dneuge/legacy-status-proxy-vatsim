package de.energiequant.vatsim.compatibility.legacyproxy.attribution;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringEscapeUtils;

import de.energiequant.apputils.misc.ApplicationInfo;
import de.energiequant.apputils.misc.attribution.License;
import de.energiequant.apputils.misc.attribution.Project;
import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;

public class CommandLineAbout {
    private final PrintStream ps;
    private final ApplicationInfo appInfo;
    private final String showLicenseOption;

    public CommandLineAbout(PrintStream ps, ApplicationInfo appInfo, String showLicenseOption) {
        this.ps = ps;
        this.appInfo = appInfo;
        this.showLicenseOption = showLicenseOption;
    }

    public void printLicenseAndQuit(String licenseName) {
        License license = null;
        try {
            license = License.valueOf(licenseName);
        } catch (Exception ex) {
            // ignore
        }

        if (license == null) {
            System.err.println("License " + licenseName + " not found");
            System.err.println();
            System.err.println("Available: " + sortedLicenseKeys().collect(Collectors.joining(", ")));
            System.exit(1);
        }

        printLicense(license);

        System.exit(0);
    }

    public void printLicense(License license) {
        String html = license.getText();

        String text = StringEscapeUtils.unescapeHtml4(html.replaceAll("<[^>]*?>", ""));

        ps.println(text);
    }

    public void printVersion() {
        ps.println(appInfo.getApplicationName());
        ps.println("version " + appInfo.getApplicationVersion());
        for (String extraInfo : appInfo.getExtraInfo()) {
            ps.println(extraInfo);
        }
        ps.println(appInfo.getApplicationUrl());
        License license = appInfo.getEffectiveLicense();
        ps.println("released under " + license.getCanonicalName() + " [" + license.name() + "]");
        ps.println(appInfo.getApplicationCopyright());
        ps.println();
        ps.println(AppConstants.DEPENDENCIES_CAPTION);
        appInfo.getDependencies().stream().sorted(Comparator.comparing(Project::getName)).forEachOrdered(this::printDependency);
        ps.println();
        ps.println("Generic copies of all involved software licenses are included with this program.");
        ps.println(
            "To view a license run with " + showLicenseOption + " <"
                + sortedLicenseKeys().collect(Collectors.joining("|")) + ">");
        ps.println("The corresponding license IDs are shown in brackets [ ] above.");
    }

    public void printDependency(Project project) {
        ps.println();
        ps.println(project.getName() + " (version " + project.getVersion() + ")");
        project.getUrl().ifPresent(ps::println);

        StringBuilder sb = new StringBuilder();
        sb.append(AppConstants.PROJECT_DEPENDENCY_LICENSE_INTRO);
        List<License> licenses = project.getLicenses()
                                        .stream()
                                        .sorted(Comparator.comparing(License::getCanonicalName))
                                        .collect(Collectors.toList());
        boolean isFirst = true;
        for (License license : licenses) {
            if (!isFirst) {
                sb.append(" & ");
            } else {
                isFirst = false;
            }
            sb.append(license.getCanonicalName());
            sb.append(" [");
            sb.append(license.name());
            sb.append("]");
        }
        ps.println(sb.toString());

        ps.println();
        ps.println(indent("    ", appInfo.getCopyrightNoticeProvider().getNotice(project)));
        ps.println();
    }

    private static String indent(String prefix, String s) {
        Pattern pattern = Pattern.compile("^", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(s);
        return matcher.replaceAll(prefix);
    }

    public void printDisclaimer() {
        ps.println(appInfo.getDisclaimer().orElseThrow(() -> new IllegalArgumentException("no disclaimer provided")));
    }

    private Stream<String> sortedLicenseKeys() {
        return Arrays.stream(License.values())
                     .map(License::name)
                     .sorted();
    }
}
