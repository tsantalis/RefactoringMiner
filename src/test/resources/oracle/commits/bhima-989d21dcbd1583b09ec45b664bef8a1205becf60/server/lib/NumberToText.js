/* eslint import/no-unresolved:off */
/**
 * @module NumberToText
 *
 *
 */

const _ = require('lodash');

const en = require('../../client/i18n/en.json');
const fr = require('../../client/i18n/fr.json');

exports.convert = convert;

let dictionary;
let languageKey;

/**
*
* Source: http://stackoverflow.com/questions/14766951/convert-digits-into-words-with-javascript
* >> Comment number 15
* "Deceptively simple task." – Potatoswatter
* Indeed. There's many little devils hanging out in the details of this problem. It was very fun to solve tho.
* EDIT: This update takes a much more compositional approach.
*  Previously there was one big function which wrapped a couple other proprietary functions.
*  Instead, this time we define generic reusable functions which could be used for many varieties of tasks.
*  More about those after we take a look at numToWords itself …
*/

function convert(input, lang, currencyName) {
  // Round to at most 2 decimal places
  const number = _.round(input, 2);

  languageKey = lang;
  dictionary = (String(lang).toLowerCase() === 'fr') ? fr : en;

  const a = [
    '',
    _.get(dictionary, 'NUMBERS.ONE'),
    _.get(dictionary, 'NUMBERS.TWO'),
    _.get(dictionary, 'NUMBERS.THREE'),
    _.get(dictionary, 'NUMBERS.FOUR'),
    _.get(dictionary, 'NUMBERS.FIVE'),
    _.get(dictionary, 'NUMBERS.SIX'),
    _.get(dictionary, 'NUMBERS.SEVEN'),
    _.get(dictionary, 'NUMBERS.EIGHT'),
    _.get(dictionary, 'NUMBERS.NINE'),
    _.get(dictionary, 'NUMBERS.TEN'),
    _.get(dictionary, 'NUMBERS.ELEVEN'),
    _.get(dictionary, 'NUMBERS.TWELVE'),
    _.get(dictionary, 'NUMBERS.THIRTEEN'),
    _.get(dictionary, 'NUMBERS.FOURTEEN'),
    _.get(dictionary, 'NUMBERS.FIFTEEN'),
    _.get(dictionary, 'NUMBERS.SIXTEEN'),
    _.get(dictionary, 'NUMBERS.SEVENTEEN'),
    _.get(dictionary, 'NUMBERS.EIGHTEEN'),
    _.get(dictionary, 'NUMBERS.NINETEEN'),
  ];

  const b = [
    '',
    '',
    _.get(dictionary, 'NUMBERS.TWENTY'),
    _.get(dictionary, 'NUMBERS.THIRTY'),
    _.get(dictionary, 'NUMBERS.FORTY'),
    _.get(dictionary, 'NUMBERS.FIFTY'),
    _.get(dictionary, 'NUMBERS.SIXTY'),
    _.get(dictionary, 'NUMBERS.SEVENTY'),
    _.get(dictionary, 'NUMBERS.EIGHTY'),
    _.get(dictionary, 'NUMBERS.NINETY'),
  ];

  const g = [
    '',
    _.get(dictionary, 'NUMBERS.THOUSAND'),
    _.get(dictionary, 'NUMBERS.MILLION'),
    _.get(dictionary, 'NUMBERS.BILLION'),
    _.get(dictionary, 'NUMBERS.TRILLION'),
    _.get(dictionary, 'NUMBERS.QUADRILLION'),
    _.get(dictionary, 'NUMBERS.QUINTILLION'),
    _.get(dictionary, 'NUMBERS.SEXTILLION'),
    _.get(dictionary, 'NUMBERS.SEPTILLION'),
    _.get(dictionary, 'NUMBERS.OCTILLION'),
    _.get(dictionary, 'NUMBERS.NONILLION'),
  ];

  const arr = x => Array.from(x);
  const num = x => Number(x) || 0;
  const isEmpty = xs => xs.length === 0;
  const take = n => xs => _.slice(xs, 0, n);
  const drop = n => xs => _.slice(xs, n);
  const reverse = xs => _.slice(xs, 0).reverse();
  const comp = f => y => x => f(y(x));
  const not = x => !x;

  const chunk = n =>
    xs => {
      if (isEmpty(xs)) {
        return [];
      }
      return [take(n)(xs), ...chunk(n)(drop(n)(xs))];
    };

  const formatHundreds = (huns) => {
    const isZero = num(huns) === 0;
    const isOne = huns === 1;
    const isFrench = languageKey === 'fr';

    if (isZero) {
      return '';
    } else if (isFrench && isOne) {
      return ` ${_.get(dictionary, 'NUMBERS.HUNDRED')} `;
    }

    return `${a[huns]} ${_.get(dictionary, 'NUMBERS.HUNDRED')} `;
  };

  const formatOnes = (ones, tens) => {
    const isZero = num(ones) === 0;
    if (isZero) {
      return b[tens];
    } else if (b[tens]) {
      return `${b[tens]}-`;
    }

    return '';
  };

  const numToWords = numbr => {
    const makeGroup = ([onesx, tens, hunsx]) => {
      const huns = _.parseInt(hunsx);
      const ones = _.parseInt(onesx);

      return [
        formatHundreds(huns),
        formatOnes(ones, tens),
        a[tens + ones] || a[ones],
      ].join('');
    };

    const thousand = (group, i) => {
      if (group === '') {
        return group;
      } else if ((group === a[1]) && (languageKey === 'fr') && (g[i] === g[1])) {
        return ` ${g[i]}`;
      }

      return `${group} ${g[i]}`;
    };

    if (typeof numbr === 'number') {
      return numToWords(String(number));
    } else if (numbr === '0') {
      return _.get(dictionary, 'NUMBERS.ZERO');
    }

    return comp(chunk(3))(reverse)(arr(numbr))
      .map(makeGroup)
      .map(thousand)
      .filter(comp(not)(isEmpty))
      .reverse()
      .join(' ');
  };
  /**
  *
  */

  const numberString = String(number);
  const numberPart = _.split(numberString, '.');
  let numberText = numToWords(numberPart[0]);

  numberText = numberPart[1] ?
    `${numberText} ${_.get(dictionary, 'NUMBERS.POINT')}  ${numToWords(numberPart[1])}` :
    numberText;

  return `${numberText} ${currencyName}`;
}
