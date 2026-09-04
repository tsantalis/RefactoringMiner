const { JSONRequest } = require('../jsonrequest');

class Proof extends JSONRequest {
  constructor(c, intDecoding, round, txID) {
    super(c, intDecoding);
    this.round = round;
    this.txID = txID;
  }

  // eslint-disable-next-line no-underscore-dangle
  _path() {
    return `/v2/blocks/${this.round}/transactions/${this.txID}/proof`;
  }
}

module.exports = { Proof };
