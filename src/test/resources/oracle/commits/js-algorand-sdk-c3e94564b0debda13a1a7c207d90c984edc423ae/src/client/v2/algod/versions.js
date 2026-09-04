class Versions {
  constructor(c) {
    this.c = c;
  }

  /**
   * retrieves the VersionResponse from the running node
   * @param headers, optional
   * @returns {Promise<*>}
   */
  async do(headers = {}) {
    const res = await this.c.get('/versions', {}, headers);
    return res.body;
  }
}

module.exports = { Versions };
