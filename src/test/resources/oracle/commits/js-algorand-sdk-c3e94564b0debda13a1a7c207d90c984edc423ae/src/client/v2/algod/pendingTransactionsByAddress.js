const encoding = require('../../../encoding/encoding');

class PendingTransactionsByAddress {
  constructor(c, address) {
    this.c = c;
    this.address = address;
    this.query = { format: 'msgpack' };
  }

  /**
   * returns all transactions for a PK [addr] in the [first, last] rounds range.
   * @param headers, optional
   * @returns {Promise<*>}
   */
  async do(headers = {}) {
    const res = await this.c.get(
      `/v2/accounts/${this.address}/transactions/pending`,
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

module.exports = { PendingTransactionsByAddress };
