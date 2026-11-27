/**
 * @description
 * This service is responsible for producing PDFs on the server, it accepts handlebar templates and uses the html
 * renderer to produce valid HTML - then streaming this through the wkhtmltopdf application to produce PDFs.
 *
 * @requires wkhtmltopdf
 * @requires lodash
 * @requires stream-to-promise
 * @requires path
 * @requires q
 * @requires process
 */

const wkhtmltopdf = require('wkhtmltopdf');
const _ = require('lodash');
const streamToPromise = require('stream-to-promise');
const path = require('path');

const html = require('./html');

const headers = {
  'Content-Type' : 'application/pdf',
};

exports.render = renderPDF;
exports.headers = headers;
exports.extension = '.pdf';

// provide uniform default configurations for reports
exports.defaultReportOptions = {
  pageSize : 'A4',
  orientation : 'portrait',
};

// standard specification for point of sale receipts
exports.posReceiptOptions = {
  pageWidth : '72mm',
  pageHeight : '290mm',
  marginLeft : '0mm',
  marginRight : '0mm',
  marginBottom : '0mm',
  marginTop : '0mm',
  orientation : 'portrait',
};

// smaller format for providing identifications/ receipts with reduced information
exports.reducedCardOptions = {
  pageWidth : '75mm',       // 2.95in
  pageHeight : '125mm',     // 4.92.in
  marginLeft : '5mm',       // 0.20in
  marginRight : '5mm',      // 0.20in
  marginTop : '5mm',        // 0.20in
  marginBottom : '5mm',     // 0.20in
  orientation : 'landscape',
};

/**
 *
 * @param {Object} context    Object of keys and values that will be made available to the handlebar template
 * @param {String} template   Path to a handlebars template
 * @returns {Promise}         Promise resolving in compiled PDF
 */
function renderPDF(context, template, options = {}) {
  // pdf requires absolute path to be passed to templates to be picked up by wkhtmltopdf on windows
  context.absolutePath = path.join(process.cwd(), 'client');

  return html.render(context, template, options)
    .then((htmlStringResult) => {
      // pick options relevent to rendering PDFs
      const pdfOptions = _.pick(options, [
        'pageSize', 'orientation', 'pageWidth', 'pageHeight', 'marginLeft', 'marginRight', 'marginTop', 'marginBottom',
      ]);

      // pass the compiled html string to the wkhtmltopdf process, this is just a wrapper for the CLI utility
      const pdfStream = wkhtmltopdf(htmlStringResult, pdfOptions);

      // this promise will only be resolved once the stream 'end' event is fired - with this implementation this will
      // not allow the client to receive chunks of data as they are available
      return streamToPromise(pdfStream);
    });
}
