const { JSONRequest } = require('../jsonrequest');

class GetAssetByID extends JSONRequest {
  constructor(c, intDecoding, index) {
    super(c, intDecoding);
    this.index = index;
  }

  // eslint-disable-next-line no-underscore-dangle
  _path() {
    return `/v2/assets/${this.index}`;
  }
}

module.exports = { GetAssetByID };
