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

package cascading.stats.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cascading.flow.FlowNode;
import cascading.management.state.ClientState;
import cascading.stats.FlowNodeStats;
import cascading.stats.FlowSliceStats;
import cascading.util.Util;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TaskCompletionEvent;
import org.apache.hadoop.mapred.TaskID;
import org.apache.hadoop.mapred.TaskReport;

/**
 *
 */
public class HadoopNodeStats extends BaseHadoopNodeStats<FlowNodeStats, Map<String, Map<String, Long>>>
  {
  private Map<TaskID, String> sliceIDCache = new HashMap<TaskID, String>( 4999 ); // caching for ids

  private HadoopStepStats parentStepStats;
  private HadoopSliceStats.Kind kind;

  /**
   * Constructor CascadingStats creates a new CascadingStats instance.
   *
   * @param parentStepStats
   * @param configuration
   * @param kind
   * @param flowNode
   * @param clientState
   */
  protected HadoopNodeStats( final HadoopStepStats parentStepStats, Configuration configuration, HadoopSliceStats.Kind kind, FlowNode flowNode, ClientState clientState )
    {
    super( flowNode, clientState );
    this.parentStepStats = parentStepStats;
    this.kind = kind;

    this.counterCache = new HadoopNodeCounterCache( this, configuration );
    }

  @Override
  public String getKind()
    {
    if( kind == null )
      return null;

    return kind.name();
    }

  private Status getParentStatus()
    {
    return parentStepStats.getStatus();
    }

  @Override
  protected boolean captureChildDetailInternal()
    {
    JobClient jobClient = parentStepStats.getJobClient();
    RunningJob runningJob = parentStepStats.getJobStatusClient();

    if( jobClient == null || runningJob == null )
      return false;

    try
      {
      TaskReport[] taskReports; // todo: use Job task reports

      if( kind == HadoopSliceStats.Kind.MAPPER )
        taskReports = jobClient.getMapTaskReports( runningJob.getID() );
      else
        taskReports = jobClient.getReduceTaskReports( runningJob.getID() );

      addTaskStats( taskReports, false );

      return true;
      }
    catch( IOException exception )
      {
      getProcessLogger().logWarn( "unable to retrieve slice stats via task reports", exception );
      }

    return false;
    }

  protected void addTaskStats( TaskReport[] taskReports, boolean skipLast )
    {
    long lastFetch = System.currentTimeMillis();

    synchronized( sliceStatsMap )
      {
      for( int i = 0; i < taskReports.length - ( skipLast ? 1 : 0 ); i++ )
        {
        TaskReport taskReport = taskReports[ i ];

        if( taskReport == null )
          {
          getProcessLogger().logWarn( "found empty task report" );
          continue;
          }

        String id = getSliceIDFor( taskReport.getTaskID() );
        sliceStatsMap.put( id, new HadoopSliceStats( id, getParentStatus(), kind, taskReport, lastFetch ) );
        }
      }
    }

  protected void addAttempt( TaskCompletionEvent event )
    {
    // the event could be a housekeeping task, which we are not tracking
    String sliceID = sliceIDCache.get( event.getTaskAttemptId().getTaskID() );

    if( sliceID == null )
      return;

    FlowSliceStats stats;

    synchronized( sliceStatsMap )
      {
      stats = sliceStatsMap.get( sliceID );
      }

    if( stats == null )
      return;

    ( (HadoopSliceStats) stats ).addAttempt( event );
    }

  private String getSliceIDFor( TaskID taskID )
    {
    // using taskID instance as #toString is quite painful
    String id = sliceIDCache.get( taskID );

    if( id == null )
      {
      id = Util.createUniqueID();
      sliceIDCache.put( taskID, id );
      }

    return id;
    }
  }
