// server/controllers/ledger.js

// Module: ledger
//
// This module exposes three methods:
//  (1) debtor
//  (2) credior
//  (3) general
// which encapsulate reporting the ledgers
// for each group, respectively.

var q = require('q');
var db = require('./../../lib/db');

exports.debtor = debtor;
exports.debtor_group = debtorGroup;
exports.employee_invoice = employeeInvoice;
exports.distributableSale = distributableSale;

/**
* HTTP Controllers
*/
exports.compileDebtorLedger = function (req, res, next) {
  debtor(req.params.id)
  .then(function (rows) {
    res.send(rows);
  })
  .catch(function (error) {
    next(error);
  })
  .done();
};

exports.compileDebtorLedgerSale = function (req, res, next) {
  debtorSale(req.params.id, req.params.saleId)
  .then(function (rows) {
    res.send(rows);
  })
  .catch(function (error) {
    next(error);
  })
  .done();
};

exports.compileGroupLedger = function (req, res, next) {
  debtorGroup(req.params.id)
  .then(function (rows) {
    res.send(rows);
  })
  .catch(function (error) {
    next(error);
  })
  .done();
};

exports.compileEmployeeLedger = function (req, res, next) {
  employeeInvoice(req.params.id)
  .then(function (rows) {
    res.send(rows);
  })
  .catch(function (error) {
    next(error);
  })
  .done();
};

exports.compileSaleLedger = function (req, res, next) {
  distributableSale(req.params.id)
  .then(function (rows) {
    res.send(rows);
  })
  .catch(function (error) {
    next(error);
  })
  .done();
};

/*
 * Utility Methods
*/
function debtor(debtorId) {
  const defer = q.defer();
  let id = debtorId;

  // debtor query
  if (!id) {
    defer.reject(new Error('No debtor id selected!'));
  } else {
    id = db.escape(id);
  }

  const query =
    `SELECT account_id
    FROM debtor JOIN debtor_group ON
      debtor.group_uuid = debtor_group.uuid
    WHERE debtor.uuid='${id}';`;

  db.exec(query)
  .then(function (ans) {
    const account = ans.pop().account_id;
    const queryString =
      `SELECT c.inv_po_id, c.trans_id, c.trans_date, c.account_id FROM (
        SELECT p.inv_po_id, p.trans_id, p.trans_date, p.account_id
        FROM posting_journal AS p
        WHERE p.deb_cred_uuid = '${id}' AND p.account_id = '${account}'
      UNION
        SELECT g.inv_po_id, g.trans_date, g.trans_id, g.account_id
        FROM general_ledger AS g
        WHERE g.deb_cred_uuid = '${id}' AND g.account_id = '${account}')
       AS c;`;

    return db.exec(queryString);
  })
  .then(function (ans) {
    if (!ans.length) { defer.resolve([]); }

    const invoices = ans.map(function (line) {
      return line.inv_po_id;
    });

    const accountId = ans.pop().account_id;

    const sql =
      `SELECT s.reference, s.project_id, t.inv_po_id, t.trans_date, SUM(t.debit_equiv) AS debit,
        SUM(t.credit_equiv) AS credit, SUM(t.debit_equiv - t.credit_equiv) as balance,
        t.account_id, t.deb_cred_uuid, t.currency_id, t.doc_num, t.description, t.account_id,
        t.comment, t.canceled, p.abbr, c.document_id,
        IF(ISNULL(c.document_id), 0, 1) AS consumed
      FROM (
        (
          SELECT pj.inv_po_id, pj.trans_date, pj.debit,
            pj.credit, pj.debit_equiv, pj.credit_equiv,
            pj.account_id, pj.deb_cred_uuid, pj.currency_id,
            pj.doc_num, pj.trans_id, pj.description, pj.comment, credit_note.sale_uuid AS canceled
          FROM posting_journal AS pj
          LEFT JOIN credit_note ON credit_note.sale_uuid = pj.inv_po_id WHERE pj.deb_cred_uuid='${id}'
        ) UNION (
          SELECT gl.inv_po_id, gl.trans_date, gl.debit,
            gl.credit, gl.debit_equiv, gl.credit_equiv,
            gl.account_id, gl.deb_cred_uuid, gl.currency_id,
            gl.doc_num, gl.trans_id, gl.description, gl.comment, credit_note.sale_uuid AS canceled
          FROM general_ledger AS gl
          LEFT JOIN credit_note ON credit_note.sale_uuid = gl.inv_po_id WHERE gl.deb_cred_uuid='${id}'
        )
      ) AS t
      JOIN sale AS s ON t.inv_po_id = s.uuid
      JOIN project AS p ON s.project_id = p.id
      LEFT JOIN consumption AS c ON t.inv_po_id = c.document_id
      WHERE t.inv_po_id IN ("${invoices.join('","')}")
      AND t.account_id = '${accountId}'
      GROUP BY t.inv_po_id;`;

    return db.exec(sql);
  })
  .then(function (ans) {
    defer.resolve(ans);
  })
  .catch(function (error) {
    defer.reject(error);
  });

  return defer.promise;
}

