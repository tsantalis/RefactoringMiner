/**
 * Price List Controller
 *
 * @module finance/priceList
 *
 * @desc Implements CRUD operations on the Price List entity.
 *
 * This module implements the following routes:
 * GET    /prices
 * GET    /prices/:uuid
 * POST   /prices
 * PUT    /prices/:uuid
 * DELETE /prices/:uuid
 */

const db = require('../../lib/db');
const uuid = require('node-uuid');
const NotFound = require('../../lib/errors/NotFound');

/**
 * Lists all price lists in the database
 *
 * GET /prices
 */
exports.list = function list(req, res, next) {
  var sql;

  if (req.query.detailed === '1') {
    sql =
      `SELECT BUID(uuid) as uuid, label, created_at, description
      FROM price_list
      WHERE enterprise_id = ?
      ORDER BY label;`;
  } else {
    sql =
      `SELECT BUID(uuid) as uuid, label
      FROM price_list
      WHERE enterprise_id = ?
      ORDER BY label;`;
  }

  db.exec(sql, [req.session.enterprise.id])
  .then(function (rows) {
    res.status(200).json(rows);
  })
  .catch(next)
  .done();
};

/**
 * @param {number} id
 * @desc Queries the price_list and price_list_item tables for a pricle list
 * with the given ID.
 *
 * @example
 * // lookup price list
 * .then(function (priceList) {
 *   res.status(200).json(priceList);
 * })
 * .catch(next)
 * .done();
 *
 * @returns {Promise}
 */
function lookupPriceList(uid) {
  var priceList;
  var sql =
    `SELECT BUID(uuid) AS uuid, label, description, created_at, updated_at
    FROM price_list WHERE uuid = ?;`;

  return db.exec(sql, [uid])
  .then(function (rows) {
    // if no matches found, send a 404 error
    if (rows.length === 0) {
      throw new NotFound(`Could not find a price list with uuid ${uuid.unparse(uid)}`);
    }

    priceList = rows[0];

    sql =
      `SELECT BUID(uuid) as uuid, BUID(inventory_uuid) as inventory_uuid, label, value, is_percentage, created_at
      FROM price_list_item WHERE price_list_uuid = ?;`;

    return db.exec(sql, [uid]);
  })
  .then(function (rows) {
    priceList.items = rows;

    // return the price list object to the next promise callback
    return priceList;
  });
}


/**
 * Retrieve the details and items of a single price list.
 *
 * GET /prices/:uuid
 */
exports.details = function details(req, res, next) {
  const uid = db.bid(req.params.uuid);

  lookupPriceList(uid)
  .then(function (priceList) {
    res.status(200).json(priceList);
  })
  .catch(next)
  .done();
};

/**
 * Convert an array of price list item objects into an array of arrays for
 * easy database insertion
 *
 * @param {String} priceListUuid
 * @param {Array} items
 *
 * @example
 * var items = [{
 *   inventory_uuid : 'a11e6b7f-fbbb-432e-ac2a-5312a66dccf4',
 *   label : 'Aspirin 15% reduction',
 *   value : 15,
 *   is_percentage : true
 * }, {
 *   uuid : '274c51ae-efcc-4238-98c6-f402bfb39866',
 *   inventory_uuid : '81af634f-321a-40de-bc6f-ceb1167a9f65',
 *   label : 'Quinine $10 reduction',
 *   value : 10,
 *   is_percentage : false
 * }];
 *
 * var priceListUuid = '32b674a6-6aa3-4098-ac11-a4d3e21fc0ba';
 *
 * // format the items into an array of arrays for database insertion
 * items = formatPriceListItems(priceListUuid, items);
 *
 * // items is now:
 * [ [ '3be232f9-a4b9-4af6-984c-5d3f87d5c107',
 *     'a11e6b7f-fbbb-432e-ac2a-5312a66dccf4',
 *     '32b674a6-6aa3-4098-ac11-a4d3e21fc0ba',
 *     'Aspirin 15% reduction',
 *     15,
 *     true ],
 *   [ '274c51ae-efcc-4238-98c6-f402bfb39866',
 *     '81af634f-321a-40de-bc6f-ceb1167a9f65',
 *     '32b674a6-6aa3-4098-ac11-a4d3e21fc0ba',
 *     'Quinine $10 reduction',
 *     10,
 *     false ] ]
 *
 * @returns {Array} items
 */
