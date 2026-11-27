
const moment = require('moment');

// format used in these helpers
const DATE_FMT = 'DD/MM/YYYY';
const TIMESTAMP_FMT = 'DD/MM/YYYY HH:mm:ss';

/**
 * @method date
 *
 * @description
 * This method returns a string date for a particular value, providing the full
 * date in DD/MM/YYYY formatting.
 *
 * @param {Date} value - the date value to be transformed
 * @returns {String} - the formatted string for insertion into templates
 */
function date(value, dateFormat) {
  dateFormat = (!dateFormat || dateFormat.name === 'date') ? DATE_FMT : dateFormat;
  let input = moment(value);
  return input.isValid() ? input.format(dateFormat) :  '';
}

/**
 * @method timestamp
 *
 *
 * @description
 * This method returns the timestamp of a particular value, showing the full date,
 * hours, minutes and seconds associated with the timestamp.
 *
 * @param {Date} value - the date value to be transformed
 * @returns {String} - the formatted string for insertion into templates
 */
function timestamp(value) {
  let input = moment(value);
  return input.isValid() ? input.format(TIMESTAMP_FMT) : '';
}

/**
 * @method age
 *
 * @description
 * This method returns the difference in years between the present time and a
 * provided date.
 *
 * @param {Date} date - the date value to be transformed
 * @returns {String} - the date difference in years between now and the provided
 *   date.
 */
function age(date) {
  return moment().diff(date, 'years');
}

exports.date = date;
exports.timestamp = timestamp;
exports.age = age;
