
/**
 * Dashboard Stats Controller
 *
 * @description This controller contains all API for stats
 * displayed on the landing page (Dashboard)
 *
 * @requires 'q'
 * @requires 'moment'
 * @requires '../../lib/db'
 */

// requirements
const Q = require('q');
const moment = require('moment');
const db = require('../../lib/db');

// expose to the API
exports.invoices = invoiceStat;
exports.patients = patientStats;

/**
 * @function invoiceStat
 *
 * @description This function help to get statistical data about invoices
 */
function invoiceStat(req, res, next) {
  const params = req.query;
  const bundle = {};

  // date handler
  const date = params.date ?
    moment(params.date).format('YYYY-MM-DD').toString() :
    moment().format('YYYY-MM-DD').toString();

  // cancelled transaction type
  const CANCELED_TRANSACTION_TYPE = 10;

  // date restriction
  const DATE_CLAUSE = '(MONTH(invoice.date) = MONTH(?) AND YEAR(invoice.date) = YEAR(?))';

  // query invoices which are not cancelled
  const sqlInvoices =
    `
    SELECT
      COUNT(*) AS total, SUM(cost) AS cost
    FROM
      invoice
     WHERE
      ${DATE_CLAUSE} AND
     invoice.uuid NOT IN (
      SELECT
        voucher.reference_uuid
      FROM
        voucher
      WHERE voucher.type_id = ${CANCELED_TRANSACTION_TYPE}
      );`;

  // query invoices
  const sqlBalance =
    `SELECT (debit - credit) as balance, project_id, cost
     FROM (
      (
        SELECT SUM(debit_equiv) as debit, SUM(credit_equiv) as credit, invoice.project_id, invoice.cost
        FROM posting_journal
        JOIN invoice ON posting_journal.record_uuid = invoice.uuid OR posting_journal.reference_uuid = invoice.uuid
        WHERE invoice.reversed = 0 AND ${DATE_CLAUSE} AND entity_uuid IS NOT NULL
        GROUP BY invoice.uuid
      ) UNION (
        SELECT SUM(debit_equiv) as debit, SUM(credit_equiv) as credit, invoice.project_id, invoice.cost
        FROM general_ledger
        JOIN invoice ON general_ledger.record_uuid = invoice.uuid OR general_ledger.reference_uuid = invoice.uuid
        WHERE invoice.reversed = 0 AND ${DATE_CLAUSE} AND entity_uuid IS NOT NULL
        GROUP BY invoice.uuid
      )
     ) AS i
     JOIN project ON i.project_id = project.id
     `;

  // promises requests
  const dbPromise = [db.exec(sqlInvoices, [date, date]), db.exec(sqlBalance, [date, date])];

  Q.all(dbPromise)
  .spread((invoices, invoiceBalances) => {
    // total invoices
    bundle.total = invoices[0].total;
    bundle.total_cost = invoices[0].cost;

    /**
     * Paid Invoices
     * Get list of invoices which are fully paid
     */
    const paid = invoiceBalances.filter(item => {
      return item.balance === 0;
    });
    bundle.invoice_paid_amount = paid.reduce((previous, current) => {
      return current.cost + previous;
    }, 0);
    bundle.invoice_paid = paid.length;

    /**
     * Partial Paid Invoices
     * Get list of invoices which are partially paid
     */
    const partial = invoiceBalances.filter(item => {
      return item.balance > 0 && item.balance !== item.cost;
    });
    bundle.invoice_partial_amount = partial.reduce((previous, current) => {
      return (current.cost - current.balance) + previous;
    }, 0);
    bundle.invoice_partial = partial.length;

    /**
     * Unpaid Invoices
     * Get list of invoices which are not paid
     */
    const unpaid = invoiceBalances.filter(item => {
      return item.balance > 0;
    });
    bundle.invoice_unpaid_amount = unpaid.reduce((previous, current) => {
      return current.balance + previous;
    }, 0);
    bundle.invoice_unpaid = unpaid.length;

    // server date
    bundle.date = date;

    res.status(200).json(bundle);
  })
  .catch(next)
  .done();
}

/**
 * @method patientStats
 *
 * @description This method help to get patient stats for visits and registrations
 */
function patientStats(req, res, next) {
  const params = req.query;
  const bundle = {};

  // date handler
  const date = params.date ?
    moment(params.date).format('YYYY-MM-DD').toString() :
    moment().format('YYYY-MM-DD').toString();

  const sqlPatient =
    `SELECT COUNT(uuid) AS total FROM patient
     WHERE MONTH(registration_date) = MONTH(?) AND YEAR(registration_date) = YEAR(?);`;

  const sqlVisit =
    `SELECT COUNT(v.uuid) AS total_visit
     FROM patient_visit v JOIN patient p ON p.uuid = v.patient_uuid
     WHERE MONTH(v.start_date) = MONTH(?) AND YEAR(v.start_date) = YEAR(?);`;

  const dbPromise = [
    db.exec(sqlPatient, [date, date]),
    db.exec(sqlVisit, [date, date]),
  ];

  Q.all(dbPromise)
  .spread((registration, visits) => {
    bundle.total = registration[0].total;
    bundle.total_visit = visits[0].total_visit;
    bundle.date = date;
    res.status(200).json(bundle);
  })
  .catch(next)
  .done();
}
