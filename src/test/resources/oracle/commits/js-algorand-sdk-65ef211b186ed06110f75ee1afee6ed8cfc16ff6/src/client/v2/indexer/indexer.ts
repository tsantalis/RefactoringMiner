import ServiceClient from '../serviceClient';
import { HTTPClient } from '../../client';
import MakeHealthCheck from './makeHealthCheck';
import LookupAssetBalances from './lookupAssetBalances';
import LookupAssetTransactions from './lookupAssetTransactions';
import LookupAccountTransactions from './lookupAccountTransactions';
import LookupBlock from './lookupBlock';
import LookupTransactionByID from './lookupTransactionByID';
import LookupAccountByID from './lookupAccountByID';
import LookupAssetByID from './lookupAssetByID';
import LookupApplications from './lookupApplications';
import SearchAccounts from './searchAccounts';
import SearchForTransactions from './searchForTransactions';
import SearchForAssets from './searchForAssets';
import SearchForApplications from './searchForApplications';

export default class IndexerClient extends ServiceClient {
  constructor(
    token: string,
    baseServer = 'http://127.0.0.1',
    port = 8080,
    headers = {}
  ) {
    // workaround to allow backwards compatibility for multiple headers
    let tokenHeader: string | Record<string, string> = token;
    if (typeof tokenHeader === 'string') {
      tokenHeader = { 'X-Indexer-API-Token': tokenHeader };
    }

    const httpClient = new HTTPClient(tokenHeader, baseServer, port, headers);
    super(httpClient);
  }

  /**
   * Returns the health object for the service.
   */
  makeHealthCheck() {
    return new MakeHealthCheck(this.c, this.intDecoding);
  }

  /**
   * Returns holder balances for the given asset.
   * @param {number} index The asset ID to look up.
   */
  lookupAssetBalances(index: number) {
    return new LookupAssetBalances(this.c, this.intDecoding, index);
  }

  /**
   * Returns transactions relating to the given asset.
   * @param {number} index The asset ID to look up.
   */
  lookupAssetTransactions(index: number) {
    return new LookupAssetTransactions(this.c, this.intDecoding, index);
  }

  /**
   * Returns transactions relating to the given account.
   * @param {string} account The address of the account.
   */
  lookupAccountTransactions(account: string) {
    return new LookupAccountTransactions(this.c, this.intDecoding, account);
  }

  /**
   * Returns the block for the passed round.
   * @param {number} round The number of the round to look up.
   */
  lookupBlock(round: number) {
    return new LookupBlock(this.c, this.intDecoding, round);
  }

  /**
   * Returns information about the given transaction.
   * @param {string} txID The ID of the transaction to look up.
   */
  lookupTransactionByID(txID: string) {
    return new LookupTransactionByID(this.c, this.intDecoding, txID);
  }

  /**
   * Returns information about the given account.
   * @param {string} account The address of the account to look up.
   */
  lookupAccountByID(account: string) {
    return new LookupAccountByID(this.c, this.intDecoding, account);
  }

  /**
   * Returns information about the passed asset.
   * @param {number} index The ID of the asset ot look up.
   */
  lookupAssetByID(index: number) {
    return new LookupAssetByID(this.c, this.intDecoding, index);
  }

  /**
   * Returns information about the passed application.
   * @param {number} index The ID of the application to look up.
   */
  lookupApplications(index) {
    return new LookupApplications(this.c, this.intDecoding, index);
  }

  /**
   * Returns information about indexed accounts.
   */
  searchAccounts() {
    return new SearchAccounts(this.c, this.intDecoding);
  }

  /**
   * Returns information about indexed transactions.
   */
  searchForTransactions() {
    return new SearchForTransactions(this.c, this.intDecoding);
  }

  /**
   * Returns information about indexed assets.
   */
  searchForAssets() {
    return new SearchForAssets(this.c, this.intDecoding);
  }

  /**
   * Returns information about indexed applications.
   */
  searchForApplications() {
    return new SearchForApplications(this.c, this.intDecoding);
  }
}
