
/**
 * Patient Invoice API Controller
 *
 *@module controllers/finance/patientInvoice
 *
 * @todo (required) major bug - Invoice items are entered based on order or attributes sent from client
 *      this doesn't seem to be consistent as of 2.X
 * @todo GET /invoices/patient/:uuid - retrieve all patient invoices for a specific patient
 *    - should this be /patients/:uuid/invoices?
 * @todo Factor in subsidies, this depends on price lists and billing services infrastructure
 */

const Q = require('q');
const moment = require('moment');
const uuid = require('node-uuid');
const _ = require('lodash');
const identifiers = require('../../config/identifiers');
const util = require('../../lib/util');
const db = require('../../lib/db');
const barcode = require('../../lib/barcode');
const FilterParser = require('../../lib/filter');
const NotFound = require('../../lib/errors/NotFound');
const BadRequest = require('../../lib/errors/BadRequest');
const createInvoice = require('./invoice/patientInvoice.create');
const Debtors = require('./debtors');

const entityIdentifier = identifiers.INVOICE.key;
const CREDIT_NOTE_ID = 10;

/** Retrieves a list of all patient invoices (accepts ?q delimiter). */
exports.list = list;

/** Retrieves details for a specific patient invoice. */
exports.detail = detail;

/** Write a new patient invoice record and attempt to post it to the journal. */
exports.create = create;

/** Filter the patient invoice table by any column via query strings */
exports.search = search;

/** Expose lookup invoice for other controllers to use internally */
exports.lookupInvoice = lookupInvoice;

exports.find = find;

/** find the balance on an invoice due the particular debtor */
exports.balance = balance;

/** Expose lookup invoice credit note for other controllers to use internally */
exports.lookupInvoiceCreditNote = lookupInvoiceCreditNote;

/**
 * list
 *
 * Retrieves a list of all patient invoices in the database
 */
function list(req, res, next) {
  find({})
    .then(function (invoices) {
      res.status(200).json(invoices);
    })
    .catch(next)
    .done();
}

/**
 * @method balance
 *
 * @description
 * This uses the lookupInvoice() and the invoiceBalances methods to find the
 * balance on a single invoice due to a debtor.
 *
 * @todo(jniles) write tests!
 */
function balance(req, res, next) {
  lookupInvoice(req.params.uuid)
    .then(invoice => {
      return Debtors.invoiceBalances(invoice.debtor_uuid, [req.params.uuid]);
    })
    .then(rows => {
      res.status(200).json(rows[0]);
    })
    .catch(next)
    .done();
}


/**
 * @method lookupInvoice
 *
 * @description
 * Find an invoice by id in the database.
 *
 * @param {string} invoiceUuid - the uuid of the invoice in question
 */
function lookupInvoice(invoiceUuid) {
  let record = {};
  const buid = db.bid(invoiceUuid);

  const invoiceDetailQuery =
    `SELECT 
      BUID(invoice.uuid) as uuid, CONCAT_WS('.', '${identifiers.INVOICE.key}', 
      project.abbr, invoice.reference) AS reference, invoice.cost, 
      invoice.description, BUID(invoice.debtor_uuid) AS debtor_uuid,
      patient.display_name AS debtor_name,   BUID(patient.uuid) as patient_uuid,
      invoice.user_id, invoice.date, user.display_name, invoice.service_id, 
      service.name AS serviceName, enterprise.currency_id
    FROM invoice
    LEFT JOIN patient ON patient.debtor_uuid = invoice.debtor_uuid
    JOIN service ON invoice.service_id = service.id
    JOIN project ON project.id = invoice.project_id
    JOIN enterprise ON enterprise.id = project.enterprise_id
    JOIN user ON user.id = invoice.user_id
    WHERE invoice.uuid = ?;`;

  const invoiceItemsQuery =
    `SELECT 
      BUID(invoice_item.uuid) as uuid, invoice_item.quantity, invoice_item.inventory_price,
      invoice_item.transaction_price, inventory.code, inventory.text, 
      inventory.consumable
    FROM invoice_item
    LEFT JOIN inventory ON invoice_item.inventory_uuid = inventory.uuid
    WHERE invoice_uuid = ?`;

  const invoiceBillingQuery =
    `SELECT 
      invoice_billing_service.value, billing_service.label, billing_service.value AS billing_value, 
      SUM(invoice_item.quantity * invoice_item.transaction_price) AS invoice_cost
    FROM invoice_billing_service
    JOIN billing_service ON billing_service.id = invoice_billing_service.billing_service_id
    JOIN invoice_item ON invoice_item.invoice_uuid = invoice_billing_service.invoice_uuid
    WHERE invoice_billing_service.invoice_uuid = ?
    GROUP BY billing_service.id`;

  const invoiceSubsidyQuery = `
    SELECT invoice_subsidy.value, subsidy.label, subsidy.value AS subsidy_value
    FROM invoice_subsidy
    JOIN subsidy ON subsidy.id = invoice_subsidy.subsidy_id
    WHERE invoice_subsidy.invoice_uuid = ?;
  `;

  return db.one(invoiceDetailQuery, [buid], invoiceUuid, 'invoice')
    .then(invoice => {
      record = invoice;
      return db.exec(invoiceItemsQuery, [buid]);
    })
    .then(rows => {
      record.items = rows;
      return db.exec(invoiceBillingQuery, [buid]);
    })
    .then(rows => {
      record.billing = rows;
      return db.exec(invoiceSubsidyQuery, [buid]);
    })
    .then(rows => {
      record.subsidy = rows;

      // provide barcode string to be rendered by client/ receipts
      record.barcode = barcode.generate(entityIdentifier, record.uuid);
      return record;
    });
}

