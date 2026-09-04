const { JSONRequest } = require('../jsonrequest');

class Supply extends JSONRequest {
  // eslint-disable-next-line no-underscore-dangle,class-methods-use-this
  _path() {
    return '/v2/ledger/supply';
  }
}

module.exports = { Supply };
