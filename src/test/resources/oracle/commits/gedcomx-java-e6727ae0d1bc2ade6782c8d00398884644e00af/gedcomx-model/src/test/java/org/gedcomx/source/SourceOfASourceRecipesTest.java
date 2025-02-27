package org.gedcomx.source;

import org.gedcomx.Gedcomx;
import org.gedcomx.agent.Address;
import org.gedcomx.agent.Agent;
import org.gedcomx.common.*;
import org.gedcomx.rt.GedcomNamespaceManager;
import org.gedcomx.test.RecipeTest;
import org.gedcomx.test.Snippet;
import org.junit.Test;

import java.util.*;

import static org.gedcomx.rt.SerializationUtil.processThroughJson;
import static org.gedcomx.rt.SerializationUtil.processThroughXml;
import static org.junit.Assert.*;


/**
 * @author Ryan Heaton
 */
public class SourceOfASourceRecipesTest extends RecipeTest {
  public static final String SRC_OF_SRC_ID = "S1";
  public static final String DEATH_CERT_FULL_CITATION = "Texas. Bureau of Vital Statistics. Texas death records, 1890-1976. State Registrar Office, Austin. FHL US/CAN Film 2242245. Family History Library, Salt Lake City, Utah";
  public static final String FHL_FILM_COLLECTION_CITATION_TEMPLATE = "http:/source-template-authority/fhl-film-collection-template";
  public static final String FLDNM_COLLECTION_LOCALITY = "collection-locality";
  public static final String FLDNM_AUTHOR = "author";
  public static final String FLDNM_TITLE = "title";
  public static final String FLDNM_ARCHIVE_NAME = "archive-name";
  public static final String FLDNM_ARCHIVE_LOCALITY = "archive-locality";
  public static final String FLDNM_FHL_FILM = "fhl-film";
  public static final String FLDVAL_COLLECTION_LOCALITY1 = "Texas";
  public static final String FLDVAL_AUTHOR1 = "Bureau of Vital Statistics";
  public static final String FLDVAL_TITLE1 = "Texas death records, 1890-1976";
  public static final String FLDVAL_ARCHIVE_NAME1 = "State Registrar Office";
  public static final String FLDVAL_ARCHIVE_LOCALITY1 = "Austin";
  public static final String FLDVAL_FHL_FILM1 = "FHL US/CAN Film 2242245";

  public static final String MEDIATOR_URI_PREFIX = "repository#";

  public static final String SRC_ID = "S2";
  public static final String DEATH_IDX_FULL_CITATION = "\"Texas Deaths, 1890-1976,\" index and images, FamilySearch (https://familysearch.org/pal:/MM9.1.1/J69H-GV1 : accessed 13 April 2012), Lyndon Baines Johnson, 1973";
  public static final String FS_INDEX_DEATHRECORD_CITATION_TEMPLATE = "http://source-template-authority/fsindex-deathrecord-template";
  public static final String FLDNM_DESCRIPTION = "description";
  public static final String FLDNM_PUBLISHER = "publisher";
  public static final String FLDNM_RECORD_PAL = "record-pal";
  public static final String FLDNM_DECADENT = "decadent";
  public static final String FLDNM_DEATH_YEAR = "death-year";
  public static final String FLDNM_DATE_ACCESSED = "date-accessed";
  public static final String RECORD_PAL_LYNDON_B_JOHNSON = "https://familysearch.org/pal:/MM9.1.1/J69H-GV1";
  public static final String PRESIDENT_LYNDON_B_JOHNSON_DEATH_CERTIFICATE = "President Lyndon B Johnson's Death Certificate";
  public static final String SRCDESC_URI_PREFIX = "#";
  public static final String FLDVAL_TITLE2 = "Texas Deaths, 1890-1976";
  public static final String FLDVAL_DESCRIPTION2 = "index and images";
  public static final String FLDVAL_PUBLISHER2 = "FamilySearch";
  public static final String FLDVAL_RECORD_PAL2 = "https://familysearch.org/pal:/MM9.1.1/J69H-GV1";
  public static final String FLDVAL_DECADENT2 = "Lyndon Baines Johnson";
  public static final String FLDVAL_DEATH_YEAR2 = "1973";
  public static final String FLDVAL_DATE_ACCESSED2 = "13 April 2012";

