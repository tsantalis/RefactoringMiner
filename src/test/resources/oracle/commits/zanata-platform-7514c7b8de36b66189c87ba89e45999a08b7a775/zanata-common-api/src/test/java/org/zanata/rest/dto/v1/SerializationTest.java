package org.zanata.rest.dto.v1;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.ValidationException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.common.ContentState;
import org.zanata.common.ContentType;
import org.zanata.common.LocaleId;
import org.zanata.common.ResourceType;
import org.zanata.rest.JaxbUtil;
import org.zanata.rest.dto.Glossary;
import org.zanata.rest.dto.GlossaryEntry;
import org.zanata.rest.dto.GlossaryTerm;
import org.zanata.rest.dto.Link;
import org.zanata.rest.dto.Links;
import org.zanata.rest.dto.Person;
import org.zanata.rest.dto.Project;
import org.zanata.rest.dto.extensions.comment.SimpleComment;
import org.zanata.rest.dto.extensions.gettext.HeaderEntry;
import org.zanata.rest.dto.extensions.gettext.PoHeader;
import org.zanata.rest.dto.extensions.gettext.PoTargetHeader;
import org.zanata.rest.dto.extensions.gettext.PotEntryHeader;
import org.zanata.rest.dto.extensions.gettext.TextFlowExtension;
import org.zanata.rest.dto.extensions.gettext.TextFlowTargetExtension;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.ResourceMeta;
import org.zanata.rest.dto.resource.TextFlow;
import org.zanata.rest.dto.resource.TextFlowTarget;
import org.zanata.rest.dto.resource.TranslationsResource;

/**
 * @deprecated These tests are no longer working.
 * @see http://java.net/jira/browse/JAXB-828 This bug in the generation of any
 *      xml schema that references the xml namespace
 *      (http://www.w3.org/XML/1998/namespace) causes these tests to fail.
 */
@Test(groups = { "unit-tests" })
public class SerializationTest {

    protected ObjectMapper mapper;
    private final Logger log = LoggerFactory.getLogger(SerializationTest.class);

    @BeforeMethod
    public void setup() {
        mapper = new ObjectMapper();
        // AnnotationIntrospector introspector = new
        // JaxbAnnotationIntrospector();
        // mapper.getDeserializationConfig().setAnnotationIntrospector(introspector);
        // mapper.getSerializationConfig().setAnnotationIntrospector(introspector);
    }

    private Person createPerson() {
        return new Person("id", "name");
    }

    @Test
    public void serializeAndDeserializeProject() throws JAXBException,
            JsonGenerationException, JsonMappingException, IOException,
            URISyntaxException {
        Project p = new Project().createSample();

        Links links = new Links();
        links.add(new Link(new URI("http://www.zanata.org"), "", "linkType"));
        links.add(new Link(new URI("http://www2.zanata.org"), "", "linkType"));
        p.setLinks(links);

        JaxbUtil.validateXml(p);

        String output = mapper.writeValueAsString(p);

        Project p2 = mapper.readValue(output, Project.class);
        assertThat(p2, notNullValue());
        JaxbUtil.validateXml(p2);

        p2 = JaxbTestUtil.roundTripXml(p);
        System.out.println(p2);
        assertThat(p2, notNullValue());
    }

    @Test
    public void serializeAndDeserializePerson() throws JAXBException,
            JsonGenerationException, JsonMappingException, IOException {
        Person p = createPerson();
        JaxbUtil.validateXml(p);

        String output = mapper.writeValueAsString(p);

        Person p2 = mapper.readValue(output, Person.class);
        assertThat(p2, notNullValue());
        JaxbUtil.validateXml(p2);

        p2 = JaxbTestUtil.roundTripXml(p);
        // System.out.println(p2);
        assertThat(p2, notNullValue());
    }

    private PoHeader createPoHeader() {
        return new PoHeader("hello world");
    }

