
const _ = require('lodash');
const Accounts = require('../../accounts');
const ReportManager = require('../../../../lib/ReportManager');

const TEMPLATE = './server/controllers/finance/reports/accounts/chart.handlebars';

/**
 * @method chart
 *
 * @description
 * Generate chart of account as a document
 */
function chart(req, res, next) {

  let report;

  let params = req.query;

  // @TODO Define server constants library
  const TITLE_ID = 4;

  params.user = req.session.user;

  const options = _.extend(req.query, { csvKey : 'accounts', filename : 'REPORT.CHART_OF_ACCOUNTS', orientation : 'landscape' });

  try {
    report = new ReportManager(TEMPLATE, req.session, options);
  } catch(e) {
    return next(e);
  }

  Accounts.lookupAccount()  
    .then(Accounts.processAccountDepth)
    .then(accounts => {

      accounts.forEach(account => {
        account.is_title_account = account.type_id === TITLE_ID;
      });

      return report.render({ accounts });
    })
    .then(result => {
      res.set(result.headers).send(result.report);
    })
    .catch(next)
    .done();
}

exports.chart = chart;
