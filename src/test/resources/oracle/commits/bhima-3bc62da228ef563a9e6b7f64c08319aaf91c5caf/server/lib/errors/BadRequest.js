const util = require('util');

/**
 * @class BadRequest
 *
 * @description
 * A custom error to wrap the 400 HTTP status code within the server.  This
 * should only be thrown in a context where it can be caught by ExpressJS's
 * {@link http://expressjs.com/en/guide/routing.html|next()} function and
 * returned to the client.
 *
 * @param {String} description - a custom description to be sent to the client
 * @param {String} key - a i18n key for translation on the client
 *
 * @example
 * // import the error into a controller
 * const BadRequest = require('lib/errors/BadRequest');
 *
 * // use by directly throwing ...
 * throw new BadRequest('An authentication error occurred!');
 *
 * // or by calling next (in the server context)
 * // note: the i18n key is optional
 * next(new BadRequest('Some description...', 'SOME.KEY');
 *
 * // or by combining both in a promise chain!
 * Promise.then(() => {
 *   throw new BadRequest('This will be caught in Promise.catch()', 'OH.NO');
 * })
 * .catch(next);
 *
 * @requires util
 */
function BadRequest(description, key) {

  // make sure we have a working stack trace
  Error.captureStackTrace(this, this.constructor);

  // HTTP status code
  this.status = 400;

  // bhima status code (for $translation)
  this.code = key || 'ERRORS.BAD_REQUEST';

  // default to an empty string if no description passed in
  this.description = description || '';
}

// prototypically inherit from the global error object
util.inherits(BadRequest, Error);

// expose the function to an external controller
module.exports = BadRequest;
