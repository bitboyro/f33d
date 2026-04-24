package ro.bitboy.f33d.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class BrokenPipeFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent event) {
        IThrowableProxy t = event.getThrowableProxy();
        while (t != null) {
            if ("java.io.IOException".equals(t.getClassName())
                    && t.getMessage() != null
                    && t.getMessage().contains("Broken pipe")) {
                return FilterReply.DENY;
            }
            t = t.getCause();
        }
        return FilterReply.NEUTRAL;
    }
}
