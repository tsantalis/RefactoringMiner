
/**
 * @function look
 * @description look value into an object given
 * @param {object} obj The concerned object
 * @param {string} property The object property
 * @param {string} property2 The object property
 */
function look(obj, property, property2) {
  let hasProperInput    = obj && property && property2;
  let hasFirstProperty  = hasProperInput && property2.name;
  let hasSecondProperty = hasProperInput && !property2.name;

  // Missing parameter take the function name as name property
  // if property2 is missing, it will be an object with an attribute name: 'function name'
  return hasFirstProperty ? obj[property] :
         hasSecondProperty ? obj[property][property2] : '' ;
}

exports.look = look;
