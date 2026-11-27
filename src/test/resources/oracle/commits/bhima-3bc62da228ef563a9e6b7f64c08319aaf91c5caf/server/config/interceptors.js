
/**
 * @overview interceptors
 * This modules defines a unified error handler for the server.
 * Each controller is expected to define specific errors that are uniformly
 * sent to the client using the using the error functions found in `lib/errors/`.
 *
 * The only unexpected errors are database errors, uniformly treated as
 * BadRequests (HTTP stats code 400).
 *
 * @requires winston
 * @requires BadRequest
 */

const winston = require('winston');
const BadRequest = require('../lib/errors/BadRequest');

const LOG_ALL_MYSQL_ERRORS = Number(process.env.LOG_ALL_MYSQL_ERRORS);

// map MySQL error codes to HTTP status codes
const map = {
  ER_DUP_KEY :
    `A key collided in a unique database field.  Please retry your action.  If
    the problem persists, contact the developers.`,
  ER_BAD_FIELD_ERROR   : 'Column does not exist in database.',
  ER_ROW_IS_REFERENCED :
    'Cannot delete entity because entity is used in another table.',
  ER_ROW_IS_REFERENCED_2 :
    'Cannot delete entity because entity is used in another table.',
  ER_BAD_NULL_ERROR : 'A column was left NULL that cannot be NULL.',
  ER_PARSE_ERROR    :
    `Your request could not  be translated into valid SQL.  Please modify your
    request and try again.`,
  ER_EMPTY_QUERY          : 'Your request had an empty body.',
  ER_NO_DEFAULT_FOR_FIELD :
    'You did not include enough information in your query.',
  ER_DATA_TOO_LONG :
    'The value provided is longer than the database record limit.',
};

// these are custom errors defined by
const SQL_STATES = {
  45001 : 'ERRORS.NO_ENTERPRISE',
  45002 : 'ERRORS.NO_PROJECT',
  45003 : 'ERRORS.NO_FISCAL_YEAR',
  45004 : 'ERRORS.NO_PERIOD',
  45005 : 'ERRORS.NO_EXCHANGE_RATE',
  45501 : 'ERRORS.OVERPAID_INVOICE',
};

/**
 * Server Error Handler
 *
 * This error handler interprets all errors and sends them to the client.
 */
exports.handler = function handler(err, req, res, next) {
  let error = err;
  // log the error to the error log (NOTE: in production, this should be 'error')
  winston.log('debug', error);

  // check if it is a database error and format the proper description if so.
  if (error.sqlState) {
    let key;
    let description;

    // todo(jniles) - unify this error handing
    if (error.code === 'ER_SIGNAL_EXCEPTION') {
      key = SQL_STATES[error.sqlState] || error.sqlState;
      description = error.toString();
    } else {
      key = `ERRORS.${error.code || error.sqlState}`;
      description = map[error.code];
    }

    if (LOG_ALL_MYSQL_ERRORS) {
      winston.log('error', error);
    }

    error = new BadRequest(description, key);
  }

  // prevent invalid http error codes.
  if (!error.status) { error.status = 500; }

  // send to the client with the appropriate status code
  res.status(error.status).json(error);
};
