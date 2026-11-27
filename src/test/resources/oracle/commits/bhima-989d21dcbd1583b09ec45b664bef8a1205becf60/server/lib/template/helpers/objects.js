
/**
 * @function look
 * @description look value into an object given
 * @param {object} obj The concerned object
 * @param {string} property The object property
 * @param {string} property2 The object property
 */
function look(obj, property, property2) {
  const hasProperInput = obj && property && property2;
  const hasFirstProperty = hasProperInput && property2.name;
  const hasSecondProperty = hasProperInput && !property2.name;

  let value;

  // Missing parameter take the function name as name property
  // if property2 is missing, it will be an object with an attribute name: 'function name'
  if (hasFirstProperty) {
    value = obj[property];
  } else if (hasSecondProperty) {
    value = obj[property][property2];
  } else {
    value = '';
  }

  return value;
}

exports.look = look;
