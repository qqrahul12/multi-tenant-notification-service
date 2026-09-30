package com.notificationservice.channel;

import com.notificationservice.domain.Channel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory Pattern — maps Channel enums to ChannelDispatcher implementations.
 *
 * Spring injects ALL ChannelDispatcher beans automatically.
 * OCP: adding a new channel dispatcher requires zero changes here.
 * DIP: callers depend on the ChannelDispatcher interface, not concrete classes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelDispatcherFactory {

    private final List<ChannelDispatcher> dispatchers;
    private Map<Channel, ChannelDispatcher> dispatcherMap;

    @PostConstruct
    public void init() {
        dispatcherMap = dispatchers.stream()
                .collect(Collectors.toMap(
                        ChannelDispatcher::getSupportedChannel,
                        Function.identity()
                ));
        log.info("ChannelDispatcherFactory initialized with channels: {}", dispatcherMap.keySet());
    }

    public ChannelDispatcher getDispatcher(Channel channel) {
        ChannelDispatcher dispatcher = dispatcherMap.get(channel);
        if (dispatcher == null) {
            throw new IllegalArgumentException("No dispatcher registered for channel: " + channel);
        }
        return dispatcher;
    }
}
