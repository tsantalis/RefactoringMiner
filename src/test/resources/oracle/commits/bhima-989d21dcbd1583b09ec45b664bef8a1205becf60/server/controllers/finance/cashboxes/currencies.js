/**
 * @overview Cashboxes/Currencies
 *
 * @description
 * Provides an interface for interacting with currencied accounts attached to
 * the cashboxes.
 *
 * @requires db
 * @requires NotFound
 * @requires Topic
 */


const db = require('../../../lib/db');
const NotFound = require('../../../lib/errors/NotFound');
const Topic = require('../../../lib/topic');

exports.list = list;
exports.detail = detail;
exports.create = create;
exports.update = update;

/**
 * @method list
 *
 * @description
 * Lists the currencied accounts associated with a given cashbox.
 *
 * GET /cashboxes/:id/currencies
 */
function list(req, res, next) {
  const sql =
    `SELECT id, currency_id, account_id, transfer_account_id
    FROM cash_box_account_currency WHERE cash_box_id = ?;`;

  db.exec(sql, [req.params.id])
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
 * Get the details of a single currencied account associated with the cashbox.
 *
 * GET /cashboxes/:id/currencies/:currencyId
 */
function detail(req, res, next) {
  const sql =
    `SELECT id, account_id, transfer_account_id
    FROM cash_box_account_currency
    WHERE cash_box_id = ? AND currency_id = ?;`;

  db.exec(sql, [req.params.id, req.params.currencyId])
    .then((rows) => {
      if (!rows.length) {
        throw new NotFound(`
          Could not find a cash box account currency with id ${req.params.currencyId}.
        `);
      }

      res.status(200).json(rows[0]);
    })
    .catch(next)
    .done();
}

// POST /cashboxes/:id/currencies
/**
 * @method create
 *
 * @description
 * This creates a new currency account in the database.
 */
function create(req, res, next) {
  const data = req.body;
  data.cash_box_id = req.params.id;

  const sql =
    'INSERT INTO cash_box_account_currency SET ?;';

  db.exec(sql, [data])
    .then((row) => {
      // currency account changes are still a cashbox update
      Topic.publish(Topic.channels.FINANCE, {
        event : Topic.events.UPDATE,
        entity : Topic.entities.CASHBOX,
        user_id : req.session.user.id,
        id : data.cashbox_id,
      });

      res.status(201).json({ id : row.insertId });
    })
    .catch(next)
    .done();
}

/**
 * @method update
 *
 * @description
 * This method updates the currencied accounts associated with a cashbox.
 *
 * PUT /cashboxes/:id/currencies/:currencyId
 */
function update(req, res, next) {
  const data = req.body;

  let sql =
    `UPDATE cash_box_account_currency SET ?
    WHERE cash_box_id = ? AND currency_id = ?;`;

  db.exec(sql, [data, req.params.id, req.params.currencyId])
  .then(() => {
    // send the changed object to the client
    sql =
      `SELECT id, account_id, transfer_account_id
      FROM cash_box_account_currency
      WHERE cash_box_id = ? AND currency_id = ?;`;

    return db.exec(sql, [req.params.id, req.params.currencyId]);
  })
  .then((rows) => {
    // in case an unknown id is sent to the server
    /** @todo - review this decision */
    if (!rows.length) {
      res.status(200).json({});
      return;
    }

    // currency account changes are still a cashbox update
    Topic.publish(Topic.channels.FINANCE, {
      event : Topic.events.UPDATE,
      entity : Topic.entities.CASHBOX,
      user_id : req.session.user.id,
      id : req.params.id,
    });

    res.status(200).json(rows[0]);
  })
  .catch(next)
  .done();
}
