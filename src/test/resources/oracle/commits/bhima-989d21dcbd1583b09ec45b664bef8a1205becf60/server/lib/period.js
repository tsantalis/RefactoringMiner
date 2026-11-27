// @TODO This is an exact copy of the client 'PeriodService', this code should
//       to determine if it's angular or node and inject accordingly - removing
//       the duplication.
const Moment = require('moment');

class PeriodService {

  constructor(clientTimestamp) {
    var self = this;
    this.timestamp = new Moment(clientTimestamp);

    this.periods = {
      today : {
        key : 'today',
        translateKey : 'PERIODS.TODAY',
        limit : calculatePeriodLimit('date'),
      },
      week : {
        key : 'week',
        translateKey : 'PERIODS.THIS_WEEK',
        limit : calculatePeriodLimit('week'),
      },
      month : {
        key : 'month',
        translateKey : 'PERIODS.THIS_MONTH',
        limit : calculatePeriodLimit('month'),
      },
      year : {
        key : 'year',
        translateKey : 'PERIODS.THIS_YEAR',
        limit : calculatePeriodLimit('year'),
      },
      yesterday : {
        key : 'yesterday',
        translateKey : 'PERIODS.YESTERDAY',
        limit : calculatePeriodLimit('date', -1),
      },
      lastWeek : {
        key : 'lastWeek',
        translateKey : 'PERIODS.LAST_WEEK',
        limit : calculatePeriodLimit('week', -1),
      },
      lastMonth : {
        key : 'lastMonth',
        translateKey : 'PERIODS.LAST_MONTH',
        limit : calculatePeriodLimit('month', -1),
      },
      lastYear : {
        key : 'lastYear',
        translateKey : 'PERIODS.LAST_YEAR',
        limit : calculatePeriodLimit('year', -1),
      },

      // components will make an exception for all time - no period has to be selected
      // on the server this simple removes the WHERE condition
      allTime : {
        key : 'allTime',
        translateKey : 'PERIODS.ALL_TIME',
      },

      custom : {
        key : 'custom',
        translateKey : 'PERIODS.CUSTOM',
      },
    };

    function calculatePeriodLimit(periodKey, modifier) {
      var dateModifier = modifier || 0;
      var currentPeriod = Moment().get(periodKey);

      return {
        start : () =>
          (new Moment(self.timestamp)).set(periodKey, currentPeriod + dateModifier).startOf(periodKey).toDate(),
        end : () =>
          (new Moment(self.timestamp)).set(periodKey, currentPeriod + dateModifier).endOf(periodKey).toDate(),
      };
    }
  }

  lookupPeriod(key) {
    return this.periods[key];
  }
}

module.exports = PeriodService;
