import JSONRequest from '../jsonrequest';
import { HTTPClient } from '../../client';
import IntDecoding from '../../../types/intDecoding';

export default class LookupAssetBalances extends JSONRequest {
  constructor(c: HTTPClient, intDecoding: IntDecoding, private index: number) {
    super(c, intDecoding);
    this.index = index;
  }

  path() {
    return `/v2/assets/${this.index}/balances`;
  }

  // limit for filter, as int
  limit(limit: number) {
    this.query.limit = limit;
    return this;
  }

  // round to filter with, as int
  round(round: number) {
    this.query.round = round;
    return this;
  }

  // filtered results should have an amount greater than this value, as int, with units representing the asset
  currencyGreaterThan(greater: number) {
    this.query['currency-greater-than'] = greater;
    return this;
  }

  // filtered results should have an amount less than this value, as int, with units representing the asset units
  currencyLessThan(lesser: number) {
    this.query['currency-less-than'] = lesser;
    return this;
  }

  // used for pagination
  nextToken(nextToken: string) {
    this.query.next = nextToken;
    return this;
  }

  // include all items including closed accounts, deleted applications, destroyed assets, opted-out asset holdings, and closed-out application localstates
  includeAll(value = true) {
    this.query['include-all'] = value;
    return this;
  }
}