    @Test(enabled = false)
    public void serializeAndDeserializeExtension()
            throws JsonGenerationException, JsonMappingException, IOException,
            JAXBException {
        // TODO are we actually trying to test serializing an extension where
        // the type is not known?

        PoHeader e = createPoHeader();
        JaxbUtil.validateXml(e);

        String output = mapper.writeValueAsString(e);
        PoHeader e2 = mapper.readValue(output, PoHeader.class);
        JaxbUtil.validateXml(e2);
        assertThat(e2, instanceOf(PoHeader.class));

        e2 = JaxbTestUtil.roundTripXml(e, PoHeader.class);
        assertThat(e2, instanceOf(PoHeader.class));
    }

    // FIXME broken test
    @Test(enabled = false)
    public void serializeAndDeserializeTranslationResource()
            throws JsonGenerationException, JsonMappingException, IOException,
            JAXBException {
        ResourceMeta res = new ResourceMeta("id");
        res.getExtensions(true).add(
                new PoHeader("comment", new HeaderEntry("h1", "v1"),
                        new HeaderEntry("h2", "v2")));
        JaxbUtil.validateXml(res, PoHeader.class);

        String output = mapper.writeValueAsString(res);
        ResourceMeta res2 = mapper.readValue(output, ResourceMeta.class);

        assertThat(res2.getExtensions().size(), is(1));
        assertThat(res2.getExtensions().iterator().next(),
                instanceOf(PoHeader.class));
        assertThat(
                ((PoHeader) res2.getExtensions().iterator().next())
                        .getComment(),
                is("comment"));

        res2 = JaxbTestUtil.roundTripXml(res, PoHeader.class);
        assertThat(res2, notNullValue());
        assertThat(res2.getExtensions().size(), is(1));
        assertThat(res2.getExtensions().iterator().next(),
                instanceOf(PoHeader.class));
    }

    // FIXME broken test
    @Test(enabled = false)
    public void serializeSourceResource() throws JsonGenerationException,
            JsonMappingException, IOException, JAXBException {
        Resource sourceResource = new Resource("Acls.pot");
        sourceResource.setType(ResourceType.FILE);
        sourceResource.setContentType(ContentType.PO);
        sourceResource.setLang(LocaleId.EN);
        TextFlow tf = new TextFlow();
        tf.setContents("ttff");
        TextFlow tf2 = new TextFlow();
        tf2.setContents("ttff2");
        sourceResource.getTextFlows().add(tf);
        sourceResource.getTextFlows().add(tf2);
        sourceResource.getExtensions(true).add(
                new PoHeader("comment", new HeaderEntry("h1", "v1"),
                        new HeaderEntry("h2", "v2")));

        JaxbUtil.validateXml(sourceResource, Resource.class);

        String output = mapper.writeValueAsString(sourceResource);
        log.info(output);
        Resource res2 = mapper.readValue(output, Resource.class);

        assertThat(res2.getExtensions().size(), is(1));
        assertThat(res2.getExtensions().iterator().next(),
                instanceOf(PoHeader.class));
        assertThat(
                ((PoHeader) res2.getExtensions().iterator().next())
                        .getComment(),
                is("comment"));
    }

    // FIXME broken test
    @Test(enabled = false)
    public void serializeAndDeserializeTextFlow() throws ValidationException,
            JsonGenerationException, JsonMappingException, IOException {
        TextFlow tf = new TextFlow();
        tf.setContents("ttff");
        SimpleComment comment = new SimpleComment("test");
        PotEntryHeader pot = new PotEntryHeader();
        pot.setContext("context");
        pot.getReferences().add("fff");
        tf.getExtensions(true).add(comment);
        tf.getExtensions(true).add(pot);

        JaxbUtil.validateXml(tf, TextFlow.class);

        String output = mapper.writeValueAsString(tf);
        TextFlow res2 = mapper.readValue(output, TextFlow.class);

        assertThat(res2.getExtensions(true).size(), is(2));
        for (TextFlowExtension e : res2.getExtensions()) {
            if (e instanceof SimpleComment) {
                assertThat(((SimpleComment) e).getValue(), is("test"));
            }
            if (e instanceof PotEntryHeader) {
                assertThat(((PotEntryHeader) e).getContext(), is("context"));
            }
        }
    }

