import * as nacl from './nacl/naclWrappers';
import * as address from './encoding/address';
import * as encoding from './encoding/encoding';
import * as txnBuilder from './transaction';
import multisig from './multisig';
import bidBuilder from './bid';
import algod from './client/algod';
import kmd from './client/kmd';
import * as convert from './convert';
import * as utils from './utils/utils';
import AnyTransaction from './types/transactions';
import { MultisigMetadata } from './types/multisig';

export const { Algod } = algod;
export const { Kmd } = kmd;

const SIGN_BYTES_PREFIX = Buffer.from([77, 88]); // "MX"

// Errors
export const MULTISIG_BAD_SENDER_ERROR_MSG =
  'The transaction sender address and multisig preimage do not match.';

/**
 * signTransaction takes an object with either payment or key registration fields and
 * a secret key and returns a signed blob.
 *
 * Payment transaction fields: from, to, amount, fee, firstRound, lastRound, genesisHash,
 * note(optional), GenesisID(optional), closeRemainderTo(optional)
 *
 * Key registration fields: fee, firstRound, lastRound, voteKey, selectionKey, voteFirst,
 * voteLast, voteKeyDilution, genesisHash, note(optional), GenesisID(optional)
 *
 * If the final calculated fee is lower than the protocol minimum fee, the fee will be increased to match the minimum.
 * @param txn object with either payment or key registration fields
 * @param sk Algorand Secret Key
 * @returns object contains the binary signed transaction and its txID
 */
export function signTransaction(
  txn: txnBuilder.TransactionLike,
  sk: Uint8Array
) {
  if (typeof txn.from === 'undefined') {
    // Get pk from sk if no sender specified
    const key = nacl.keyPairFromSecretKey(sk);
    // eslint-disable-next-line no-param-reassign
    txn.from = address.encodeAddress(key.publicKey);
  }
  const algoTxn = txnBuilder.instantiateTxnIfNeeded(txn);

  return {
    txID: (algoTxn as txnBuilder.Transaction).txID().toString(),
    blob: (algoTxn as txnBuilder.Transaction).signTxn(sk),
  };
}

/**
 * signBid takes an object with the following fields: bidder key, bid amount, max price, bid ID, auctionKey, auction ID,
 * and a secret key and returns a signed blob to be inserted into a transaction Algorand note field.
 * @param bid Algorand Bid
 * @param sk Algorand secret key
 * @returns Uint8Array binary signed bid
 */
export function signBid(
  bid: ConstructorParameters<typeof bidBuilder.Bid>[0],
  sk: Uint8Array
) {
  const signedBid = new bidBuilder.Bid(bid);
  return signedBid.signBid(sk);
}

/**
 * signBytes takes arbitrary bytes and a secret key, prepends the bytes with "MX" for domain separation, signs the bytes
 * with the private key, and returns the signature.
 * @param bytes Uint8array
 * @param sk Algorand secret key
 * @returns binary signature
 */
export function signBytes(bytes: Uint8Array, sk: Uint8Array) {
  const toBeSigned = Buffer.from(utils.concatArrays(SIGN_BYTES_PREFIX, bytes));
  const sig = nacl.sign(toBeSigned, sk);
  return sig;
}

/**
 * verifyBytes takes array of bytes, an address, and a signature and verifies if the signature is correct for the public
 * key and the bytes (the bytes should have been signed with "MX" prepended for domain separation).
 * @param bytes Uint8Array
 * @param signature binary signature
 * @param addr string address
 * @returns bool
 */
export function verifyBytes(
  bytes: Uint8Array,
  signature: Uint8Array,
  addr: string
) {
  const toBeVerified = Buffer.from(
    utils.concatArrays(SIGN_BYTES_PREFIX, bytes)
  );
  const pk = address.decodeAddress(addr).publicKey;
  return nacl.verify(toBeVerified, signature, pk);
}

/**
 * signMultisigTransaction takes a raw transaction (see signTransaction), a multisig preimage, a secret key, and returns
 * a multisig transaction, which is a blob representing a transaction and multisignature account preimage. The returned
 * multisig txn can accumulate additional signatures through mergeMultisigTransactions or appendMultisigTransaction.
 * @param txn object with either payment or key registration fields
 * @param version multisig version
 * @param threshold multisig threshold
 * @param addrs a list of Algorand addresses representing possible signers for this multisig. Order is important.
 * @param sk Algorand secret key. The corresponding pk should be in the pre image.
 * @returns object containing txID, and blob of partially signed multisig transaction (with multisig preimage information)
 * If the final calculated fee is lower than the protocol minimum fee, the fee will be increased to match the minimum.
 */
