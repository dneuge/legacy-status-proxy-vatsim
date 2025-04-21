package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.apputils.misc.attribution.License;
import de.energiequant.apputils.misc.attribution.Project;

public class AboutWindow extends JFrame {
    private final JTabbedPane tabbedPane;
    private final LicensesPanel licensesPanel;

    private static final int DISCLAIMER_TAB_INDEX = 1;
    private static final int LICENSES_TAB_INDEX = 2;

    public AboutWindow() {
        super("About...");

        Collection<Project> dependencies = Main.getDependencies();
        Set<License> licenses = new HashSet<License>();
        licenses.add(Main.getEffectiveLicense());
        for (Project dependency : dependencies) {
            licenses.addAll(dependency.getLicenses());
        }

        tabbedPane = new JTabbedPane();
        licensesPanel = new LicensesPanel(licenses);

        tabbedPane.addTab("About this program", new AboutThisProgramPanel(dependencies, this::showLicense));
        tabbedPane.addTab("Disclaimer", new DisclaimerPanel());
        tabbedPane.addTab("Licenses", licensesPanel);

        add(tabbedPane);

        setMinimumSize(new Dimension(500, 300));
        setSize(new Dimension(800, 700));
    }

    public void showDisclaimer() {
        tabbedPane.setSelectedIndex(DISCLAIMER_TAB_INDEX);
        setVisible(true);
    }

    private void showLicense(License license) {
        licensesPanel.selectLicense(license);
        tabbedPane.setSelectedIndex(LICENSES_TAB_INDEX);
    }
}
