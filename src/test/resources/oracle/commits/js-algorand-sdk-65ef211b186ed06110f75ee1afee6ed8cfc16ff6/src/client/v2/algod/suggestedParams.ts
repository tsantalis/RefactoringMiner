import JSONRequest from '../jsonrequest';

/**
 * Returns the common needed parameters for a new transaction, in a format the transaction builder expects
 */
export default class SuggestedParams extends JSONRequest {
  /* eslint-disable class-methods-use-this */
  path() {
    return '/v2/transactions/params';
  }

  prepare(body: Record<string, any>) {
    return {
      flatFee: false,
      fee: body.fee,
      firstRound: body['last-round'],
      lastRound: body['last-round'] + 1000,
      genesisID: body['genesis-id'],
      genesisHash: body['genesis-hash'],
    };
  }
  /* eslint-enable class-methods-use-this */
}
