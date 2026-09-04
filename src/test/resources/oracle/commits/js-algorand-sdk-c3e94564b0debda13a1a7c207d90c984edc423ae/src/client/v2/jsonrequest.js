class JSONRequest {
  /**
   * @param {HttpClient} client HTTPClient object.
   * @param {"default" | "safe" | "mixed" | "bigint" | undefined} intDecoding The method to use
   *   for decoding integers from this request's response. See the setIntDecoding method for more
   *   details.
   */
  constructor(client, intDecoding = undefined) {
    this.c = client;
    this.query = {};
    this.intDecoding = intDecoding || 'default';
  }

  /**
   * @returns {string} The path of this request.
   */
  // eslint-disable-next-line no-underscore-dangle,class-methods-use-this
  _path() {
    throw new Error('Must be overriden by implementing class.');
  }

  /**
   * Execute the request.
   * @param {object} headers Additional headers to send in the request. Optional.
   * @returns {Promise<object>} A promise which resolves to the response data.
   */
  async do(headers = {}) {
    const jsonOptions = {};
    if (this.intDecoding !== 'default') {
      jsonOptions.intDecoding = this.intDecoding;
    }
    const res = await this.c.get(
      // eslint-disable-next-line no-underscore-dangle
      this._path(),
      this.query,
      headers,
      jsonOptions
    );
    return res.body;
  }

  /**
   * Configure how integers in this request's JSON response will be decoded.
   *
   * The options are:
   * * "default": Integers will be decoded according to JSON.parse, meaning they will all be
   *   Numbers and any values greater than Number.MAX_SAFE_INTEGER will lose precision.
   * * "safe": All integers will be decoded as Numbers, but if any values are greater than
   *   Number.MAX_SAFE_INTEGER an error will be thrown.
   * * "mixed": Integers will be decoded as Numbers if they are less than or equal to
   *   Number.MAX_SAFE_INTEGER, otherwise they will be decoded as BigInts.
   * * "bigint": All integers will be decoded as BigInts.
   *
   * @param {"default" | "safe" | "mixed" | "bigint"} method The method to use when parsing the
   *   response for this request. Must be one of "default", "safe", "mixed", or "bigint".
   */
  setIntDecoding(method) {
    if (
      method !== 'default' &&
      method !== 'safe' &&
      method !== 'mixed' &&
      method !== 'bigint'
    )
      throw new Error(`Invalid method for int decoding: ${method}`);
    this.intDecoding = method;
    return this;
  }
}

module.exports = { JSONRequest };
