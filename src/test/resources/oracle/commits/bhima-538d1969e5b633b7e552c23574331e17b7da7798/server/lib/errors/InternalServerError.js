const util = require('util');

/**
 * @class InternalServerError
 *
 * @description
 * A custom error to wrap the 500 HTTP status code within the server.  This
 * should only be thrown in a context where it can be caught by ExpressJS's
 * {@link http://expressjs.com/en/guide/routing.html|next()} function and
 * returned to the client.
 *
 * @param {String} description - a custom description to be sent to the client
 *
 * @example
 * // import the error into a controller
 * const InternalServerError = require('lib/errors/InternalServerError');
 *
 * throw new InternalServerError('A fatal error occurred!');
 *
 * // or by calling next in the server context
 * next(new InternalServerError('Oops!'));
 *
 * // or by combining both in a promise chain!
 * Promise.then(() => {
 *   throw new InternalServerError('This will be caught in Promise.catch()');
 * })
 * .catch(next);
 *
 * @requires util
 */
function InternalServerError(description) {
  // make sure we have a working stack trace
  Error.captureStackTrace(this, this.constructor);

  // HTTP status code
  this.status = 500;

  // bhima status code (for $translation)
  this.code = 'ERRORS.INTERNAL_SERVER_ERROR';

  // default to an empty string if no description passed in
  this.description = description || '';
}

// prototypically inherit from the global error object
util.inherits(InternalServerError, Error);

// expose the function to an external controller
module.exports = InternalServerError;