function debtorGroup(id) {
  var defer = q.defer();

  // debtor query
  if (!id) { defer.reject(new Error('No debtor_group id selected!')); }

  const query =
    `SELECT debtor_group.account_id FROM debtor_group
     WHERE debtor_group.uuid=?`;

  db.exec(query, [id])
  .then(function (ans) {
    const accountId = ans.pop().account_id;
    const dgQuery =
      `SELECT c.inv_po_id, c.trans_id, c.trans_date, c.account_id FROM (
        SELECT p.inv_po_id, p.trans_id, p.trans_date, p.account_id
        FROM posting_journal AS p
        WHERE p.account_id=?
        UNION
        SELECT g.inv_po_id, g.trans_date, g.trans_id, g.account_id
        FROM general_ledger AS g
        WHERE g.account_id=?
      ) AS c ;`;

    return db.exec(dgQuery, [accountId, accountId]);
  })
  .then(function (ans) {
    if (!ans.length) { defer.resolve([]); }

    const invoices = ans.map(function (line) {
      return line.inv_po_id;
    });

    const accountId = ans.pop().account_id;

    const sql =
      `SELECT s.reference, s.project_id, t.inv_po_id, t.trans_date, SUM(t.debit_equiv) AS debit,
        SUM(t.credit_equiv) AS credit, SUM(t.debit_equiv - t.credit_equiv) as balance,
        t.account_id, t.deb_cred_uuid, t.currency_id, t.doc_num, t.description, t.account_id,
        t.comment
      FROM (
        (
          SELECT pj.inv_po_id, pj.trans_date, pj.debit,
            pj.credit, pj.debit_equiv, pj.credit_equiv,
            pj.account_id, pj.deb_cred_uuid, pj.currency_id,
            pj.doc_num, pj.trans_id, pj.description, pj.comment
          FROM posting_journal AS pj
        ) UNION (
          SELECT gl.inv_po_id, gl.trans_date, gl.debit,
            gl.credit, gl.debit_equiv, gl.credit_equiv,
            gl.account_id, gl.deb_cred_uuid, gl.currency_id,
            gl.doc_num, gl.trans_id, gl.description, gl.comment
          FROM general_ledger AS gl
        )
      ) AS t JOIN sale AS s on t.inv_po_id = s.uuid
      WHERE t.inv_po_id IN ("${invoices.join('","')}")
      AND t.account_id=?
      GROUP BY t.inv_po_id;`;

    return db.exec(sql, [accountId]);
  })
  .then(function (ans) {
    defer.resolve(ans);
  })
  .catch(function (error) {
    defer.reject(error);
  });

  return defer.promise;
}

