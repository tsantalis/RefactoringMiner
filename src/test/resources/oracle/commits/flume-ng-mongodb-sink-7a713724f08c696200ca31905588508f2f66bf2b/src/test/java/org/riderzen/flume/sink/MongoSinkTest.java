package org.riderzen.flume.sink;

import com.mongodb.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.flume.*;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.conf.Configurables;
import org.apache.flume.event.EventBuilder;
import org.json.simple.JSONObject;
import org.testng.annotations.*;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;

import static org.testng.Assert.*;

/**
 * User: guoqiang.li
 * Date: 12-9-12
 * Time: 下午3:31
 */
@Test(singleThreaded = true, threadPoolSize = 1)
public class MongoSinkTest {
    private static Mongo mongo;
    public static final String DBNAME = "myDb";

    private static Context ctx = new Context();
    private static Channel channel;


    @BeforeMethod(groups = {"dev"})
    public static void setup() throws UnknownHostException {
        mongo = new Mongo("localhost", 27017);

        Map<String, String> ctxMap = new HashMap<String, String>();
        ctxMap.put(MongoSink.HOST, "localhost");
        ctxMap.put(MongoSink.PORT, "27017");
        ctxMap.put(MongoSink.DB_NAME, "test_events");
        ctxMap.put(MongoSink.COLLECTION, "test_log");
        ctxMap.put(MongoSink.BATCH_SIZE, "100");

        ctx.putAll(ctxMap);

        Context channelCtx = new Context();
        channelCtx.put("capacity", "1000000");
        channelCtx.put("transactionCapacity", "1000000");
        channel = new MemoryChannel();
        Configurables.configure(channel, channelCtx);
    }

    @AfterMethod(groups = {"dev"})
    public static void tearDown() {
        mongo.dropDatabase(DBNAME);
        mongo.dropDatabase("test_events");
        mongo.dropDatabase("dynamic_db");
        mongo.close();
    }

    @Test(groups = "dev", invocationCount = 1)
    public void sinkDynamicTest() throws EventDeliveryException, InterruptedException {
        ctx.put(MongoSink.MODEL, MongoSink.CollectionModel.dynamic.name());
        MongoSink sink = new MongoSink();
        Configurables.configure(sink, ctx);

        sink.setChannel(channel);
        sink.start();

        JSONObject msg = new JSONObject();
        msg.put("age", 11);
        msg.put("birthday", new Date().getTime());

        Transaction tx;

        for (int i = 0; i < 100; i++) {
            tx = channel.getTransaction();
            tx.begin();
            msg.put("name", "test" + i);
            JSONObject header = new JSONObject();
            header.put(MongoSink.COLLECTION, "my_events");

            Event e = EventBuilder.withBody(msg.toJSONString().getBytes(), header);
            channel.put(e);
            tx.commit();
            tx.close();
        }
        sink.process();
        sink.stop();

        for (int i = 0; i < 10; i++) {
            msg.put("name", "test" + i);

            System.out.println("i = " + i);

            DB db = mongo.getDB("test_events");
            DBCollection collection = db.getCollection("my_events");
            DBCursor cursor = collection.find(new BasicDBObject(msg));
            assertTrue(cursor.hasNext());
            DBObject dbObject = cursor.next();
            assertNotNull(dbObject);
            assertEquals(dbObject.get("name"), msg.get("name"));
            assertEquals(dbObject.get("age"), msg.get("age"));
            assertEquals(dbObject.get("birthday"), msg.get("birthday"));
        }
    }

    @Test(groups = "dev")
    public void sinkDynamicTest2() throws EventDeliveryException, InterruptedException {
        ctx.put(MongoSink.MODEL, MongoSink.CollectionModel.dynamic.name());
        MongoSink sink = new MongoSink();
        Configurables.configure(sink, ctx);

        sink.setChannel(channel);
        sink.start();

        JSONObject msg = new JSONObject();
        msg.put("age", 11);
        msg.put("birthday", new Date().getTime());

        Transaction tx;

        for (int i = 0; i < 100; i++) {
            tx = channel.getTransaction();
            tx.begin();
            msg.put("name", "test" + i);
            JSONObject header = new JSONObject();
            header.put(MongoSink.COLLECTION, "my_events" + i % 10);

            Event e = EventBuilder.withBody(msg.toJSONString().getBytes(), header);
            channel.put(e);
            tx.commit();
            tx.close();
        }
        sink.process();
        sink.stop();

        for (int i = 0; i < 10; i++) {
            msg.put("name", "test" + i);

            System.out.println("i = " + i);

            DB db = mongo.getDB("test_events");
            DBCollection collection = db.getCollection("my_events" + i % 10);
            DBCursor cursor = collection.find(new BasicDBObject(msg));
            assertTrue(cursor.hasNext());
            DBObject dbObject = cursor.next();
            assertNotNull(dbObject);
            assertEquals(dbObject.get("name"), msg.get("name"));
            assertEquals(dbObject.get("age"), msg.get("age"));
            assertEquals(dbObject.get("birthday"), msg.get("birthday"));
        }
    }