function formatPriceListItems(priceListUuid, items) {
  // format the price list items into a format that can be easily inserted into
  // the database
  return items.map(function (item) {
    //  prevent missing inventory_uuids from crashing the server
    var inventoryId = item.inventory_uuid ? db.bid(item.inventory_uuid) : null;

    return [
      db.bid(item.uuid || uuid.v4()),
      inventoryId,
      priceListUuid,
      item.label,
      item.value,
      item.is_percentage,
    ];
  });
}


/**
 * Create a new price list entity, along with price list items.
 *
 * POST /prices
 */
exports.create = function create(req, res, next) {
  var items;
  var data = req.body.list;
  var trans = db.transaction();
  var priceListSql =
    `INSERT INTO price_list (uuid, label, description, enterprise_id)
    VALUES (?, ?, ?, ?);`;
  var priceListItemSql =
    `INSERT INTO price_list_item (uuid, inventory_uuid, price_list_uuid,
    label, value, is_percentage) VALUES ?;`;

  // generate a UUID if not provided
  data.uuid = db.bid(data.uuid || uuid.v4());
  // if the client didn't send price list items, do not create them.
  if (data.items) {
    items = formatPriceListItems(data.uuid, data.items);
  }

  // remove before database insertion
  delete data.items;

  // enqueue the queries to the transaction
  trans.addQuery(priceListSql, [
    data.uuid, data.label, data.description, req.session.enterprise.id,
  ]);

  // only enqueue the price list items if they exist
  if (items) { trans.addQuery(priceListItemSql, [items]); }

  trans.execute()
  .then(function () {
    // respond to the client with a 201 CREATED
    res.status(201).json({ uuid : uuid.unparse(data.uuid) });
  })
  .catch(next)
  .done();
};


/**
 * Updates a price list (and associated items) in the database.
 *
 * PUT /prices/:uuid
 */
exports.update = function update(req, res, next) {
  var items;
  var data = req.body.list;
  var priceListSql =
    'UPDATE price_list SET ? WHERE uuid = ?;';

  var priceListDeleteItemSql =
    'DELETE FROM price_list_item WHERE price_list_uuid = ?';

  var priceListCreateItemSql =
    `INSERT INTO price_list_item (uuid, inventory_uuid, price_list_uuid,
    label, value, is_percentage) VALUES ?;`;

  var trans = db.transaction();
  const uid = db.bid(req.params.uuid);

  // if the client didn't send price list items, do not create them.
  if (data.items) {
    items = formatPriceListItems(uid, data.items);
  }

  // remove non-updatable properties before queries
  delete data.uuid;
  delete data.items;

  // make sure the price list exists
  lookupPriceList(uid)
  .then(function (priceList) {
    // if we get here, it means the price list exists and we can perform an
    // update query on it. Since we are doing multiple operations at once,
    // wrap the queries in a DB transaction.
    if (!isEmptyObject(data)) {
      trans.addQuery(priceListSql, [data, uid]);
    }

    // only trigger price list item updates if the items have been sent back to
    // the server

    if (items) {
      trans.addQuery(priceListDeleteItemSql, [uid]);
      trans.addQuery(priceListCreateItemSql, [items]);
    }

    return trans.execute();
  })
  .then(function () {
    // send the full resource object back to the client via lookup function
    return lookupPriceList(uid);
  })
  .then(function (priceList) {
    res.status(200).json(priceList);
  })
  .catch(next)
  .done();
};


/**
 * Delete a price list (and associated items) in the database.
 *
 * DELETE /prices/:uuid
 */
exports.delete = function del(req, res, next) {
  const uid = db.bid(req.params.uuid);

  const sql =
    'DELETE FROM price_list WHERE uuid = ?;';

  // ensure that the price list exists
  lookupPriceList(uid)
  .then(function () {
    return db.exec(sql, [uid]);
  })
  .then(function () {
    // respond with 204 'NO CONTENT'
    res.status(204).json();
  })
  .catch(next)
  .done();
};

function isEmptyObject(object) {
  return Object.keys(object).length === 0;
}
