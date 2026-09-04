const encoding = require('../../../encoding/encoding');

class PendingTransactionInformation {
  constructor(c, txid) {
    this.c = c;
    this.txid = txid;
    this.query = {};
    this.query.format = 'msgpack';
  }

  /**
   * returns the transaction information for a specific txid of a pending transaction
   * @param headers, optional
   * @returns {Promise<*>}
   */
  async do(headers = {}) {
    const res = await this.c.get(
      `/v2/transactions/pending/${this.txid}`,
      this.query,
      headers
    );
    if (res.body && res.body.byteLength > 0) {
      return encoding.decode(res.body);
    }
    return undefined;
  }

  // max sets the maximum number of txs to return
  max(max) {
    this.query.max = max;
    return this;
  }
}

module.exports = { PendingTransactionInformation };
