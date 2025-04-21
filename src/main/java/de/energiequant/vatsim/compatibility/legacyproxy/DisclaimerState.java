package de.energiequant.vatsim.compatibility.legacyproxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.apputils.misc.ApplicationInfo;

public class DisclaimerState {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisclaimerState.class);

    private final String disclaimer;
    private final String disclaimerAcceptanceText;
    private final String disclaimerHash;
    private final Set<Runnable> listeners = Collections.synchronizedSet(new HashSet<>());
    private final AtomicBoolean isDisclaimerAccepted = new AtomicBoolean();

    private static final String DEFAULT_ACCEPTANCE_TEXT = "I understand and accept the disclaimer";

    public DisclaimerState(ApplicationInfo appInfo) {
        disclaimer = appInfo.getDisclaimer().orElseThrow(() -> new IllegalArgumentException("Disclaimer missing"));
        disclaimerAcceptanceText = appInfo.getDisclaimerAcceptanceText().orElse(DEFAULT_ACCEPTANCE_TEXT);
        disclaimerHash = DigestUtils.md5Hex(disclaimer);
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public String getDisclaimerAcceptanceText() {
        return disclaimerAcceptanceText;
    }

    public String getDisclaimerHash() {
        return disclaimerHash;
    }

    public boolean isAccepted() {
        return isDisclaimerAccepted.get();
    }

    public void setAccepted(boolean isDisclaimerAccepted) {
        boolean previous = this.isDisclaimerAccepted.getAndSet(isDisclaimerAccepted);
        if (previous != isDisclaimerAccepted) {
            notifyListeners(listeners);
        }
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners(Set<Runnable> listeners) {
        Collection<Runnable> copy = new ArrayList<>(listeners);
        for (Runnable listener : copy) {
            try {
                listener.run();
            } catch (Exception ex) {
                LOGGER.warn("notification of disclaimer listener failed", ex);
            }
        }
    }
}
