class SuggestedParams {
  constructor(c) {
    this.c = c;
  }

  /**
   * returns the common needed parameters for a new transaction, in a format the transaction builder expects
   * @param headers, optional
   * @returns {Object}
   */
  async do(headers = {}) {
    const res = await this.c.get('/v2/transactions/params', {}, headers);
    return {
      flatFee: false,
      fee: res.body.fee,
      firstRound: res.body['last-round'],
      lastRound: res.body['last-round'] + 1000,
      genesisID: res.body['genesis-id'],
      genesisHash: res.body['genesis-hash'],
    };
  }
}

module.exports = { SuggestedParams };
