/**
 * Sets the default header (if not previously set)
 * @param headers
 * @returns {*}
 */
function setHeaders(headers) {
  let hdrs = headers;
  if (Object.keys(hdrs).every((key) => key.toLowerCase() !== 'content-type')) {
    hdrs = { ...headers };
    hdrs['Content-Type'] = 'text/plain';
  }
  return hdrs;
}

class Compile {
  constructor(c, source) {
    this.c = c;
    this.source = source;
  }

  /**
   * Executes compile
   * @param headers, optional
   * @returns {Promise<*>}
   */
  async do(headers = {}) {
    const txHeaders = setHeaders(headers);
    const res = await this.c.post(
      '/v2/teal/compile',
      Buffer.from(this.source),
      txHeaders
    );
    return res.body;
  }
}

module.exports = { Compile, setHeaders };
