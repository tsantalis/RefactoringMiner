const encoding = require('../../../encoding/encoding');

class Block {
  constructor(c, roundNumber) {
    if (!Number.isInteger(roundNumber))
      throw Error('roundNumber should be an integer');
    this.c = c;
    this.round = roundNumber;
    this.query = { format: 'msgpack' };
  }

  /**
   * block gets the block info for the given round. this call may block
   * @param headers, optional
   * @returns {Promise<*>}
   */
  async do(headers = {}) {
    const res = await this.c.get(
      `/v2/blocks/${this.round}`,
      this.query,
      headers
    );
    if (res.body && res.body.byteLength > 0) {
      return encoding.decode(res.body);
    }
    return undefined;
  }
}

module.exports = { Block };