    @Test(groups = "dev")
    public void sinkSingleModelTest() throws EventDeliveryException {
        ctx.put(MongoSink.MODEL, MongoSink.CollectionModel.single.name());

        MongoSink sink = new MongoSink();
        Configurables.configure(sink, ctx);

        sink.setChannel(channel);
        sink.start();

        Transaction tx = channel.getTransaction();
        tx.begin();
        JSONObject msg = new JSONObject();
        msg.put("name", "test");
        msg.put("age", 11);
        msg.put("birthday", new Date().getTime());

        Event e = EventBuilder.withBody(msg.toJSONString().getBytes());
        channel.put(e);
        tx.commit();
        tx.close();

        sink.process();
        sink.stop();

        DB db = mongo.getDB("test_events");
        DBCollection collection = db.getCollection("test_log");
        DBCursor cursor = collection.find(new BasicDBObject(msg));
        assertTrue(cursor.hasNext());
        DBObject dbObject = cursor.next();
        assertNotNull(dbObject);
        assertEquals(dbObject.get("name"), msg.get("name"));
        assertEquals(dbObject.get("age"), msg.get("age"));
        assertEquals(dbObject.get("birthday"), msg.get("birthday"));
    }

    @Test(groups = "dev")
    public void dbTest() {
        DB db = mongo.getDB(DBNAME);
        db.getCollectionNames();
        List<String> names = mongo.getDatabaseNames();

        assertNotNull(names);
        boolean hit = false;

        for (String name : names) {
            if (DBNAME.equals(name)) {
                hit = true;
                break;
            }
        }

        assertTrue(hit);
    }

    @Test(groups = "dev")
    public void collectionTest() {
        DB db = mongo.getDB(DBNAME);
        DBCollection myCollection = db.getCollection("myCollection");
        myCollection.save(new BasicDBObject(MapUtils.putAll(new HashMap(), new Object[]{"name", "leon", "age", 33})));
        myCollection.findOne();

        Set<String> names = db.getCollectionNames();

        assertNotNull(names);
        boolean hit = false;

        for (String name : names) {
            if ("myCollection".equals(name)) {
                hit = true;
                break;
            }
        }

        assertTrue(hit);
    }

    @Test(groups = "dev")
    public void autoWrapTest() throws EventDeliveryException {
        ctx.put(MongoSink.AUTO_WRAP, Boolean.toString(true));
        ctx.put(MongoSink.DB_NAME, "test_wrap");

        MongoSink sink = new MongoSink();
        Configurables.configure(sink, ctx);

        sink.setChannel(channel);
        sink.start();

        Transaction tx = channel.getTransaction();
        tx.begin();
        String msg = "2012/10/26 11:23:08 [error] 7289#0: *6430831 open() \"/usr/local/nginx/html/50x.html\" failed (2: No such file or directory), client: 10.160.105.161, server: sg15.redatoms.com, request: \"POST /mojo/ajax/embed HTTP/1.0\", upstream: \"fastcgi://unix:/tmp/php-fpm.sock:\", host: \"sg15.redatoms.com\", referrer: \"http://sg15.redatoms.com/mojo/mobile/package\"";

        Event e = EventBuilder.withBody(msg.getBytes());
        channel.put(e);
        tx.commit();
        tx.close();

        sink.process();
        sink.stop();

        DB db = mongo.getDB("test_wrap");
        DBCollection collection = db.getCollection("test_log");
        DBCursor cursor = collection.find(new BasicDBObject(MongoSink.DEFAULT_WRAP_FIELD, msg));
        assertTrue(cursor.hasNext());
        DBObject dbObject = cursor.next();
        assertNotNull(dbObject);
        assertEquals(dbObject.get(MongoSink.DEFAULT_WRAP_FIELD), msg);
        mongo.dropDatabase("test_wrap");
    }

