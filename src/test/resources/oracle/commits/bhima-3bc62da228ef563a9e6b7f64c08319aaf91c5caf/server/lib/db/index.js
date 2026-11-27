
const q = require('q');
const mysql = require('mysql');
const winston = require('winston');
const uuid = require('node-uuid');
const Transaction = require('./transaction');

const BadRequest = require('../errors/BadRequest');
const NotFound = require('../errors/NotFound');

/**
 * @class DatabaseConnector
 *
 * @description
 * The database connector forms a layer between HTTP controllers and the mysql
 * database.  The connector is mainly responsible for setting up the initial
 * connection based on parameters in the environment variables, and then wrapping
 * all database queries in promise calls.
 *
 * @requires q
 * @requires mysql
 * @requires winston
 * @requires node-uuid
 * @requires Transaction
 */
class DatabaseConnector {

  /** @constructor */
  constructor() {
    const params = {
      host     : process.env.DB_HOST,
      user     : process.env.DB_USER,
      password : process.env.DB_PASS,
      database : process.env.DB_NAME,
    };

    this.pool = mysql.createPool(params);
  }

  /**
   * @method exec
   *
   * @description
   * This method forms a loose wrapper for acquiring a database connection,
   * templating in the SQL query, executing it, and resolving/rejecting a
   * promise with the query result.
   *
   * Note that this is NOT a transactional interface.  If you need a transaction,
   * please use the `transaction()` method, which allows transaction, serial
   * execution of queries.  It also destroys the connection, ensuring that data
   * is not shared between consecutive calls.
   *
   * @param {String} sql - the SQL template query to call the database with
   * @param {Object|Array|Undefined} params - the parameter object to be
   *   combined with the SQL statement before calling the database driver
   * @returns {Promise} the result of the database query
   *
   * @example
   * const db = require('db');
   * db.exec('SELECT 1;')
   *
   *   // logs '1'
   *   .then(rows => console.log(rows))
   *
   *   // if an error occurs in the connection to the database or query
   *   // execution, it will be caught here.
   *   .catch(err => console.log(err));
   */
  // executes an SQL statement as a
  exec(sql, params) {
    const deferred = q.defer();
    this.pool.getConnection((error, connection) => {
      if (error) { return deferred.reject(error); }

      // format the SQL statement using MySQL's escapes
      const statement = mysql.format(sql.trim(), params);

      connection.query(statement, (err, rows) => {
        connection.release();
        return (err) ? deferred.reject(err) : deferred.resolve(rows);
      });

      // log the formatted SQL if in debug mode
      winston.debug(statement);
    });

    return deferred.promise;
  }

  // gets a transaction object to be executed
  transaction() {
    return new Transaction(this);
  }

  /**
   * @method one
   *
   * @description
   * A simply wrapper to make controllers DRY.  It wraps the exec() method in a
   * rejection if the returned value is not exactly 1.
   *
   * @param {String} sql - the SQL template query to call the database with
   * @param {Object|Array|Undefined} params - the parameter object to be
   *   combined with the SQL statement before calling the database driver
   * @param {String} id - the unique id sought
   * @param {String|Undefined} entity - the entity targeted for pretty printing.
   * @returns {Promise} the result of the database query
   */
  one(sql, params, id, entity) {
    return this.exec(sql, params)
      .then((rows) => {
        if (rows.length < 1) {
          throw new NotFound(
            `Expected ${entity || 'record'} to contain a single record with id ${id}, but none were found!`
          );
        }

        if (rows.length > 1) {
          throw new BadRequest(`
            Expected ${entity || 'record'} to contain a single record with id ${id},
            but found ${rows.length} records!
          `);
        }

        return rows[0];
      });
  }


  /**
   * @function bid
   *
   * @description
   * Converts a (dash separated) string uuid to a binary buffer for insertion
   * into the database.
   *
   * @param {String|Buffer} hexUuid - a 36 character length string to be inserted into
   * the database
   * @returns {Buffer} uuid - a 16-byte binary buffer for insertion into the
   * database
   *
   * @example
   * // load the database module
   * const db = require('db');
   *
   * // some uuid string
   * let uuid = '7dfa6933-1165-4924-abb6-822138ec47d7'
   * let binary = db.bid(uuid);
   *
   * // ... later ...
   *
   * // the binary uuid will now be inserted as binary into MySQL
   * db.exec('INSERT INTO table SET uuid = ?;', binary);
   */
  bid(hexUuid) {

    // if already a buffer, no need to convert
    if (hexUuid instanceof Buffer) {
      return hexUuid;
    }

    return new Buffer(uuid.parse(hexUuid));
  }

  /**
   * @function convert
   *
   * @description
   * Converts values on the data object to binary uuids if they exist.  If not, it
   * will gracefully skip the key.
   *
   * @param {Object} data - an object with uuids to convert to binary
   * @param {Array} keys - an array of keys on the data object, specifying which
   * fields to convert
   * @returns {Object} data - the data converted object
   *
   * @example
   * // example data with two uuids needing conversion to binary
   * let data = {
   *   key : 'value',
   *   id : 'ee727be0-7fde-4d21-8d8b-a726830f6e37',
   *   date : new Date(),
   *   link : '26dc9608-d039-4677-95ab-31530da2411b'
   * };
   *
   * // convert the two keys (using db.bid())
   * data = db.convert(data, ['id', 'link']);
   *
   * // ... later ...
   *
   * // the converted values can be safely inserted into MySQL as binary
   * db.exec('INSERT into table SET ?;', [data]);
   */
  convert(data, keys) {

    // loop through each key
    keys.forEach(key => {

      // the key exists on the object and value is a string
      if (data[key] && typeof data[key] === 'string') {
        data[key] = this.bid(data[key]);
      }
    });

    return data;
  }

  /**
   * @method escape
   *
   * @description
   * This is just an alias for mysql.escape();
   */
  escape(key) {
    return mysql.escape(key);
  }
}

module.exports = new DatabaseConnector();
