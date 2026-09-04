const { JSONRequest } = require('../jsonrequest');

class SearchForTransactions extends JSONRequest {
  // eslint-disable-next-line no-underscore-dangle,class-methods-use-this
  _path() {
    return '/v2/transactions';
  }

  // notePrefix to filter with, as uint8array
  notePrefix(prefix) {
    this.query['note-prefix'] = prefix;
    return this;
  }

  // txtype to filter with, as string
  txType(type) {
    this.query['tx-type'] = type;
    return this;
  }

  // sigtype to filter with, as string
  sigType(type) {
    this.query['sig-type'] = type;
    return this;
  }

  // txid to filter with, as string
  txid(txid) {
    this.query.txid = txid;
    return this;
  }

  // round to filter with, as int
  round(round) {
    this.query.round = round;
    return this;
  }

  // min round to filter with, as int
  minRound(round) {
    this.query['min-round'] = round;
    return this;
  }

  // max round to filter with, as int
  maxRound(round) {
    this.query['max-round'] = round;
    return this;
  }

  // asset ID to filter with, as int
  assetID(id) {
    this.query['asset-id'] = id;
    return this;
  }

  // limit for filter, as int
  limit(limit) {
    this.query.limit = limit;
    return this;
  }

  // before-time to filter with, as rfc3339 string
  beforeTime(before) {
    this.query['before-time'] = before;
    return this;
  }

  // after-time to filter with, as rfc3339 string
  afterTime(after) {
    this.query['after-time'] = after;
    return this;
  }

  // filtered results should have an amount greater than this value, as int, representing microAlgos, unless an asset-id is provided, in which case units are in the asset's units
  currencyGreaterThan(greater) {
    this.query['currency-greater-than'] = greater;
    return this;
  }

  // filtered results should have an amount less than this value, as int, representing microAlgos, unless an asset-id is provided, in which case units are in the asset's units
  currencyLessThan(lesser) {
    this.query['currency-less-than'] = lesser;
    return this;
  }

  // combined with address, defines what address to filter on, as string
  addressRole(role) {
    this.query['address-role'] = role;
    return this;
  }

  // address to filter with, as string
  address(address) {
    this.query.address = address;
    return this;
  }

  // whether or not to consider the close-to field as a receiver when filtering transactions, as bool. set to true to ignore close-to
  excludeCloseTo(exclude) {
    this.query['exclude-close-to'] = exclude;
    return this;
  }

  // used for pagination
  nextToken(nextToken) {
    this.query.next = nextToken;
    return this;
  }

  // whether or not to include rekeying transactions
  rekeyTo(rekeyTo) {
    this.query['rekey-to'] = rekeyTo;
    return this;
  }

  // filter for this application
  applicationID(applicationID) {
    this.query['application-id'] = applicationID;
    return this;
  }
}

module.exports = { SearchForTransactions };