  public static final String LANG_EN_US = "en-US";
  public static final String NOTE_TEXT_1 = "Image available with record if you sign into FamilySearch.";
  public static final String CONTRIBUTOR_1_ID = "#contributorid";
  public static final long MODIFIED_20111111_11_11_11_111 = 1321027871111L;
  public static final String CHANGE_MESSAGE = "Matches published information about his death; referenced by other researchers.";

  public static final String ORG_FS_ID = "R1";
  public static final String FAMILY_SEARCH_INTERNATIONAL = "FamilySearch International";
  public static final String FAMILYSEARCH_HOME_PAGE = "http://familysearch.org/";

  public static final String ORG_FHL_ID = "R2";
  public static final String FAMILY_HISTORY_LIBRARY = "Family History Library";
  public static final String FAMILY_HISTORY_LIBRARY_HOME_PAGE = "https://familysearch.org/locations/saltlakecity-library";
  public static final String FHL_ADDRESS = "Family History Library\n35 N West Temple St\nSalt Lake City, UT 84150";
  public static final String FHL_ADDRESS_STREET1 = "35 N West Temple St";
  public static final String FHL_CITY = "Salt Lake City";
  public static final String FHL_STATE = "UT";
  public static final String FHL_POSTAL_CODE = "84150";
  public static final String FHL_COUNTRY = "United States";
  public static final String FHL_TELEPHONE = "tel:+1-866-406-1830";

  static {
    GedcomNamespaceManager.registerKnownJsonType(SourceDescription.class);
    GedcomNamespaceManager.registerKnownJsonType(Agent.class);
    GedcomNamespaceManager.registerKnownJsonType(Note.class);
  }

  private final Agent orgFamilySearch;
  private final Agent orgFhl;
  {
    orgFamilySearch = new Agent();
    orgFamilySearch.setId(ORG_FS_ID);
    orgFamilySearch.setNames(Arrays.asList(new TextValue(FAMILY_SEARCH_INTERNATIONAL)));
    orgFamilySearch.setHomepage(new ResourceReference(URI.create(FAMILYSEARCH_HOME_PAGE)));

    orgFhl = new Agent();
    orgFhl.setId(ORG_FHL_ID);
    orgFhl.setNames(Arrays.asList(new TextValue(FAMILY_HISTORY_LIBRARY)));
    orgFhl.setHomepage(new ResourceReference(URI.create(FAMILY_HISTORY_LIBRARY_HOME_PAGE)));
    orgFhl.setAddresses(new ArrayList<Address>());
    orgFhl.getAddresses().add(new Address());
    orgFhl.getAddresses().get(0).setValue(FHL_ADDRESS);
    orgFhl.getAddresses().get(0).setStreet(FHL_ADDRESS_STREET1);
    orgFhl.getAddresses().get(0).setCity(FHL_CITY);
    orgFhl.getAddresses().get(0).setStateOrProvince(FHL_STATE);
    orgFhl.getAddresses().get(0).setPostalCode(FHL_POSTAL_CODE);
    orgFhl.getAddresses().get(0).setCountry(FHL_COUNTRY);
    orgFhl.setPhones(new ArrayList<ResourceReference>());
    orgFhl.getPhones().add(new ResourceReference(URI.create(FHL_TELEPHONE)));
  }

