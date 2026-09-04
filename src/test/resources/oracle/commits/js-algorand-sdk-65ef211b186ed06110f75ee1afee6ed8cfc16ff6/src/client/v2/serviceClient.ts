import { HTTPClient } from '../client';
import IntDecoding from '../../types/intDecoding';

/**
 * Abstract service client to encapsulate shared AlgodClient and IndexerClient logic
 */
export default abstract class ServiceClient {
  c: HTTPClient;
  intDecoding: IntDecoding;

  constructor(httpClient: HTTPClient) {
    this.c = httpClient;
    this.intDecoding = IntDecoding.DEFAULT;
  }

  /**
   * Set the default int decoding method for all JSON requests this client creates.
   * @param {"default" | "safe" | "mixed" | "bigint"} method The method to use when parsing the
   *   response for request. Must be one of "default", "safe", "mixed", or "bigint". See
   *   JSONRequest.setIntDecoding for more details about what each method does.
   */
  setIntEncoding(method: IntDecoding) {
    this.intDecoding = method;
  }

  /**
   * Get the default int decoding method for all JSON requests this client creates.
   */
  getIntEncoding() {
    return this.intDecoding;
  }
}
