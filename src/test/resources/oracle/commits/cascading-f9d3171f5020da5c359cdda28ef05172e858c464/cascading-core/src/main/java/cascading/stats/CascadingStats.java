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

package cascading.stats;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cascading.flow.Flow;
import cascading.management.state.ClientState;
import cascading.util.ProcessLogger;

/**
 * Class CascadingStats is the base class for all Cascading statistics gathering. It also reports the status of
 * core elements that have state.
 * <p/>
 * There are eight states the stats object reports; PENDING, SKIPPED, STARTED, SUBMITTED, RUNNING, SUCCESSFUL, STOPPED, and FAILED.
 * <ul>
 * <li>{@code pending} - when the Flow or Cascade has yet to start.</li>
 * <li>{@code skipped} - when the Flow was skipped by the parent Cascade.</li>
 * <li>{@code started} - when {@link cascading.flow.Flow#start()} was called.</li>
 * <li>{@code submitted} - when the Step was submitted to the underlying platform for work.</li>
 * <li>{@code running} - when the Flow or Cascade is executing a workload.</li>
 * <li>{@code stopped} - when the user calls {@link cascading.flow.Flow#stop()} on the Flow or Cascade.</li>
 * <li>{@code failed} - when the Flow or Cascade threw an error and failed to finish the workload.</li>
 * <li>{@code successful} - when the Flow or Cascade naturally completed its workload without failure.</li>
 * </ul>
 * <p/>
 * CascadingStats also reports four unique timestamps.
 * <ul>
 * <li>{@code startTime} - when the {@code start()} method was called.</li>
 * <li>{@code submitTime} - when the unit of work was actually submitted for execution. Not supported by all sub-classes.</li>
 * <li>{@code runTime} - when the unit of work actually began to execute work. This value may be affected by any "polling interval" in place.</li>
 * <li>{@code finishedTime} - when all work has completed successfully, failed, or stopped.</li>
 * </ul>
 * <p/>
 * A unit of work is considered {@code finished} when the Flow or Cascade is no longer processing a workload and {@code successful},
 * {@code skipped}, {@code failed}, or {@code stopped} is true.
 * <p/>
 * It is important to note all the timestamps are client side observations. Not values reported by the underlying
 * platform. That said, the transitions are seen by polling the client interface to the underlying platform so are
 * effected by the {@link cascading.flow.FlowProps#getJobPollingInterval()} value.
 *
 * @see CascadeStats
 * @see FlowStats
 * @see FlowStepStats
 */