  @Test
  public void testTexasDeaths() throws Exception {
    createRecipe("Describing a Texas Deaths Online Record")
      .withDescription("Example for describing an online Texas Death record.")
      .applicableTo(SourceDescription.class);

    Note note = new Note();
    note.setText(NOTE_TEXT_1);
    note.setLang(LANG_EN_US);
    note.setAttribution(new Attribution());
    note.getAttribution().setContributor(new ResourceReference(URI.create(CONTRIBUTOR_1_ID)));
    note.getAttribution().setModified(new Date(MODIFIED_20111111_11_11_11_111)); // 11 Nov 2011 11:11:11.111

    SourceDescription srcDesc1 = new SourceDescription();
    srcDesc1.setId(SRC_OF_SRC_ID);
    srcDesc1.setCitations(Arrays.asList(new SourceCitation()));
    srcDesc1.getCitation().setLang(LANG_EN_US);
    srcDesc1.getCitation().setValue(DEATH_CERT_FULL_CITATION);
    //srcDesc1.getCitation().setCitationTemplate(new ResourceReference(new URI(FHL_FILM_COLLECTION_CITATION_TEMPLATE)));
    //srcDesc1.getCitation().setFields(new ArrayList<CitationField>());
    //srcDesc1.getCitation().getFields().add(new CitationField(URI.create(FLDNM_COLLECTION_LOCALITY), FLDVAL_COLLECTION_LOCALITY1)); // URI to cover relevant constructor
    //srcDesc1.getCitation().getFields().add(new CitationField(FLDNM_AUTHOR, FLDVAL_AUTHOR1));
    //srcDesc1.getCitation().getFields().add(new CitationField(FLDNM_TITLE, FLDVAL_TITLE1));
    //srcDesc1.getCitation().getFields().add(new CitationField(FLDNM_ARCHIVE_NAME, FLDVAL_ARCHIVE_NAME1));
    //srcDesc1.getCitation().getFields().add(new CitationField(FLDNM_ARCHIVE_LOCALITY, FLDVAL_ARCHIVE_LOCALITY1));
    //srcDesc1.getCitation().getFields().add(new CitationField());
    //srcDesc1.getCitation().getFields().get(5).setNameValue(FLDNM_FHL_FILM);
    //srcDesc1.getCitation().getFields().get(5).setValue(FLDVAL_FHL_FILM1);
    srcDesc1.setMediatorURI(URI.create(MEDIATOR_URI_PREFIX + ORG_FHL_ID));

    SourceDescription srcDesc2 = new SourceDescription();
    srcDesc2.setId(SRC_ID);
    srcDesc2.setCitations(Arrays.asList(new SourceCitation()));
    srcDesc2.getCitation().setValue(DEATH_IDX_FULL_CITATION);
    //srcDesc2.getCitation().setCitationTemplate(new ResourceReference(new URI(FS_INDEX_DEATHRECORD_CITATION_TEMPLATE)));
    //srcDesc2.getCitation().setFields(new ArrayList<CitationField>());
    //srcDesc2.getCitation().getFields().add(new CitationField(FLDNM_TITLE, FLDVAL_TITLE2));
    //srcDesc2.getCitation().getFields().add(new CitationField(FLDNM_DESCRIPTION, FLDVAL_DESCRIPTION2));
    //srcDesc2.getCitation().getFields().add(new CitationField(FLDNM_PUBLISHER, FLDVAL_PUBLISHER2));
    //srcDesc2.getCitation().getFields().add(new CitationField(FLDNM_RECORD_PAL, FLDVAL_RECORD_PAL2));
    //srcDesc2.getCitation().getFields().add(new CitationField(FLDNM_DECADENT, FLDVAL_DECADENT2));
    //srcDesc2.getCitation().getFields().add(new CitationField(FLDNM_DEATH_YEAR, FLDVAL_DEATH_YEAR2));
    //srcDesc2.getCitation().getFields().add(new CitationField(FLDNM_DATE_ACCESSED, FLDVAL_DATE_ACCESSED2));
    srcDesc2.setAbout(URI.create(RECORD_PAL_LYNDON_B_JOHNSON));
    srcDesc2.setSources(new ArrayList<SourceReference>());
    srcDesc2.getSources().add(new SourceReference());
    srcDesc2.getSources().get(0).setDescriptionRef(URI.create(SRCDESC_URI_PREFIX + SRC_OF_SRC_ID));
    srcDesc2.setTitles(Arrays.asList(new TextValue((PRESIDENT_LYNDON_B_JOHNSON_DEATH_CERTIFICATE))));
    srcDesc2.setMediatorURI(URI.create(MEDIATOR_URI_PREFIX + ORG_FS_ID));
    srcDesc2.setNotes(new ArrayList<Note>());
    srcDesc2.getNotes().add(note);
    srcDesc2.setAttribution(new Attribution());
    srcDesc2.getAttribution().setContributor(new ResourceReference(URI.create(CONTRIBUTOR_1_ID)));
    srcDesc2.getAttribution().setModified(new Date(MODIFIED_20111111_11_11_11_111)); // 11 Nov 2011 11:11:11.111
    srcDesc2.getAttribution().setChangeMessage(CHANGE_MESSAGE);

    Gedcomx gedcomx = new Gedcomx();
    gedcomx.setSourceDescriptions(new ArrayList<SourceDescription>());
    gedcomx.getSourceDescriptions().add(srcDesc2);
    gedcomx.getSourceDescriptions().add(srcDesc1);
    gedcomx.setAgents(new ArrayList<Agent>());
    gedcomx.getAgents().add(orgFamilySearch);
    gedcomx.getAgents().add(orgFhl);

    Snippet snippet = new Snippet();
    Gedcomx gedcomxThurXml = processThroughXml(gedcomx, snippet);
    Gedcomx gedcomxThruJson = processThroughJson(gedcomx, snippet);
    addSnippet(snippet);

    verifyDeathCertExample(gedcomxThurXml);
    verifyDeathCertExample(gedcomxThruJson);

    // the following adds code coverage for CitationField
    //srcDesc1.getCitation().getFields().get(5).setNameValue((String) null); // for branch coverage on setName
    //assertNull(srcDesc1.getCitation().getFields().get(5).getName()); // for branch coverage on setName

    // the following adds code coverage for SourceDescription
    srcDesc1.setCitations(new ArrayList<SourceCitation>());
    srcDesc1.setMediatorURI((URI) null);
    srcDesc1.setTitles(new ArrayList<TextValue>());
    assertNull(srcDesc1.getCitation());
    assertNull(srcDesc1.getMediator());
    assertEquals(srcDesc1.getTitles().size(), 0);
    assertNull(srcDesc1.getTitle());
    srcDesc1.setCitations(null);
    srcDesc1.setTitles(null);
    assertNull(srcDesc1.getCitation());
    assertNull(srcDesc1.getTitles());
    assertNull(srcDesc1.getTitle());
  }

