const client = require('../../client');
const mhc = require('./makeHealthCheck');
const lacbid = require('./lookupAccountByID');
const lact = require('./lookupAccountTransactions');
const lapp = require('./lookupApplications');
const lasb = require('./lookupAssetBalances');
const lasbid = require('./lookupAssetByID');
const last = require('./lookupAssetTransactions');
const lb = require('./lookupBlock');
const ltbid = require('./lookupTransactionByID');
const sfas = require('./searchForAssets');
const sfapp = require('./searchForApplications');
const sft = require('./searchForTransactions');
const sac = require('./searchAccounts');

class IndexerClient {
  constructor(
    token,
    baseServer = 'http://127.0.0.1',
    port = 8080,
    headers = {}
  ) {
    // workaround to allow backwards compatibility for multiple headers
    let tokenHeader = token;
    if (typeof tokenHeader === 'string') {
      tokenHeader = { 'X-Indexer-API-Token': tokenHeader };
    }

    this.c = new client.HTTPClient(tokenHeader, baseServer, port, headers);

    this.intDecoding = 'default';
  }

  /**
   * Set the default int decoding method for all JSON requests this client creates.
   * @param {"default" | "safe" | "mixed" | "bigint"} method The method to use when parsing the
   *   response for request. Must be one of "default", "safe", "mixed", or "bigint". See
   *   JSONRequest.setIntDecoding for more details about what each method does.
   */
  setIntEncoding(method) {
    this.intDecoding = method;
  }

  /**
   * Get the default int decoding method for all JSON requests this client creates.
   */
  getIntEncoding() {
    return this.intDecoding;
  }

  /**
   * Returns the health object for the service.
   */
  makeHealthCheck() {
    return new mhc.MakeHealthCheck(this.c, this.intDecoding);
  }

  /**
   * Returns holder balances for the given asset.
   * @param {number} index The asset ID to look up.
   */
  lookupAssetBalances(index) {
    return new lasb.LookupAssetBalances(this.c, this.intDecoding, index);
  }

  /**
   * Returns transactions relating to the given asset.
   * @param {number} index The asset ID to look up.
   */
  lookupAssetTransactions(index) {
    return new last.LookupAssetTransactions(this.c, this.intDecoding, index);
  }

  /**
   * Returns transactions relating to the given account.
   * @param {string} account The address of the account.
   */
  lookupAccountTransactions(account) {
    return new lact.LookupAccountTransactions(
      this.c,
      this.intDecoding,
      account
    );
  }

  /**
   * Returns the block for the passed round.
   * @param {number} round The number of the round to look up.
   */
  lookupBlock(round) {
    return new lb.LookupBlock(this.c, this.intDecoding, round);
  }

  /**
   * Returns information about the given transaction.
   * @param {string} txID The ID of the transaction to look up.
   */
  lookupTransactionByID(txID) {
    return new ltbid.LookupTransactionByID(this.c, this.intDecoding, txID);
  }

  /**
   * Returns information about the given account.
   * @param {string} account The address of the account to look up.
   */
  lookupAccountByID(account) {
    return new lacbid.LookupAccountByID(this.c, this.intDecoding, account);
  }

  /**
   * Returns information about the passed asset.
   * @param {number} index The ID of the asset ot look up.
   */
  lookupAssetByID(index) {
    return new lasbid.LookupAssetByID(this.c, this.intDecoding, index);
  }

  /**
   * Returns information about the passed application.
   * @param {number} index The ID of the application to look up.
   */
  lookupApplications(index) {
    return new lapp.LookupApplications(this.c, this.intDecoding, index);
  }

  /**
   * Returns information about indexed accounts.
   */
  searchAccounts() {
    return new sac.SearchAccounts(this.c, this.intDecoding);
  }

  /**
   * Returns information about indexed transactions.
   */
  searchForTransactions() {
    return new sft.SearchForTransactions(this.c, this.intDecoding);
  }

  /**
   * Returns information about indexed assets.
   */
  searchForAssets() {
    return new sfas.SearchForAssets(this.c, this.intDecoding);
  }

  /**
   * Returns information about indexed applications.
   */
  searchForApplications() {
    return new sfapp.SearchForApplications(this.c, this.intDecoding);
  }
}
module.exports = { IndexerClient };
