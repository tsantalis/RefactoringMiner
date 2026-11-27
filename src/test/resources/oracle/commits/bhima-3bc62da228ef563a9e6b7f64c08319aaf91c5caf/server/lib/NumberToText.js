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

function convert(number, lang, currencyName){
  //Round to at most 2 decimal places
  number = _.round(number, 2);
  
  languageKey = lang;  
  dictionary = (String(lang).toLowerCase() === 'fr') ? fr : en;

  let a = [
    '', _.get(dictionary, 'NUMBERS.ONE'), _.get(dictionary, 'NUMBERS.TWO'), _.get(dictionary, 'NUMBERS.THREE'), _.get(dictionary, 'NUMBERS.FOUR'),
    _.get(dictionary, 'NUMBERS.FIVE'), _.get(dictionary, 'NUMBERS.SIX'), _.get(dictionary, 'NUMBERS.SEVEN'), _.get(dictionary, 'NUMBERS.EIGHT'), 
    _.get(dictionary, 'NUMBERS.NINE'), _.get(dictionary, 'NUMBERS.TEN'), _.get(dictionary, 'NUMBERS.ELEVEN'), _.get(dictionary, 'NUMBERS.TWELVE'), 
    _.get(dictionary, 'NUMBERS.THIRTEEN'), _.get(dictionary, 'NUMBERS.FOURTEEN'),
    _.get(dictionary, 'NUMBERS.FIFTEEN'), _.get(dictionary, 'NUMBERS.SIXTEEN'), _.get(dictionary, 'NUMBERS.SEVENTEEN'), 
    _.get(dictionary, 'NUMBERS.EIGHTEEN'), _.get(dictionary, 'NUMBERS.NINETEEN')
  ];

  let b = [
    '', '', _.get(dictionary, 'NUMBERS.TWENTY'), _.get(dictionary, 'NUMBERS.THIRTY'), _.get(dictionary, 'NUMBERS.FORTY'),
    _.get(dictionary, 'NUMBERS.FIFTY'), _.get(dictionary, 'NUMBERS.SIXTY'), _.get(dictionary, 'NUMBERS.SEVENTY'),
    _.get(dictionary, 'NUMBERS.EIGHTY'), _.get(dictionary, 'NUMBERS.NINETY')
  ];

  let g = [
    '', _.get(dictionary, 'NUMBERS.THOUSAND'), _.get(dictionary, 'NUMBERS.MILLION'), _.get(dictionary, 'NUMBERS.BILLION'),
    _.get(dictionary, 'NUMBERS.TRILLION'), _.get(dictionary, 'NUMBERS.QUADRILLION'),
    _.get(dictionary, 'NUMBERS.QUINTILLION'), _.get(dictionary, 'NUMBERS.SEXTILLION'), _.get(dictionary, 'NUMBERS.SEPTILLION'),
    _.get(dictionary, 'NUMBERS.OCTILLION'), _.get(dictionary, 'NUMBERS.NONILLION')
  ];

  let arr = x => Array.from(x);
  let num = x => Number(x) || 0;
  let str = x => String(x);
  let isEmpty = xs => xs.length === 0;
  let take = n => xs => _.slice(xs, 0, n);
  let drop = n => xs => _.slice(xs, n);
  let reverse = xs => _.slice(xs, 0).reverse();
  let comp = f => g => x => f (g (x));
  let not = x => !x;
  let chunk = n => xs =>
    isEmpty(xs) ? [] : [take(n)(xs), ...chunk (n) (drop (n) (xs))];


  let numToWords = number => {
    
    let makeGroup = ([ones,tens,huns]) => {
      huns =  _.parseInt(huns);
      ones = _.parseInt(ones);

      return [
        num(huns) === 0 ? '' :
          ((languageKey === 'fr') && (huns === 1)) ? ' ' + _.get(dictionary, 'NUMBERS.HUNDRED') + ' ' :  a[huns] + ' ' + _.get(dictionary, 'NUMBERS.HUNDRED') + ' ',
        num(ones) === 0 ? b[tens] : b[tens] && b[tens] + '-' || '',
        a[tens+ones] || a[ones]
      ].join('');                    

    };
    
    let thousand = (group,i) => group === '' ? group : ((group === a[1]) && (languageKey === 'fr') && (g[i] === g[1] )) ? ` ${g[i]}` :  `${group} ${g[i]}`;
    
    if (typeof number === 'number')
      return numToWords(String(number));
    else if (number === '0')
      return _.get(dictionary, 'NUMBERS.ZERO');
    else
      return comp (chunk(3)) (reverse) (arr(number))
        .map(makeGroup)
        .map(thousand)
        .filter(comp(not)(isEmpty))
        .reverse()
        .join(' ');
  };
  /**
  *
  */

  var numberString = String(number);
  var numberPart = _.split(numberString, '.');
  var numberText = numToWords(numberPart[0]);
  
  numberText = numberPart[1] ? numberText + _.get(dictionary, 'NUMBERS.POINT') + ' ' + numToWords(numberPart[1]) : numberText;
  return numberText + ' ' + currencyName;
}