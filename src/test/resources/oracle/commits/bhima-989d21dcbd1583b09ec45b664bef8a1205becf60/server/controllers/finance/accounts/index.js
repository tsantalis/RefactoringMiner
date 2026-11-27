/**
 * @module Accounts
 *
 * @description
 * Implements CRUD operations on the Account entity.
 *
 * This module implements the following routes:
 *  GET    /accounts
 *  GET    /accounts/:id
 *  GET    /accounts/:id/balance/
 *  POST   /accounts
 *  PUT    /accounts/:id
 *  DELETE    /accounts/:id
 *
 * @todo - move away from calling lookup() before action.  This is an
 * unnecessary database request.
 *
 * @requires db
 * @requires NotFound
 * @requires accounts/types
 */

const db = require('../../../lib/db');
const NotFound = require('../../../lib/errors/NotFound');
const BadRequest = require('../../../lib/errors/BadRequest');
const types = require('./types');


/**
 * @method create
 *
 * @description
 * Create a new account entity in the database.
 *
 * POST /accounts
 */
function create(req, res, next) {
  const data = req.body;
  const sql = 'INSERT INTO account SET ?';

  delete data.id;
  data.enterprise_id = req.session.enterprise.id;

  db.exec(sql, [data])
    .then(result => {
      res.status(201).json({ id : result.insertId });
    })
    .catch(next)
    .done();
}

/**
 * @method update
 *
 * @description
 * Updates an account in the database.
 *
 * PUT /accounts/:id
 */
function update(req, res, next) {
  const id = req.params.id;
  const data = req.body;
  const sql = 'UPDATE account SET ? WHERE id = ?';

  delete data.id;

  lookupAccount(id)
    .then(() => db.exec(sql, [data, id]))
    .then(() => lookupAccount(id))
    .then((account) => {
      res.status(200).json(account);
    })
    .catch(next)
    .done();
}


/**
 * @method remove
 *
 * @description
 * Removes an account in the database.
 *
 * DELETE /accounts/:id
 */
function remove(req, res, next) {
  const sql = `SELECT COUNT(id) AS childrens FROM account WHERE parent = ?;`;
  db.exec(sql, [req.params.id])
    .then((rows) => {
      if (rows[0].childrens > 0) {
        throw new BadRequest(
          `Could not delete account with id: ${req.params.id}. This account contains child accounts.`
        );
      }

      const sqlDelete = 'DELETE FROM account WHERE id = ?;';
      return db.exec(sqlDelete, [req.params.id]);
    })
    .then(result => {
      if (!result.affectedRows) {
        throw new NotFound(`Could not find an Account with id ${req.params.id}.`);
      }

      res.sendStatus(204);
    })
    .catch(next)
    .done();
}


/**
 * @method list
 *
 * @description
 * Lists all accounts in the database.
 *
 * GET /accounts
 */
function list(req, res, next) {
  let sql =
    'SELECT a.id, a.number, a.label, a.locked, a.type_id, a.parent FROM account AS a';

  let locked;

  if (req.query.detailed === '1') {
    sql = `
      SELECT a.id, a.enterprise_id, a.locked, a.cc_id, a.pc_id, a.created,
        a.classe, a.is_asset, a.reference_id, a.is_brut_link, a.is_charge,
        a.number, a.label, a.parent, a.type_id, a.is_title, at.type,
        at.translation_key, cc.text AS cost_center_text, pc.text AS profit_center_text
      FROM account AS a JOIN account_type AS at ON a.type_id = at.id
      LEFT JOIN cost_center AS cc ON a.cc_id = cc.id
      LEFT JOIN profit_center AS pc ON a.pc_id = pc.id
    `;
  }

  // convert locked to a number if it exists
  if (req.query.locked) {
    locked = Number(req.query.locked);
  }

  // if locked is a number, filter on it
  if (!isNaN(locked)) {
    sql += ` WHERE a.locked = ${locked}`;
  }

  sql += ` ORDER BY a.number;`;

  db.exec(sql)
  .then((rows) => {
    res.status(200).json(rows);
  })
  .catch(next)
  .done();
}

/**
 * @method detail
 *
 * @description
 * Retrieves the details of a single account from the database by its id.
 *
 * GET /accounts/:id
 */
function detail(req, res, next) {
  lookupAccount(req.params.id)
    .then((account) => {
      res.status(200).json(account);
    })
    .catch(next)
    .done();
}

/**
 * @method getBalance
 *
 * @description
 * Returns the current balance of an account by query the entire freakin'
 * posting journal.
 *
 * @todo - use period totals
 *
 * GET /accounts/:id/balance
 */
