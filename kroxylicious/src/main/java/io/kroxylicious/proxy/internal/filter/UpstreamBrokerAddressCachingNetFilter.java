/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal.filter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kroxylicious.proxy.bootstrap.FilterChainFactory;
import io.kroxylicious.proxy.filter.KrpcFilter;
import io.kroxylicious.proxy.filter.MetadataResponseFilter;
import io.kroxylicious.proxy.filter.NetFilter;
import io.kroxylicious.proxy.service.ClusterEndpointConfigProvider;
import io.kroxylicious.proxy.service.HostPort;

/**
 * Temporary implementation of NetFilter that uses the port number from the local end of the
 * downstream connection to identify the virtual clusters broker.  It then uses this to identify
 * the upstream broker address from a cache built from a previous connection's metadata response.
 */
public class UpstreamBrokerAddressCachingNetFilter implements NetFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamBrokerAddressCachingNetFilter.class);

    private final HostPort targetClusterBootstrap;
    private final FilterChainFactory filterChainFactory;
    private final ClusterEndpointConfigProvider endpointConfigProvider;

    private final Map<Integer, HostPort> upstreamBrokers = new ConcurrentHashMap<>();

    public UpstreamBrokerAddressCachingNetFilter(HostPort targetClusterBootstrap, FilterChainFactory filterChainFactory,
                                                 ClusterEndpointConfigProvider endpointConfigProvider) {
        this.targetClusterBootstrap = targetClusterBootstrap;
        this.filterChainFactory = filterChainFactory;
        this.endpointConfigProvider = endpointConfigProvider;
    }

    @Override
    public void selectServer(NetFilterContext context) {
        var filters = new ArrayList<>(Arrays.stream(filterChainFactory.createFilters()).toList());

        // Add a filter to the *end of the chain* that gathers the true nodeId/upstream broker mapping.
        filters.add((MetadataResponseFilter) (header, response, filterContext) -> {
            response.brokers().forEach(b -> {
                var replacement = new HostPort(b.host(), b.port());
                var existing = upstreamBrokers.put(b.nodeId(), replacement);
                if (!replacement.equals(existing)) {
                    LOGGER.info("Got upstream for broker {} : {}", b.nodeId(), replacement);
                }
            });
            filterContext.forwardResponse(response);
        });

        var targetPort = ((InetSocketAddress) context.localAddress()).getPort();
        var endpointMatchResult = endpointConfigProvider.hasMatchingEndpoint(context.sniHostname(), targetPort);

        HostPort target;
        if (endpointMatchResult.matched() && endpointMatchResult.nodeId() != null) {
            var upstreamBroker = upstreamBrokers.get(endpointMatchResult.nodeId());
            if (upstreamBroker != null) {
                target = upstreamBroker;
            }
            else {
                // TODO: this behaviour is sub-optimal as it means a client will proceed with a connection to the wrong broker.
                // This will lead to difficult to diagnose failure cases later (produces going to the wrong broker, metadata refresh cycles, etc).
                LOGGER.warn("An upstream address for broker {} is not yet known, connecting the client to bootstrap instead.", endpointMatchResult.nodeId());
                target = targetClusterBootstrap;
            }
        }
        else if (endpointMatchResult.matched()) {
            target = targetClusterBootstrap;
        }
        else {
            throw new RuntimeException(
                    "Connection to %s:%d cannot be routed to an upstream endpoint".formatted(context.sniHostname() == null ? "" : context.sniHostname(),
                            targetPort));
        }

        context.initiateConnect(target.host(), target.port(), filters.toArray(new KrpcFilter[0]));
    }

}