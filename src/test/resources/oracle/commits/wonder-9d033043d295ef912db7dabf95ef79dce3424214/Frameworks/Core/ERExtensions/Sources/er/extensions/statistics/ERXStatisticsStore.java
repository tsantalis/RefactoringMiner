package er.extensions.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOSession;
import com.webobjects.appserver.WOStatisticsStore;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.appserver.ERXApplication;
import er.extensions.appserver.ERXSession;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXObjectStoreCoordinator;
import er.extensions.foundation.ERXProperties;
import er.extensions.statistics.store.ERXDumbStatisticsStoreListener;
import er.extensions.statistics.store.IERXStatisticsStoreListener;
import er.extensions.statistics.store.ERXEmptyRequestDescription;
import er.extensions.statistics.store.ERXNormalRequestDescription;
import er.extensions.statistics.store.IERXRequestDescription;

/**
 * Enhances the normal stats store with a bunch of useful things which get
 * displayed in the ERXStatisticsPage.
 * <ul>
 * <li>will dump warning and error messages when a request takes too long, complete with stack traces of all threads.</li>
 * <li>logs fatal messages that occurred before a request finished processing.</li>
 * <li>fixes an incompatibility with 5.4.</li>
 * </ul>
 *
 * <p>In order to turn on this functionality, you must make this call in your Application null constructor:<br/>
 * <pre>this.setStatisticsStore(new ERXStatisticsStore());</pre>
 * 
 * Then configure the behavior of this class with the three properties that determine how much it logs and when it logs.
 *
 * @property er.extensions.ERXStatisticsStore.milliSeconds.warn defaults to 2000 ms
 * @property er.extensions.ERXStatisticsStore.milliSeconds.error defaults to 10 seconds
 * @property er.extensions.ERXStatisticsStore.milliSeconds.fatal defaults to 5 minutes
 *
 * @author ak
 * @author kieran (Oct 14, 2009) - minor changes to capture thread name in middle of the request (useful for {@link ERXSession#threadName()}}
 */
public class ERXStatisticsStore extends WOStatisticsStore {

	private static final Logger log = Logger.getLogger(ERXStatisticsStore.class);

	private final StopWatchTimer _timer = new StopWatchTimer();

	private StopWatchTimer timer() {
		return _timer;
	}

    private final IERXStatisticsStoreListener listener;

    public ERXStatisticsStore() {
        listener = new ERXDumbStatisticsStoreListener();
    }

    /**
     * Create a statistics store with a custom listener. For example this listener might
     * notify an external system when a response is very slow in coming.
     * 
     * @param listener a customer listener to do something 'special' when requests are slow
     */
    public ERXStatisticsStore(IERXStatisticsStoreListener listener) {
        this.listener = listener;
    }

    /**
	 * Thread that checks each second for running requests and makes a snapshot
	 * after a certain amount of time has expired.
	 * 
	 * 
	 * @author ak
	 */
	class StopWatchTimer implements Runnable {

		long _maximumRequestErrorTime;
		long _maximumRequestWarnTime;
		long _maximumRequestFatalTime;
		long _lastLog;

		Map<Thread, Long> _requestThreads = new WeakHashMap<Thread, Long>();
		Map<Thread, Map<Thread, StackTraceElement[]>> _warnTraces = Collections.synchronizedMap(new WeakHashMap<Thread, Map<Thread, StackTraceElement[]>>());
		Map<Thread, Map<Thread, StackTraceElement[]>> _errorTraces = Collections.synchronizedMap(new WeakHashMap<Thread, Map<Thread, StackTraceElement[]>>());
		Map<Thread, Map<Thread, StackTraceElement[]>> _fatalTraces = Collections.synchronizedMap(new WeakHashMap<Thread, Map<Thread, StackTraceElement[]>>());
		Map<Thread, Map<Thread, String>> _warnTracesNames = Collections.synchronizedMap(new WeakHashMap<Thread, Map<Thread, String>>());
		Map<Thread, Map<Thread, String>> _errorTracesNames = Collections.synchronizedMap(new WeakHashMap<Thread, Map<Thread, String>>());
		Map<Thread, Map<Thread, String>> _fatalTracesNames = Collections.synchronizedMap(new WeakHashMap<Thread, Map<Thread, String>>());

		public StopWatchTimer() {
			Thread timerThread = new Thread(this);
			timerThread.setDaemon(true);
			timerThread.start();
			_maximumRequestWarnTime = ERXProperties.longForKeyWithDefault("er.extensions.ERXStatisticsStore.milliSeconds.warn", 2000L);
			_maximumRequestErrorTime = ERXProperties.longForKeyWithDefault("er.extensions.ERXStatisticsStore.milliSeconds.error", 10000L);
			_maximumRequestFatalTime = ERXProperties.longForKeyWithDefault("er.extensions.ERXStatisticsStore.milliSeconds.fatal", 5 * 60 * 1000L);
		}

		private long time() {
			synchronized (_requestThreads) {
				Long time = _requestThreads.get(Thread.currentThread());
				return time == null ? 0L : time.longValue();
			}
		}

