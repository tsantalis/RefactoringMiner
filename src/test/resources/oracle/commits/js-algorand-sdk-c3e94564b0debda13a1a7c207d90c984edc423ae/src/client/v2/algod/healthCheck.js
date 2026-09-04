class HealthCheck {
  constructor(c) {
    this.c = c;
  }

  /**
   * healthCheck returns an empty object iff the node is running
   * @param headers, optional
   * @returns {Promise<*>}
   */
  async do(headers = {}) {
    const res = await this.c.get('/health', {}, headers);
    if (!res.ok) {
      throw new Error(`Health response: ${res.status}`);
    }
    return {};
  }
}

module.exports = { HealthCheck };