function getBalance(req, res, next) {
  const id = req.params.id;
  let optional = '';
  const params = [id];

  // include the posting journal with a switch
  if (req.query.journal === '1') {
    optional = `
      UNION ALL
        SELECT pj.account_id, IFNULL(SUM(pj.debit), 0) AS debit,
        IFNULL(SUM(pj.credit), 0) AS credit, IFNULL((pj.debit - pj.credit), 0) AS balance
      FROM posting_journal AS pj
      WHERE pj.account_id = ? GROUP BY pj.account_id
    `;
    params.push(id);
  }

  const sql = `
    SELECT t.account_id, IFNULL(SUM(t.debit), 0) AS debit, IFNULL(SUM(t.credit), 0) AS credit,
      IFNULL(t.balance, 0) AS balance
    FROM (
      SELECT gl.account_id, IFNULL(SUM(gl.debit), 0) AS debit,
        IFNULL(SUM(gl.credit), 0) AS credit,
        IFNULL((gl.debit - gl.credit), 0) AS balance
      FROM general_ledger AS gl
      WHERE gl.account_id = ? GROUP BY gl.account_id ${optional}
    ) AS t GROUP BY t.account_id;
  `;

  lookupAccount(id)
    .then(() => db.exec(sql, params))
    .then((rows) => {
      const response = (rows.length === 0) ?
        { account_id : id, debit : 0, credit : 0, balance : 0 } :
        rows[0];

      res.status(200).json(response);
    })
    .catch(next)
    .done();
}

/**
 * @method lookupAccount
 *
 * @description
 * Returns the account details matching the id. If there is no matching account,
 * the function throws a NotFound() error.
 *
 * @param {Number} id - the id of the accout to fetch in the database
 * @returns {Promise} - a promise resolving to the account object.
 */
function lookupAccount(id) {
  let sql = `
    SELECT a.id, a.enterprise_id, a.locked, a.cc_id, a.pc_id, a.created,
      a.classe, a.is_asset, a.reference_id, a.is_brut_link, a.is_charge,
      a.number, a.label, a.parent, a.type_id, a.is_title, at.type,
      at.translation_key, cc.text AS cost_center_text, pc.text AS profit_center_text
    FROM account AS a JOIN account_type AS at ON a.type_id = at.id
    LEFT JOIN cost_center AS cc ON a.cc_id = cc.id
    LEFT JOIN profit_center AS pc ON a.pc_id = pc.id
    `;

  sql += id ? ' WHERE a.id = ? ORDER BY CAST(a.number AS CHAR(15)) ASC;' : ' ORDER BY CAST(a.number AS CHAR(15)) ASC;';

  return db.exec(sql, id)
    .then(rows => {
      if (rows.length === 0) {
        throw new NotFound(`Could not find account with id ${id}.`);
      }

      return id ? rows[0] : rows;
    });
}


/**
 * @method processAccountDepth
 * @description get the depth of an account
 * @param {array} accounts list of accounts
 * @return {array} accounts the updated list of accounts with depths
 */
function processAccountDepth(accounts) {
  // root node
  const ROOT_NODE = 0;

  // build the account tree
  const tree = getChildren(accounts, ROOT_NODE);

  // return a flattened tree (in order)
  const processedAccounts = flatten(tree);

  // remove the children property after flattening to avoid recursive references
  processedAccounts.forEach(account => delete account.children);

  return processedAccounts;
}

/**
 * @function getChildren
 * @description return the children accounts of an account given
 * @param {array} accounts list of accounts
 * @param {number} parentId The parent id
 */
function getChildren(accounts, parentId) {
  if (accounts.length === 0) { return null; }

  const children = accounts.filter(account => account.parent === parentId);

  children.forEach((account) => {
    account.children = getChildren(accounts, account.id);
  });

  return children;
}

/**
 * @function flatten
 * @description return a flatten array
 * @param {array} tree list of accounts as tree
 * @param {number} depth A depth
 */
function flatten(tree, depth) {
  let currentDepth = isNaN(depth) ? -1 : depth;
  currentDepth += 1;

  return tree.reduce((array, node) => {
    node.depth = currentDepth;
    const items = [node].concat(node.children ? flatten(node.children, currentDepth) : []);
    return array.concat(items);
  }, []);
}

exports.list = list;
exports.create = create;
exports.update = update;
exports.detail = detail;
exports.getBalance = getBalance;
exports.types = types;
exports.lookupAccount = lookupAccount;
exports.processAccountDepth = processAccountDepth;
exports.list = list;
exports.remove = remove;
