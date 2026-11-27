/**
 * @overview routes
 * Application Routing
 *
 * This file initializes the links between route controllers and the express
 * HTTP server.
 *
 * @todo Pass authenticate and authorize middleware down through
 * controllers, allowing for modules to subscribe to different
 * levels of authority
 *
 * @requires winston
 * @requires uploader
 */

const winston = require('winston');
const upload = require('../lib/uploader');

// unclassified routes
const auth = require('../controllers/auth');
const tree = require('../controllers/tree');
const units = require('../controllers/units');
const system = require('../controllers/system');
const report = require('../controllers/report');

// admin routes
const users = require('../controllers/admin/users');
const projects = require('../controllers/admin/projects');
const enterprises = require('../controllers/admin/enterprises');
const employees = require('../controllers/admin/employees');
const services = require('../controllers/admin/services');
const suppliers = require('../controllers/admin/suppliers');
const functions = require('../controllers/admin/functions');
const grades = require('../controllers/admin/grades');
const languages = require('../controllers/admin/languages');
const locations = require('../controllers/admin/locations');
const groups = require('../controllers/groups');

// medical routes
const patients = require('../controllers/medical/patients');
const patientGroups = require('../controllers/medical/patientGroups');
const snis = require('../controllers/medical/snis');
const medicalReports = require('../controllers/medical/reports');
const diagnoses = require('../controllers/medical/diagnoses');

// stock and inventory routes
const inventory = require('../controllers/inventory');
const depots = require('../controllers/inventory/depots');
const inventoryReports = require('../controllers/inventory/reports');
const stock = require('../controllers/stock');
const stockReports = require('../controllers/stock/reports');

// finance routes
const trialBalance = require('../controllers/finance/trialBalance');
const ledger = require('../controllers/finance/ledger');
const fiscal = require('../controllers/finance/fiscal');
const gl = require('../controllers/finance/ledgers/general');
const purchases = require('../controllers/finance/purchases');
const debtors = require('../controllers/finance/debtors');
const cashboxes = require('../controllers/finance/cashboxes');
const exchange = require('../controllers/finance/exchange');
const cash = require('../controllers/finance/cash');
const priceList = require('../controllers/finance/priceList');
const billingServices = require('../controllers/finance/billingServices');
const accounts = require('../controllers/finance/accounts');
const subsidies = require('../controllers/finance/subsidies');
const patientInvoice = require('../controllers/finance/patientInvoice');
const financeReports = require('../controllers/finance/reports');
const discounts = require('../controllers/finance/discounts');
const debtorGroups = require('../controllers/finance/debtors/groups');
const currencies = require('../controllers/finance/currencies');
const vouchers = require('../controllers/finance/vouchers');
const creditorGroups = require('../controllers/finance/creditorGroups');
const creditors = require('../controllers/finance/creditors.js');
const journal = require('../controllers/finance/journal');
const transactionType = require('../controllers/admin/transactionType');
const generalLedger = require('../controllers/finance/generalLedger');
const financialPatient = require('../controllers/finance/patient');

const dashboardDebtors = require('../controllers/dashboard/debtorGroups');
const stats = require('../controllers/dashboard/stats');

