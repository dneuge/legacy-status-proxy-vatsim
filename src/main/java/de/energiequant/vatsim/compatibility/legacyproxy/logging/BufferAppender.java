package de.energiequant.vatsim.compatibility.legacyproxy.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "Buffer", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class BufferAppender extends AbstractAppender {
    private final List<FormattedEvent> formattedEvents = new ArrayList<>();
    private final AtomicBoolean isEnabled = new AtomicBoolean(true);
    private final Layout<? extends Serializable> layout;

    private static final Collection<BufferAppender> instances = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicBoolean isGloballyEnabled = new AtomicBoolean(true);

    public static class FormattedEvent {
        private final Level level;
        private final String message;

        private FormattedEvent(Level level, String message) {
            this.level = level;
            this.message = message;
        }

        public Level getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }
    }

    private BufferAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, false, new Property[0]);
        this.layout = layout;
        this.isEnabled.set(isGloballyEnabled.get());
    }

    @Override
    public void append(LogEvent event) {
        if (!isEnabled.get()) {
            return;
        }

        FormattedEvent formattedEvent = new FormattedEvent(event.getLevel(), new String(layout.toByteArray(event)));

        synchronized (formattedEvents) {
            formattedEvents.add(formattedEvent);
        }
    }

    private void disableAndClear() {
        isEnabled.set(false);
        synchronized (formattedEvents) {
            formattedEvents.clear();
        }
    }

    public static void disableAndClearAll() {
        isGloballyEnabled.set(false);

        for (BufferAppender instance : getInstances()) {
            instance.disableAndClear();
        }
    }

    public List<FormattedEvent> getFormattedEventsAndClear() {
        List<FormattedEvent> copied = new ArrayList<>(formattedEvents);
        synchronized (formattedEvents) {
            copied.addAll(formattedEvents);
            formattedEvents.clear();
        }

        return copied;
    }

    public static Collection<BufferAppender> getInstances() {
        return instances;
    }

    @PluginFactory
    public static BufferAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filter") Filter filter) {
        BufferAppender appender = new BufferAppender(name, filter, layout);
        instances.add(appender);
        return appender;
    }
}
