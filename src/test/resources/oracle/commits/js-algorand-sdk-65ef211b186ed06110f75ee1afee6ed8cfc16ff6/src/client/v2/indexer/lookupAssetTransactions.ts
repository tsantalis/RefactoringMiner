import JSONRequest from '../jsonrequest';
import { HTTPClient } from '../../client';
import IntDecoding from '../../../types/intDecoding';
import { base64StringFunnel } from './lookupAccountTransactions';

export default class LookupAssetTransactions extends JSONRequest {
  constructor(c: HTTPClient, intDecoding: IntDecoding, private index: number) {
    super(c, intDecoding);
    this.index = index;
  }

  path() {
    return `/v2/assets/${this.index}/transactions`;
  }

  /**
   * notePrefix to filter with
   * @param prefix base64 string or uint8array
   */
  notePrefix(prefix: Uint8Array | string) {
    this.query['note-prefix'] = base64StringFunnel(prefix);
    return this;
  }

  // txtype to filter with, as string
  txType(type: string) {
    this.query['tx-type'] = type;
    return this;
  }

  // sigtype to filter with, as string
  sigType(type: string) {
    this.query['sig-type'] = type;
    return this;
  }

  // txid to filter with, as string
  txid(txid: string) {
    this.query.txid = txid;
    return this;
  }

  // round to filter with, as int
  round(round) {
    this.query.round = round;
    return this;
  }

  // min round to filter with, as int
  minRound(round: number) {
    this.query['min-round'] = round;
    return this;
  }

  // max round to filter with, as int
  maxRound(round: number) {
    this.query['max-round'] = round;
    return this;
  }

  // asset ID to filter with, as int
  assetID(id: number) {
    this.query['asset-id'] = id;
    return this;
  }

  // limit for filter, as int
  limit(limit: number) {
    this.query.limit = limit;
    return this;
  }

  // before-time to filter with, as rfc3339 string
  beforeTime(before: string) {
    this.query['before-time'] = before;
    return this;
  }

  // after-time to filter with, as rfc3339 string
  afterTime(after: string) {
    this.query['after-time'] = after;
    return this;
  }

  // filtered results should have an amount greater than this value, as int, representing asset units
  currencyGreaterThan(greater: number) {
    this.query['currency-greater-than'] = greater;
    return this;
  }

  // filtered results should have an amount less than this value, as int, representing asset units
  currencyLessThan(lesser: number) {
    this.query['currency-less-than'] = lesser;
    return this;
  }

  // combined with address, defines what address to filter on, as string
  addressRole(role: string) {
    this.query['address-role'] = role;
    return this;
  }

  // address to filter on as string
  address(address: string) {
    this.query.address = address;
    return this;
  }

  // whether or not to consider the close-to field as a receiver when filtering transactions, as bool. set to true to ignore close-to
  excludeCloseTo(exclude: boolean) {
    this.query['exclude-close-to'] = exclude;
    return this;
  }

  // used for pagination
  nextToken(nextToken: string) {
    this.query.next = nextToken;
    return this;
  }

  // whether or not to include rekeying transactions
  rekeyTo(rekeyTo: boolean) {
    this.query['rekey-to'] = rekeyTo;
    return this;
  }
}
