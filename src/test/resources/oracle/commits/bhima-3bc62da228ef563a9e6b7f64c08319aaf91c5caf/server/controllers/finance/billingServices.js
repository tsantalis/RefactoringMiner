/**
 * The /billing_service API module
 *
 * @module controllers/finance/billingServices
 *
 * @description This module is responsible for CRUD operations on the billing_service
 * table.  A billing_service increases a patient's invoice by a set percentage
 * of the total invoice amount.
 *
 * @requires lib/db
 * @requires lodash/template
 * @requires lib/errors/NotFound
 * @requires lib/errors/BadRequest
 *
 */
const _ = require('lodash');
const db = require('../../lib/db');
const NotFound = require('../../lib/errors/NotFound');
const BadRequest = require('../../lib/errors/BadRequest');

/**
 * Looks up a billing service by id.
 *
 * @param {Number} id - the billing service id
 * @returns {Promise} billingService - a promise resolvinng to the billing
 * service entity.
 */
function lookupBillingService(id) {
  var sql =
    `SELECT bs.id, bs.account_id, bs.label, bs.description, bs.value,
      bs.created_at, bs.updated_at, a.number
    FROM billing_service AS bs JOIN account AS a ON bs.account_id = a.id
    WHERE bs.id = ?;`;

  return db.exec(sql, [id])
    .then(function (rows) {
      // if no records matching, throw a 404
      if (rows.length === 0) {
        throw new NotFound(`Could not find a billing service with id: ${id}.`);
      }
      // return a single JSON of the record
      return rows[0];
    });
}


/**
 * GET /billing_services/:id
 *
 * @description retrieve the details of a single billing service.
 */
exports.detail = function detail(req, res, next) {
  // looks up the billing service by ID
  lookupBillingService(req.params.id)
    .then(function (billingService) {
      res.status(200).json(billingService);
    })
    .catch(next)
    .done();
};


/**
 * GET /billing_services
 *
 * @description lists all billing services in the database, in configurable
 * levels of detail
 */
exports.list = function list(req, res, next) {
  let sql =
    `SELECT bs.id, bs.label, bs.created_at
    FROM billing_service AS bs
    ORDER BY bs.label;`;

  // provide as more information as necessary, if the client asks for it.
  if (req.query.detailed === '1') {
    sql =
      `SELECT bs.id, bs.label, bs.created_at, bs.updated_at, bs.account_id,
        bs.description, bs.value, a.number
      FROM billing_service AS bs JOIN account AS a
        ON bs.account_id = a.id
      ORDER BY bs.id;`;
  }

  db.exec(sql)
    .then(function (rows) {
      res.status(200).json(rows);
    })
    .catch(next)
    .done();
};


/**
 * POST /billing_services
 *
 * @desc creates a new billing service
 */
exports.create = function create(req, res, next) {
  // cache posted data for easy lookup
  const data = req.body.billingService;

  // delete the id if it exists -- the db will create one via auto-increment
  delete data.id;

  // ensure that values inserted are positive
  if (data.value <= 0) {
    next(
      new BadRequest(
        `The value submitted to a billing service must be positive.
         You provided the negative value ${data.value}.`
      )
    );

    return;
  }

  const sql =
    `INSERT INTO billing_service (account_id, label, description, value)
    VALUES (?, ?, ?, ?);`;

  db.exec(sql, [data.account_id, data.label, data.description, data.value])
    .then(function (results) {
      // return the id to the client for future lookups.
      res.status(201).json({ id : results.insertId });
    })
    .catch(next)
    .done();
};


/**
 * PUT /billing_services/:id
 *
 * @desc updates an existing billing service with new information
 */
exports.update = function update(req, res, next) {
  // cache the id
  const id = req.params.id;
  const data = req.body.billingService;

  // remove the :id if it exists inside the billingService object
  delete data.id;

  const sql =
    'UPDATE billing_service SET ? WHERE id = ?;';

  // ensure that the billing service matching :id exists
  lookupBillingService(id)
    .then(function () {
      return db.exec(sql, [data, req.params.id]);
    })
    .then(function () {
      // return the full changed object
      return lookupBillingService(id);
    })
    .then(function (billingService) {
      res.status(200).json(billingService);
    })
    .catch(next)
    .done();
};


/**
 * DELETE /billing_services/:id
 *
 * @desc deletes a billing service in the database
 */
exports.delete = function del(req, res, next) {
  const sql =
    'DELETE FROM billing_service WHERE id = ?;';

  // first make sure that the billing service exists
  lookupBillingService(req.params.id)
    .then(function () {
      return db.exec(sql, [req.params.id]);
    })
    .then(function () {
      res.sendStatus(204);
    })
    .catch(next)
    .done();
};
