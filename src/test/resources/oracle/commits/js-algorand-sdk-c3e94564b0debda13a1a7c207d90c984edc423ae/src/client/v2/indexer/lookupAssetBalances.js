const { JSONRequest } = require('../jsonrequest');

class LookupAssetBalances extends JSONRequest {
  constructor(c, intDecoding, index) {
    super(c, intDecoding);
    this.index = index;
  }

  // eslint-disable-next-line no-underscore-dangle
  _path() {
    return `/v2/assets/${this.index}/balances`;
  }

  // limit for filter, as int
  limit(limit) {
    this.query.limit = limit;
    return this;
  }

  // round to filter with, as int
  round(round) {
    this.query.round = round;
    return this;
  }

  // filtered results should have an amount greater than this value, as int, with units representing the asset
  currencyGreaterThan(greater) {
    this.query['currency-greater-than'] = greater;
    return this;
  }

  // filtered results should have an amount less than this value, as int, with units representing the asset units
  currencyLessThan(lesser) {
    this.query['currency-less-than'] = lesser;
    return this;
  }

  // used for pagination
  nextToken(nextToken) {
    this.query.next = nextToken;
    return this;
  }

  // include all items including closed accounts, deleted applications, destroyed assets, opted-out asset holdings, and closed-out application localstates
  includeAll(value = true) {
    this.query['include-all'] = value;
    return this;
  }
}

module.exports = { LookupAssetBalances };
