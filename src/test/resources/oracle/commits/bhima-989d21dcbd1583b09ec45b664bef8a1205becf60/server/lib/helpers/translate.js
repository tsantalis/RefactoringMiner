/* eslint import/no-unresolved:off */
const _ = require('lodash');

const en = require('../../../client/i18n/en.json');
const fr = require('../../../client/i18n/fr.json');

/**
 * @function getTranslationHelper
 *
 * @param {String} languageKey - either 'fr' or 'en'
 */
function getTranslationHelper(languageKey) {
  const dictionary = (String(languageKey).toLowerCase() === 'fr') ? fr : en;

  /**
   * @function translate
   *
   * This helper method is responsible for looking up a translation value from
   * a JSON object. It allows the template to specify nested keys a string as follows
   *  'FIRST_CATEGORY.SECOND_CATEGORY.ATTRIBUTE'
   */
  return function translate(translateCode) {
    // lodash's get() method returns an object's value corresponding to the path matched.
    // If the path does not exist, it returns undefined.
    // See https://lodash.com/docs/4.15.0#at
    return _.get(dictionary, translateCode) || translateCode;
  };
}


module.exports = getTranslationHelper;
