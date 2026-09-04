import JSONRequest from '../jsonrequest';
import { HTTPClient } from '../../client';
import IntDecoding from '../../../types/intDecoding';

export default class LookupTransactionByID extends JSONRequest {
  constructor(c: HTTPClient, intDecoding: IntDecoding, private txID: string) {
    super(c, intDecoding);
    this.txID = txID;
  }

  path() {
    return `/v2/transactions/${this.txID}`;
  }
}
