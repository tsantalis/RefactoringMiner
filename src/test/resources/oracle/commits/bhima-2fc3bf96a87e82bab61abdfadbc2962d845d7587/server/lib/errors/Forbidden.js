/**
 * @class Forbidden
 * @description A custom error to wrap the 401 HTTP status code.
 */
class Forbidden extends Error {
  /**
   * @param {string} description - A custom description to be sent to the client
   * @param {string} code - A translation key or internal error code
   */
  constructor(description = '', code = 'ERRORS.FORBIDDEN') {
    super(description);
    this.status = 403;
    this.code = code;
    this.description = description;

    Error.captureStackTrace(this, this.constructor);
  }
}

module.exports = Forbidden;
