/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.php
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.addthis.hydra.job.web.resources;

import javax.annotation.Nonnull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.addthis.codec.jackson.Jackson;
import com.addthis.codec.json.CodecJSON;
import com.addthis.hydra.job.HostFailWorker;
import com.addthis.hydra.job.auth.PermissionsManager;
import com.addthis.hydra.job.spawn.HealthCheckResult;
import com.addthis.hydra.job.spawn.Spawn;
import com.addthis.hydra.job.spawn.balancer.SpawnBalancer;
import com.addthis.hydra.job.spawn.balancer.SpawnBalancerConfig;
import com.addthis.hydra.job.spawn.SystemManager;
import com.addthis.hydra.job.store.DataStoreUtil.DataStoreType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/system")
public class SystemResource {

    private static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    @Nonnull private final SystemManager systemManager;
    @Nonnull private final SpawnBalancer spawnBalancer;
    @Nonnull private final HostFailWorker hostFailWorker;
    @Nonnull private final PermissionsManager permissionsManager;


    public SystemResource(Spawn spawn) {
        this.systemManager = spawn.getSystemManager();
        this.spawnBalancer = spawn.getSpawnBalancer();
        this.hostFailWorker = spawn.getHostFailWorker();
        this.permissionsManager = spawn.getPermissionsManager();
    }

    @GET
    @Path("/quiesce")
    @Produces(MediaType.APPLICATION_JSON)
    public Response quiesceCluster(@QueryParam("quiesce") String quiesce,
                                   @QueryParam("user") String user,
                                   @QueryParam("token") String token,
                                   @QueryParam("sudo") String sudo) {
        try {
            if (permissionsManager.adminAction(user, token, sudo)) {
                boolean quiesced = systemManager.quiesceCluster(quiesce.equals("1"), user);
                String json = Jackson.defaultMapper().createObjectNode()
                        .put("quiesced", (quiesced ? "1" : "0")).toString();
                return Response.ok(json).build();
            } else {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/balance.params.get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBalanceParams() {
        try {
            return Response.ok(CodecJSON.encodeString(spawnBalancer.getConfig())).build();
        } catch (Exception e) {
            return Response.serverError().entity("Error getting balance parameters: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/balance.params.set")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setBalanceParams(@QueryParam("params") String params) {
        try {
            SpawnBalancerConfig config = CodecJSON.decodeString(SpawnBalancerConfig.class, params);
            spawnBalancer.setConfig(config);
            spawnBalancer.saveConfigToDataStore();
            return Response.ok().build();
        } catch (Exception e) {
            String err = "Failed to set SpawnBalanceConfig: " + e.getMessage();
            log.warn(err, e);
            return Response.serverError().entity(err).build();
        }
    }

    @GET
    @Path("/hostfailworker.obeyTaskLimit.set")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setObeyTaskLimit(@QueryParam("obey") boolean obey) {
        hostFailWorker.setObeyTaskSlots(obey);
        return Response.ok().build();
    }

    @GET
    @Path("/git.properties")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGitProperties() {
        try {
            return Response.ok(CodecJSON.encodeString(systemManager.getGitProperties())).build();
        } catch (Exception e) {
            String err = "Error loading git properties: " + e.getMessage();
            log.warn(err, e);
            return Response.serverError().entity(err).build();
        }
    }

    @GET
    @Path("/datastore.cutover")
    @Produces(MediaType.TEXT_PLAIN)
    public Response datastoreCutover(
            @QueryParam("src") String src,
            @QueryParam("tar") String tar,
            @QueryParam("checkAll") int checkAll) {

        try {
            DataStoreType sourceType = DataStoreType.valueOf(src);
            DataStoreType targetType = DataStoreType.valueOf(tar);
            boolean checkAllWrites = (checkAll == 1);
            systemManager.cutoverDataStore(sourceType, targetType, checkAllWrites);
            return Response.ok("Cut over successfully.").build();
        } catch (IllegalStateException e) {
            return Response.serverError().entity("Spawn must be quiesced to cut over stored data.").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            String err = "Error cutting over data store: " + e.getMessage();
            log.error(err, e);
            return Response.serverError().entity(err).build();
        }
    }

    /**
     * Performs system health check.
     *  
     * @param retries   The max number of retries if validation fails on the first run. This 
     *                  reduces the likelihood of false alarms from certain checks. Default is 3.
     * @param details   If {@code true}, the response will contain details of the health check
     *                  result; otherwise the response is either "true" (pass) or "false" (failure).
     *                  Default is {@code false}.
     */
    @GET
    @Path("/healthcheck")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck(
            @QueryParam("retries") @DefaultValue("3") int retries,
            @QueryParam("details") @DefaultValue("false") boolean details) {
        try {
            HealthCheckResult result = systemManager.healthCheck(retries);
            if (details) {
                return Response.ok(CodecJSON.encodeString(result)).build();
            } else {
                return Response.ok(String.valueOf(result.isEverythingOK())).build();
            }
            
        } catch (Exception e) {
            String err = "Error running health check: " + e.getMessage();
            log.error(err, e);
            return Response.serverError().entity(err).build();
        }
    }

}
