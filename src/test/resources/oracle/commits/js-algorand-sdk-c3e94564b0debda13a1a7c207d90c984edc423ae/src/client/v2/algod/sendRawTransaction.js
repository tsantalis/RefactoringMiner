/**
 * Sets the default header (if not previously set) for sending a raw
 * transaction.
 * @param headers
 * @returns {*}
 */
function setSendTransactionHeaders(headers) {
  let hdrs = headers;
  if (Object.keys(hdrs).every((key) => key.toLowerCase() !== 'content-type')) {
    hdrs = { ...headers };
    hdrs['Content-Type'] = 'application/x-binary';
  }
  return hdrs;
}

class SendRawTransaction {
  constructor(c, stxOrStxs) {
    let forPosting = stxOrStxs;
    function isByteArray(array) {
      return !!(array && array.byteLength !== undefined);
    }
    if (Array.isArray(stxOrStxs)) {
      if (!stxOrStxs.every(isByteArray)) {
        throw new TypeError('Array elements must be byte arrays');
      }
      forPosting = Array.prototype.concat(
        ...stxOrStxs.map((arr) => Array.from(arr))
      );
    } else if (!isByteArray(forPosting)) {
      throw new TypeError('Argument must be byte array');
    }
    this.txnBytesToPost = forPosting;
    this.c = c;
  }

  /**
   * broadcasts the passed signed txns to the network
   * @param headers, optional
   * @returns {Promise<*>}
   */
  async do(headers = {}) {
    const txHeaders = setSendTransactionHeaders(headers);
    const res = await this.c.post(
      '/v2/transactions',
      Buffer.from(this.txnBytesToPost),
      txHeaders
    );
    return res.body;
  }
}

module.exports = { SendRawTransaction, setSendTransactionHeaders };
