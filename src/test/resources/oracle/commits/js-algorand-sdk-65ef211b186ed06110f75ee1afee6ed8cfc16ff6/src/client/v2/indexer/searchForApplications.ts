import JSONRequest from '../jsonrequest';

export default class SearchForApplications extends JSONRequest {
  // eslint-disable-next-line class-methods-use-this
  path() {
    return '/v2/applications';
  }

  // application ID for filter, as int
  index(index: number) {
    this.query['application-id'] = index;
    return this;
  }

  // token for pagination
  nextToken(next: string) {
    this.query.next = next;
    return this;
  }

  // limit results for pagination
  limit(limit: number) {
    this.query.limit = limit;
    return this;
  }

  // include all items including closed accounts, deleted applications, destroyed assets, opted-out asset holdings, and closed-out application localstates
  includeAll(value = true) {
    this.query['include-all'] = value;
    return this;
  }
}
