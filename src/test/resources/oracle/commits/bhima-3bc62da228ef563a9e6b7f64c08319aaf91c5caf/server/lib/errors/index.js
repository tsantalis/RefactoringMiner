/**
 * @overview
 * This file exposes error classes for use throughout the application.  All
 * errors are expected to eventually be sent back to the client, so they are
 * directly associated with an HTTP status code.
 *
 * @example
 * // you can import the entire module ...
 * const errors = require('../lib/errors');
 *
 * // or a single error at a time (recommended).
 * const NotFound = require('../lib/errors/NotFound');
 *
 * // example usage for some route
 * app.get('/some/route/:id', function (res, req, next) {
 *   const id = req.params.id;
 *   db.exec('some sql', id)
 *   .then(function (rows) {
 *
 *     // there were no records returned!  Throw an error
 *     if (rows.length === 0) {
 *
 *       // recommended practice - call errors by name
 *       throw new NotFound(`Could not find a record with id: ${id}`);
 *
 *       // alternatively, we could have done the following:
 *       // throw new errors.NotFound('some description');
 *     }
 *   })
 *   .catch(next)
 *   .done();
 * });
 */
module.exports = {
  BadRequest:          require('./BadRequest'),
  Forbidden:           require('./Forbidden'),
  Unauthorized:        require('./Unauthorized'),
  NotFound:            require('./NotFound'),
  InternalServerError: require('./InternalServerError')
};
