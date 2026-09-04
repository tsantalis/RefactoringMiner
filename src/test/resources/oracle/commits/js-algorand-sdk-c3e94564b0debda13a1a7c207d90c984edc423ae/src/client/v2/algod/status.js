const { JSONRequest } = require('../jsonrequest');

class Status extends JSONRequest {
  // eslint-disable-next-line no-underscore-dangle,class-methods-use-this
  _path() {
    return '/v2/status';
  }
}

module.exports = { Status };
