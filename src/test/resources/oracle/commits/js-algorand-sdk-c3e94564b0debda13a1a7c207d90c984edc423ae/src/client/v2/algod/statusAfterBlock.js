const { JSONRequest } = require('../jsonrequest');

class StatusAfterBlock extends JSONRequest {
  constructor(c, intDecoding, round) {
    super(c, intDecoding);
    if (!Number.isInteger(round)) throw Error('round should be an integer');
    this.round = round;
  }

  // eslint-disable-next-line no-underscore-dangle
  _path() {
    return `/v2/status/wait-for-block-after/${this.round}`;
  }
}

module.exports = { StatusAfterBlock };
