import assert from 'assert';
import * as utils from '../src/utils/utils';

describe('utils', () => {
  describe('concatArrays', () => {
    it('should concat two Uint8Arrays', () => {
      const a = new Uint8Array([1, 2, 3]);
      const b = new Uint8Array([4, 5, 6]);

      const expected = new Uint8Array([1, 2, 3, 4, 5, 6]);
      const actual = utils.concatArrays(a, b);
      assert.deepStrictEqual(actual, expected);
    });

    it('should concat two number arrays as a Uint8Array', () => {
      const a = [1, 2, 3];
      const b = [4, 5, 6];

      const expected = new Uint8Array([1, 2, 3, 4, 5, 6]);
      const actual = utils.concatArrays(a, b);
      assert.deepStrictEqual(actual, expected);
      assert(actual instanceof Uint8Array);
    });

    it('should concat three Uint8Arrays', () => {
      const a = new Uint8Array([1, 2]);
      const b = new Uint8Array([3, 4]);
      const c = new Uint8Array([5, 6]);

      const expected = new Uint8Array([1, 2, 3, 4, 5, 6]);
      const actual = utils.concatArrays(a, b, c);
      assert.deepStrictEqual(expected, actual);
    });
  });
});