    @Test(groups = "dev")
    public void sinkDynamicDbTest() throws EventDeliveryException {
        ctx.put(MongoSink.MODEL, MongoSink.CollectionModel.dynamic.name());
        MongoSink sink = new MongoSink();
        Configurables.configure(sink, ctx);

        sink.setChannel(channel);
        sink.start();

        JSONObject msg = new JSONObject();
        msg.put("age", 11);
        msg.put("birthday", new Date().getTime());

        Transaction tx;

        for (int i = 0; i < 10; i++) {
            tx = channel.getTransaction();
            tx.begin();
            msg.put("name", "test" + i);
            JSONObject header = new JSONObject();
            header.put(MongoSink.COLLECTION, "my_events");
            header.put(MongoSink.DB_NAME, "dynamic_db");

            Event e = EventBuilder.withBody(msg.toJSONString().getBytes(), header);
            channel.put(e);
            tx.commit();
            tx.close();
        }
        sink.process();
        sink.stop();

        for (int i = 0; i < 10; i++) {
            msg.put("name", "test" + i);

            System.out.println("i = " + i);

            DB db = mongo.getDB("dynamic_db");
            DBCollection collection = db.getCollection("my_events");
            DBCursor cursor = collection.find(new BasicDBObject(msg));
            assertTrue(cursor.hasNext());
            DBObject dbObject = cursor.next();
            assertNotNull(dbObject);
            assertEquals(dbObject.get("name"), msg.get("name"));
            assertEquals(dbObject.get("age"), msg.get("age"));
            assertEquals(dbObject.get("birthday"), msg.get("birthday"));
        }

    }

    @Test(groups = "dev")
    public void timestampNewFieldTest() throws EventDeliveryException {
        ctx.put(MongoSink.MODEL, MongoSink.CollectionModel.dynamic.name());
        String tsField = "createdOn";
        ctx.put(MongoSink.TIMESTAMP_FIELD, tsField);
        MongoSink sink = new MongoSink();
        Configurables.configure(sink, ctx);

        sink.setChannel(channel);
        sink.start();

        JSONObject msg = new JSONObject();
        msg.put("age", 11);
        msg.put("birthday", new Date().getTime());

        Transaction tx;

        for (int i = 0; i < 10; i++) {
            tx = channel.getTransaction();
            tx.begin();
            msg.put("name", "test" + i);
            JSONObject header = new JSONObject();
            header.put(MongoSink.COLLECTION, "my_events");
            header.put(MongoSink.DB_NAME, "dynamic_db");

            Event e = EventBuilder.withBody(msg.toJSONString().getBytes(), header);
            channel.put(e);
            tx.commit();
            tx.close();
        }
        sink.process();
        sink.stop();

        for (int i = 0; i < 10; i++) {
            msg.put("name", "test" + i);

            System.out.println("i = " + i);

            DB db = mongo.getDB("dynamic_db");
            DBCollection collection = db.getCollection("my_events");
            DBCursor cursor = collection.find(new BasicDBObject(msg));
            assertTrue(cursor.hasNext());
            DBObject dbObject = cursor.next();
            assertNotNull(dbObject);
            assertEquals(dbObject.get("name"), msg.get("name"));
            assertEquals(dbObject.get("age"), msg.get("age"));
            assertEquals(dbObject.get("birthday"), msg.get("birthday"));
            assertTrue(dbObject.get(tsField) instanceof Date);
        }

    }

    @Test(groups = "dev")
    public void timestampExistingFieldTest() throws EventDeliveryException, ParseException {
        ctx.put(MongoSink.MODEL, MongoSink.CollectionModel.dynamic.name());
        String tsField = "createdOn";
        ctx.put(MongoSink.TIMESTAMP_FIELD, tsField);
        MongoSink sink = new MongoSink();
        Configurables.configure(sink, ctx);

        sink.setChannel(channel);
        sink.start();

        JSONObject msg = new JSONObject();
        msg.put("age", 11);
        msg.put("birthday", new Date().getTime());
        String dateText = "2013-01-07T17:53:15+08:00";
        msg.put(tsField, dateText);

        Transaction tx;

        for (int i = 0; i < 10; i++) {
            tx = channel.getTransaction();
            tx.begin();
            msg.put("name", "test" + i);
            JSONObject header = new JSONObject();
            header.put(MongoSink.COLLECTION, "my_events");
            header.put(MongoSink.DB_NAME, "dynamic_db");

            Event e = EventBuilder.withBody(msg.toJSONString().getBytes(), header);
            channel.put(e);
            tx.commit();
            tx.close();
        }
        sink.process();
        sink.stop();

        msg.put(tsField, MongoSink.dateTimeFormatter.parseDateTime(dateText).toDate());
        for (int i = 0; i < 10; i++) {
            msg.put("name", "test" + i);

            System.out.println("i = " + i);

            DB db = mongo.getDB("dynamic_db");
            DBCollection collection = db.getCollection("my_events");
            DBCursor cursor = collection.find(new BasicDBObject(msg));
            assertTrue(cursor.hasNext());
            DBObject dbObject = cursor.next();
            assertNotNull(dbObject);
            assertEquals(dbObject.get("name"), msg.get("name"));
            assertEquals(dbObject.get("age"), msg.get("age"));
            assertEquals(dbObject.get("birthday"), msg.get("birthday"));
            assertTrue(dbObject.get(tsField) instanceof Date);
            System.out.println("ts = " + dbObject.get(tsField));
        }

    }
}
