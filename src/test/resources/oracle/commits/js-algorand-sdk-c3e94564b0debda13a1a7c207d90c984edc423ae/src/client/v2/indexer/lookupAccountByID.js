const { JSONRequest } = require('../jsonrequest');

class LookupAccountByID extends JSONRequest {
  constructor(c, intDecoding, account) {
    super(c, intDecoding);
    this.account = account;
  }

  // eslint-disable-next-line no-underscore-dangle
  _path() {
    return `/v2/accounts/${this.account}`;
  }

  // specific round to search
  round(round) {
    this.query.round = round;
    return this;
  }

  // include all items including closed accounts, deleted applications, destroyed assets, opted-out asset holdings, and closed-out application localstates
  includeAll(value = true) {
    this.query['include-all'] = value;
    return this;
  }
}

module.exports = { LookupAccountByID };
