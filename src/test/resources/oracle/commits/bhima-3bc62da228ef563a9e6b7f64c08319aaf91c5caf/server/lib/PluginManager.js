/**
 * @overview PluginManager
 * A bare bones plugin manager.  It is meant to look very much like the linux
 * process management.  It creates hooks for the startup of plugins, restarts,
 * and halts.
 *
 * @requires path
 * @requires child_process
 * @requires winston
 */

const path    = require('path');
const thread  = require('child_process');
const winston = require('winston');

const MAX_RESTARTS = 3;

/**
 * @class Plugin
 *
 * @constructor
 *
 * @description
 * Constructs an instance of Plugin on a target script.
 *
 * @param {string} script - the script to execute a node process on
 */
function Plugin(script) {

  // the location of the program
  this.script = script;

  // number of attempted restarts
  this.restarts = 0;

  // the number of times to attempt a restart
  // in case of unknown termination signal.
  this.maxRestarts = MAX_RESTARTS;

  // perform the initial startup
  this.startup();
}


/**
 * @method startup
 *
 * @description
 * Performs the initial fork of the script. It sets up exit and error handlers
 * for the Plugin, ensuring that it restarts correctly as needed.
 */
Plugin.prototype.startup = function () {

  // fork the process and assign it to this.process
  this.process = thread.fork(__dirname + this.script);

  // set up the exit handler
  this.process.on('exit', this.exitHandler);

  // the child fork is running
  this.running = true;
};


/**
 * @method exitHandler
 *
 * @description
 * Ensures that the Plugin implements proper exiting behavior.  If an internal
 * error occurs, the Plugin attempts to restart itself.  If an external kill
 * signal is received, there is no attempt to restart the script.
 */
Plugin.prototype.exitHandler = function (code, signal) {

  // 0 is the successful exit code
  // SIGTERM is emitted on process.kill()
  if (code !== 0 && signal !== 'SIGTERM') {

    // if we have received more than the maxRestarts, something may be amiss
    // and we should quit trying to restart
    if (this.restarts < this.maxRestarts) {
      this.startup();
      this.restarts++;
      winston.info(
        `${this.script} died. Attempting ${this.restarts} out of ${this.maxRestarts} restarts.`
      );
    }
  }
};


/**
 * @method emit
 *
 * @description
 * Emits events to the underlying process via node's child_process module.
 *
 * @param {String} event - an event to be sent to the underlying node process
 */
Plugin.prototype.emit = function (event, data) {
  this.process.send({ event : event, data : data });
};


/** wraps process.kill() */
Plugin.prototype.kill = function (code) {
  this.process.kill(code);
};


/**
 * @method register
 *
 * @description
 * Wraps `process.on()` to register callbacks
 *
 * @todo - implement subscribe/unsubscribe() methods
 *
 * @param {String} event - an event to be sent to the underlying node process
 */
Plugin.prototype.register = function (event, callback) {
  this.process.on(event, callback);
};


/**
 * @class PluginManager
 *
 * @description
 * A class to manage all plugins and hook them up to the server events.
 *
 * @constructor
 *
 * @param {Array} cfgArray - configuration array with plugins names and scripts
 */
function PluginManager(cfgArray) {

  const plugins = this.plugins = {};
  const NAMESPACE = 'PluginManager';

  // PluginManager Methods

  function _onStartup() {
    winston.log('debug', '[%s] onStartup() event called.', NAMESPACE);

    // TODO Should we have a 'priority' tag to determine which plugins are
    // initialized first?  This would require sorting the array by priority
    // prior to loading it

    // load and map the plugins to their namespaces
    cfgArray.forEach(function (plugin) {
      winston.log('debug', '[%s] Loading %s', NAMESPACE, plugin.name);
      plugins[plugin.name] = new Plugin(plugin.script);
    });
  }

  // kills all subprocesses in the case that the parent process dies.
  this.killChildren = function (e) {
    winston.log('debug', '[%s] Killing all subprocesses', NAMESPACE);

    // loop through the plugins, terminating each
    Object.keys(plugins).forEach(function (key) {
      var plugin = plugins[key];
      winston.debug(`[${NAMESPACE}] Sending kill signal to ${plugin.script}`);
      plugin.kill('SIGTERM');
    });

    winston.log('debug', '[%s] %s is exiting.', NAMESPACE, NAMESPACE);
    process.exit(e);
  };

  // parses events and routes them to the correct plugin
  this.routeEvent = function (event, data) {
    try {
      // parse the plugin name from the event
      var params = event.split('::'),
          pluginId = params[0],
          eventId = params[1];

      // error if the plugin is not defined for the manager
      if (!this.plugins[pluginId]) {
        winston.log('warn', '[%s] PluginId %s not found.', NAMESPACE, pluginId);
        throw new Error('Error: Plugin not found %s'.replace('%s', pluginId));
      }

      // send the event to the plugin
      this.plugins[pluginId].emit(eventId);
    } catch (e) {

      // ensure that the event failure is broadcast
      winston.log('error', '[%s] %j', NAMESPACE, e);
    }
  };

  _onStartup();
}


/* expose routes to the greater bhima server */
module.exports = function (app, pluginConfig) {

  var pm = new PluginManager(pluginConfig);

  // configure plugin routes

  // :action is actually {pluginId}::{eventId}}
  // Example : /plugin/events/mail::restart
  app.post('/plugin/events/:action', function (req, res, next) {

    // make sure the plugin exists
    try {
      pm.routeEvent(req.params.action, req.body.data);
    } catch (err) {
      return res.status(500).json(err);
    }

    res.status(200).send();
  });

  // clean up children one exception, error, exit
  process.on('uncaughtException', pm.killChildren);
  process.on('SIGINT', pm.killChildren);
  process.on('SIGTERM', pm.killChildren);
};
