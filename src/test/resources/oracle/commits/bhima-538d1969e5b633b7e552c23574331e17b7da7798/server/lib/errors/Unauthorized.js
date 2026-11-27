const util = require('util');

/**
 * @class Unauthorized
 *
 * @description
 * A custom error to wrap the 401 HTTP status code within the server.  This
 * should only be thrown in a context where it can be caught by ExpressJS's
 * {@link http://expressjs.com/en/guide/routing.html|next()} function and
 * returned to the client.
 *
 * @param {String} description - a custom description to be sent to the client
 *
 * @example
 * // import the error into a controller
 * const Unauthorized = require('lib/errors/Unauthorized');
 *
 * // use by directly throwing ...
 * throw new Unauthorized('An authentication error occurred!');
 *
 * // or by calling next in the server context
 * next(new Unauthorized('Some description...'));
 *
 * // or by combining both in a promise chain!
 * Promise.then(() => {
 *   throw new Unauthorized('This will be caught in Promise.catch()');
 * })
 * .catch(next);
 *
 * @requires util
 */
function Unauthorized(description, key) {
  // make sure we have a working stack trace
  Error.captureStackTrace(this, this.constructor);

  // HTTP status code
  this.status = 401;

  // bhima status code (for $translation)
  this.code = key || 'ERRORS.UNAUTHORIZED';

  // default to an empty string if no description passed in
  this.description = description || '';
}

// prototypically inherit from the global error object
util.inherits(Unauthorized, Error);

// expose the function to an external controller
module.exports = Unauthorized;
