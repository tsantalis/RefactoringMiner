/**
 * @class BadRequest
 * @description A custom error to wrap the 400 HTTP status code.
 */
class BadRequest extends Error {
  /**
   * @param {string} description - A custom description to be sent to the client
   * @param {string} code - A translation key or internal error code
   */
  constructor(description = '', code = 'ERRORS.BAD_REQUEST') {
    super(description);
    this.status = 400;
    this.code = code;
    this.description = description;

    Error.captureStackTrace(this, this.constructor);
  }
}

module.exports = BadRequest;
