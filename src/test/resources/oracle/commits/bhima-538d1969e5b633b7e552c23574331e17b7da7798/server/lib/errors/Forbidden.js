const util = require('util');

/**
 * @class Forbidden
 *
 * @description
 * A custom error to wrap the 403 HTTP status code within the server.  This
 * should only be thrown in a context where it can be caught by ExpressJS's
 * {@link http://expressjs.com/en/guide/routing.html|next()} function and
 * returned to the client.
 *
 * @param {String} description - a custom description to be sent to the client
 *
 * @example
 * // import the error into a controller
 * const Forbidden = require('lib/errors/Forbidden');
 *
 * throw new Forbidden('You are not allowed to access this resource!');
 *
 * // or by calling next in the server context
 * next(new Forbidden('Oops!'));
 *
 * // or by combining both in a promise chain!
 * Promise.then(() => {
 *   throw new Forbidden('This will be caught in Promise.catch()');
 * })
 * .catch(next);
 *
 * @requires util
 */

/**
 * This implements an HTTP error code that should eventually be passed through
 * next() to an error handling middleware.
 *
 * @param {String} description - a custom description to be sent to the client
 *
 * @example
 * // import the error into a controller
 * const Forbidden = require('lib/errors/Forbidden');
 *
 * // use the error in either a promise chain or directly via next()
 * return next(new Forbidden('Some description...'));
 *
 * @constructor
 */
function Forbidden(description) {
  // make sure we have a working stack trace
  Error.captureStackTrace(this, this.constructor);

  // HTTP status code
  this.status = 403;

  // bhima status code (for $translation)
  this.code = 'ERRORS.FORBIDDEN';

  // default to an empty string if no description passed in
  this.description = description || '';
}

// prototypically inherit from the global error object
util.inherits(Forbidden, Error);

// expose the function to an external controller
module.exports = Forbidden;
