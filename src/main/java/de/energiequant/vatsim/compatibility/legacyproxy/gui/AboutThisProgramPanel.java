package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import static de.energiequant.apputils.misc.gui.SwingHelper.styleBold;
import static de.energiequant.apputils.misc.gui.SwingHelper.styleMonospaced;
import static de.energiequant.apputils.misc.gui.SwingHelper.stylePlain;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.apputils.misc.ApplicationInfo;
import de.energiequant.apputils.misc.attribution.CopyrightNoticeProvider;
import de.energiequant.apputils.misc.attribution.License;
import de.energiequant.apputils.misc.attribution.Project;
import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;

public class AboutThisProgramPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AboutThisProgramPanel.class);

    private final CopyrightNoticeProvider copyrightNoticeProvider;
    private final Consumer<License> licenseClickedCallback;

    private static final boolean CAN_BROWSE = Desktop.isDesktopSupported()
        && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);

    public AboutThisProgramPanel(ApplicationInfo appInfo, Consumer<License> licenseClickedCallback) {
        super();

        this.copyrightNoticeProvider = appInfo.getCopyrightNoticeProvider();
        this.licenseClickedCallback = licenseClickedCallback;

        String applicationUrl = appInfo.getApplicationUrl();
        JLabel applicationUrlLabel = new JLabel(applicationUrl);
        linkExternal(applicationUrlLabel, applicationUrl);

        License programLicense = appInfo.getEffectiveLicense();
        JLabel programLicenseLabel = new JLabel("released under " + programLicense.getCanonicalName());
        linkLicense(programLicenseLabel, programLicense);

        List<Project> sortedDependencies = appInfo.getDependencies()
                                                  .stream()
                                                  .sorted(Comparator.comparing(Project::getName))
                                                  .collect(Collectors.toList());
        DependenciesList dependenciesList = new DependenciesList(sortedDependencies);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel(appInfo.getApplicationName()), gbc);

        gbc.gridy++;
        add(new JLabel("Version " + appInfo.getApplicationVersion()), gbc);

        for (String extraInfo : appInfo.getExtraInfo()) {
            gbc.gridy++;
            add(
                new JLabel(
                    extraInfo
                ),
                gbc
            );
        }

        gbc.gridy++;
        add(applicationUrlLabel, gbc);

        gbc.gridy++;
        add(programLicenseLabel, gbc);

        gbc.gridy++;
        add(new JLabel(appInfo.getApplicationCopyright()), gbc);

        gbc.gridy++;
        JPanel spacer = new JPanel();
        spacer.setMinimumSize(new Dimension(1, 20));
        add(spacer, gbc);

        gbc.gridy++;
        add(new JLabel("<html>" + AppConstants.DEPENDENCIES_CAPTION + "</html>"), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JScrollPane dependenciesScrollPane = new JScrollPane(dependenciesList);
        dependenciesScrollPane.getVerticalScrollBar().setUnitIncrement(12);
        add(dependenciesScrollPane, gbc);
    }

    private class DependenciesList extends JPanel {
        public DependenciesList(List<Project> dependencies) {
            super();

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            boolean isFirst = true;
            for (Project dependency : dependencies) {
                ProjectCard projectCard = new ProjectCard(dependency);
                if (!isFirst) {
                    projectCard.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
                } else {
                    isFirst = false;
                }

                gbc.gridy++;
                add(projectCard, gbc);
            }

            gbc.gridy++;
            gbc.weighty = 0.1;
            gbc.fill = GridBagConstraints.BOTH;
            add(new JPanel(), gbc);
        }
    }

    private class ProjectCard extends JPanel {
        public ProjectCard(Project project) {
            super();

            setOpaque(true);
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 2, 2, 2);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            add(styleBold(new JLabel(project.getName())), gbc);

            gbc.gridx++;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            add(stylePlain(new JLabel("version " + project.getVersion())), gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            String url = project.getUrl().orElse(null);
            if (url != null) {
                JLabel urlLabel = new JLabel(url);
                stylePlain(urlLabel);
                linkExternal(urlLabel, url);
                add(urlLabel, gbc);
                gbc.gridy++;
            }

            add(new ProjectCardLicensesPanel(project.getLicenses()), gbc);

            gbc.gridy++;
            gbc.insets = new Insets(5, 25, 5, 5);
            String copyrightNotice = getHtmlCopyrightNotice(project);
            add(styleMonospaced(new JLabel("<html>" + copyrightNotice + "</html>")), gbc);
        }

        private String getHtmlCopyrightNotice(Project project) {
            return StringEscapeUtils.escapeHtml4(copyrightNoticeProvider.getNotice(project))
                                    .replaceAll("\n", "<br/>");
        }
    }

    private class ProjectCardLicensesPanel extends JPanel {
        public ProjectCardLicensesPanel(Set<License> licenses) {
            super();

            setOpaque(true);
            setBackground(Color.WHITE);

            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

            List<License> sortedLicenses = licenses.stream()
                                                   .sorted(Comparator.comparing(License::getCanonicalName))
                                                   .collect(Collectors.toList());

            boolean isFirst = true;

            add(stylePlain(new JLabel(AppConstants.PROJECT_DEPENDENCY_LICENSE_INTRO)));
            for (License license : sortedLicenses) {
                if (!isFirst) {
                    add(stylePlain(new JLabel(" & ")));
                } else {
                    isFirst = false;
                }

                JLabel label = new JLabel(license.getCanonicalName());
                stylePlain(label);
                linkLicense(label, license);
                add(label);
            }
        }
    }

    private void linkExternal(JLabel label, String urlString) {
        if (!CAN_BROWSE) {
            return;
        }

        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException ex) {
            LOGGER.warn("Failed to convert to URI: \"{}\"", urlString, ex);
            return;
        }

        styleAsLink(label);

        addClickListener(
            label,
            () -> {
                try {
                    Desktop.getDesktop().browse(uri);
                } catch (IOException ex) {
                    LOGGER.warn("Failed to open {}", uri, ex);
                }
            }
        );
    }

    private void linkLicense(JLabel label, License license) {
        styleAsLink(label);

        addClickListener(
            label,
            () -> {
                licenseClickedCallback.accept(license);
            }
        );
    }

    private void addClickListener(JLabel label, Runnable runnable) {
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                runnable.run();
            }
        });
    }

    private void styleAsLink(JLabel label) {
        label.setForeground(Color.BLUE.darker());
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Font font = label.getFont();
        Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        Font underlinedFont = font.deriveFont(attributes);
        label.setFont(underlinedFont);
    }
}