function employeeInvoice(employeId) {
  let id = employeId;
  const defer = q.defer();

  // debtor query
  if (!id) {
    defer.reject(new Error('No employee id selected!'));
  } else {
    id = db.escape(id);
  }

  const query =
    `SELECT creditor_group.account_id
    FROM creditor_group
    JOIN creditor ON creditor.group_uuid = creditor_group.uuid
    WHERE creditor.uuid='${id}';`;

  db.exec(query)
  .then(function (ans) {
    const account = ans.pop().account_id;
    const empQuery =
      `SELECT c.inv_po_id, c.trans_id, c.trans_date, c.account_id FROM (
        SELECT p.inv_po_id, p.trans_id, p.trans_date, p.account_id
        FROM posting_journal AS p
        WHERE p.deb_cred_uuid = '${id}'
      UNION
        SELECT g.inv_po_id, g.trans_date, g.trans_id, g.account_id
        FROM general_ledger AS g
        WHERE g.deb_cred_uuid = '${id}')
      AS c;`;

    return db.exec(empQuery);
  })
  .then(function (ans) {
    if (!ans.length) { defer.resolve([]); }

    const invoices = ans.map(function (line) {
      return line.inv_po_id;
    });

    const accountId = ans.pop().account_id;

    const sql =
      `SELECT s.reference, s.project_id, t.inv_po_id, t.trans_date, SUM(t.debit_equiv) AS debit,
      SUM(t.credit_equiv) AS credit, SUM(t.debit_equiv - t.credit_equiv) as balance,
      t.account_id, t.deb_cred_uuid, t.currency_id, t.doc_num, t.deb_cred_type, t.description,
      t.comment
      FROM (
        (
          SELECT 
            posting_journal.inv_po_id, posting_journal.trans_date, posting_journal.debit,
            posting_journal.credit, posting_journal.debit_equiv, posting_journal.credit_equiv,
            posting_journal.account_id, posting_journal.deb_cred_uuid, posting_journal.currency_id,
            posting_journal.doc_num, posting_journal.deb_cred_type, posting_journal.trans_id,
            posting_journal.description, posting_journal.comment
          FROM posting_journal
          WHERE posting_journal.deb_cred_uuid = ' + id + ' AND posting_journal.deb_cred_type = 'C'
        ) UNION (
          SELECT general_ledger.inv_po_id, general_ledger.trans_date, general_ledger.debit,
            general_ledger.credit, general_ledger.debit_equiv, general_ledger.credit_equiv,
            general_ledger.account_id, general_ledger.deb_cred_uuid, general_ledger.currency_id,
            general_ledger.doc_num, general_ledger.deb_cred_type, general_ledger.trans_id,
            general_ledger.description, general_ledger.comment
          FROM general_ledger
          WHERE general_ledger.deb_cred_uuid = '${id}' AND general_ledger.deb_cred_type = 'C'
        )
      ) AS t JOIN sale AS s on t.inv_po_id = s.uuid
      GROUP BY t.inv_po_id;`;

    return db.exec(sql);
  })
  .then(function (ans) {
    defer.resolve(ans);
  })
  .catch(function (error) {
    defer.reject(error);
  });

  return defer.promise;
}

function distributableSale(saleId) {
  let id = saleId;
  var defer = q.defer();

  // debtor query
  if (!id) {
    defer.reject(new Error('No debtor id selected!'));
  } else {
    id = db.escape(id);
  }

  const dsQuery =
    `SELECT account_id
    FROM debtor JOIN debtor_group ON
      debtor.group_uuid = debtor_group.uuid
    WHERE debtor.uuid='id';`;

  db.exec(dsQuery)
  .then(function (ans) {
    const account = ans.pop().account_id;
    const query =
      `SELECT c.inv_po_id, c.account_id, consumption.tracking_number FROM (
        SELECT p.inv_po_id, p.account_id
        FROM posting_journal AS p
        WHERE p.deb_cred_uuid = '${id}' AND p.account_id = '${account}'
      UNION
        SELECT g.inv_po_id, g.account_id
        FROM general_ledger AS g
        WHERE g.deb_cred_uuid = '${id}' AND g.account_id = '${account}')
      AS c left join consumption_patient on c.inv_po_id = consumption_patient.sale_uuid
      JOIN consumption ON consumption_patient.consumption_uuid = consumption.uuid;`;

    return db.exec(query);
  })
  .then(function (ans) {
    if (!ans.length) { defer.resolve([]); }

    const trackingNumbers = ans.filter(function (an) {
      return !an.tracking_number;
    });

    const invoices = trackingNumbers.map(function (line) {
      return line.inv_po_id;
    });

    const accountId = ans.pop().account_id;

    const sql =
      `SELECT s.reference, s.project_id, t.inv_po_id, t.trans_date, SUM(t.debit_equiv) AS debit,
      SUM(t.credit_equiv) AS credit, SUM(t.debit_equiv - t.credit_equiv) as balance,
      t.account_id, t.deb_cred_uuid, t.currency_id, t.doc_num, t.description, t.account_id,
      t.comment
      FROM (
        (
          SELECT posting_journal.inv_po_id, posting_journal.trans_date, posting_journal.debit,
            posting_journal.credit, posting_journal.debit_equiv, posting_journal.credit_equiv,
            posting_journal.account_id, posting_journal.deb_cred_uuid, posting_journal.currency_id,
            posting_journal.doc_num, posting_journal.trans_id, posting_journal.description, posting_journal.comment
          FROM posting_journal
        ) UNION (
          SELECT general_ledger.inv_po_id, general_ledger.trans_date, general_ledger.debit,
            general_ledger.credit, general_ledger.debit_equiv, general_ledger.credit_equiv,
            general_ledger.account_id, general_ledger.deb_cred_uuid, general_ledger.currency_id,
            general_ledger.doc_num, general_ledger.trans_id, general_ledger.description, general_ledger.comment
          FROM general_ledger
        )
      ) AS t JOIN sale AS s on t.inv_po_id = s.uuid
      WHERE t.inv_po_id IN ("${invoices.join('","')}")
      AND t.account_id = '${accountId}'
      GROUP BY t.inv_po_id;`;

    return db.exec(sql);
  })
  .then(function (ans) {
    defer.resolve(ans);
  })
  .catch(function (error) {
    defer.reject(error);
  });

  return defer.promise;
}