export function signMultisigTransaction(
  txn: txnBuilder.TransactionLike,
  { version, threshold, addrs }: MultisigMetadata,
  sk: Uint8Array
) {
  // check that the from field matches the mSigPreImage. If from field is not populated, fill it in.
  const expectedFromRaw = address.fromMultisigPreImgAddrs({
    version,
    threshold,
    addrs,
  });
  if (Object.prototype.hasOwnProperty.call(txn, 'from')) {
    const actualSender =
      typeof txn.from === 'string'
        ? txn.from
        : address.encodeAddress(txn.from.publicKey);

    if (actualSender !== expectedFromRaw) {
      throw new Error(MULTISIG_BAD_SENDER_ERROR_MSG);
    }
  } else {
    // eslint-disable-next-line no-param-reassign
    txn.from = expectedFromRaw;
  }
  // build pks for partialSign
  const pks = addrs.map((addr) => address.decodeAddress(addr).publicKey);
  // `txn` needs to be handled differently if it's a constructed `Transaction` vs a dict of constructor args
  const txnAlreadyBuilt = txn instanceof txnBuilder.Transaction;
  let algoTxn: multisig.MultisigTransaction;
  let blob: Uint8Array;
  if (txnAlreadyBuilt) {
    algoTxn = (txn as unknown) as multisig.MultisigTransaction;
    blob = multisig.MultisigTransaction.prototype.partialSignTxn.call(
      algoTxn,
      { version, threshold, pks },
      sk
    );
  } else {
    algoTxn = new multisig.MultisigTransaction(txn as AnyTransaction);
    blob = algoTxn.partialSignTxn({ version, threshold, pks }, sk);
  }
  return {
    txID: algoTxn.txID().toString(),
    blob,
  };
}

/**
 * mergeMultisigTransactions takes a list of multisig transaction blobs, and merges them.
 * @param multisigTxnBlobs a list of blobs representing encoded multisig txns
 * @returns blob representing encoded multisig txn
 */
export function mergeMultisigTransactions(multisigTxnBlobs: Uint8Array[]) {
  return multisig.mergeMultisigTransactions(multisigTxnBlobs);
}

/**
 * appendSignMultisigTransaction takes a multisig transaction blob, and appends our signature to it.
 * While we could derive public key preimagery from the partially-signed multisig transaction,
 * we ask the caller to pass it back in, to ensure they know what they are signing.
 * @param multisigTxnBlob an encoded multisig txn. Supports non-payment txn types.
 * @param version multisig version
 * @param threshold multisig threshold
 * @param addrs a list of Algorand addresses representing possible signers for this multisig. Order is important.
 * @param sk Algorand secret key
 * @returns object containing txID, and blob representing encoded multisig txn
 */
export function appendSignMultisigTransaction(
  multisigTxnBlob: Uint8Array,
  { version, threshold, addrs }: MultisigMetadata,
  sk: Uint8Array
) {
  const pks = addrs.map((addr) => address.decodeAddress(addr).publicKey);
  // obtain underlying txn, sign it, and merge it
  const multisigTxObj: any = encoding.decode(multisigTxnBlob);
  const msigTxn = multisig.MultisigTransaction.from_obj_for_encoding(
    multisigTxObj.txn
  );
  const partialSignedBlob = msigTxn.partialSignTxn(
    { version, threshold, pks },
    sk
  );
  return {
    txID: msigTxn.txID().toString(),
    blob: mergeMultisigTransactions([multisigTxnBlob, partialSignedBlob]),
  };
}

/**
 * multisigAddress takes multisig metadata (preimage) and returns the corresponding human readable Algorand address.
 * @param version mutlisig version
 * @param threshold multisig threshold
 * @param addrs list of Algorand addresses
 */
export function multisigAddress({
  version,
  threshold,
  addrs,
}: MultisigMetadata) {
  return address.fromMultisigPreImgAddrs({ version, threshold, addrs });
}

/**
 * encodeObj takes a javascript object and returns its msgpack encoding
 * Note that the encoding sorts the fields alphabetically
 * @param o js obj
 * @returns Uint8Array binary representation
 */
export function encodeObj(o: Record<string | number | symbol, any>) {
  return new Uint8Array(encoding.encode(o));
}

/**
 * decodeObj takes a Uint8Array and returns its javascript obj
 * @param o Uint8Array to decode
 * @returns object
 */
export function decodeObj(o: ArrayLike<number>) {
  return encoding.decode(o);
}

export const ERROR_MULTISIG_BAD_SENDER = new Error(
  MULTISIG_BAD_SENDER_ERROR_MSG
);
export const ERROR_INVALID_MICROALGOS = new Error(
  convert.INVALID_MICROALGOS_ERROR_MSG
);

export * from './types/transactions';
export { default as IntDecoding } from './types/intDecoding';
export { default as Algodv2 } from './client/v2/algod/algod';
export { default as Indexer } from './client/v2/indexer/indexer';
export {
  isValidAddress,
  encodeAddress,
  decodeAddress,
} from './encoding/address';
export { encodeUint64, decodeUint64 } from './encoding/uint64';
export { default as generateAccount } from './account';
export { secretKeyToMnemonic, mnemonicToSecretKey } from './mnemonic/mnemonic';
export { default as modelsv2 } from './client/v2/algod/models/types';
export {
  mnemonicToMasterDerivationKey,
  masterDerivationKeyToMnemonic,
} from './mnemonic/mnemonic';
export {
  microalgosToAlgos,
  algosToMicroalgos,
  INVALID_MICROALGOS_ERROR_MSG,
} from './convert';
export { computeGroupID, assignGroupID } from './group';
export {
  makeLogicSig,
  signLogicSigTransaction,
  signLogicSigTransactionObject,
  logicSigFromByte,
  tealSign,
  tealSignFromProgram,
} from './logicsig';
export { default as LogicTemplates } from './logicTemplates';

export * from './makeTxn';
export * from './transaction';
