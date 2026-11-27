
const accountingjs = require('accounting-js');
const NumberToText = require('../../../lib/NumberToText');

const USD_FMT = { precision: 2 };

const FC_FMT = {
  symbol: 'FC',
  precision: 2,
  thousand: '.',
  decimal: ',',
  format: '%v %s' // value before symbol
};

/** @todo use the currency filter fork written for the client to perform the same behaviour here */
function currency(value, currencyId) {
  // if currencyId is not defined, defaults to USD.
  // @TODO - super-hardcoded values for the moment.  Can we do better?
  const fmt = (Number(currencyId) === 1) ? FC_FMT : USD_FMT;
  return accountingjs.formatMoney(value || 0, fmt);
}

/**
 * @function numberToText
 * @value is the ammount to convert
 * @lang is the selected language
 * @currencyName is the Name of currency
 */
function numberToText(value, lang, currencyName) {
  const numberText = NumberToText.convert(value, lang, currencyName);
      
  const fmt = numberText;
  return fmt;
} 

const INDENTATION_STEP = 40;

/**
 * @function indentAccount
 * @description indent with 40px accounts based on the account depth for the chart of accounts
 * @param {number} depth the account number
 * @return {number} number the processed indent
 */
function indentAccount(depth) {
  // indentation step is fixed arbitrary to 40 (40px)
  let number = Number(depth);
  return number ? number * INDENTATION_STEP : 0;
}

exports.currency = currency;
exports.indentAccount = indentAccount;
exports.numberToText = numberToText;
