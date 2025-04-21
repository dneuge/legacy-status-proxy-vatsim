package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Dimension;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import de.energiequant.apputils.misc.ApplicationInfo;
import de.energiequant.apputils.misc.attribution.License;

public class AboutWindow extends JFrame {
    private final JTabbedPane tabbedPane;
    private final LicensesPanel licensesPanel;

    private static final int DISCLAIMER_TAB_INDEX = 1;
    private static final int LICENSES_TAB_INDEX = 2;

    public AboutWindow(ApplicationInfo appInfo) {
        super("About...");

        tabbedPane = new JTabbedPane();
        licensesPanel = new LicensesPanel(appInfo);

        tabbedPane.addTab("About this program", new AboutThisProgramPanel(appInfo, this::showLicense));

        Optional<String> disclaimer = appInfo.getDisclaimer();
        if (disclaimer.isPresent()) {
            tabbedPane.addTab("Disclaimer", new DisclaimerPanel(disclaimer.get()));
        }

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
