package org.riderzen.flume.sink;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: guoqiang.li
 * Date: 12-9-12
 * Time: 下午3:31
 */
public class MongoSink extends AbstractSink implements Configurable {
    private static Logger logger = LoggerFactory.getLogger(MongoSink.class);

    private static DateTimeParser[] parsers = {
            DateTimeFormat.forPattern("yyyy-MM-dd").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS Z").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssz").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz").getParser(),
    };
    public static DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().append(null, parsers).toFormatter();

    public static final String[] DATE_PATTERNS = new String[]{
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss.SSS Z",
            "yyyy-MM-dd HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssZ"
    };

    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String MODEL = "model";
    public static final String DB_NAME = "db";
    public static final String COLLECTION = "collection";
    public static final String NAME_PREFIX = "MongSink_";
    public static final String BATCH_SIZE = "batch";
    public static final String AUTO_WRAP = "autoWrap";
    public static final String WRAP_FIELD = "wrapField";
    public static final String TIMESTAMP_FIELD = "timestampField";

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 27017;
    public static final String DEFAULT_DB = "events";
    public static final String DEFAULT_COLLECTION = "events";
    public static final int DEFAULT_BATCH = 100;
    private static final Boolean DEFAULT_AUTO_WRAP = false;
    public static final String DEFAULT_WRAP_FIELD = "log";
    public static final String DEFAULT_TIMESTAMP_FIELD = null;

    public static final char NAMESPACE_SEPARATOR = '.';

    private static AtomicInteger counter = new AtomicInteger();

    private Mongo mongo;
    private DB db;

    private String host;
    private int port;
    private String username;
    private String password;
    private CollectionModel model;
    private String dbName;
    private String collectionName;
    private int batchSize;
    private boolean autoWrap;
    private String wrapField;
    private String timestampField;

    @Override
    public void configure(Context context) {
        setName(NAME_PREFIX + counter.getAndIncrement());

        host = context.getString(HOST, DEFAULT_HOST);
        port = context.getInteger(PORT, DEFAULT_PORT);
        username = context.getString(USERNAME);
        password = context.getString(PASSWORD);
        model = CollectionModel.valueOf(context.getString(MODEL, CollectionModel.single.name()));
        dbName = context.getString(DB_NAME, DEFAULT_DB);
        collectionName = context.getString(COLLECTION, DEFAULT_COLLECTION);
        batchSize = context.getInteger(BATCH_SIZE, DEFAULT_BATCH);
        autoWrap = context.getBoolean(AUTO_WRAP, DEFAULT_AUTO_WRAP);
        wrapField = context.getString(WRAP_FIELD, DEFAULT_WRAP_FIELD);
        timestampField = context.getString(TIMESTAMP_FIELD, DEFAULT_TIMESTAMP_FIELD);


        logger.info("MongoSink {} context { host:{}, port:{}, username:{}, password:{}, model:{}, dbName:{}, collectionName:{}, batch: {}, autoWrap: {}, wrapField: {}, timestampField: {} }",
                new Object[]{getName(), host, port, username, password, model, dbName, collectionName, batchSize, autoWrap, wrapField, timestampField});
    }

    @Override
    public synchronized void start() {
        logger.info("Starting {}...", getName());

        try {
            mongo = new Mongo(host, port);
            db = mongo.getDB(dbName);
        } catch (UnknownHostException e) {
            logger.error("Can't connect to mongoDB", e);
        }

        super.start();
        logger.info("Started {}.", getName());
    }

    @Override
    public Status process() throws EventDeliveryException {
        logger.debug("{} start to process event", getName());

        Status status = Status.READY;
        try {
            status = parseEvents();
        } catch (Exception e) {
            logger.error("can't process events", e);
        }
        logger.debug("{} processed event", getName());
        return status;
    }

    private void saveEvents(Map<String, List<DBObject>> eventMap) {
        if (eventMap.isEmpty()) {
            logger.debug("eventMap is empty");
            return;
        }

        for (String eventCollection : eventMap.keySet()) {
            List<DBObject> docs = eventMap.get(eventCollection);
            if (logger.isDebugEnabled()) {
                logger.debug("collection: {}, length: {}", eventCollection, docs.size());
            }
            int separatorIndex = eventCollection.indexOf(NAMESPACE_SEPARATOR);
            String eventDb = eventCollection.substring(0, separatorIndex);
            String collectionName = eventCollection.substring(separatorIndex + 1);

            //Warning: please change the WriteConcern level if you need high datum consistence.
            CommandResult result = mongo.getDB(eventDb).getCollection(collectionName).insert(docs, WriteConcern.NORMAL).getLastError();
            if (result.ok()) {
                String errorMessage = result.getErrorMessage();
                if (errorMessage != null) {
                    logger.error("can't insert documents with error: {} ", errorMessage);
                    logger.error("with exception", result.getException());

                    throw new MongoException(errorMessage);
                }
            } else {
                logger.error("can't get last error");
            }
        }
    }

    private Status parseEvents() throws EventDeliveryException {
        Status status = Status.READY;
        Channel channel = getChannel();
        Transaction tx = null;
        Map<String, List<DBObject>> eventMap = new HashMap<String, List<DBObject>>();
        try {
            tx = channel.getTransaction();
            tx.begin();

            for (int i = 0; i < batchSize; i++) {
                Event event = channel.take();
                if (event == null) {
                    status = Status.BACKOFF;
                    break;
                } else {
                    processEvent(eventMap, event);
                }
            }
            if (!eventMap.isEmpty()) {
                saveEvents(eventMap);
            }

            tx.commit();
        } catch (Exception e) {
            logger.error("can't process events, drop it!", e);
            if (tx != null) {
                tx.commit();// commit to drop bad event, otherwise it will enter dead loop.
            }

            throw new EventDeliveryException(e);
        } finally {
            if (tx != null) {
                tx.close();
            }
        }
        return status;
    }

    private void processEvent(Map<String, List<DBObject>> eventMap, Event event) {
        switch (model) {
            case single:
                putSingleEvent(eventMap, event);

                break;
            case dynamic:
                putDynamicEvent(eventMap, event);

                break;
            default:
                logger.error("can't support model: {}, please check configuration.", model);
        }
    }

    private void putDynamicEvent(Map<String, List<DBObject>> eventMap, Event event) {
        String eventCollection;
        Map<String, String> headers = event.getHeaders();
        String eventDb = headers.get(DB_NAME);
        eventCollection = headers.get(COLLECTION);

        if (!StringUtils.isEmpty(eventDb)) {
            eventCollection = eventDb + NAMESPACE_SEPARATOR + eventCollection;
        } else {
            eventCollection = dbName + NAMESPACE_SEPARATOR + eventCollection;
        }

        if (!eventMap.containsKey(eventCollection)) {
            eventMap.put(eventCollection, new ArrayList<DBObject>());
        }

        List<DBObject> documents = eventMap.get(eventCollection);
        addEventToList(documents, event);
    }

    private void putSingleEvent(Map<String, List<DBObject>> eventMap, Event event) {
        String eventCollection;
        eventCollection = dbName + NAMESPACE_SEPARATOR + collectionName;
        if (!eventMap.containsKey(eventCollection)) {
            eventMap.put(eventCollection, new ArrayList<DBObject>());
        }

        List<DBObject> docs = eventMap.get(eventCollection);
        addEventToList(docs, event);
    }

    private List<DBObject> addEventToList(List<DBObject> documents, Event event) {
        if (documents == null) {
            documents = new ArrayList<DBObject>(batchSize);
        }

        DBObject eventJson;
        byte[] body = event.getBody();
        if (autoWrap) {
            eventJson = new BasicDBObject(wrapField, new String(body));
        } else {
            try {
                eventJson = (DBObject) JSON.parse(new String(body));
            } catch (Exception e) {
                logger.error("Can't parse events: " + new String(body), e);
                return documents;
            }
        }

        if (timestampField != null) {
            Date timestamp;
            if (eventJson.containsField(timestampField)) {
                try {
                    String dateText = (String) eventJson.get(timestampField);
                    timestamp = dateTimeFormatter.parseDateTime(dateText).toDate();
                    eventJson.removeField(timestampField);
                } catch (Exception e) {
                    logger.error("can't parse date ", e);

                    timestamp = new Date();
                }
            } else {
                timestamp = new Date();
            }
            eventJson.put(timestampField, timestamp);
        }

        documents.add(eventJson);

        return documents;
    }

    public static enum CollectionModel {
        dynamic, single
    }
}
