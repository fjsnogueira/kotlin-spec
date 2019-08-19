source directories.sh
source settings.sh

init_settings "html"

export PROJECT_DIR

cd ${PROJECT_DIR}/docs


TMP_DIR=$(pwd)/../${BUILD_DIRECTORY}/~tmp

mkdir -p $TMP_DIR

gpp -H ./index.md \
| pandoc ${PREAMBLE_OPTIONS} ${COMMON_PANDOC_OPTIONS} -t json \
| bash ${FILTERS_DIR}/processTodoFilter.sh html \
| bash ${FILTERS_DIR}/markSentencesFilter.sh html \
| bash ${FILTERS_DIR}/copyPasteFilter.sh html \
| bash ${FILTERS_DIR}/inlineDiagramFilter.sh html \
| bash ${FILTERS_DIR}/inlineCodeIndentFilter.sh html \
| bash ${FILTERS_DIR}/mathCleanUpFilter.sh html \
| bash ${FILTERS_DIR}/splitSections.sh --output-directory=$TMP_DIR

mkdir -p ../${BUILD_DIRECTORY}/html/sections

for f in $TMP_DIR/*.json;
do \
pandoc $f ${HTML_ASSETS_OPTIONS} -s -o ../${BUILD_DIRECTORY}/html/sections/"$(basename "$f" .json).html";
done

rm -rf $TMP_DIR