public abstract class CascadingStats<Child> implements ProvidesCounters, Serializable
  {
  public static final String STATS_STORE_INTERVAL = "cascading.stats.store.interval";

  /**
   * Method setStatsStoreInterval sets the interval time between store operations against the underlying
   * document storage services. This affects the rate at which metrics and status information is updated.
   *
   * @param properties of type Properties
   * @param intervalMs milliseconds between storage calls
   */
  public static void setStatsStoreInterval( Map<Object, Object> properties, long intervalMs )
    {
    if( intervalMs <= 0 )
      throw new IllegalArgumentException( "interval must be greater than zero, got: " + intervalMs );

    properties.put( STATS_STORE_INTERVAL, Long.toString( intervalMs ) );
    }

  public enum Type
    {
      CASCADE, FLOW, STEP, NODE, SLICE, ATTEMPT;

    public boolean isChild( Type type )
      {
      return ordinal() < type.ordinal();
      }
    }

  public enum Status
    {
      PENDING( false ), SKIPPED( true ), STARTED( false ), SUBMITTED( false ), RUNNING( false ), SUCCESSFUL( true ), STOPPED( true ), FAILED( true );

    boolean isFinished = false; // is this a completed state

    Status( boolean isFinished )
      {
      this.isFinished = isFinished;
      }

    public boolean isFinished()
      {
      return isFinished;
      }
    }

  private transient String prefixID; // cached sub-string

  /** Field name */
  final String name;
  protected final ClientState clientState;

  /** Field status */
  Status status = Status.PENDING;

  Set<StatsListener> listeners;

  /** Field pendingTime */
  long pendingTime;
  /** Field startTime */
  long startTime;
  /** Field submitTime */
  long submitTime;
  /** Field runTime */
  long runTime;
  /** Field finishedTime */
  long finishedTime;
  /** Field throwable */
  Throwable throwable;

  protected CascadingStats( String name, ClientState clientState )
    {
    this.name = name;
    this.clientState = clientState;
    }

  /** Method prepare initializes this instance. */
  public void prepare()
    {
    clientState.startService();
    }

  /** Method cleanup destroys any resources allocated by this instance. */
  public void cleanup()
    {
    clientState.stopService();
    }

  /**
   * Method getID returns the ID of this CascadingStats object.
   *
   * @return the ID (type Object) of this CascadingStats object.
   */
  public abstract String getID();

  /**
   * Method getName returns the name of this CascadingStats object.
   *
   * @return the name (type String) of this CascadingStats object.
   */
  public String getName()
    {
    return name;
    }

  public abstract Type getType();

  /**
   * Method getThrowable returns the throwable of this CascadingStats object.
   *
   * @return the throwable (type Throwable) of this CascadingStats object.
   */
  public Throwable getThrowable()
    {
    return throwable;
    }

  /**
   * Method isPending returns true if no work has been submitted.
   *
   * @return the pending (type boolean) of this CascadingStats object.
   */
  public boolean isPending()
    {
    return status == Status.PENDING;
    }

  /**
   * Method isSkipped returns true when the works was skipped.
   * <p/>
   * Flows are skipped if the appropriate {@link cascading.flow.FlowSkipStrategy#skipFlow(Flow)}
   * returns {@code true};
   *
   * @return the skipped (type boolean) of this CascadingStats object.
   */
  public boolean isSkipped()
    {
    return status == Status.SKIPPED;
    }

  /**
   * Method isStarted returns true when work has started.
   *
   * @return the started (type boolean) of this CascadingStats object.
   */
  public boolean isStarted()
    {
    return status == Status.STARTED;
    }

  /**
   * Method isSubmitted returns true if no work has started.
   *
   * @return the submitted (type boolean) of this CascadingStats object.
   */
  public boolean isSubmitted()
    {
    return status == Status.SUBMITTED;
    }

  /**
   * Method isRunning returns true when work has begun.
   *
   * @return the running (type boolean) of this CascadingStats object.
   */
  public boolean isRunning()
    {
    return status == Status.RUNNING;
    }

  /**
   * Method isEngaged returns true when there is work being executed, if
   * {@link #isStarted()}, {@link #isSubmitted()}, or {@link #isRunning()} returns true;
   *
   * @return the engaged (type boolean) of this CascadingStats object.
   */
  public boolean isEngaged()
    {
    return isStarted() || isSubmitted() || isRunning();
    }

  /**
   * Method isSuccessful returns true when work has completed successfully.
   *
   * @return the completed (type boolean) of this CascadingStats object.
   */
  public boolean isSuccessful()
    {
    return status == Status.SUCCESSFUL;
    }

  /**
   * Method isFailed returns true when the work ended with an error.
   *
   * @return the failed (type boolean) of this CascadingStats object.
   */
  public boolean isFailed()
    {
    return status == Status.FAILED;
    }

  /**
   * Method isStopped returns true when the user stopped the work.
   *
   * @return the stopped (type boolean) of this CascadingStats object.
   */
  public boolean isStopped()
    {
    return status == Status.STOPPED;
    }

  /**
   * Method isFinished returns true if the current status shows no work currently being executed,
   * if {@link #isSkipped()}, {@link #isSuccessful()}, {@link #isFailed()}, or {@link #isStopped()} returns true.
   *
   * @return the finished (type boolean) of this CascadingStats object.
   */
  public boolean isFinished()
    {
    return status == Status.SUCCESSFUL || status == Status.FAILED || status == Status.STOPPED || status == Status.SKIPPED;
    }

  /**
   * Method getStatus returns the {@link Status} of this CascadingStats object.
   *
   * @return the status (type Status) of this CascadingStats object.
   */
  public Status getStatus()
    {
    return status;
    }

  /** Method recordStats forces recording of current status information. */
  public void recordStats()
    {
    clientState.recordStats( this );
    }

  public abstract void recordInfo();

  /** Method markPending sets the status to {@link Status#PENDING}. */
  public synchronized void markPending()
    {
    markPendingTime();

    fireListeners( null, Status.PENDING );

    recordStats();
    recordInfo();
    }

  protected void markPendingTime()
    {
    if( pendingTime == 0 )
      pendingTime = System.currentTimeMillis();
    }

  /**
   * Method markStartedThenRunning consecutively marks the status as {@link Status#STARTED} then {@link Status#RUNNING}
   * and forces the start and running time to be equals.
   */
  public synchronized void markStartedThenRunning()
    {
    if( status != Status.PENDING )
      throw new IllegalStateException( "may not mark as " + Status.STARTED + ", is already " + status );

    markStartToRunTime();
    markStarted();
    markRunning();
    }

  protected void markStartToRunTime()
    {
    startTime = submitTime = runTime = System.currentTimeMillis();
    }

  /** Method markStarted sets the status to {@link Status#STARTED}. */
  public synchronized void markStarted()
    {
    if( status != Status.PENDING )
      throw new IllegalStateException( "may not mark as " + Status.STARTED + ", is already " + status );

    Status priorStatus = status;
    status = Status.STARTED;
    markStartTime();

    fireListeners( priorStatus, status );

    clientState.start( startTime );
    clientState.setStatus( status, startTime );
    recordStats();
    }

  protected void markStartTime()
    {
    if( startTime == 0 )
      startTime = System.currentTimeMillis();
    }

  /** Method markSubmitted sets the status to {@link Status#SUBMITTED}. */
  public synchronized void markSubmitted()
    {
    if( status == Status.SUBMITTED )
      return;

    if( status != Status.STARTED )
      throw new IllegalStateException( "may not mark as " + Status.SUBMITTED + ", is already " + status );

    Status priorStatus = status;
    status = Status.SUBMITTED;
    markSubmitTime();

    fireListeners( priorStatus, status );

    clientState.submit( submitTime );
    clientState.setStatus( status, submitTime );
    recordStats();
    recordInfo();
    }

  protected void markSubmitTime()
    {
    if( submitTime == 0 )
      submitTime = System.currentTimeMillis();
    }

  /** Method markRunning sets the status to {@link Status#RUNNING}. */
  public synchronized void markRunning()
    {
    if( status == Status.RUNNING )
      return;

    if( status != Status.STARTED && status != Status.SUBMITTED )
      throw new IllegalStateException( "may not mark as " + Status.RUNNING + ", is already " + status );

    Status priorStatus = status;
    status = Status.RUNNING;
    markRunTime();

    fireListeners( priorStatus, status );

    clientState.run( runTime );
    clientState.setStatus( status, runTime );
    recordStats();
    }

  protected void markRunTime()
    {
    if( runTime == 0 )
      runTime = System.currentTimeMillis();
    }

  /** Method markSuccessful sets the status to {@link Status#SUCCESSFUL}. */
  public synchronized void markSuccessful()
    {
    if( status != Status.RUNNING && status != Status.SUBMITTED )
      throw new IllegalStateException( "may not mark as " + Status.SUCCESSFUL + ", is already " + status );

    Status priorStatus = status;
    status = Status.SUCCESSFUL;
    markFinishedTime();

    fireListeners( priorStatus, status );

    clientState.setStatus( status, finishedTime );
    clientState.stop( finishedTime );
    recordStats();
    recordInfo();
    }

  private void markFinishedTime()
    {
    finishedTime = System.currentTimeMillis();
    }

  /**
   * Method markFailed sets the status to {@link Status#FAILED}.
   *
   * @param throwable of type Throwable
   */
  public synchronized void markFailed( Throwable throwable )
    {
    if( status != Status.STARTED && status != Status.RUNNING && status != Status.SUBMITTED )
      throw new IllegalStateException( "may not mark as " + Status.FAILED + ", is already " + status );

    Status priorStatus = status;
    status = Status.FAILED;
    markFinishedTime();
    this.throwable = throwable;

    fireListeners( priorStatus, status );

    clientState.setStatus( status, finishedTime );
    clientState.stop( finishedTime );
    recordStats();
    recordInfo();
    }

  /** Method markStopped sets the status to {@link Status#STOPPED}. */
  public synchronized void markStopped()
    {
    if( status != Status.PENDING && status != Status.STARTED && status != Status.SUBMITTED && status != Status.RUNNING )
      throw new IllegalStateException( "may not mark as " + Status.STOPPED + ", is already " + status );

    Status priorStatus = status;
    status = Status.STOPPED;
    markFinishedTime();

    fireListeners( priorStatus, status );

    clientState.setStatus( status, finishedTime );
    recordStats();
    recordInfo();
    clientState.stop( finishedTime );
    }

  /** Method markSkipped sets the status to {@link Status#SKIPPED}. */
  public synchronized void markSkipped()
    {
    if( status != Status.PENDING )
      throw new IllegalStateException( "may not mark as " + Status.SKIPPED + ", is already " + status );

    Status priorStatus = status;
    status = Status.SKIPPED;

    fireListeners( priorStatus, status );

    clientState.setStatus( status, System.currentTimeMillis() );
    recordStats();
    }

  /**
   * Method getPendingTime returns the pendingTime of this CascadingStats object.
   *
   * @return the pendingTime (type long) of this CascadingStats object.
   */
  public long getPendingTime()
    {
    return pendingTime;
    }

  /**
   * Method getStartTime returns the startTime of this CascadingStats object.
   *
   * @return the startTime (type long) of this CascadingStats object.
   */
  public long getStartTime()
    {
    return startTime;
    }

  /**
   * Method getSubmitTime returns the submitTime of this CascadingStats object.
   *
   * @return the submitTime (type long) of this CascadingStats object.
   */
  public long getSubmitTime()
    {
    return submitTime;
    }

  /**
   * Method getRunTime returns the runTime of this CascadingStats object.
   *
   * @return the runTime (type long) of this CascadingStats object.
   */
  public long getRunTime()
    {
    return runTime;
    }

  /**
   * Method getFinishedTime returns the finishedTime of this CascadingStats object.
   *
   * @return the finishedTime (type long) of this CascadingStats object.
   */
  public long getFinishedTime()
    {
    return finishedTime;
    }

  /**
   * Method getDuration returns the duration the work executed before being finished.
   * <p/>
   * This method will return zero until the work is finished. See {@link #getCurrentDuration()}
   * if you wish to poll for the current duration value.
   * <p/>
   * Duration is calculated as {@code finishedTime - startTime}.
   *
   * @return the duration (type long) of this CascadingStats object.
   */
  public long getDuration()
    {
    if( finishedTime != 0 )
      return finishedTime - startTime;
    else
      return 0;
    }

  /**
   * Method getCurrentDuration returns the current duration of the current work whether or not
   * the work is finished. When finished, the return value will be the same as {@link #getDuration()}.
   * <p/>
   * Duration is calculated as {@code finishedTime - startTime}.
   *
   * @return the currentDuration (type long) of this CascadingStats object.
   */
  public long getCurrentDuration()
    {
    if( finishedTime != 0 )
      return finishedTime - startTime;
    else
      return System.currentTimeMillis() - startTime;
    }

  @Override
  public Collection<String> getCountersFor( Class<? extends Enum> group )
    {
    return getCountersFor( group.getName() );
    }

  /**
   * Method getCounterGroupsMatching returns all the available counter group names that match
   * the given regular expression.
   *
   * @param regex of type String
   * @return Collection<String>
   */
  public abstract Collection<String> getCounterGroupsMatching( String regex );

  /**
   * Method captureDetail will recursively capture details about nested systems. Use this method to persist
   * statistics about a given Cascade, Flow, FlowStep, or FlowNode.
   * <p/>
   * Each CascadingStats object must be individually inspected for any system specific details.
   * <p/>
   * Each call to this method will refresh the internal cache unless the current Stats object is marked finished. One
   * additional refresh will happen after this instance is marked finished.
   */
  public void captureDetail()
    {
    captureDetail( Type.ATTEMPT );
    }

  public abstract void captureDetail( Type depth );

  /**
   * Method getChildren returns any relevant child statistics instances. They may not be of type CascadingStats, but
   * instead platform specific.
   *
   * @return a Collection of child statistics
   */
  public abstract Collection<Child> getChildren();

  /**
   * Method getChildWith returns a child stats instance with the given ID value.
   *
   * @param id the id of a child instance
   * @return the child stats instance or null if not found
   */
  public abstract Child getChildWith( String id );

  public synchronized void addListener( StatsListener statsListener )
    {
    if( listeners == null )
      listeners = new LinkedHashSet<>();

    listeners.add( statsListener );
    }

  public synchronized boolean removeListener( StatsListener statsListener )
    {
    return listeners != null && listeners.remove( statsListener );
    }

  protected synchronized void fireListeners( CascadingStats.Status fromStatus, CascadingStats.Status toStatus )
    {
    if( listeners == null )
      return;

    for( StatsListener listener : listeners )
      {
      try
        {
        listener.notify( this, fromStatus, toStatus );
        }
      catch( Throwable throwable )
        {
        logWarn( "error during listener notification, continuing with remaining listener notification", throwable );
        }
      }
    }

  protected abstract ProcessLogger getProcessLogger();

  protected String getStatsString()
    {
    String string = "status=" + status + ", startTime=" + startTime;

    if( finishedTime != 0 )
      string += ", duration=" + ( finishedTime - startTime );

    return string;
    }

  @Override
  public String toString()
    {
    return "Cascading{" + getStatsString() + '}';
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

  protected void logError( String message, Object... arguments )
    {
    getProcessLogger().logError( getPrefix() + message, arguments );
    }

  protected void logError( String message, Throwable throwable )
    {
    getProcessLogger().logError( getPrefix() + message, throwable );
    }

  private String getPrefix()
    {
    if( prefixID == null )
      prefixID = getType().name().toLowerCase() + "[" + getID().substring( 0, 5 ) + "] ";

    return prefixID;
    }
  }
