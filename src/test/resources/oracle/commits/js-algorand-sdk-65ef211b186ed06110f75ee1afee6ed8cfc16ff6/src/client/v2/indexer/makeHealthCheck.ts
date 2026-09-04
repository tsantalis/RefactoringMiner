import JSONRequest from '../jsonrequest';

export default class MakeHealthCheck extends JSONRequest {
  // eslint-disable-next-line class-methods-use-this
  path() {
    return '/health';
  }
}
