
// test equality
function equal(a, b, options) {
  if (a === b) {
    return options.fn(this);
  }
  return options.inverse(this);
}

// test equality
function gt(a, b, options) {
  if (a >= b) {
    return options.fn(this);
  }
  return options.inverse(this);
}

exports.equal = equal;
exports.gt = gt;
