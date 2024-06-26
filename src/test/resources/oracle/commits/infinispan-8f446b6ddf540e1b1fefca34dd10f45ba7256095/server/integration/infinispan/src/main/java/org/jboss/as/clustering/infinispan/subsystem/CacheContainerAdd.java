/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.commons.util.ServiceFinder;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.jgroups.ChannelFactory;
import org.infinispan.server.jgroups.subsystem.ChannelFactoryService;
import org.infinispan.server.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerConfigurationService.AuthorizationConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.Services;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Paul Ferraro
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 */
public class CacheContainerAdd extends AbstractAddStepHandler {

    private static final Logger log = Logger.getLogger(CacheContainerAdd.class.getPackage().getName());

    public static final CacheContainerAdd INSTANCE = new CacheContainerAdd();

    static ModelNode createOperation(ModelNode address, ModelNode existing) throws OperationFailedException {
        ModelNode operation = Util.getEmptyOperation(ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) throws OperationFailedException {
        // AS7-3488 make default-cache non required attrinbute
        // target.get(ModelKeys.DEFAULT_CACHE).set(source.get(ModelKeys.DEFAULT_CACHE));

        CacheContainerResource.DEFAULT_CACHE.validateAndSet(source, target);
        // TODO: need to handle list types
        if (source.hasDefined(ModelKeys.ALIASES)) {
            target.get(ModelKeys.ALIASES).set(source.get(ModelKeys.ALIASES));
        }
        CacheContainerResource.JNDI_NAME.validateAndSet(source, target);
        CacheContainerResource.START.validateAndSet(source, target);
        CacheContainerResource.LISTENER_EXECUTOR.validateAndSet(source, target);
        CacheContainerResource.EVICTION_EXECUTOR.validateAndSet(source, target);
        CacheContainerResource.EXPIRATION_EXECUTOR.validateAndSet(source, target);
        CacheContainerResource.STATE_TRANSFER_EXECUTOR.validateAndSet(source, target);
        CacheContainerResource.ASYNC_EXECUTOR.validateAndSet(source, target);
        CacheContainerResource.REPLICATION_QUEUE_EXECUTOR.validateAndSet(source, target);
        CacheContainerResource.CACHE_CONTAINER_MODULE.validateAndSet(source, target);
        CacheContainerResource.STATISTICS.validateAndSet(source, target);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        populate(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // Because we use child resources in a read-only manner to configure the cache container, replace the local model with the full model
        installRuntimeServices(context, operation, Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
    }

    Collection<ServiceController<?>> installRuntimeServices(OperationContext context, ModelNode operation, ModelNode containerModel) throws OperationFailedException {

        final PathAddress address = getCacheContainerAddressFromOperation(operation);
        final String name = address.getLastElement().getValue();
        final ServiceTarget target = context.getServiceTarget();

        // pick up the attribute values from the model
        ModelNode resolvedValue = null ;
        // make default cache non required (AS7-3488)
        final String defaultCache = (resolvedValue = CacheContainerResource.DEFAULT_CACHE.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String jndiName = (resolvedValue = CacheContainerResource.JNDI_NAME.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String listenerExecutor = (resolvedValue = CacheContainerResource.LISTENER_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String asyncExecutor = (resolvedValue = CacheContainerResource.ASYNC_EXECUTOR.resolveModelAttribute(context,
              containerModel)).isDefined() ? resolvedValue.asString() : null;
        String expirationExecutor = (resolvedValue = CacheContainerResource.EXPIRATION_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        if (expirationExecutor == null) {
           expirationExecutor = (resolvedValue = CacheContainerResource.EVICTION_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        }
        final String replicationQueueExecutor = (resolvedValue = CacheContainerResource.REPLICATION_QUEUE_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final String stateTransferExecutor = (resolvedValue = CacheContainerResource.STATE_TRANSFER_EXECUTOR.resolveModelAttribute(context, containerModel)).isDefined() ? resolvedValue.asString() : null ;
        final ServiceController.Mode initialMode = StartMode.valueOf(CacheContainerResource.START.resolveModelAttribute(context, containerModel).asString()).getMode();
        final boolean statistics = CacheContainerResource.STATISTICS.resolveModelAttribute(context, containerModel).asBoolean();

        ServiceName[] aliases = null;
        if (containerModel.hasDefined(ModelKeys.ALIASES)) {
            List<ModelNode> list = operation.get(ModelKeys.ALIASES).asList();
            aliases = new ServiceName[list.size()];
            for (int i = 0; i < list.size(); i++) {
                aliases[i] = EmbeddedCacheManagerService.getServiceName(list.get(i).asString());
            }
        }

        final ModuleIdentifier moduleId = (resolvedValue = CacheContainerResource.CACHE_CONTAINER_MODULE.resolveModelAttribute(context, containerModel)).isDefined() ? ModuleIdentifier.fromString(resolvedValue.asString()) : null;

        // if we have a transport defined, pick up the transport-related attributes and install a channel
        final Transport transportConfig = containerModel.hasDefined(ModelKeys.TRANSPORT) && containerModel.get(ModelKeys.TRANSPORT).hasDefined(ModelKeys.TRANSPORT_NAME) ? new Transport() : null;

        String stack = null;
        String transportExecutor = null;
        String totalOrderExecutor = null;
        String remoteCommandExecutor = null;

        Collection<ServiceController<?>> controllers = new LinkedList<ServiceController<?>>();

        if (transportConfig != null) {
            ModelNode transport = containerModel.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);

            stack = (resolvedValue = TransportResource.STACK.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null ;
            // if cluster is not defined, use the cache container name as the default
            final String cluster = (resolvedValue = TransportResource.CLUSTER.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : name ;
            long lockTimeout = TransportResource.LOCK_TIMEOUT.resolveModelAttribute(context, transport).asLong();
            transportExecutor = (resolvedValue = TransportResource.EXECUTOR.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
            totalOrderExecutor = (resolvedValue = TransportResource.TOTAL_ORDER_EXECUTOR.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
            remoteCommandExecutor = (resolvedValue = TransportResource.REMOTE_COMMAND_EXECUTOR.resolveModelAttribute(context, transport)).isDefined() ? resolvedValue.asString() : null;
            final boolean strictPeerToPeer = TransportResource.STRICT_PEER_TO_PEER.resolveModelAttribute(context, transport).asBoolean();
            transportConfig.setStrictPeerToPeer(strictPeerToPeer);

            // initialise the Transport
            transportConfig.setLockTimeout(lockTimeout);

            controllers.add(this.installChannelService(target, name, cluster, stack));

            for (ChannelDependentServiceProvider provider: ServiceFinder.load(ChannelDependentServiceProvider.class, ChannelDependentServiceProvider.class.getClassLoader())) {
                controllers.add(provider.install(target, name));
            }
        }

        Authorization authorizationConfig = null;
        if (containerModel.hasDefined(ModelKeys.SECURITY) && containerModel.get(ModelKeys.SECURITY).hasDefined(ModelKeys.SECURITY_NAME)) {
            ModelNode securityModel = containerModel.get(ModelKeys.SECURITY, ModelKeys.SECURITY_NAME);

            if (securityModel.hasDefined(ModelKeys.AUTHORIZATION) && securityModel.get(ModelKeys.AUTHORIZATION).hasDefined(ModelKeys.AUTHORIZATION_NAME)) {
                ModelNode authzModel = securityModel.get(ModelKeys.AUTHORIZATION, ModelKeys.AUTHORIZATION_NAME);

                authorizationConfig = new Authorization();
                authorizationConfig.setPrincipalMapper((resolvedValue = CacheContainerAuthorizationResource.MAPPER.resolveModelAttribute(context, authzModel)).isDefined() ? resolvedValue.asString() : null);

                for(ModelNode roleNode : authzModel.get(ModelKeys.ROLE).asList()) {
                    ModelNode role = roleNode.get(0);
                    String roleName = AuthorizationRoleResource.NAME.resolveModelAttribute(context, role).asString();
                    List<String> permissions = new ArrayList<String>();
                    for(ModelNode permission : AuthorizationRoleResource.PERMISSIONS.resolveModelAttribute(context, role).asList()) {
                        permissions.add(permission.asString());
                    }
                    authorizationConfig.getRoles().put(roleName, permissions);
                }

            }
        }

        // install the cache container configuration service
        controllers.add(this.installContainerConfigurationService(target, name, defaultCache, statistics, moduleId,
                stack, transportConfig, authorizationConfig, transportExecutor, totalOrderExecutor,
                remoteCommandExecutor, listenerExecutor, asyncExecutor, expirationExecutor, replicationQueueExecutor, stateTransferExecutor));

        // install a cache container service
        controllers.add(this.installContainerService(target, name, aliases, transportConfig, initialMode));

        // install a name service entry for the cache container
        controllers.add(this.installJndiService(target, name, InfinispanJndiName.createCacheContainerJndiName(jndiName, name)));

        controllers.add(this.installKeyAffinityServiceFactoryService(target, name));

        log.debugf("%s cache container installed", name);
        return controllers;
     }

     void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = getCacheContainerAddressFromOperation(operation);
        final String containerName = address.getLastElement().getValue();

        // need to remove all container-related services started, in reverse order
        context.removeService(KeyAffinityServiceFactoryService.getServiceName(containerName));

        // remove the BinderService entry
        ModelNode resolvedValue = null;
        final String jndiName = (resolvedValue = CacheContainerResource.JNDI_NAME.resolveModelAttribute(context, model)).isDefined() ? resolvedValue.asString() : null;
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(InfinispanJndiName.createCacheContainerJndiName(jndiName, containerName));
        context.removeService(bindInfo.getBinderServiceName());

        // remove the cache container
        context.removeService(EmbeddedCacheManagerService.getServiceName(containerName));
        context.removeService(EmbeddedCacheManagerConfigurationService.getServiceName(containerName));

        // check if a channel was installed
        final ServiceName channelServiceName = ChannelService.getServiceName(containerName) ;
        final ServiceController<?> channelServiceController = context.getServiceRegistry(false).getService(channelServiceName);
        if (channelServiceController != null) {
            for (ChannelDependentServiceProvider provider: ServiceFinder.load(ChannelDependentServiceProvider.class, ChannelDependentServiceProvider.class.getClassLoader())) {
                context.removeService(provider.getServiceName(containerName));
            }
            context.removeService(channelServiceName);
        }
    }

    ServiceController<?> installKeyAffinityServiceFactoryService(ServiceTarget target, String containerName) {
        return target.addService(KeyAffinityServiceFactoryService.getServiceName(containerName), new KeyAffinityServiceFactoryService(10))
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    ServiceController<?> installChannelService(ServiceTarget target, String containerName, String cluster, String stack) {

        final InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
        return target.addService(ChannelService.getServiceName(containerName), new ChannelService(cluster, channelFactory))
                .addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, channelFactory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    PathAddress getCacheContainerAddressFromOperation(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR)) ;
    }

    ServiceController<?> installContainerConfigurationService(ServiceTarget target,
            String containerName, String defaultCache, boolean statistics, ModuleIdentifier moduleId, String stack, Transport transportConfig, Authorization authorizationConfig,
            String transportExecutor, String totalOrderExecutor, String remoteCommandExecutor, String listenerExecutor, String asyncExecutor,
            String expirationExecutor, String replicationQueueExecutor, String stateTransferExecutor) {

        final ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);
        final EmbeddedCacheManagerDependencies dependencies = new EmbeddedCacheManagerDependencies(transportConfig, authorizationConfig);
        final Service<EmbeddedCacheManagerConfiguration> service = new EmbeddedCacheManagerConfigurationService(containerName, defaultCache, statistics, moduleId, dependencies);
        final ServiceBuilder<EmbeddedCacheManagerConfiguration> configBuilder = target.addService(configServiceName, service)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, dependencies.getModuleLoaderInjector())
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, dependencies.getMBeanServerInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        // add these dependencies only if we have a transport defined
        if (transportConfig != null) {
            if (transportExecutor != null) {
                addExecutorDependency(configBuilder, transportExecutor, transportConfig.getExecutorInjector());
                addExecutorDependency(configBuilder, totalOrderExecutor, transportConfig.getTotalorderExecutorInjector());
                addExecutorDependency(configBuilder, remoteCommandExecutor, transportConfig.getRemoteCommandExecutorInjector());
            }
            configBuilder.addDependency(ChannelFactoryService.getServiceName(stack), ChannelFactory.class, transportConfig.getChannelFactoryInjector());
        }

        addExecutorDependency(configBuilder, listenerExecutor, dependencies.getListenerExecutorInjector());
        addExecutorDependency(configBuilder, asyncExecutor, dependencies.getAsyncExecutorInjector());
        addExecutorDependency(configBuilder, stateTransferExecutor, dependencies.getStateTransferExecutorInjector());
        addScheduledExecutorDependency(configBuilder, expirationExecutor, dependencies.getExpirationExecutorInjector());
        addScheduledExecutorDependency(configBuilder, replicationQueueExecutor, dependencies.getReplicationQueueExecutorInjector());

        return configBuilder.install();
    }

    ServiceController<?> installContainerService(ServiceTarget target, String containerName, ServiceName[] aliases, Transport transport, ServiceController.Mode initialMode) {

        final ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
        final ServiceName configServiceName = EmbeddedCacheManagerConfigurationService.getServiceName(containerName);
        final InjectedValue<EmbeddedCacheManagerConfiguration> config = new InjectedValue<EmbeddedCacheManagerConfiguration>();
        final Service<EmbeddedCacheManager> service = new EmbeddedCacheManagerService(config);
        ServiceBuilder<EmbeddedCacheManager> builder = target.addService(containerServiceName, service)
                .addDependency(configServiceName, EmbeddedCacheManagerConfiguration.class, config)
                .addAliases(aliases)
                .setInitialMode(initialMode)
        ;
        if (transport != null) {
            builder.addDependency(ChannelService.getServiceName(containerName));
        }
        return builder.install();
    }

    ServiceController<?> installJndiService(ServiceTarget target, String containerName, String jndiName) {

        final ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);

        final BinderService binder = new BinderService(bindInfo.getBindName());
        return target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName))
                .addDependency(containerServiceName, CacheContainer.class, new ManagedReferenceInjector<CacheContainer>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install()
        ;
    }

    private void addExecutorDependency(ServiceBuilder<EmbeddedCacheManagerConfiguration> builder, String executor, Injector<Executor> injector) {
        if (executor != null) {
            builder.addDependency(ThreadsServices.executorName(executor), Executor.class, injector);
        }
    }

    private void addScheduledExecutorDependency(ServiceBuilder<EmbeddedCacheManagerConfiguration> builder, String executor, Injector<ScheduledExecutorService> injector) {
        if (executor != null) {
            builder.addDependency(ThreadsServices.executorName(executor), ScheduledExecutorService.class, injector);
        }
    }

    static class EmbeddedCacheManagerDependencies implements EmbeddedCacheManagerConfigurationService.Dependencies {
        private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
        private final InjectedValue<Executor> listenerExecutor = new InjectedValue<Executor>();
        private final InjectedValue<Executor> asyncExecutor = new InjectedValue<Executor>();
        private final InjectedValue<ScheduledExecutorService> expirationExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<ScheduledExecutorService> replicationQueueExecutor = new InjectedValue<ScheduledExecutorService>();
        private final InjectedValue<Executor> stateTransferExecutor = new InjectedValue<>();
        private final EmbeddedCacheManagerConfigurationService.TransportConfiguration transport;
        private final EmbeddedCacheManagerConfigurationService.AuthorizationConfiguration authorization;
        private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<ModuleLoader>();

        EmbeddedCacheManagerDependencies(EmbeddedCacheManagerConfigurationService.TransportConfiguration transport, EmbeddedCacheManagerConfigurationService.AuthorizationConfiguration authorization) {
            this.transport = transport;
            this.authorization = authorization;
        }

        Injector<MBeanServer> getMBeanServerInjector() {
            return this.mbeanServer;
        }

        Injector<Executor> getListenerExecutorInjector() {
            return this.listenerExecutor;
        }

        Injector<Executor> getAsyncExecutorInjector() {
            return this.asyncExecutor;
        }

        Injector<Executor> getStateTransferExecutorInjector() {
            return this.stateTransferExecutor;
        }

        Injector<ScheduledExecutorService> getExpirationExecutorInjector() {
            return this.expirationExecutor;
        }

        Injector<ScheduledExecutorService> getReplicationQueueExecutorInjector() {
            return this.replicationQueueExecutor;
        }

        Injector<ModuleLoader> getModuleLoaderInjector() {
            return this.moduleLoader;
        }

        @Override
        public EmbeddedCacheManagerConfigurationService.TransportConfiguration getTransportConfiguration() {
            return this.transport;
        }

        @Override
        public AuthorizationConfiguration getAuthorizationConfiguration() {
            return this.authorization;
        }

        @Override
        public MBeanServer getMBeanServer() {
            return this.mbeanServer.getOptionalValue();
        }

        @Override
        public Executor getListenerExecutor() {
            return this.listenerExecutor.getOptionalValue();
        }

        @Override
        public Executor getAsyncExecutor() {
            return this.asyncExecutor.getOptionalValue();
        }
       
        @Override
        public Executor getStateTransferExecutor() {
           return this.stateTransferExecutor.getOptionalValue();
        }

        @Override
        public ScheduledExecutorService getExpirationExecutor() {
            return this.expirationExecutor.getOptionalValue();
        }

        @Override
        public ScheduledExecutorService getReplicationQueueExecutor() {
            return this.replicationQueueExecutor.getOptionalValue();
        }

        @Override
        public ModuleLoader getModuleLoader() {
            return this.moduleLoader.getValue();
        }
    }

    static class Transport implements EmbeddedCacheManagerConfigurationService.TransportConfiguration {
        private final InjectedValue<ChannelFactory> channelFactory = new InjectedValue<ChannelFactory>();
        private final InjectedValue<Executor> executor = new InjectedValue<Executor>();
        private final InjectedValue<Executor> totalOrderExecutor = new InjectedValue<Executor>();
        private final InjectedValue<Executor> remoteCommandExecutor = new InjectedValue<Executor>();

        private Long lockTimeout;
        private boolean strictPeerToPeer;

        void setLockTimeout(long lockTimeout) {
            this.lockTimeout = lockTimeout;
        }

        void setStrictPeerToPeer(boolean strictPeerToPeer) {
            this.strictPeerToPeer = strictPeerToPeer;
        }

        Injector<ChannelFactory> getChannelFactoryInjector() {
            return this.channelFactory;
        }

        Injector<Executor> getExecutorInjector() {
            return this.executor;
        }

        Injector<Executor> getTotalorderExecutorInjector() {
            return this.totalOrderExecutor;
        }

        Injector<Executor> getRemoteCommandExecutorInjector() {
          return this.remoteCommandExecutor;
       }

       @Override
        public ChannelFactory getChannelFactory() {
            return this.channelFactory.getValue();
        }

        @Override
        public Executor getExecutor() {
            return this.executor.getOptionalValue();
        }

        @Override
        public Executor getTotalOrderExecutor() {
            return this.totalOrderExecutor.getOptionalValue();
        }

        @Override
        public Executor getRemoteCommandExecutor() {
            return this.remoteCommandExecutor.getOptionalValue();
        }

       @Override
        public boolean isStrictPeerToPeer() {
            return this.strictPeerToPeer;
        }

        @Override
        public Long getLockTimeout() {
            return this.lockTimeout;
        }
    }

    static class Authorization implements EmbeddedCacheManagerConfigurationService.AuthorizationConfiguration {
        private String principalMapper;
        private Map<String, List<String>> roles = new HashMap<String, List<String>>();

        public void setPrincipalMapper(String principalMapper) {
            this.principalMapper = principalMapper;
        }

        public void setRoles(Map<String, List<String>> roles) {
            this.roles = roles;
        }

        @Override
        public String getPrincipalMapper() {
            return principalMapper;
        }

        @Override
        public Map<String, List<String>> getRoles() {
            return roles;
        }
    }
}
