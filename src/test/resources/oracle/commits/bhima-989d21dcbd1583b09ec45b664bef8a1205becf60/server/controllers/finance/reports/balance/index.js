/**
 * Balance Controller
 *
 * This controller is responsible for processing the balance report.
 *
 * @module finance/balance
 *
 * @requires lodash
 * @requires lib/db
 * @requires lib/ReportManager
 * @requires lib/errors/BadRequest
 */

const _ = require('lodash');
const db = require('../../../../lib/db');
const ReportManager = require('../../../../lib/ReportManager');

// report template
const TEMPLATE = './server/controllers/finance/reports/balance/report.handlebars';

const TITLE_ACCOUNT_TYPE = 4;

// expose to the API
exports.document = document;

/**
 * @function document
 * @description process and render the balance report document
 */
function document(req, res, next) {
  const params = req.query;
  const session = {};
  let report;

  // date option
  if (params.dateFrom && params.dateTo) {
    session.dateFrom = new Date(params.dateFrom);
    session.dateTo = new Date(params.dateTo);
  } else {
    session.date = new Date(params.date);
  }

  session.classe = params.classe;
  session.classe_name = params.classe_name;
  session.enterprise = req.session.enterprise;

  _.defaults(params, { orientation : 'landscape', user : req.session.user });

  try {
    report = new ReportManager(TEMPLATE, req.session, params);
  } catch (e) {
    next(e);
    return;
  }

  let accounts;
  let totals;

  params.enterpriseId = session.enterprise.id;

  balanceReporting(params)
    .then(balances => processAccounts(balances, accounts, totals))
    .then((result) => report.render({ accounts : result.accounts, totals : result.totals, session }))
    .then((result) => {
      res.set(result.headers).send(result.report);
    })
    .catch(next)
    .done();
}

/**
 * @method processAccounts
 * @description process and format accounts balance
 * @param {object} balances The result of balanceReporting function
 */
function processAccounts(balances, a, totals) {
  // format and process opening balance for accounts
  const accounts = balances.beginning.reduce((account, row) => {
    const id = row.number;
    const obj = {};
    account[id] = obj;
    const sold = getSold(row);
    obj.label = row.label;
    obj.number = row.number;
    obj.beginDebit = sold.debit;
    obj.beginCredit = sold.credit;
    obj.middleDebit = 0;
    obj.middleCredit = 0;
    obj.is_charge = row.is_charge;
    obj.is_asset = row.is_asset;
    return account;
  }, {});

  // format and process the monthly balance for accounts
  balances.middle.forEach((row) => {
    const account = accounts[row.number] || {};
    account.middleDebit = row.debit;
    account.middleCredit = row.credit;
    account.label = row.label;
    account.is_charge = row.is_charge;
    account.is_asset = row.is_asset;
    accounts[row.number] = account;
  });

  Object.keys(accounts).forEach((item) => {
    accounts[item].endDebit = 0;
    accounts[item].endCredit = 0;
    const sold = (accounts[item].beginDebit || 0 - accounts[item].beginCredit || 0)
      + (accounts[item].middleDebit - accounts[item].middleCredit);
    if (sold < 0) {
      accounts[item].endCredit = sold * -1;
    } else {
      accounts[item].endDebit = sold;
    }
  });

  // process for getting totals
  const vtotals = Object.keys(accounts)
    .reduce((t, key) => {
      const account = accounts[key];
      t.beginDebit += (account.beginDebit || 0);
      t.beginCredit += (account.beginCredit || 0);
      t.middleDebit += (account.middleDebit || 0);
      t.middleCredit += (account.middleCredit || 0);
      t.endDebit += (account.endDebit || 0);
      t.endCredit += (account.endCredit || 0);
      return totals;
    }, {
      beginDebit   : 0,
      beginCredit  : 0,
      middleDebit  : 0,
      middleCredit : 0,
      endDebit     : 0,
      endCredit    : 0,
    });

  return { accounts, totals : vtotals };
}

