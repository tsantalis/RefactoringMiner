import JSONRequest from '../jsonrequest';
import { HTTPClient } from '../../client';
import IntDecoding from '../../../types/intDecoding';

export default class Proof extends JSONRequest {
  constructor(
    c: HTTPClient,
    intDecoding: IntDecoding,
    private round: number,
    private txID: string
  ) {
    super(c, intDecoding);

    this.round = round;
    this.txID = txID;
  }

  path() {
    return `/v2/blocks/${this.round}/transactions/${this.txID}/proof`;
  }
}
