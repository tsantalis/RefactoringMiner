/**
 * @overview server
 * Basic Hospital Information Management Application
 *
 * This is the central server of bhima.  It is responsible for setting up the
 * HTTP server, logging infrastructure, and environmental variables.  These are
 * global throughout the application, and are configured here.
 *
 * The application routes are configured in {@link server/config/routes}, while
 * the middleware is configured in {@link server/config/express}.
 *
 * @requires http
 * @requires dotenv
 * @requires express
 * @requires winston
 *
 * @requires config/express
 * @requires config/routes
 *
 * @license GPL-2.0
 * @copyright IMA World Health 2016
 */

require('use-strict');

const http = require('http');
const express = require('express');
const winston = require('winston');
const dotEnv = require('dotenv');

const app = express();

/**
 * @function configureEnvironmentVariables
 *
 * @description
 * Uses dotenv to add environmental variables from the .env.* file to the
 * process object.  If the NODE_ENV system variable is not set, the function
 * defaults to 'production'
 */
function configureEnvironmentVariables() {
  // if the process NODE_ENV is not set, default to production.
  process.env.NODE_ENV = process.env.NODE_ENV || 'production';

  // normalize the environmental variable name
  const env = process.env.NODE_ENV.toLowerCase();

  // decode the file path for the environmental variables.
  const dotfile = `server/.env.${env}`.trim();

  // load the environmental variables into process using the dotenv module
  winston.info(`[app] Loading configuration from ${dotfile}.`);
  dotEnv.config({ path: dotfile });
}

/**
 * @function configureLogger
 *
 * @description
 * Harnesses winston to log both events uniformly across the server.  This
 * includes HTTP requests (using morgan), application events, and plugin events.
 *
 * By default, the only configured logging interface is the console.  This
 * should not be the case in production.  If the LOG_FILE environmental
 * variable exists, the server will use it write all logs.
 *
 */
function configureLogger() {
  // set logging levels to that found in the configuration file (default: warn)
  winston.level = (process.env.LOG_LEVEL || 'warn');

  const logFile = process.env.LOG_FILE;

  // allow logging to a file if needed
  if (logFile) {
    winston.add(winston.transports.File, { filename: logFile });
  }

  // be sure to log unhandled exceptions
  winston.handleExceptions(new winston.transports.Console({
    humanReadableUnhandledException : true,
    colorize                        : true,
    prettyPrint                     : true,
  }));
}

/**
 * @function configureServer
 *
 * @description
 * Set up the HTTP server to listen on the correct
 */
function configureServer() {
  // destruct the environmental variables
  const port = process.env.PORT;
  const mode = process.env.NODE_ENV;

  // create the server
  http.createServer(app)
    .listen(process.env.PORT, () => {
      winston.info(`Server started in mode ${mode} on port ${port}.`);
    });
}

// run configuration tools
configureEnvironmentVariables();
configureLogger();
configureServer();

// Configure application middleware stack, inject authentication session
require('./config/express').configure(app);

// Link routes
require('./config/routes').configure(app);

// link error handling
require('./config/express').errorHandling(app);

// ensure the process terminates gracefully when an error occurs.
process.on('uncaughtException', () => process.exit(1));

process.on('warning', (warning) => {
  winston.warn(warning.message);
  winston.warn(warning.stack);
});
