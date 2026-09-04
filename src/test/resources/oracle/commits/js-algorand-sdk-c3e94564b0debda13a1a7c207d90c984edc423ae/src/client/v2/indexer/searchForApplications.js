const { JSONRequest } = require('../jsonrequest');

class SearchForApplications extends JSONRequest {
  // eslint-disable-next-line no-underscore-dangle,class-methods-use-this
  _path() {
    return '/v2/applications';
  }

  // application ID for filter, as int
  index(index) {
    this.query['application-id'] = index;
    return this;
  }

  // token for pagination
  nextToken(next) {
    this.query.next = next;
    return this;
  }

  // limit results for pagination
  limit(limit) {
    this.query.limit = limit;
    return this;
  }

  // include all items including closed accounts, deleted applications, destroyed assets, opted-out asset holdings, and closed-out application localstates
  includeAll(value = true) {
    this.query['include-all'] = value;
    return this;
  }
}

module.exports = { SearchForApplications };