  private void verifyDeathCertExample(Gedcomx gedcomx) {
    SourceDescription srcDesc2 = null;
    SourceDescription srcDesc1 = null;
    assertEquals(gedcomx.getSourceDescriptions().size(), 2);
    for (SourceDescription srcDesc : gedcomx.getSourceDescriptions()) {
      if (srcDesc.getId().equals(SRC_OF_SRC_ID)) {
        srcDesc1 = srcDesc;
      } else {
        srcDesc2 = srcDesc;
      }
    }

    Agent orgFamilySearch = null;
    Agent orgFhl = null;
    assertEquals(gedcomx.getAgents().size(), 2);
    for (Agent organization : gedcomx.getAgents()) {
      if (organization.getId().equals(ORG_FHL_ID)) {
        orgFhl = organization;
      } else {
        orgFamilySearch = organization;
      }
    }

    assertNotNull(srcDesc1);
    assertEquals(srcDesc1.getId(), SRC_OF_SRC_ID);
    assertNull(srcDesc1.getSources());
    assertNotNull(srcDesc1.getCitation());
    assertEquals(srcDesc1.getCitation().getLang(), LANG_EN_US);
    assertEquals(srcDesc1.getCitation().getValue(), DEATH_CERT_FULL_CITATION);
    //assertEquals(srcDesc1.getCitation().getCitationTemplate().getResource().toURI().toString(), FHL_FILM_COLLECTION_CITATION_TEMPLATE);
    //assertNotNull(srcDesc1.getCitation().getFields());
    //assertEquals(srcDesc1.getCitation().getFields().size(), 6);
    //Map<String, String> fieldNameValuePairs = new HashMap<String, String>();
    //for (CitationField field : srcDesc1.getCitation().getFields()) {
      //fieldNameValuePairs.put(field.getName().toURI().toString(), field.getValue());
    //}
    //assertEquals(fieldNameValuePairs.get(FLDNM_COLLECTION_LOCALITY), FLDVAL_COLLECTION_LOCALITY1);
    //assertEquals(fieldNameValuePairs.get(FLDNM_AUTHOR), FLDVAL_AUTHOR1);
    //assertEquals(fieldNameValuePairs.get(FLDNM_TITLE), FLDVAL_TITLE1);
    //assertEquals(fieldNameValuePairs.get(FLDNM_ARCHIVE_NAME), FLDVAL_ARCHIVE_NAME1);
    //assertEquals(fieldNameValuePairs.get(FLDNM_ARCHIVE_LOCALITY), FLDVAL_ARCHIVE_LOCALITY1);
    //assertEquals(fieldNameValuePairs.get(FLDNM_FHL_FILM), FLDVAL_FHL_FILM1);
    assertEquals(srcDesc1.getMediator().getResource().toURI().toString(), MEDIATOR_URI_PREFIX + ORG_FHL_ID);

    assertNotNull(srcDesc2);
    assertEquals(srcDesc2.getId(), SRC_ID);
    assertEquals(srcDesc2.getAbout().toURI().toString(), RECORD_PAL_LYNDON_B_JOHNSON);
    assertNotNull(srcDesc2.getSources());
    assertEquals(srcDesc2.getSources().size(), 1);
    assertEquals(srcDesc2.getSources().get(0).getDescriptionRef().toURI().toString(), SRCDESC_URI_PREFIX + SRC_OF_SRC_ID);
    assertNotNull(srcDesc2.getCitation());
    assertEquals(srcDesc2.getCitation().getValue(), DEATH_IDX_FULL_CITATION);
    //assertEquals(srcDesc2.getCitation().getCitationTemplate().getResource().toURI().toString(), FS_INDEX_DEATHRECORD_CITATION_TEMPLATE);
    //assertNotNull(srcDesc2.getCitation().getFields());
    //assertEquals(srcDesc2.getCitation().getFields().size(), 7);
    //fieldNameValuePairs = new HashMap<String, String>();
    //for (CitationField field : srcDesc2.getCitation().getFields()) {
      //fieldNameValuePairs.put(field.getName().toURI().toString(), field.getValue());
    //}
    //assertEquals(fieldNameValuePairs.get(FLDNM_TITLE), FLDVAL_TITLE2);
    //assertEquals(fieldNameValuePairs.get(FLDNM_DESCRIPTION), FLDVAL_DESCRIPTION2);
    //assertEquals(fieldNameValuePairs.get(FLDNM_PUBLISHER), FLDVAL_PUBLISHER2);
    //assertEquals(fieldNameValuePairs.get(FLDNM_RECORD_PAL), FLDVAL_RECORD_PAL2);
    //assertEquals(fieldNameValuePairs.get(FLDNM_DECADENT), FLDVAL_DECADENT2);
    //assertEquals(fieldNameValuePairs.get(FLDNM_DEATH_YEAR), FLDVAL_DEATH_YEAR2);
    //assertEquals(fieldNameValuePairs.get(FLDNM_DATE_ACCESSED), FLDVAL_DATE_ACCESSED2);
    //assertEquals(srcDesc2.getMediator().getResource().toURI().toString(), MEDIATOR_URI_PREFIX + ORG_FS_ID);
    //assertEquals(srcDesc2.getTitle().getValue(), PRESIDENT_LYNDON_B_JOHNSON_DEATH_CERTIFICATE);

    assertNotNull(srcDesc2.getNotes());
    assertEquals(srcDesc2.getNotes().size(), 1);
    assertEquals(srcDesc2.getNotes().get(0).getLang(), LANG_EN_US);
    assertEquals(srcDesc2.getNotes().get(0).getText(), NOTE_TEXT_1);
    assertEquals(srcDesc2.getNotes().get(0).getAttribution().getContributor().getResource().toURI().toString(), CONTRIBUTOR_1_ID);
    assertEquals(srcDesc2.getNotes().get(0).getAttribution().getModified().getTime(), MODIFIED_20111111_11_11_11_111);

    assertNotNull(srcDesc2.getAttribution());
    assertEquals(srcDesc2.getAttribution().getContributor().getResource().toURI().toString(), CONTRIBUTOR_1_ID);
    assertEquals(srcDesc2.getAttribution().getModified().getTime(), MODIFIED_20111111_11_11_11_111);
    assertEquals(srcDesc2.getAttribution().getChangeMessage(), CHANGE_MESSAGE);

    assertNotNull(orgFamilySearch);
    assertEquals(orgFamilySearch.getId(), ORG_FS_ID);
    assertEquals(orgFamilySearch.getName().getValue(), FAMILY_SEARCH_INTERNATIONAL);
    assertEquals(orgFamilySearch.getHomepage().getResource().toString(), FAMILYSEARCH_HOME_PAGE);
    assertNull(orgFamilySearch.getAddresses());
    assertNull(orgFamilySearch.getAccounts());
    assertNull(orgFamilySearch.getEmails());
    assertNull(orgFamilySearch.getExtensionElements());
    assertNull(orgFamilySearch.getOpenid());
    assertNull(orgFamilySearch.getPhones());

    assertNotNull(orgFhl);
    assertEquals(orgFhl.getId(), ORG_FHL_ID);
    assertEquals(orgFhl.getName().getValue(), FAMILY_HISTORY_LIBRARY);
    assertEquals(orgFhl.getHomepage().getResource().toString(), FAMILY_HISTORY_LIBRARY_HOME_PAGE);
    assertNotNull(orgFhl.getAddresses());
    assertEquals(orgFhl.getAddresses().size(), 1);
    assertEquals(orgFhl.getAddresses().get(0).getValue(), FHL_ADDRESS);
    assertEquals(orgFhl.getAddresses().get(0).getStreet(), FHL_ADDRESS_STREET1);
    assertNull(orgFhl.getAddresses().get(0).getStreet2());
    assertNull(orgFhl.getAddresses().get(0).getStreet3());
    assertEquals(orgFhl.getAddresses().get(0).getCity(), FHL_CITY);
    assertEquals(orgFhl.getAddresses().get(0).getStateOrProvince(), FHL_STATE);
    assertEquals(orgFhl.getAddresses().get(0).getCountry(), FHL_COUNTRY);
    assertNotNull(orgFhl.getPhones());
    assertEquals(orgFhl.getPhones().size(), 1);
    assertEquals(orgFhl.getPhones().get(0).getResource().toURI().toString(), FHL_TELEPHONE);
    assertNull(orgFhl.getAccounts());
    assertNull(orgFhl.getEmails());
    assertNull(orgFhl.getExtensionElements());
    assertNull(orgFhl.getOpenid());
  }

