const { JSONRequest } = require('../jsonrequest');

class MakeHealthCheck extends JSONRequest {
  // eslint-disable-next-line no-underscore-dangle,class-methods-use-this
  _path() {
    return '/health';
  }
}

module.exports = { MakeHealthCheck };