// expose routes to the server.
exports.configure = function configure(app) {
  winston.debug('Configuring routes');

  // exposed to the outside without authentication
  app.get('/languages', languages.list);
  app.get('/projects', projects.list);
  app.get('/units', units.list);

  // auth gateway
  app.post('/auth/login', auth.login);
  app.get('/auth/logout', auth.logout);
  app.post('/auth/reload', auth.reload);

  // system and event helpers
  app.get('/system/events', system.events);
  app.get('/system/stream', system.stream);
  app.get('/system/information', system.info);

  // dashbord stats
  app.get('/patients/stats', stats.patients);
  app.get('/invoices/stats', stats.invoices);

  // exchange rate modules
  app.get('/exchange', exchange.list);
  app.post('/exchange', exchange.create);
  app.put('/exchange/:id', exchange.update);
  app.delete('/exchange/:id', exchange.delete);

  // API for locations
  app.get('/locations/villages', locations.villages);
  app.get('/locations/sectors', locations.sectors);
  app.get('/locations/provinces', locations.provinces);
  app.get('/locations/countries', locations.countries);
  app.post('/locations/countries', locations.create.country);
  app.post('/locations/provinces', locations.create.province);
  app.post('/locations/sectors', locations.create.sector);
  app.post('/locations/villages', locations.create.village);

  app.get('/locations/detail/:uuid', locations.detail);
  app.get('/locations/detail/', locations.list);

  app.put('/locations/villages/:uuid', locations.update.village);
  app.put('/locations/sectors/:uuid', locations.update.sector);
  app.put('/locations/provinces/:uuid', locations.update.province);
  app.put('/locations/countries/:uuid', locations.update.country);

  app.post('/groups/:key/:id', groups.updateSubscriptions);

  // API for account type routes CRUD
  app.get('/accounts/types', accounts.types.list);
  app.get('/accounts/types/:id', accounts.types.detail);
  app.post('/accounts/types', accounts.types.create);
  app.put('/accounts/types/:id', accounts.types.update);
  app.delete('/accounts/types/:id', accounts.types.remove);
  app.delete('/accounts/:id', accounts.remove);

  // API for account routes crud
  app.get('/accounts', accounts.list);
  app.get('/accounts/:id', accounts.detail);
  app.get('/accounts/:id/balance', accounts.getBalance);
  app.post('/accounts', accounts.create);
  app.put('/accounts/:id', accounts.update);

  // API for service routes
  app.post('/services', services.create);
  app.get('/services', services.list);
  app.get('/services/:id', services.detail);
  app.put('/services/:id', services.update);
  app.delete('/services/:id', services.remove);

  // API for subsidies routes crud
  app.get('/subsidies', subsidies.list);
  app.get('/subsidies/:id', subsidies.detail);
  app.post('/subsidies', subsidies.create);
  app.put('/subsidies/:id', subsidies.update);
  app.delete('/subsidies/:id', subsidies.remove);

  // API for journal
  app.get('/journal', journal.list);
  app.get('/journal/count', journal.count);
  app.get('/journal/:record_uuid', journal.getTransaction);
  app.post('/journal/:record_uuid/edit', journal.editTransaction);
  app.post('/journal/:uuid/reverse', journal.reverse);

  // API for general ledger
  app.get('/general_ledger', generalLedger.list);
  app.get('/general_ledger/accounts', generalLedger.listAccounts);

  // API for trial balance
  app.post('/trial_balance/data_per_account', trialBalance.getDataPerAccount);
  app.post('/trial_balance/checks', trialBalance.checkTransactions);
  app.post('/trial_balance/post_transactions', trialBalance.postToGeneralLedger);

  // ledger routes
  // TODO : needs renaming
  app.get('/ledgers/debtor/:id', ledger.compileDebtorLedger);
  app.get('/ledgers/debtor_group/:id', ledger.compileGroupLedger);
  app.get('/ledgers/employee_invoice/:id', ledger.compileEmployeeLedger);

  /* fiscal year controller */
  app.get('/fiscal', fiscal.list);
  app.get('/fiscal/date', fiscal.getFiscalYearsByDate);
  app.get('/fiscal/:id', fiscal.detail);
  app.get('/fiscal/:id/balance/:period_number', fiscal.getBalance);
  app.post('/fiscal/:id/opening_balance', fiscal.setOpeningBalance);
  app.post('/fiscal', fiscal.create);
  app.put('/fiscal/:id/closing', fiscal.closing);
  app.put('/fiscal/:id', fiscal.update);
  app.delete('/fiscal/:id', fiscal.remove);

  /* load a user's tree */
  app.get('/tree', tree.generate);

  // snis controller
  app.get('/snis/healthZones', snis.healthZones);

  // Employee management
  app.get('/employee_list/', employees.list);
  app.get('/holiday_list/:pp/:employee_id', employees.listHolidays);
  app.get('/getCheckHollyday/', employees.checkHoliday);
  app.get('/getCheckOffday/', employees.checkOffday);

  /*  Inventory and Stock Management */
  app.post('/inventory/metadata', inventory.createInventoryItems);
  app.get('/inventory/metadata', inventory.getInventoryItems);
  app.get('/inventory/metadata/search', inventory.searchInventoryItems);
  app.get('/inventory/metadata/:uuid', inventory.getInventoryItemsById);
  app.put('/inventory/metadata/:uuid', inventory.updateInventoryItems);

  // route for inventory list receipt

  /**
   * @deprecated: /inventory/:uuid/metadata route
   * these routes below are deprecated
   * use /inventory/metadata/:uuid route for the API instead
   */
  app.get('/inventory/:uuid/metadata', inventory.getInventoryItemsById);
  app.put('/inventory/:uuid/metadata', inventory.updateInventoryItems);

  /** Inventory Group API endpoints */
  app.post('/inventory/groups', inventory.createInventoryGroups);
  app.get('/inventory/groups', inventory.listInventoryGroups);
  app.get('/inventory/groups/:uuid', inventory.detailsInventoryGroups);
  app.get('/inventory/groups/:uuid/count', inventory.countInventoryGroups);
  app.put('/inventory/groups/:uuid', inventory.updateInventoryGroups);
  app.delete('/inventory/groups/:uuid', inventory.deleteInventoryGroups);

  /** Inventory Type API endpoints */
  app.post('/inventory/types', inventory.createInventoryTypes);
  app.get('/inventory/types', inventory.listInventoryTypes);
  app.get('/inventory/types/:id', inventory.detailsInventoryTypes);
  app.put('/inventory/types/:id', inventory.updateInventoryTypes);
  app.delete('/inventory/types/:id', inventory.deleteInventoryTypes);

  /** Inventory Units API endpoints */
  app.post('/inventory/units', inventory.createInventoryUnits);
  app.get('/inventory/units', inventory.listInventoryUnits);
  app.get('/inventory/units/:id', inventory.detailsInventoryUnits);
  app.put('/inventory/units/:id', inventory.updateInventoryUnits);
  app.delete('/inventory/units/:id', inventory.deleteInventoryUnits);


  /** @todo: These routes below need to be implemented */
  /*
  app.get('/inventory/consumption', inventory.getInventoryConsumption);
  app.get('/inventory/:uuid/consumption', inventory.getInventoryConsumptionById);

  app.get('/inventory/leadtimes', inventory.getInventoryLeadTimes);
  app.get('/inventory/:uuid/leadtimes', inventory.getInventoryLeadTimesById);

  app.get('/inventory/stock', inventory.getInventoryStockLevels);
  app.get('/inventory/:uuid/stock', inventory.getInventoryStockLevelsById);

  app.get('/inventory/expirations', inventory.getInventoryExpirations);
  app.get('/inventory/:uuid/expirations', inventory.getInventoryExpirationsById);

  app.get('/inventory/lots', inventory.getInventoryLots);
  app.get('/inventory/:uuid/lots', inventory.getInventoryLotsById);

  app.get('/inventory/status', inventory.getInventoryStatus);
  app.get('/inventory/:uuid/status', inventory.getInventoryStatusById);
  */

  /* Depot routes */
  app.get('/depots', depots.list);
  app.get('/depots/:uuid', depots.detail);
  app.put('/depots/:uuid', depots.update);
  app.post('/depots', depots.create);
  app.delete('/depots/:uuid', depots.remove);

  /* Depot distributions routes */
  app.get('/depots/:depotId/distributions', depots.listDistributions);
  app.get('/depots/:depotId/distributions/:uuid', depots.detailDistributions);
  app.post('/depots/:depotId/distributions', depots.createDistributions);

  /**
   * Depot inventories and lots routes
   * get the lots of a particular inventory item in the depot
   * @todo -- should this be renamed? /stock? /lots?
   */
  app.get('/depots/:depotId/inventory', depots.listAvailableLots);
  app.get('/depots/:depotId/inventory/:uuid', depots.detailAvailableLots);
  app.get('/depots/:depotId/expired', depots.listExpiredLots);
  app.get('/depots/:depotId/expirations', depots.listStockExpirations);

  // general ledger controller
  // transitioning to a more traditional angular application architecture
  app.get('/ledgers/general', gl.route);

  // currencies API
  app.get('/currencies', currencies.list);
  app.get('/currencies/:id', currencies.detail);

  // Patient invoice API
  app.get('/invoices', patientInvoice.list);
  app.post('/invoices', patientInvoice.create);
  app.get('/invoices/search', patientInvoice.search);
  app.get('/invoices/:uuid', patientInvoice.detail);
  app.get('/invoices/:uuid/balance', patientInvoice.balance);

  // route for invoice Report

  // reports API: Invoices (receipts)
  app.get('/reports/medical/patients', medicalReports.patientRegistrations);
  app.get('/reports/medical/patients/:uuid', medicalReports.receipts.patients);
  app.get('/reports/medical/patients/:uuid/checkins', medicalReports.patientCheckins);

  app.get('/reports/inventory/purchases/:uuid', inventoryReports.receipts.purchases);
  app.get('/reports/inventory/items', inventoryReports.reports.prices);

  app.get('/reports/finance/invoices', financeReports.invoices.report);
  app.get('/reports/finance/invoices/:uuid', financeReports.invoices.receipt);
  app.get('/reports/finance/invoices/:uuid/creditNote', financeReports.invoices.creditNote);
  app.get('/reports/finance/cash', financeReports.cash.report);
  app.get('/reports/finance/cash/:uuid', financeReports.cash.receipt);
  app.get('/reports/finance/debtors/aged', financeReports.debtors.aged);
  app.get('/reports/finance/debtors/open', financeReports.debtors.open);
  app.get('/reports/finance/vouchers', financeReports.vouchers.report);
  app.get('/reports/finance/vouchers/:uuid', financeReports.vouchers.receipt);
  app.get('/reports/finance/accounts/chart', financeReports.accounts.chart);
  app.get('/reports/finance/cashflow', financeReports.cashflow.document);
  app.get('/reports/finance/cashflow/services', financeReports.cashflow.byService);
  app.get('/reports/finance/financialPatient/:uuid', financeReports.patient);
  app.get('/reports/finance/income_expense', financeReports.incomeExpense.document);
  app.get('/reports/finance/balance', financeReports.balance.document);
  app.get('/reports/finance/account', financeReports.reportAccounts.document);
  app.get('/reports/finance/journal', financeReports.journal.postingReport);
  app.get('/reports/finance/posted_journal', financeReports.journal.postedReport);
  app.get('/reports/finance/clientsReport', financeReports.clientsReport.document);
  app.get('/reports/finance/general_ledger/', financeReports.generalLedger.report);
  app.get('/reports/finance/general_ledger/:account_id', financeReports.generalLedger.accountSlip);
  app.get('/reports/finance/creditors/aged', financeReports.creditors.aged);

  app.get('/reports/keys/:key', report.keys);

  // list of saved reports
  app.get('/reports/saved/:reportId', report.list);

  // lookup saved report document
  app.get('/reports/archive/:uuid', report.sendArchived);
  app.delete('/reports/archive/:uuid', report.deleteArchived);

  app.get('/dashboard/debtors', dashboardDebtors.getReport);

  // patient group routes
  app.get('/patients/groups', patientGroups.list);
  app.get('/patients/groups/:uuid', patientGroups.detail);
  app.post('/patients/groups', patientGroups.create);
  app.put('/patients/groups/:uuid', patientGroups.update);
  app.delete('/patients/groups/:uuid', patientGroups.remove);

  app.get('/patients/search', patients.search);

  // route specifically for quick searches on patient name, it will return minimal info
  app.get('/patients/search/name', patients.searchByName);

  app.get('/patients/visits', patients.visits.list);

  // Patients API
  app.get('/patients', patients.list);
  app.post('/patients', patients.create);
  app.get('/patients/:uuid', patients.detail);
  app.put('/patients/:uuid', patients.update);
  app.get('/patients/:uuid/groups', patients.groups.list);
  app.post('/patients/:uuid/groups', patients.groups.update);

  app.get('/patients/hospital_number/:id/exists', patients.hospitalNumberExists);

  app.get('/patients/:uuid/services', patients.billingServices);
  app.get('/patients/:uuid/subsidies', patients.subsidies);

  app.get('/patients/:uuid/documents', patients.documents.list);
  app.post('/patients/:uuid/documents', upload.middleware('docs', 'documents'), patients.documents.create);
  app.delete('/patients/:uuid/documents/all', patients.documents.deleteAll);
  app.delete('/patients/:uuid/documents/:documentUuid', patients.documents.delete);
  app.post('/patients/:uuid/pictures', upload.middleware('pics', 'pictures'), patients.pictures.set);

  app.get('/patients/visits/:uuid', patients.visits.detail);
  app.get('/patients/:patientUuid/visits/:uuid', patients.visits.detail);
  app.get('/patients/:uuid/visits', patients.visits.listByPatient);
  app.post('/patients/:uuid/visits/admission', patients.visits.admission);
  app.post('/patients/:uuid/visits/discharge', patients.visits.discharge);

  // misc patients financial routes
  app.get('/patients/:uuid/invoices/latest', patients.latestInvoice);
  app.get('/patients/:uuid/finance/balance', financialPatient.balance);

  // Barcode API
  app.get('/barcode/:key', report.barcodeLookup);

  // redirect the request directly to the relevent client document
  app.get('/barcode/redirect/:key', report.barcodeRedirect);

  // Debtors API
  /** @deprecated `/debtors/groups` please use `/debtor_groups` at the client side */
  /** @deprecated `/debtors/groups/:uuid` please use `/debtor_groups/:uuid` at the client side */
  app.get('/debtors/groups', debtorGroups.list);
  app.get('/debtors/groups/:uuid', debtorGroups.detail);
  app.get('/debtors/:uuid/invoices', debtors.invoices);
  app.put('/debtors/:uuid', debtors.update);
  app.get('/debtors/:uuid', debtors.detail);
  app.get('/debtors', debtors.list);

  // Debtor Groups API
  app.get('/debtor_groups', debtorGroups.list);
  app.get('/debtor_groups/:uuid', debtorGroups.detail);
  app.get('/debtor_groups/:uuid/invoices', debtorGroups.invoices);
  app.post('/debtor_groups', debtorGroups.create);
  app.put('/debtor_groups/:uuid', debtorGroups.update);
  app.delete('/debtor_groups/:uuid', debtorGroups.delete);


  // users controller
  app.get('/users', users.list);
  app.post('/users', users.create);
  app.get('/users/:id', users.detail);
  app.put('/users/:id', users.update);
  app.delete('/users/:id', users.delete);
  app.get('/users/:id/projects', users.projects.list);
  app.get('/users/:id/permissions', users.permissions.list);
  app.post('/users/:id/permissions', users.permissions.create);
  app.put('/users/:id/password', users.password);

  // projects controller
  app.get('/projects/:id', projects.detail);
  app.put('/projects/:id', projects.update);
  app.post('/projects', projects.create);
  app.delete('/projects/:id', projects.delete);

  // cashbox controller
  app.get('/cashboxes', cashboxes.list);
  app.get('/cashboxes/:id', cashboxes.detail);
  app.post('/cashboxes', cashboxes.create);
  app.put('/cashboxes/:id', cashboxes.update);
  app.delete('/cashboxes/:id', cashboxes.delete);

  // cashbox currencies
  app.get('/cashboxes/:id/currencies', cashboxes.currencies.list);
  app.get('/cashboxes/:id/currencies/:currencyId', cashboxes.currencies.detail);
  app.post('/cashboxes/:id/currencies', cashboxes.currencies.create);
  app.put('/cashboxes/:id/currencies/:currencyId', cashboxes.currencies.update);

  // price lists
  app.get('/prices', priceList.list);
  app.get('/prices/:uuid', priceList.details);
  app.post('/prices', priceList.create);
  app.put('/prices/:uuid', priceList.update);
  app.delete('/prices/:uuid', priceList.delete);

  // cash API
  app.get('/cash', cash.list);
  app.get('/cash/search', cash.search);
  app.get('/cash/:uuid', cash.detail);
  app.post('/cash', cash.create);
  app.put('/cash/:uuid', cash.update);
  app.get('/cash/references/:reference', cash.reference);
  app.get('/cash/checkin/:invoiceUuid', cash.checkInvoicePayment);

  // Enterprises api
  app.get('/enterprises', enterprises.list);
  app.get('/enterprises/:id', enterprises.detail);
  app.post('/enterprises', enterprises.create);
  app.put('/enterprises/:id', enterprises.update);

  // employees
  app.get('/employees', employees.list);
  app.get('/employees/:id', employees.detail);

  // @todo - use query strings
  app.get('/employees/:key/:value', employees.search);
  app.put('/employees/:id', employees.update);
  app.post('/employees', employees.create);

  // billing services
  app.get('/billing_services', billingServices.list);
  app.get('/billing_services/:id', billingServices.detail);
  app.post('/billing_services', billingServices.create);
  app.put('/billing_services/:id', billingServices.update);
  app.delete('/billing_services/:id', billingServices.delete);

  // discounts
  app.get('/discounts', discounts.list);
  app.get('/discounts/:id', discounts.detail);
  app.post('/discounts', discounts.create);
  app.put('/discounts/:id', discounts.update);
  app.delete('/discounts/:id', discounts.delete);

  // voucher api endpoint
  app.get('/vouchers', vouchers.list);
  app.get('/vouchers/:uuid', vouchers.detail);
  app.post('/vouchers', vouchers.create);

  // suppliers api
  app.get('/suppliers/search', suppliers.search);
  app.get('/suppliers', suppliers.list);
  app.get('/suppliers/:uuid', suppliers.detail);
  app.post('/suppliers', suppliers.create);
  app.put('/suppliers/:uuid', suppliers.update);

  // purchase
  app.post('/purchases', purchases.create);
  app.get('/purchases', purchases.list);
  app.get('/purchases/search', purchases.search);
  app.get('/purchases/:uuid', purchases.detail);
  app.put('/purchases/:uuid', purchases.update);
  app.get('/purchases/:uuid/stock_status', purchases.stockStatus);
  app.get('/purchases/:uuid/stock_balance', purchases.stockBalance);

  // functions api
  app.get('/functions', functions.list);
  app.get('/functions/:id', functions.detail);
  app.post('/functions', functions.create);
  app.put('/functions/:id', functions.update);
  app.delete('/functions/:id', functions.delete);

  // grades api
  app.get('/grades', grades.list);
  app.get('/grades/:uuid', grades.detail);
  app.post('/grades', grades.create);
  app.put('/grades/:uuid', grades.update);
  app.delete('/grades/:uuid', grades.delete);

  // creditor groups API
  app.post('/creditors/groups', creditorGroups.create);
  app.get('/creditors/groups', creditorGroups.list);
  app.get('/creditors/groups/:uuid', creditorGroups.detail);
  app.put('/creditors/groups/:uuid', creditorGroups.update);
  app.delete('/creditors/groups/:uuid', creditorGroups.remove);

  app.get('/creditors', creditors.list);
  app.get('/creditors/:uuid', creditors.detail);

  // transfer type API
  app.post('/transaction_type', transactionType.create);
  app.get('/transaction_type', transactionType.list);
  app.get('/transaction_type/:id', transactionType.detail);
  app.put('/transaction_type/:id', transactionType.update);
  app.delete('/transaction_type/:id', transactionType.remove);

  // @todo - this should use the JSON renderer instead of it's own route!
  app.get('/finance/cashflow', financeReports.cashflow.report);
  app.get('/finance/incomeExpense', financeReports.incomeExpense.report);

  // stock flux
  app.get('/stock/flux', stock.listStockFlux);

  // stock management API
  app.post('/stock/lots/movements', stock.createMovement);
  app.get('/stock/lots/movements', stock.listLotsMovements);
  app.post('/stock/lots', stock.createStock);
  app.get('/stock/lots', stock.listLots);
  app.get('/stock/lots/origins', stock.listLotsOrigins);

  app.get('/stock/lots/depots/', stock.listLotsDepot);
  app.get('/stock/inventories/depots', stock.listInventoryDepot);

  // stock integration
  app.post('/stock/integration', stock.createIntegration);

  // stock reports API
  app.get('/reports/stock/lots', stockReports.stockLotsReport);
  app.get('/reports/stock/movements', stockReports.stockMovementsReport);
  app.get('/reports/stock/inventories', stockReports.stockInventoriesReport);

  // stock receipts API
  app.get('/receipts/stock/exit_patient/:document_uuid', stockReports.stockExitPatientReceipt);
  app.get('/receipts/stock/exit_service/:document_uuid', stockReports.stockExitServiceReceipt);
  app.get('/receipts/stock/exit_depot/:document_uuid', stockReports.stockExitDepotReceipt);
  app.get('/receipts/stock/exit_loss/:document_uuid', stockReports.stockExitLossReceipt);

  app.get('/receipts/stock/entry_depot/:document_uuid', stockReports.stockEntryDepotReceipt);
  app.get('/receipts/stock/entry_purchase/:document_uuid', stockReports.stockEntryPurchaseReceipt);
  app.get('/receipts/stock/entry_integration/:document_uuid', stockReports.stockEntryIntegrationReceipt);

  app.get('/receipts/stock/adjustment/:document_uuid', stockReports.stockAdjustmentReceipt);

  app.get('/diagnoses', diagnoses.list);
};
