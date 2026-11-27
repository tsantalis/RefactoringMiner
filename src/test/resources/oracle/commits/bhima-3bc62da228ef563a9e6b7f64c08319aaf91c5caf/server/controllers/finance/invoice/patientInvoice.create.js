
/**
 * Patient Invoice - Create State
 * @module controllers/finance/patientInvoice
 *
 * This module is responsible for preparing a series of MySQL commands
 * (a transaction) for creating patient invoices, the transaction will be executed by the API handler, error and results
 * will be propagated through to the client.
 *
 * @todo multiple inserts during the staging process could have performance
 * implications on smaller data sets (larger seems to be negligible), the
 * data that must be staged could be passed in through a string to be concatted
 * into a prepared statement
 */
const db = require('../../../lib/db');
const uuid = require('node-uuid');
const util = require('../../../lib/util');
const _ = require('lodash');

module.exports = createInvoice;

/**
 * POST /invoices
 *
 * The function is responsible for billing a patient and calculating the total
 * due on their invoice.  It will create a record in the `invoice` table.
 *
 * Up to three additional tables may be affected:
 *  1. `invoice_items`
 *  2. `invoice_billing_service`
 *  3. `invoice_subsidy`
 *
 * The invoicing procedure of a patient's total invoice goes like this:
 *  1. First, the total sum of the invoice items are recorded as sent from the
 *  client.  The Patient Invoice module is allowed to edit the item costs as it
 *  sees fit, so we use the POSTed costs.
 *  2. Next, each billing service is added to the invoice by writing records to
 *  the `invoice_billing_service` table.  The cost of each billing service is
 *  determined by multiplying the billing service's value (as a percentage) to
 *  the total invoice cost.
 *  3. Finally, the subsidy for the bill is determined.  NOTE - as of #343, we
 *  are only allowing a single subsidy per invoice.  The array of subsidies is
 *  treated identically to the billing_services, except that it subtracts from
 *  the total bill amount.
 *
 * @todo - change the API to pass in only an array of billingService and subsidy
 * ids.
 */
function createInvoice(invoiceDetails) {
  const transaction = db.transaction();
  const invoiceUuid = db.bid(invoiceDetails.uuid || uuid.v4());

  let billingServices = processBillingServices(invoiceUuid, invoiceDetails.billingServices);
  let subsidies = processSubsidies(invoiceUuid, invoiceDetails.subsidies);
  let items = processInvoiceItems(invoiceUuid, invoiceDetails.items);

  const invoice = processInvoice(invoiceUuid, invoiceDetails);

  // 'stage' - make all data that will be required for writing an invoice available to the database procedures
  transaction.addQuery('CALL StageInvoice(?)', [invoice]);
  items.forEach(item =>
    transaction.addQuery('CALL StageInvoiceItem(?)', [item]));
  billingServices.forEach(billingService =>
    transaction.addQuery('CALL StageBillingService(?)', [billingService]));
  subsidies.forEach(subsidy =>
    transaction.addQuery('CALL StageSubsidy(?)', [subsidy]));

  // write and post invoice to the posting journal
  transaction.addQuery('CALL WriteInvoice(?)', [invoiceUuid]);
  transaction.addQuery('CALL PostInvoice(?)', [invoiceUuid]);
  return transaction;
}

function processInvoice(invoiceUuid, invoice) {
  // ensure date is sanitised
  if (invoice.date) {
    invoice.date = new Date(invoice.date);
  }

  // convert debtor uuid if required
  if (invoice.debtor_uuid) {
    invoice.debtor_uuid = db.bid(invoice.debtor_uuid);
  }

  invoice.uuid = invoiceUuid;

  // cleanup details not directly tied to invoice
  delete invoice.items;
  delete invoice.billingServices;
  delete invoice.subsidies;
  delete invoice.reference;

  const keys = [
    'date', 'cost', 'description', 'service_id',
    'debtor_uuid', 'project_id', 'user_id', 'uuid'
  ];

  return keys.map(key => invoice[key]);
}

/**
 * @method processBillingServices
 *
 * @description
 * Maps an array of billing service ids into billing service ids and invoice
 * UUID tuples.
 *
 * @param {Buffer} invoiceUuid - the binary invoice UUID
 * @param {Array|Undefined} subsidiesDetails - an array of billing service ids
 *   if they exist.
 * @returns {Array} - a possibly empty array billing service ids and invoice UUID pairs.
 *
 * @private
 */
function processBillingServices(invoiceUuid, billingServiceDetails) {
  let billingServices = billingServiceDetails || [];
  return billingServices.map(billingServiceId => [billingServiceId, invoiceUuid]);
}

/**
 * @method processSubsidies
 *
 * @description
 * Maps an array of subsidy ids into subsidy id and invoice uuid tuples
 *
 * @param {Buffer} invoiceUuid - the binary invoice uuid
 * @param {Array|Undefined} subsidiesDetails - an array of subsidy ids if they
 *   exist.
 * @returns {Array} - a possibly empty array subsidy ids and invoice UUID pairs.
 *
 * @private
 */
function processSubsidies(invoiceUuid, subsidiesDetails) {
  let subsidies = subsidiesDetails || [];
  return subsidies.map(subsidyId => [subsidyId, invoiceUuid]);
}

// process invoice items, transforming UUIDs into binary.
function processInvoiceItems(invoiceUuid, invoiceItems) {
  let items = invoiceItems || [];

  // make sure that invoice items have their uuids
  items.forEach(function (item) {
    item.uuid = db.bid(item.uuid || uuid.v4());
    item.invoice_uuid = invoiceUuid;

    // should every item have an inventory uuid?
    item.inventory_uuid = db.bid(item.inventory_uuid);

    // FIXME -- where is this supposed to have been defined?
    item.debit = 0;
  });

  // create a filter to align invoice item columns to the SQL columns
  let filter =
    util.take('uuid', 'inventory_uuid', 'quantity', 'transaction_price', 'inventory_price', 'debit', 'credit', 'invoice_uuid');

  // prepare invoice items for insertion into database
  return _.map(items, filter);
}