		protected void endTimer(WOContext aContext, String aString) {
			try {
				long requestTime = 0;
				if (hasTimerStarted()) {
					requestTime = System.currentTimeMillis() - time();
				}
				
				// Don't get the traces string if we have already logged all
				// of the stacks within the last 10s. All of this logging
				// could just makes it worse for an application that is 
				// already struggling.
				String trace = " - (skipped stack traces)";
				long currentTime = System.currentTimeMillis();
				if (currentTime - _lastLog > 10000) {
					trace = stringFromTraces();
					_lastLog = currentTime;
				}
			
                IERXRequestDescription requestDescription = descriptionObjectForContext(aContext, aString);
                listener.log(requestTime, requestDescription);
				if (requestTime > _maximumRequestFatalTime) {
					log.fatal("Request did take too long : " + requestTime + "ms request was: " + requestDescription + trace);
				}
				else if (requestTime > _maximumRequestErrorTime) {
					log.error("Request did take too long : " + requestTime + "ms request was: " + requestDescription + trace);
				}
				else if (requestTime > _maximumRequestWarnTime) {
					log.warn("Request did take too long : " + requestTime + "ms request was: " + requestDescription + trace);
				}
			}
			catch (Exception ex) {
				// AK: pretty important we don't mess up here
				log.error(ex, ex);
			}
		}

		private String stringFromTraces() {
			String result;
			Thread currentThread = Thread.currentThread();
			Map<Thread, StackTraceElement[]> traces = _fatalTraces.remove(currentThread);
			Map<Thread, String> names = _fatalTracesNames.remove(currentThread);
			if (traces == null) {
				traces = _errorTraces.remove(currentThread);
				names = _errorTracesNames.remove(currentThread);
			}
			if (traces == null) {
				traces = _warnTraces.remove(currentThread);
				names = _warnTracesNames.remove(currentThread);
			}

			result = stringFromTracesAndNames(traces, names);
			
			synchronized (_requestThreads) {
				_requestThreads.remove(Thread.currentThread());
			}
			
			return result;
		}

		private String stringFromTracesAndNames(Map<Thread, StackTraceElement[]> traces, Map<Thread, String> names) {
			String trace = null;
			if (traces != null) {
				String capturedThreadName = null;
				if (names == null) {
					capturedThreadName = Thread.currentThread().getName();
				} else {
					capturedThreadName = names.get(Thread.currentThread());
				}
				
				StringBuffer sb = new StringBuffer();
				sb.append("\nRequest Thread Name: ").append(capturedThreadName).append("\n\n");
				for (Iterator iterator = traces.keySet().iterator(); iterator.hasNext();) {
					Thread t = (Thread) iterator.next();
					StackTraceElement stack[] = traces.get(t);
					String name = t.getName() != null ? t.getName() : "No name";
					String groupName = t.getThreadGroup() != null ? t.getThreadGroup().getName() : "No group";

					if (stack != null && stack.length > 2 && !name.equals("main") && !name.equals("ERXStopWatchTimer") && !groupName.equals("system")) {
						StackTraceElement func = stack[0];
						if (func != null && func.getClassName() != null && !func.getClassName().equals("java.net.PlainSocketImpl")) {
							if (names != null) {
								String customThreadName = names.get(t);
								if (customThreadName != null) {
									sb.append(customThreadName).append(":\n");
								}					
							}
							sb.append(t).append(":\n");
							for (int i = 0; i < stack.length; i++) {
								StackTraceElement stackTraceElement = stack[i];
								sb.append("\tat ").append(stackTraceElement).append("\n");
							}
						}
					}
				}
				trace = "\n" + sb.toString();
				// trace =
				// trace.replaceAll("at\\s+(com.webobjects|java|er|sun)\\..*?\\n",
				// "...\n");
				// trace = trace.replaceAll("(\t\\.\\.\\.\n+)+", "\t...\n");
			}
			else {
				trace = "";
			}
			return trace;
		}

		private boolean hasTimerStarted() {
			return time() != 0;
		}

		protected void startTimer() {
			if (!hasTimerStarted()) {
				synchronized (_requestThreads) {
					_requestThreads.put(Thread.currentThread(), Long.valueOf(System.currentTimeMillis()));
				}
			}
		}

		public String descriptionForContext(WOContext aContext, String string) {
			return descriptionObjectForContext(aContext, string).toString();
		}

        public IERXRequestDescription descriptionObjectForContext(WOContext aContext, String string) {
            if (aContext != null) {
                try {
                    WOComponent component = aContext.page();
                    String componentName = component != null ? component.name() : "NoNameComponent";
                    String additionalInfo = "(no additional Info)";
                    WORequest request = aContext.request();
                    String requestHandler = request != null ? request.requestHandlerKey() : "NoRequestHandler";
                    if (!requestHandler.equals("wo")) {
                        additionalInfo = additionalInfo + aContext.request().uri();
                    }
                    return new ERXNormalRequestDescription(componentName, requestHandler, additionalInfo);
                }
                catch (RuntimeException e) {
                    log.error("Cannot get context description since received exception " + e, e);
                }
            }
            return new ERXEmptyRequestDescription(string);
        }



