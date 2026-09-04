import JSONRequest from '../jsonrequest';

export default class SearchAccounts extends JSONRequest {
  // eslint-disable-next-line class-methods-use-this
  path() {
    return '/v2/accounts';
  }

  // filtered results should have an amount greater than this value, as int, representing microAlgos, unless an asset-id is provided, in which case units are in the asset's units
  currencyGreaterThan(greater: number) {
    this.query['currency-greater-than'] = greater;
    return this;
  }

  // filtered results should have an amount less than this value, as int, representing microAlgos, unless an asset-id is provided, in which case units are in the asset's units
  currencyLessThan(lesser: number) {
    this.query['currency-less-than'] = lesser;
    return this;
  }

  // limit for filter, as int
  limit(limit: number) {
    this.query.limit = limit;
    return this;
  }

  // asset ID to filter with, as int
  assetID(id: number) {
    this.query['asset-id'] = id;
    return this;
  }

  // used for pagination
  nextToken(nextToken: string) {
    this.query.next = nextToken;
    return this;
  }

  // specific round to search
  round(round: number) {
    this.query.round = round;
    return this;
  }

  // include accounts that use this spending key
  authAddr(authAddr: string) {
    this.query['auth-addr'] = authAddr;
    return this;
  }

  // filter for this application
  applicationID(applicationID: number) {
    this.query['application-id'] = applicationID;
    return this;
  }

  // include all items including closed accounts, deleted applications, destroyed assets, opted-out asset holdings, and closed-out application localstates
  includeAll(value = true) {
    this.query['include-all'] = value;
    return this;
  }
}
