/**
* Debtors Controller
*
* This module is responsible for handling crud operations on the debtor
* table.  Currently, this is limited to update (debtors are created in
* association with other entities, such as patients).
*
* There are also some specialized queries such as looking up imbalanced
* invoices, and looking up the balance on a debtor's account.
*
* @module controllers/finance/debtors
*
* @requires lib/db
* @requires lib/errors/NotFound
* @requires lib/errors/BadRequest
*/


const q = require('q');
const uuid = require('node-uuid');

const db          = require('../../../lib/db');
const NotFound    = require('../../../lib/errors/NotFound');
const BadRequest  = require('../../../lib/errors/BadRequest');

const identifiers = require('../../../config/identifiers');

exports.list   = list;
exports.detail = detail;
exports.update = update;
exports.invoices = invoices;
exports.invoiceBalances = invoiceBalances;
exports.getDebtorInvoices = getDebtorInvoices;
exports.balance = balance;

/**
 * List of debtors
 */
function list(req, res, next) {
  const sql = `
    SELECT BUID(d.uuid) AS uuid, BUID(d.group_uuid) AS group_uuid, 
      d.text, map.text as hr_entity
    FROM debtor d 
    JOIN entity_map map ON map.uuid = d.uuid;
  `;

  db.exec(sql)
    .then(function (rows) {
      res.status(200).send(rows);
    })
    .catch(next);
}

/*
 * Detail of debtors
 */
function detail(req, res, next) {
  const uid = db.bid(req.params.uuid);

  lookupDebtor(uid)
  .then(function (debtor) {
    res.status(200).json(debtor);
  })
  .catch(next);
}

/**
 * Updates a debtor's details (particularly group_uuid)
 */
function update(req, res, next) {

  var sql =
    'UPDATE debtor SET ? WHERE uuid = ?';

  // delete the uuid if it exists
  delete req.body.uuid;

  // cache the incoming uuid for fast lookups
  const uid = db.bid(req.params.uuid);

  // escape the group_uuid if it exists
  if (req.body.group_uuid) {
    req.body.group_uuid = db.bid(req.body.group_uuid);
  }

  db.exec(sql, [req.body, uid])
  .then(function () {
    return lookupDebtor(uid);
  })
  .then(function (debtor) {
    res.status(200).json(debtor);
  })
  .catch(next)
  .done();
}

/**
 * Find a debtor by their uuid.
 *
 * @param {String} uid - the uuid of the debtor
 * @returns {Promise} promise resolving to the debtor object
 */
function lookupDebtor(uid) {
  const sql = `
    SELECT BUID(uuid) AS uuid, BUID(group_uuid) AS group_uuid, text
    FROM debtor
    WHERE uuid = ?;
  `;

  return db.exec(sql, [db.bid(uid)])
    .then(function (rows) {
      if (!rows.length) {
        throw new NotFound(`Could not find a debtor with uuid ${uuid.unparse(uid)}`);
      }
      return rows[0];
    });
}


/**
 * This function returns all invoices billed to a particular debtor.
 *
 * The algorithm works like this:
 *  1) Look up all invoices billed to that debtor
 *  2) Look up those invoices in the combined ledger, as well as payments
 *    against them.  These are summed into debits and credits.
 *
 * The database will optionally filter invoices based on whether they are
 * balanced (paid off) or not.
 *
 * NOTE - this function is not suitable for reporting, and should only be used
 * by modules that need up-to-the minute debtor status.  There is no control
 * over the dataset queried only the debtor
 *
 * @method invoices
 *
 * @todo - this function should be replaced by an SQL function stored in
 * procedures.sql for easy lookup
 */
function invoices(req, res, next) {
  const options = req.query;

  getDebtorInvoices(req.params.uuid)
    .then(function (uuids){
      return invoiceBalances(req.params.uuid, uuids, options);
    })
    .then(function (invoices) {
      res.status(200).json(invoices);
    })
    .catch(next)
    .done();
}

/**
 * This function sends back a list of invoices uuids
 * which belong to a particular debtor
 **/

function getDebtorInvoices (debtorUid){
  debtorUid = db.bid(debtorUid);
  const reversalVoucherType = 10;

  // get the debtor invoice uuids from the invoice table
  let sql =`
    SELECT BUID(invoice.uuid) as uuid
    FROM invoice
    WHERE debtor_uuid = ? AND invoice.reversed = 0
    ORDER BY invoice.date ASC, invoice.reference;
  `;

  return db.exec(sql, [debtorUid, reversalVoucherType])
    .then(uuids => {
      // if nothing found, return an empty array
      if (!uuids.length) { return []; }
      uuids = uuids.map(item => item.uuid);
      return uuids;
    });
}

