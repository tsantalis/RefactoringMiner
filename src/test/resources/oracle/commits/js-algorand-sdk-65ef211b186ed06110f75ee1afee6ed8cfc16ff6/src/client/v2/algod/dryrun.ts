import JSONRequest from '../jsonrequest';
import { HTTPClient } from '../../client';
import modelsv2 from './models/types';
import * as encoding from '../../../encoding/encoding';
import { setHeaders } from './compile';

export default class Dryrun extends JSONRequest {
  private blob: Uint8Array;

  constructor(c: HTTPClient, dr: modelsv2.DryrunSource) {
    super(c);
    this.blob = encoding.encode(dr.get_obj_for_encoding());
  }

  // eslint-disable-next-line class-methods-use-this
  path() {
    return '/v2/teal/dryrun';
  }

  /**
   * Executes dryrun
   * @param headers, optional
   */
  async do(headers = {}) {
    const txHeaders = setHeaders(headers);
    const res = await this.c.post(
      this.path(),
      Buffer.from(this.blob),
      txHeaders
    );
    return res.body;
  }
}
