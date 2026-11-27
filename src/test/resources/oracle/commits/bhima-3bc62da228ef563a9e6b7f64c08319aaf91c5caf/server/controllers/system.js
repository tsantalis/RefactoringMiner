/**
 * @overview
 * This controller uses the Topic library to broadcast events to the client
 * along a server-sent events channel.  It includes two channels:
 *  1. `/stream` for real-time event broadcasts.
 *  2. `/events` for eventsing all events in the last day
 *
 * @requires os
 * @requires lib/db
 * @requires lib/topic
 */


const os = require('os');
const db = require('../lib/db');
const Topic = require('../lib/topic');
const pkg = require('../../../package.json');

// GET system/stream
exports.stream = stream;

// GET system/events
exports.events = events;

// GET system/info
exports.info = info;

/**
 * @method stream
 *
 * @description
 * This is a server-sent event stream to be send data to the client in real
 * time.  This is useful for system activity monitoring.
 */
function stream(req, res) {
  // ensure the socket hangs open forever
  res.set('Content-Type', 'text/event-stream');
  res.set('Content-Control', 'no-cache');
  res.set('Connection', 'keep-alive');

  // this listener publishes events to the client as server-sent events
  function listener(data) {
    res.write(`retry: 10000\ndata: ${JSON.stringify(data)}\n\n`);
    res.flush();
  }

  // listener for server events and echo them to the client
  const subscription = Topic.subscribe(Topic.channels.ALL, listener);

  // remove listener on when the client closes the connection
  res.on('close', () => {
    Topic.unsubscribe(Topic.channels.ALL, subscription);
  });
}

/**
 * @method events
 *
 * @description
 * Retrieved cached events from the database. Events are stored with a
 * timestamp, some metadata, and a large TEXT blob.  The TEXT blob must
 * be parsed into valid JSON structure before being shipped to the client.
 *
 * @todo - should the parsing happen on the client?
 */
function events(req, res, next) {
  const sql = `
    SELECT event.data FROM event LIMIT 1000;
  `;

  db.exec(sql)
  .then(rows => {
      // events are stored as TEXT, that need to be parsed into JSON data.
    const eventString = rows.map(row => row.data);
    res.status(200).json(eventString);
  })
  .catch(next)
  .done();
}

// send operating system information
function info(req, res) {
  // platform information string
  const platformString = `${os.platform()}-${os.arch()}-${os.release()}`;

  // data to be returned to the client
  const data = {
    platform : platformString,
    numCPUs : os.cpus().length,
    machineUptime : os.uptime() * 1000,       // change to milliseconds
    processUptime : process.uptime() * 1000,  // change to milliseconds
    memoryUsage : (1 - (os.freemem() / os.totalmem())) * 100,
    version : pkg.version,
  };

  // respond with the system statistics
  res.status(200).json(data);
}
