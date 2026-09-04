import JSONRequest from '../jsonrequest';
import { HTTPClient } from '../../client';
import IntDecoding from '../../../types/intDecoding';

export default class LookupAccountByID extends JSONRequest {
  constructor(
    c: HTTPClient,
    intDecoding: IntDecoding,
    private account: string
  ) {
    super(c, intDecoding);
    this.account = account;
  }

  path() {
    return `/v2/accounts/${this.account}`;
  }

  // specific round to search
  round(round: number) {
    this.query.round = round;
    return this;
  }

  // include all items including closed accounts, deleted applications, destroyed assets, opted-out asset holdings, and closed-out application localstates
  includeAll(value = true) {
    this.query['include-all'] = value;
    return this;
  }
}