  @Test
  public void test1930Census() throws Exception {
    createRecipe("Describing a 1930 Census Online Record")
      .withDescription("Example for describing an online 1930 Census Record.")
      .applicableTo(SourceDescription.class);

    String orgIdNara = "R3";

    Agent orgNara = new Agent();
    orgNara.setId(orgIdNara);
    orgNara.setNames(Arrays.asList(new TextValue("National Archives and Records Administration")));
    orgNara.setHomepage(new ResourceReference(URI.create("http://www.archives.gov/")));
    orgNara.setAddresses(new ArrayList<Address>());
    orgNara.getAddresses().add(new Address());
    orgNara.getAddresses().get(0).setValue("The National Archives and Records Administration\n8601 Adelphi Road\nCollege Park, MD 20740-6001");
    orgNara.getAddresses().get(0).setStreet("8601 Adelphi Rd");
    orgNara.getAddresses().get(0).setCity("College Park");
    orgNara.getAddresses().get(0).setStateOrProvince("MD");
    orgNara.getAddresses().get(0).setPostalCode("20740-6001");
    orgNara.getAddresses().get(0).setCountry("United States");
    orgNara.setPhones(new ArrayList<ResourceReference>());
    orgNara.getPhones().add(new ResourceReference(URI.create("tel:+1-866-272-6272")));
    orgNara.getPhones().add(new ResourceReference(URI.create("fax:+1-301-837-0483")));

    Note note = new Note();
    note.setLang("en-US");
    note.setText("Image available with record.");
    note.setAttribution(new Attribution());
    note.getAttribution().setContributor(new ResourceReference(URI.create("#contributorid")));
    note.getAttribution().setModified(new Date(1321027871111L)); // 11 Nov 2011 11:11:11.111

    String sourceS1 = "S1";
    String sourceOfS1 = "S2";
    String sourceOfS2 = "S3";
    String sourceS3 = "S4";

    SourceDescription srcDesc0 = new SourceDescription();
    srcDesc0.setId(sourceS3);
    srcDesc0.setCitations(Arrays.asList(new SourceCitation()));
    srcDesc0.getCitation().setValue("Bureau of the Census. \"Population Schedules for the 1930 Census.\" NARA microfilm publication T626, roll 523. National Archives and Records Administration, Washington D.C.");
    //srcDesc0.getCitation().setCitationTemplate(new ResourceReference(new URI("http:/source-template-authority/nara-microfilm-pub-template")));
    //srcDesc0.getCitation().setFields(new ArrayList<CitationField>());
    //srcDesc0.getCitation().getFields().add(new CitationField("creator", "Bureau of the Census"));
    //srcDesc0.getCitation().getFields().add(new CitationField("title", "Population Schedules for the 1930 Census"));
    //srcDesc0.getCitation().getFields().add(new CitationField("nara-film-pub", "T626"));
    //srcDesc0.getCitation().getFields().add(new CitationField("nara-film-roll", "523"));
    //srcDesc0.getCitation().getFields().add(new CitationField("archive", "National Archives and Records Administration"));
    //srcDesc0.getCitation().getFields().add(new CitationField("archive-locality", "Washington D.C."));
    srcDesc0.setMediatorURI(URI.create("repository#" + orgIdNara));

    SourceDescription srcDesc1 = new SourceDescription();
    srcDesc1.setId(sourceOfS2);
    srcDesc1.setCitations(Arrays.asList(new SourceCitation()));
    srcDesc1.getCitation().setValue("United States. Bureau of the Census. 15th census, 1930. United States, 1930 federal census : population schedules; NARA microfilm publication T626. National Archives and Records Administration, Washington D.C. FHL US/CAN Census Area Film 2340258. Family History Library, Salt Lake City, Utah");
    //srcDesc1.getCitation().setCitationTemplate(new ResourceReference(new URI("http:/source-template-authority/fhl-film-collection-template")));
    //srcDesc1.getCitation().setFields(new ArrayList<CitationField>());
    //srcDesc1.getCitation().getFields().add(new CitationField("collection-locality", "United States"));
    //srcDesc1.getCitation().getFields().add(new CitationField("author", "Bureau of the Census. 15th census, 1930"));
    //srcDesc1.getCitation().getFields().add(new CitationField("title", "United States, 1930 federal census : population schedules ; NARA microfilm publication T626"));
    //srcDesc1.getCitation().getFields().add(new CitationField("archive-name", "National Archives and Records Administration"));
    //srcDesc1.getCitation().getFields().add(new CitationField("archive-locality", "Washington D.C"));
    //srcDesc1.getCitation().getFields().add(new CitationField("fhl-film", "FHL US/CAN Census Area Film 2340258"));
    srcDesc1.setSources(new ArrayList<SourceReference>());
    srcDesc1.getSources().add(new SourceReference());
    srcDesc1.getSources().get(0).setDescriptionRef(URI.create("#" + sourceS3));
    srcDesc1.setMediatorURI(URI.create("repository#" + ORG_FHL_ID));

    SourceDescription srcDesc2 = new SourceDescription();
    srcDesc2.setId(sourceOfS1);
    srcDesc2.setCitations(Arrays.asList(new SourceCitation()));
    srcDesc2.getCitation().setValue("\"United States Census, 1930,\" index and images, FamilySearch (https://familysearch.org/pal:/MM9.1.1/XSYY-Q6P : accessed 12 July 2012), Ronald Reagan in household of John E Reagan, Dixon, Lee, Illinois.");
    //srcDesc2.getCitation().setCitationTemplate(new ResourceReference(new URI("http://source-template-authority/fsindex-uscensus-template")));
    //srcDesc2.getCitation().setFields(new ArrayList<CitationField>());
    //srcDesc2.getCitation().getFields().add(new CitationField("title", "United States Census, 1930"));
    //srcDesc2.getCitation().getFields().add(new CitationField("description", "index and images"));
    //srcDesc2.getCitation().getFields().add(new CitationField("publisher", "FamilySearch"));
    srcDesc2.setAbout(URI.create("https://www.familysearch.org/search/collection/show#uri=http://www.familysearch.org/searchapi/search/collection/1810731"));
    srcDesc2.setSources(new ArrayList<SourceReference>());
    srcDesc2.getSources().add(new SourceReference());
    srcDesc2.getSources().get(0).setDescriptionRef(URI.create("#" + sourceOfS2));
    srcDesc2.setTitles(Arrays.asList(new TextValue("1930 US Census")));
    srcDesc2.setMediatorURI(URI.create("repository#" + ORG_FS_ID));
    srcDesc2.setNotes(new ArrayList<Note>());
    srcDesc2.getNotes().add(note);


    SourceDescription srcDesc3 = new SourceDescription();
    srcDesc3.setId(sourceS1);
    srcDesc3.setCitations(Arrays.asList(new SourceCitation()));
    srcDesc3.getCitation().setValue("(https://familysearch.org/pal:/MM9.1.1/XSYY-Q6P : accessed 12 July 2012), Ronald Reagan in household of John E Reagan, Dixon, Lee, Illinois.");
    //srcDesc3.getCitation().setCitationTemplate(new ResourceReference(new URI("http://source-template-authority/fsindex-uscensus-template/details")));
    //srcDesc3.getCitation().setFields(new ArrayList<CitationField>());
    //srcDesc3.getCitation().getFields().add(new CitationField("record-pal", "https://familysearch.org/pal:/MM9.1.1/J69H-GV1"));
    //srcDesc3.getCitation().getFields().add(new CitationField("person-of-interest", "Ronald Reagan"));
    //srcDesc3.getCitation().getFields().add(new CitationField("head-of-household-if-not-poi", "John E Reagan"));
    //srcDesc3.getCitation().getFields().add(new CitationField("line", "44"));
    //srcDesc3.getCitation().getFields().add(new CitationField("family", "207"));
    //srcDesc3.getCitation().getFields().add(new CitationField("sheet", "7A"));
    //srcDesc3.getCitation().getFields().add(new CitationField("enumeration-district", "52-0017"));
    //srcDesc3.getCitation().getFields().add(new CitationField("incorporated-place", "Dixon"));
    //srcDesc3.getCitation().getFields().add(new CitationField("county", "Lee"));
    //srcDesc3.getCitation().getFields().add(new CitationField("state", "Illinois"));
    //srcDesc3.getCitation().getFields().add(new CitationField("accessed", "12 July 2012"));
    srcDesc3.setAbout(URI.create("https://familysearch.org/pal:/MM9.1.1/XSYY-Q6P"));
    srcDesc3.setComponentOf(new SourceReference());
    srcDesc3.getComponentOf().setDescriptionRef(URI.create("#" + sourceOfS1));
    srcDesc3.setTitles(Arrays.asList(new TextValue("President Ronald Reagan with his parents in 1830 census")));
    srcDesc3.setNotes(new ArrayList<Note>());
    srcDesc3.getNotes().add(note);

    Gedcomx gedcomx = new Gedcomx();
    gedcomx.setSourceDescriptions(new ArrayList<SourceDescription>());
    gedcomx.getSourceDescriptions().add(srcDesc3);
    gedcomx.getSourceDescriptions().add(srcDesc2);
    gedcomx.getSourceDescriptions().add(srcDesc1);
    gedcomx.getSourceDescriptions().add(srcDesc0);
    gedcomx.setAgents(new ArrayList<Agent>());
    gedcomx.getAgents().add(orgFamilySearch);
    gedcomx.getAgents().add(orgFhl);
    gedcomx.getAgents().add(orgNara);

    Snippet snippet = new Snippet();
    Gedcomx gedcomxThruXml = processThroughXml(gedcomx, snippet);
    Gedcomx gedcomxThruJson = processThroughJson(gedcomx, snippet);
    addSnippet(snippet);

    verify1930CensusExample(gedcomxThruXml);
    verify1930CensusExample(gedcomxThruJson);
  }

  void verify1930CensusExample(Gedcomx gedcomx) {
    // TODO: verify contents of resource set
  }
}
