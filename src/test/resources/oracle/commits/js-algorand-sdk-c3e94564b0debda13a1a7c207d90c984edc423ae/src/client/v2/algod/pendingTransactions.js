const encoding = require('../../../encoding/encoding');

class PendingTransactions {
  constructor(c) {
    this.c = c;
    this.query = {};
    this.query.format = 'msgpack';
  }

  /**
   * pendingTransactionsInformation returns transactions that are pending in the pool
   * @param headers, optional
   * @returns {Promise<*>}
   */
  async do(headers = {}) {
    const res = await this.c.get(
      '/v2/transactions/pending',
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

module.exports = { PendingTransactions };
