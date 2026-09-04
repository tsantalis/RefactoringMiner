const { JSONRequest } = require('../jsonrequest');

class GetApplicationByID extends JSONRequest {
  constructor(c, intDecoding, index) {
    super(c, intDecoding);
    this.index = index;
  }

  // eslint-disable-next-line no-underscore-dangle
  _path() {
    return `/v2/applications/${this.index}`;
  }
}

module.exports = { GetApplicationByID };