    // FIXME broken test
    @Test(enabled = false)
    public void serializeAndDeserializeTextFlowTarget()
            throws ValidationException, JsonGenerationException,
            JsonMappingException, IOException {
        TextFlowTarget tf = new TextFlowTarget();
        tf.setTranslator(createPerson());
        tf.setContents("ttff");
        SimpleComment comment = new SimpleComment("testcomment");
        tf.getExtensions(true).add(comment);

        JaxbUtil.validateXml(tf, TextFlowTarget.class);

        String output = mapper.writeValueAsString(tf);
        TextFlowTarget res2 = mapper.readValue(output, TextFlowTarget.class);

        assertThat(res2.getExtensions(true).size(), is(1));
        for (TextFlowTargetExtension e : res2.getExtensions()) {
            if (e instanceof SimpleComment) {
                assertThat(((SimpleComment) e).getValue(), is("testcomment"));
            }
        }
    }

    // FIXME broken test
    @Test(enabled = false)
    public void serializeAndDeserializeTranslation()
            throws JsonGenerationException, JsonMappingException, IOException,
            JAXBException {
        TranslationsResource entity = new TranslationsResource();
        TextFlowTarget target = new TextFlowTarget("rest1");
        target.setContents("hello world");
        target.setState(ContentState.Translated);
        target.setTranslator(new Person("root@localhost", "Admin user"));
        // for the convenience of test
        entity.getTextFlowTargets().add(target);
        entity.getExtensions(true);
        PoTargetHeader poTargetHeader =
                new PoTargetHeader("target header comment", new HeaderEntry(
                        "ht", "vt1"), new HeaderEntry("th2", "tv2"));

        entity.getExtensions(true).add(poTargetHeader);

        JaxbUtil.validateXml(entity, TranslationsResource.class);

        String output = mapper.writeValueAsString(entity);
        TranslationsResource res2 =
                mapper.readValue(output, TranslationsResource.class);

        assertThat(res2.getExtensions().size(), is(1));
        assertThat(res2.getExtensions().iterator().next(),
                instanceOf(PoTargetHeader.class));
        assertThat(
                ((PoTargetHeader) res2.getExtensions().iterator().next())
                        .getComment(),
                is("target header comment"));
    }

    // FIXME broken test
    @Test(enabled = false)
    public void serializeAndDeserializeGlossary()
            throws JsonGenerationException, JsonMappingException, IOException,
            JAXBException {
        Glossary glossary = new Glossary();
        glossary.getSourceLocales().add("en-US");

        glossary.getTargetLocales().add("jp");
        glossary.getTargetLocales().add("de");

        GlossaryEntry entry = new GlossaryEntry();
        entry.setSrcLang(LocaleId.EN_US);
        entry.setSourcereference("source ref");

        GlossaryTerm term = new GlossaryTerm();
        term.setContent("testData1");
        term.setLocale(LocaleId.EN_US);
        term.getComments().add("comment1");
        term.getComments().add("comment2");
        term.getComments().add("comment3");

        GlossaryTerm term2 = new GlossaryTerm();
        term2.setContent("testData2");
        term2.setLocale(LocaleId.DE);
        term2.getComments().add("comment4");
        term2.getComments().add("comment5");
        term2.getComments().add("comment6");

        entry.getGlossaryTerms().add(term);
        entry.getGlossaryTerms().add(term2);
        glossary.getGlossaryEntries().add(entry);

        // System.out.println(glossary);
        JaxbUtil.validateXml(glossary, Glossary.class);
        String output = mapper.writeValueAsString(glossary);
        Glossary glossary2 = mapper.readValue(output, Glossary.class);
        assertThat(glossary2.getGlossaryEntries().size(), is(1));
        assertThat(glossary2.getGlossaryEntries().get(0).getGlossaryTerms()
                .size(), is(2));

    }
}