		public void run() {
			Thread.currentThread().setName("ERXStopWatchTimer");
			boolean done = false;
			while (!done) {
				checkThreads();
				try {
					Thread.sleep(1000L);
				}
				catch (InterruptedException e) {
					done = true;
				}
			}
		}
		
		private void checkThreads() {
			Map<Thread, Long> requestThreads = new HashMap<Thread, Long>();
			synchronized (_requestThreads) {
	            requestThreads.putAll(_requestThreads);
			}
			if (!requestThreads.isEmpty()) {
                int deadlocksCount = 0;
				Map traces = null; 
				for (Iterator iterator = requestThreads.keySet().iterator(); iterator.hasNext();) {
					Thread thread = (Thread) iterator.next();
					Long time = requestThreads.get(thread);
					if (time != null) {
						time = System.currentTimeMillis() - time;
						if (time > _maximumRequestWarnTime/2 && _warnTraces.get(thread) == null) {
							if(traces == null) {
								traces = Thread.getAllStackTraces();
							}
							Map names = getCurrentThreadNames(traces.keySet());
							_warnTraces.put(thread, traces);
						}
						if (time > _maximumRequestErrorTime/2 && _errorTraces.get(thread) == null) {
							if(traces == null) {
								traces = Thread.getAllStackTraces();
							}
							Map names = getCurrentThreadNames(traces.keySet());
							_errorTraces.put(thread, traces);
							_errorTracesNames.put(thread, names);
						}
						if (time > _maximumRequestFatalTime && _fatalTraces.get(thread) == null) {
							if(traces == null) {
								traces = Thread.getAllStackTraces();
							}
							Map names = getCurrentThreadNames(traces.keySet());
							_fatalTraces.put(thread, traces);
							_fatalTracesNames.put(thread, names);
							String message = "Request is taking too long, possible deadlock: " + time + " ms ";
							message += stringFromTracesAndNames(traces, names);
							message += "EC info:\n" + ERXEC.outstandingLockDescription();
							message += "OSC info:\n" + ERXObjectStoreCoordinator.outstandingLockDescription();
							log.fatal(message);
                            deadlocksCount++;
						}
					}
				}
                listener.deadlock(deadlocksCount);
			}
		}

		private Map getCurrentThreadNames(Set<Thread> keySet) {
			Map names = new HashMap<Thread, String>();
			for (Thread thread : keySet) {
				names.put(thread, thread.getName());
			}
			return names;
		}

	}

	public NSDictionary statistics() {
		NSDictionary stats = super.statistics();
		if (ERXApplication.isWO54()) {
			NSMutableDictionary fixed = stats.mutableClone();
			for (Enumeration enumerator = stats.keyEnumerator(); enumerator.hasMoreElements();) {
				Object key = enumerator.nextElement();
				Object value = stats.objectForKey(key);
				fixed.setObjectForKey(fix(value), key);
			}
			stats = fixed;
		}
		return stats;
		
	}

	protected NSMutableArray sessions = new NSMutableArray<WOSession>();

	protected void _applicationCreatedSession(WOSession wosession) {
		synchronized (this) {
			sessions.addObject(wosession);
			super._applicationCreatedSession(wosession);
		}
	}

	protected void _sessionTerminating(WOSession wosession) {
		synchronized (this) {
			super._sessionTerminating(wosession);
			sessions.removeObject(wosession);
		}
	}

	public NSArray activeSession() {
		return sessions;
	}

	private void startTimer() {
		timer().startTimer();
	}

	private void endTimer(String aString) {
		timer().endTimer(null, aString);
	}

	public void applicationWillHandleComponentActionRequest() {
		startTimer();
		super.applicationWillHandleComponentActionRequest();
	}

	public void applicationDidHandleComponentActionRequestWithPageNamed(String aString) {
		endTimer(aString);
		super.applicationDidHandleComponentActionRequestWithPageNamed(aString);
	}

	public void applicationWillHandleDirectActionRequest() {
		startTimer();
		super.applicationWillHandleDirectActionRequest();
	}

	public void applicationDidHandleDirectActionRequestWithActionNamed(String aString) {
		endTimer(aString);
		super.applicationDidHandleDirectActionRequestWithActionNamed(aString);
	}

	public void applicationWillHandleWebServiceRequest() {
		startTimer();
		super.applicationWillHandleWebServiceRequest();
	}

	public void applicationDidHandleWebServiceRequestWithActionNamed(String aString) {
		endTimer(aString);
		super.applicationDidHandleWebServiceRequestWithActionNamed(aString);
	}

	private Object fix(Object value) {
		if (value instanceof ArrayList) {
			ArrayList converted = (ArrayList) value;
			return new NSArray(converted, false);
		}
		else if (value instanceof HashMap) {
			HashMap converted = (HashMap) value;
			return new NSDictionary(converted, false);
		}
		return value;
	}
	
	@Override
	public Object valueForKey(String s) {
		Object result = super.valueForKey(s);
		return fix(result);
	}
}