/**
 * @function getSold
 * @description return the balance of an account
 * @param {object} object An object from the array returned by balanceReporting function
 */
function getSold(item) {
  let debit = 0;
  let credit = 0;
  let sold = 0;

  if (item.is_asset === 1 || item.is_charge === 1) {
    sold = item.debit - item.credit;
    if (sold < 0) {
      credit = sold * -1;
    } else {
      debit = sold;
    }
  } else {
    sold = item.credit - item.debit;
    if (sold < 0) {
      debit = sold * -1;
    } else {
      credit = sold;
    }
  }
  return { debit, credit };
}

/**
 * @function balanceReporting
 * @description return the balance needed according the given params
 * @param {object} params An object which contains dates range and the account class
 */
function balanceReporting(params) {
  const query = params;
  const data = {};

  const hasClasse = (query.classe !== '*');
  const dateRange = (query.dateFrom && query.dateTo);

  // gets the amount up to the current period
  let sql = `
    SELECT a.number, a.id, a.label, a.type_id, a.is_charge, a.is_asset, SUM(pt.credit) AS credit, SUM(pt.debit) AS debit
    FROM period_total AS pt JOIN account AS a ON pt.account_id = a.id
    JOIN period AS p ON pt.period_id = p.id
    WHERE p.end_date <= DATE(?) AND pt.enterprise_id = ?
    ${hasClasse ? 'AND a.classe = ? ' : ''}
    GROUP BY a.id `;

  if (dateRange) {
    sql = `
    SELECT a.number, a.id, a.label, a.type_id, a.is_charge, a.is_asset, SUM(pt.credit) AS credit, SUM(pt.debit) AS debit
    FROM period_total AS pt JOIN account AS a ON pt.account_id = a.id
    JOIN period AS p ON pt.period_id = p.id
    WHERE p.start_date >= DATE(?) AND start_date < DATE(?) AND pt.enterprise_id = ?
     ${hasClasse ? 'AND a.classe = ? ' : ''}
    GROUP BY a.id `;
  }

  const queryParameters = (dateRange) ?
    [query.dateFrom, query.dateTo, query.enterpriseId, query.classe] :
    [query.date, query.enterpriseId, query.classe];

  return db.exec(sql, queryParameters)
  .then((rows) => {
    data.beginning = rows;

    sql = `
      SELECT a.number, a.label, a.id, a.type_id, a.is_charge, a.is_asset,
        SUM(pt.credit) AS credit, SUM(pt.debit) AS debit
      FROM period_total AS pt JOIN account AS a ON pt.account_id = a.id
      JOIN period AS p ON pt.period_id = p.id
      WHERE DATE(?) BETWEEN p.start_date AND p.end_date AND pt.enterprise_id = ?
       ${hasClasse ? 'AND a.classe = ? ' : ''}
      GROUP BY a.id;`;

    query.date = (dateRange) ? query.dateTo : query.date;

    return db.exec(sql, [query.date, query.enterpriseId, query.classe]);
  })
  .then((rows) => {
    data.middle = rows;
    return data;
  })
  .then(() => {
    // Manual mixing

    // fill with zero if all accounts
    sql =
      `SELECT a.number, a.id, a.label, a.type_id, a.is_charge, a.is_asset, '0' AS credit, '0' AS debit
       FROM account a WHERE a.type_id <> ${TITLE_ACCOUNT_TYPE} AND a.locked = 0;`;

    return query.accountOption === 'all' ? db.exec(sql) : false;
  })
  .then((rows) => {
    if (!rows) { return data; }

    // Naive manipulation for filling with zero
    const accounts = rows;
    const touched = data.beginning.map(item => item.id);

    accounts.forEach(item => {
      if (touched.indexOf(item.id) === -1) {
        data.beginning.push(item);
      }
    });

    return data;
  });
}
