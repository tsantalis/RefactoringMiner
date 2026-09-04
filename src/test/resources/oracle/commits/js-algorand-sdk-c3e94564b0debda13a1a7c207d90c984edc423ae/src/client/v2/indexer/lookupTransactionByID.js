const { JSONRequest } = require('../jsonrequest');

class LookupTransactionByID extends JSONRequest {
  constructor(c, intDecoding, txID) {
    super(c, intDecoding);
    this.txID = txID;
  }

  // eslint-disable-next-line no-underscore-dangle
  _path() {
    return `/v2/transactions/${this.txID}`;
  }
}

module.exports = { LookupTransactionByID };
