const { JSONRequest } = require('../jsonrequest');

class AccountInformation extends JSONRequest {
  constructor(c, intDecoding, account) {
    super(c, intDecoding);
    this.account = account;
  }

  // eslint-disable-next-line no-underscore-dangle
  _path() {
    return `/v2/accounts/${this.account}`;
  }
}

module.exports = { AccountInformation };
