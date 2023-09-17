/*
 * Copyright (c) 2007-2015 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cascading.stats.tez;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import cascading.CascadingException;
import cascading.flow.FlowNode;
import cascading.flow.hadoop.util.HadoopUtil;
import cascading.flow.stream.annotations.StreamMode;
import cascading.management.state.ClientState;
import cascading.property.PropertyUtil;
import cascading.stats.FlowSliceStats;
import cascading.stats.hadoop.BaseHadoopNodeStats;
import cascading.stats.tez.util.TaskStatus;
import cascading.stats.tez.util.TimelineClient;
import cascading.tap.Tap;
import cascading.util.Util;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.tez.common.counters.TezCounters;
import org.apache.tez.dag.api.TezException;
import org.apache.tez.dag.api.client.DAGClient;
import org.apache.tez.dag.api.client.Progress;
import org.apache.tez.dag.api.client.StatusGetOpts;
import org.apache.tez.dag.api.client.VertexStatus;
import org.apache.tez.dag.api.oldrecords.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cascading.stats.tez.util.TezStatsUtil.STATUS_GET_COUNTERS;
import static cascading.util.Util.formatDurationFromMillis;
import static cascading.util.Util.isEmpty;

/**
 *
 */
public class TezNodeStats extends BaseHadoopNodeStats<DAGClient, TezCounters>
  {
  private static final Logger LOG = LoggerFactory.getLogger( TezNodeStats.class );

  /**
   * Sets the fetch limit from the timeline server. May be set as a System property.
   */
  public static final String TIMELINE_FETCH_LIMIT = "cascading.stats.timeline.fetch.limit";
  public static final int DEFAULT_FETCH_LIMIT = 500;

  private static int fetchLimit = -1;

  private transient String prefixID; // cached sub-string

  public enum Kind
    {
      SPLIT, PARTITIONED
    }

  private TezStepStats parentStepStats;
  private Kind kind;

  private String vertexID;
  private int totalTaskCount;
  private int succeededTaskCount;
  private int failedTaskCount;
  private int killedTaskCount;
  private int runningTaskCount;
  private boolean allTasksAreFinished;

  private static void setFetchLimit( Configuration configuration )
    {
    if( fetchLimit > -1 )
      return;

    fetchLimit = PropertyUtil.getIntProperty( HadoopUtil.createProperties( configuration ), TIMELINE_FETCH_LIMIT, DEFAULT_FETCH_LIMIT );

    if( fetchLimit < 2 )
      {
      LOG.warn( "property: {}, was set to: {}, may not be less than 2, setting to 2", TIMELINE_FETCH_LIMIT, fetchLimit );
      fetchLimit = 2;
      }
    }

  protected TezNodeStats( final TezStepStats parentStepStats, FlowNode flowNode, ClientState clientState, Configuration configuration )
    {
    super( flowNode, clientState );

    setFetchLimit( configuration );

    this.parentStepStats = parentStepStats;
    this.kind = getStreamedTaps( flowNode ).isEmpty() ? Kind.PARTITIONED : Kind.SPLIT;

    this.counterCache = new TezCounterCache<DAGClient>( this, configuration )
    {
    @Override
    protected DAGClient getJobStatusClient()
      {
      return parentStepStats.getJobStatusClient();
      }

    protected TezCounters getCounters( DAGClient dagClient ) throws IOException
      {
      VertexStatus vertexStatus = updateProgress( dagClient, STATUS_GET_COUNTERS );

      if( vertexStatus == null )
        return null;

      TezCounters vertexCounters = vertexStatus.getVertexCounters();

      if( vertexCounters == null )
        logWarn( "could not retrieve vertex counters in stats status: {}, and vertex state: {}", getStatus(), vertexStatus.getState() );

      return vertexCounters;
      }
    };
    }

  /**
   * Current rule sets do not guarantee setting Streamed annotation, but do for Accumulated
   */
  private Set<Tap> getStreamedTaps( FlowNode flowNode )
    {
    Set<Tap> taps = new HashSet<>( flowNode.getSourceTaps() );

    taps.remove( flowNode.getSourceElements( StreamMode.Accumulated ) );

    return taps;
    }

  @Override
  public String getKind()
    {
    if( kind == null )
      return null;

    return kind.name();
    }

  private String retrieveVertexID( DAGClient dagClient )
    {
    if( vertexID != null || !( dagClient instanceof TimelineClient ) )
      return vertexID;

    try
      {
      vertexID = ( (TimelineClient) dagClient ).getVertexID( getID() );
      }
    catch( IOException | CascadingException | TezException exception )
      {
      logWarn( "unable to get vertex id", exception );
      }

    return vertexID;
    }

  public int getTotalTaskCount()
    {
    return totalTaskCount;
    }

  public int getSucceededTaskCount()
    {
    return succeededTaskCount;
    }

  public int getFailedTaskCount()
    {
    return failedTaskCount;
    }

  public int getKilledTaskCount()
    {
    return killedTaskCount;
    }

  public int getRunningTaskCount()
    {
    return runningTaskCount;
    }

  @Override
  protected boolean captureChildDetailInternal()
    {
    DAGClient dagClient = parentStepStats.getJobStatusClient();

    if( dagClient == null )
      return false;

    // we cannot get task counters without the timeline server running
    if( dagClient instanceof TimelineClient )
      return withTimelineServer( (TimelineClient) dagClient );

    // these are just placeholders without counters, otherwise the order would be reversed as a failover mechanism
    return withoutTimelineServer( dagClient );
    }

  private boolean withTimelineServer( TimelineClient timelineClient )
    {
    updateProgress( (DAGClient) timelineClient, null ); // get latest task counts

    if( sliceStatsMap.size() == getTotalTaskCount() )
      return updateAllTasks( timelineClient );

    return fetchAllTasks( timelineClient );
    }

  private boolean updateAllTasks( TimelineClient timelineClient )
    {
    if( allTasksAreFinished )
      return true;

    long startTime = System.currentTimeMillis();

    int count = 0;

    for( FlowSliceStats sliceStats : sliceStatsMap.values() )
      {
      if( sliceStats.getStatus().isFinished() )
        continue;

      TaskStatus taskStatus = getTaskStatusFor( timelineClient, sliceStats.getProcessSliceID() );

      updateSliceWith( (TezSliceStats) sliceStats, taskStatus, System.currentTimeMillis() );

      count++;
      }

    if( count == 0 )
      allTasksAreFinished = true;

    logInfo( "updated {} slices in: {}", count, formatDurationFromMillis( System.currentTimeMillis() - startTime ) );

    return sliceStatsMap.size() == getTotalTaskCount();
    }

  private boolean fetchAllTasks( TimelineClient timelineClient )
    {
    long startTime = System.currentTimeMillis();
    String fromTaskId = null;
    int startSize = sliceStatsMap.size();
    int iteration = 0;
    boolean continueIterating = true;

    while( continueIterating && sliceStatsMap.size() != getTotalTaskCount() )
      {
      long lastFetch = System.currentTimeMillis();

      // we will see the same tasks twice as we paginate
      Iterator<TaskStatus> vertexChildren = getTaskStatusIterator( timelineClient, fromTaskId );

      if( vertexChildren == null )
        return false;

      int added = 0;
      int updated = 0;

      while( vertexChildren.hasNext() )
        {
        TaskStatus taskStatus = vertexChildren.next();

        fromTaskId = taskStatus.getTaskID();

        TezSliceStats sliceStats = (TezSliceStats) sliceStatsMap.get( fromTaskId );

        if( sliceStats == null )
          {
          added++;

          sliceStats = new TezSliceStats( Util.createUniqueID(), kind, this.getStatus(), fromTaskId );

          sliceStatsMap.put( sliceStats.getProcessSliceID(), sliceStats );
          }
        else
          {
          updated++;
          }

        updateSliceWith( sliceStats, taskStatus, lastFetch );
        }

      int retrieved = added + updated;

      if( added == 0 && updated == 1 ) // if paginating, will have at least retrieved 1 task
        continueIterating = false;
      else
        continueIterating = retrieved != 0;

      if( continueIterating )
        logInfo( "iteration retrieved: {}, added {}, updated {} slices in iteration: {}, fetch limit: {}", retrieved, added, updated, ++iteration, fetchLimit );
      }

    int total = sliceStatsMap.size();
    int added = total - startSize;
    int remaining = getTotalTaskCount() - total;
    String duration = formatDurationFromMillis( System.currentTimeMillis() - startTime );

    if( iteration == 0 && total == 0 )
      logInfo( "no slices stats available yet, expecting: {}", remaining );
    else
      logInfo( "added {} slices, in iterations: {}, with duration: {}, total fetched: {}, remaining: {}", added, iteration, duration, total, remaining );

    return total == getTotalTaskCount();
    }

  private void updateSliceWith( TezSliceStats sliceStats, TaskStatus taskStatus, long lastFetch )
    {
    if( taskStatus == null )
      return;

    sliceStats.setStatus( getStatusForTaskStatus( taskStatus.getStatus() ) ); // ignores nulls

    Map<String, Map<String, Long>> counters = taskStatus.getCounters();

    sliceStats.setCounters( counters ); // ignores nulls

    if( counters != null )
      sliceStats.setLastFetch( lastFetch );
    }

  private TaskStatus getTaskStatusFor( TimelineClient timelineClient, String taskID )
    {
    try
      {
      return timelineClient.getVertexChild( taskID );
      }
    catch( TezException exception )
      {
      logWarn( "unable to get slice stat from timeline server for task id: {}", taskID, exception );
      }

    return null;
    }

  private Iterator<TaskStatus> getTaskStatusIterator( TimelineClient timelineClient, String startTaskID )
    {
    try
      {
      String vertexID = retrieveVertexID( (DAGClient) timelineClient );

      if( vertexID == null )
        {
        logWarn( "unable to get slice stats from timeline server, did not retrieve valid vertex id for vertex name: {}", getID() );
        return null;
        }

      return timelineClient.getVertexChildren( vertexID, fetchLimit, startTaskID );
      }
    catch( IOException | CascadingException | TezException exception )
      {
      logWarn( "unable to get slice stats from timeline server", exception );
      }

    return null;
    }

  private boolean withoutTimelineServer( DAGClient dagClient )
    {
    VertexStatus vertexStatus = updateProgress( dagClient, STATUS_GET_COUNTERS );

    if( vertexStatus == null )
      return false;

    int total = sliceStatsMap.size();

    if( total == 0 ) // yet to be initialized
      logWarn( "'" + YarnConfiguration.TIMELINE_SERVICE_ENABLED + "' is disabled, task level counters cannot be retrieved" );

    for( int i = total; i < totalTaskCount; i++ )
      {
      TezSliceStats sliceStats = new TezSliceStats( Util.createUniqueID(), kind, this.getStatus(), null );

      // we don't have the taskId, so we are using the id as the key
      sliceStatsMap.put( sliceStats.getID(), sliceStats );
      }

    // a placeholder to simulate actual slice stats for now
    Iterator<FlowSliceStats> iterator = sliceStatsMap.values().iterator();

    for( int i = 0; i < runningTaskCount && iterator.hasNext(); i++ )
      ( (TezSliceStats) iterator.next() ).setStatus( Status.RUNNING );

    for( int i = 0; i < succeededTaskCount && iterator.hasNext(); i++ )
      ( (TezSliceStats) iterator.next() ).setStatus( Status.SUCCESSFUL );

    for( int i = 0; i < failedTaskCount && iterator.hasNext(); i++ )
      ( (TezSliceStats) iterator.next() ).setStatus( Status.FAILED );

    for( int i = 0; i < killedTaskCount && iterator.hasNext(); i++ )
      ( (TezSliceStats) iterator.next() ).setStatus( Status.STOPPED );

    List<String> diagnostics = vertexStatus.getDiagnostics();

    for( String diagnostic : diagnostics )
      logInfo( "vertex diagnostics: {}", diagnostic );

    return true;
    }

  private Status getStatusForTaskStatus( @Nullable String status )
    {
    if( isEmpty( status ) )
      return null;

    TaskState state = TaskState.valueOf( status );

    switch( state )
      {
      case NEW:
        return Status.PENDING;
      case SCHEDULED:
        return Status.SUBMITTED;
      case RUNNING:
        return Status.RUNNING;
      case SUCCEEDED:
        return Status.SUCCESSFUL;
      case FAILED:
        return Status.FAILED;
      case KILLED:
        return Status.STOPPED;
      }

    return null;
    }

  private VertexStatus updateProgress( DAGClient dagClient, Set<StatusGetOpts> statusGetOpts )
    {
    VertexStatus vertexStatus = null;

    try
      {
      vertexStatus = dagClient.getVertexStatus( getID(), statusGetOpts );
      }
    catch( IOException | TezException exception )
      {
      logWarn( "unable to get vertex status for: {}", getID(), exception );
      }

    if( vertexStatus == null )
      return null;

    Progress progress = vertexStatus.getProgress();

    totalTaskCount = progress.getTotalTaskCount();
    runningTaskCount = progress.getRunningTaskCount();
    succeededTaskCount = progress.getSucceededTaskCount();
    failedTaskCount = progress.getFailedTaskCount();
    killedTaskCount = progress.getKilledTaskCount();

    return vertexStatus;
    }

  protected void logInfo( String message, Object... arguments )
    {
    getProcessLogger().logInfo( getPrefix() + message, arguments );
    }

  protected void logDebug( String message, Object... arguments )
    {
    getProcessLogger().logDebug( getPrefix() + message, arguments );
    }

  protected void logWarn( String message, Object... arguments )
    {
    getProcessLogger().logWarn( getPrefix() + message, arguments );
    }

  private String getPrefix()
    {
    if( prefixID == null )
      prefixID = "[" + getID().substring( 0, 5 ) + "] ";

    return prefixID;
    }
  }