/**
 * @todo Read the balance remaining on the debtors account given the invoice as an auxiliary step
 */
function detail(req, res, next) {
  // this assumes a value must be past for this route to initially match
  lookupInvoice(req.params.uuid)
    .then(function (record) {
      res.status(200).json(record);
    })
    .catch(next)
    .done();
}

function create(req, res, next) {
  const invoice = req.body.invoice;
  invoice.user_id = req.session.user.id;

  const hasInvoiceItems = (invoice.items && invoice.items.length > 0);

  // detect missing items early and respond with an error
  if (!hasInvoiceItems) {
    next(
      new BadRequest(`An invoice must be submitted with invoice items.`)
    );
    return;
  }

  const preparedTransaction = createInvoice(invoice);
  preparedTransaction.execute()
    .then(() => {
      res.status(201).json({ uuid : uuid.unparse(invoice.uuid) });
    })
    .catch(next)
    .done();
}

function find(options) {
  // ensure expected options are parsed as binary
  db.convert(options, ['patientUuid']);

  const filters = new FilterParser(options, { tableAlias : 'invoice' });

  // @FIXME Remove this with client side filter design
  delete options.patientNames;

  const sql = `
    SELECT BUID(invoice.uuid) as uuid, invoice.project_id, invoice.date,
      patient.display_name as patientName, invoice.cost, BUID(invoice.debtor_uuid) as debtor_uuid,
      CONCAT_WS('.', '${identifiers.INVOICE.key}', project.abbr, invoice.reference) AS reference,
      CONCAT_WS('.', '${identifiers.PATIENT.key}', project.abbr, patient.reference) AS patientReference,
      service.name as serviceName, user.display_name, invoice.user_id, invoice.reversed
    FROM invoice
    LEFT JOIN patient ON invoice.debtor_uuid = patient.debtor_uuid
    JOIN service ON service.id = invoice.service_id
    JOIN user ON user.id = invoice.user_id
    JOIN project ON project.id = invoice.project_id
  `;

  filters.equals('patientUuid', 'uuid', 'patient');
  filters.dateFrom('billingDateFrom', 'date');
  filters.dateTo('billingDateTo', 'date');

  filters.custom(
    'cash_uuid',
    'invoice.uuid IN (SELECT cash_item.invoice_uuid FROM cash_item WHERE cash_item.cash_uuid = HUID(?))'
  );

  filters.period('defaultPeriod', 'date');

  const referenceStatement = `CONCAT_WS('.', '${identifiers.INVOICE.key}', project.abbr, invoice.reference) = ?`;
  filters.custom('reference', referenceStatement);

  const patientReferenceStatement = `CONCAT_WS('.', '${identifiers.PATIENT.key}', project.abbr, patient.reference) = ?`;
  filters.custom('patientReference', patientReferenceStatement);

  // @TODO Support ordering query (reference support for limit)?
  filters.setOrder('ORDER BY invoice.date DESC, invoice.reference DESC');

  const query = filters.applyQuery(sql);
  const parameters = filters.parameters();
  return db.exec(query, parameters);
}

/**
 * Searches for a invoice by query parameters provided.
 *
 * GET /invoices/search
 */
function search(req, res, next) {
  find(req.query)
    .then(function (rows) {
      res.status(200).json(rows);
    })
    .catch(next)
    .done();
}

/**
 * CreditNote for an invoice
 */
function lookupInvoiceCreditNote(invoiceUuid) {
  const buid = db.bid(invoiceUuid);
  const sql = `
    SELECT BUID(v.uuid) AS uuid, v.date, CONCAT_WS('.', '${identifiers.VOUCHER.key}', p.abbr, v.reference) AS reference,
      v.currency_id, v.amount, v.description, v.reference_uuid, u.display_name
    FROM voucher v
    JOIN project p ON p.id = v.project_id
    JOIN user u ON u.id = v.user_id
    JOIN invoice i ON i.uuid = v.reference_uuid
    WHERE v.type_id = ${CREDIT_NOTE_ID} AND v.reference_uuid = ?`;
  return db.one(sql, [buid])
    .then(creditNote => {
      return creditNote;
    })
    .catch(err => {
      // db.one throw a critical error when there is not any record
      // and it must be handled
      return null;
    });
}