// Sale Balance debtor
function debtorSale(debtorId, saleId) {
  let id = debtorId;
  const defer = q.defer();

  // debtor query
  if (!id) {
    defer.reject(new Error('No debtor id selected!'));
  } else {
    id = db.escape(id);
  }

  const query =
    `SELECT account_id
    FROM debtor JOIN debtor_group ON
      debtor.group_uuid = debtor_group.uuid
    WHERE debtor.uuid='${id}';`;

  db.exec(query)
  .then(function (ans) {
    const account = ans.pop().account_id;

    const debtorSaleQuery =
      `SELECT c.inv_po_id, c.trans_id, c.trans_date, c.account_id FROM (
        SELECT p.inv_po_id, p.trans_id, p.trans_date, p.account_id
        FROM posting_journal AS p
        WHERE p.deb_cred_uuid = '${id}' AND p.account_id = '${account}'
      UNION
        SELECT g.inv_po_id, g.trans_date, g.trans_id, g.account_id
        FROM general_ledger AS g
        WHERE g.deb_cred_uuid = '${id}' AND g.account_id = '${account}')
      AS c;`;

    return db.exec(debtorSaleQuery);
  })
  .then(function (ans) {
    if (!ans.length) { defer.resolve([]); }

    const invoices = ans.map(function (line) {
      return line.inv_po_id;
    });

    const accountId = ans.pop().account_id;

    const sql =
      `SELECT s.reference, s.project_id, t.inv_po_id, t.trans_date, SUM(t.debit_equiv) AS debit,
        SUM(t.credit_equiv) AS credit, SUM(t.debit_equiv - t.credit_equiv) as balance,
        t.account_id, t.deb_cred_uuid, t.currency_id, t.doc_num, t.description, t.account_id,
        t.comment, t.canceled, p.abbr, c.document_id,
        IF(ISNULL(c.document_id), 0, 1) AS consumed
      FROM (
        (
          SELECT pj.inv_po_id, pj.trans_date, pj.debit,
            pj.credit, pj.debit_equiv, pj.credit_equiv,
            pj.account_id, pj.deb_cred_uuid, pj.currency_id,
            pj.doc_num, pj.trans_id, pj.description, pj.comment, credit_note.sale_uuid AS canceled
          FROM posting_journal AS pj
          LEFT JOIN credit_note ON credit_note.sale_uuid = pj.inv_po_id WHERE pj.deb_cred_uuid='${id}'
        ) UNION (
          SELECT gl.inv_po_id, gl.trans_date, gl.debit,
            gl.credit, gl.debit_equiv, gl.credit_equiv,
            gl.account_id, gl.deb_cred_uuid, gl.currency_id,
            gl.doc_num, gl.trans_id, gl.description, gl.comment, credit_note.sale_uuid AS canceled
          FROM general_ledger AS gl
          LEFT JOIN credit_note ON credit_note.sale_uuid = gl.inv_po_id WHERE gl.deb_cred_uuid='${id}''
        )
      ) AS t
      JOIN sale AS s ON t.inv_po_id = s.uuid
      JOIN project AS p ON s.project_id = p.id
      LEFT JOIN consumption AS c ON t.inv_po_id = c.document_id
      WHERE t.inv_po_id ='${db.escape(saleId)}'
      AND t.account_id = '${accountId}'
      GROUP BY t.inv_po_id;`;

    return db.exec(sql);
  })
  .then(function (ans) {
    defer.resolve(ans);
  })
  .catch(function (error) {
    defer.reject(error);
  });

  return defer.promise;
}
