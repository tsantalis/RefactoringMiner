
function add(a, b) {
  return a + b;
}

function substract(a, b) {
  return a - b;
}

function multiply(a, b) {
  return a * b;
}


/**
 * @function sum
 * @desc This function is responsible to calculate the sum of an array given
 * @param {array} array The array of element
 * @param {string} column The column of summation
 * @param {string} ponderation The column to use as factor of the given column
 * @example
 * // Example with handlebars template
 * // we suppose that `items` is an array of objects
 * // we suppose that `unit_price` is a property of an object in `items` array
 * // we suppose that `quantity` is another property of an object in `items` array
 * {{sum items 'unit_price'}} // will returns only the sum of unit_price value in `items` array
 * {{sum items 'unit_price' 'quantity'}} // will returns the sum of unit_price * quntity value in `items` array
 * `items` <=> array, `unit_price` <=> column, and `quantity` <=> ponderation
 * @return {number} the summation
 */
function sum(array, column, ponderation) {
  const p = (typeof ponderation === 'string') ? ponderation : null;

  if (!Array.isArray(array)) { return ''; }

  return array.reduce((a, b) => {
    return p ? (b[column] * b[p]) + a : b[column] + a;
  }, 0);
}

exports.multiply = multiply;
exports.add = add;
exports.sum = sum;
exports.substract = substract;
