/* eslint-disable import/no-extraneous-dependencies */
const del = require('del');
const merge = require('merge-stream');
const { exec } = require('child_process');
const mergeJson = require('gulp-merge-json');

const {
  src, dest, series, parallel, watch,
} = require('gulp');

const {
  CLIENT_FOLDER,
} = require('./util');

// toggle client javascript minification
const supportedLanguages = {
  en : { key : 'en', path : 'client/src/i18n/en/' },
  fr : { key : 'fr', path : 'client/src/i18n/fr/' },
};

function collectTranslationFiles(details) {
  return src(`${details.path}/**/*.json`)
    .pipe(mergeJson({ fileName : `${details.key}.json` }))
    .pipe(dest(`${CLIENT_FOLDER}/i18n/`));
}

// Custom i18n linting task, promise wrapper to work with gulp ecosystem
// TODO(@jniles) - gulp allows you to return exec().  Can we just return
// the raw exec() call without the callback wrapper?
// See: https://gulpjs.com/docs/en/getting-started/async-completion
function lintI18n() {
  const compareUtilityPath = './utilities/translation/tfcomp.js';
  const utilityCommand = `node ${compareUtilityPath} ${supportedLanguages.en.path} ${supportedLanguages.fr.path}`;
  return exec(utilityCommand);
}

function compileI18n() {
  const en = collectTranslationFiles(supportedLanguages.en);
  const fr = collectTranslationFiles(supportedLanguages.fr);
  return merge(en, fr);
}

// Cleaner helpers
// These methods clean up folders that are affected by changes in the repository
const cleanI18n = () => del(`${CLIENT_FOLDER}/i18n`);
const buildI18n = series(parallel(lintI18n, cleanI18n), compileI18n);

const compile = buildI18n;
exports.watch = () => watch('client/src/i18n/**/*.json', compile);
exports.compile = compile;