function invoiceBalances(debtorUuid, uuids, paramOptions){
  let debtorUid = db.bid(debtorUuid);
  let options = paramOptions || {};

  let balanced =
     (options.balanced === '1') ? 'HAVING balance = 0' :
     (options.balanced === '0') ? 'HAVING balance <> 0' :
     '';
  let invoices = uuids.map(uuid => db.bid(uuid));

  if (uuids.length===0) {
    return [];
  }


  // select all invoice and payments against invoices from the combined ledger
  // @TODO this query is used in many places and is crucial for finding balances
  //       it currently uses 5 sub queries - this should be improved
  let sql = `
    SELECT BUID(i.uuid) AS uuid, CONCAT_WS('.', '${identifiers.INVOICE.key}', project.abbr, invoice.reference) AS reference,
      credit, debit, balance, BUID(entity_uuid) AS entity_uuid, invoice.date
    FROM (
      SELECT uuid, SUM(debit) AS debit, SUM(credit) AS credit, SUM(debit-credit) AS balance, entity_uuid
      FROM (
        SELECT record_uuid AS uuid, debit_equiv as debit, credit_equiv as credit, entity_uuid
        FROM posting_journal
        WHERE posting_journal.record_uuid IN (?) AND entity_uuid = ?

        UNION ALL

        SELECT record_uuid AS uuid, debit_equiv as debit, credit_equiv as credit, entity_uuid
         FROM  general_ledger
         WHERE general_ledger.record_uuid IN (?) AND entity_uuid = ?

         UNION ALL

        SELECT reference_uuid AS uuid, debit_equiv as debit, credit_equiv as credit, entity_uuid
        FROM posting_journal
        WHERE posting_journal.reference_uuid IN (?) AND entity_uuid = ?

        UNION ALL

        SELECT reference_uuid AS uuid, debit_equiv as debit, credit_equiv as credit, entity_uuid
        FROM general_ledger
        WHERE general_ledger.reference_uuid IN (?) AND entity_uuid = ?

      ) AS ledger
      GROUP BY ledger.uuid ${balanced}
    ) AS i
      JOIN invoice ON i.uuid = invoice.uuid
      JOIN project ON invoice.project_id = project.id
    ORDER BY invoice.date ASC, invoice.reference;
  `;

  return db.exec(sql, [invoices, debtorUid, invoices, debtorUid, invoices, debtorUid, invoices, debtorUid]);
}

/**
 * This function returns the balance of a debtors account with the hospital.
 *
 * The algorithm works like this:
 *  1) Look up all transaction lines associated with that debtor and sum the
 *  debits and credits.
 *
 * NOTE - this function is not suitable for reporting, and should only be used
 * by modules that need up-to-the minute debtor status.  There is no control
 * over the dataset queried only the debtor
 *
 * @method balance
 *
 * @todo - this function should be replaced by an SQL function stored in
 * procedures.sql for easy lookup
 */
function balance(debtorUid) {
  debtorUid = db.bid(debtorUid);

  // make sure the debtor exists
  var sql =
    'SELECT BUID(uuid) as uuid FROM debtor WHERE uuid = ?;';

  return db.exec(sql, [debtorUid])
  .then(function (rows) {

    // if the debtor doesn't exist, throw an error
    if (!rows.length) {
      throw new NotFound(
        `Could not find a debtor with uuid ${debtorUid}`
      );
    }

    // select all invoice and payments against invoices from the combined ledger
    sql = `
      SELECT SUM(debit - credit) AS balance, BUID(entity_uuid) as entity_uuid
      FROM (
        SELECT entity_uuid, record_uuid as uuid, debit_equiv as debit, credit_equiv as credit
        FROM posting_journal
        WHERE entity_uuid = ?

        UNION ALL

        SELECT entity_uuid, record_uuid as uuid, debit_equiv as debit, credit_equiv as credit
        FROM general_ledger
        WHERE entity_uuid = ?

      ) AS ledger
      GROUP BY ledger.entity_uuid;
    `;

    return db.one(sql, [debtorUid, debtorUid]);
  });
}