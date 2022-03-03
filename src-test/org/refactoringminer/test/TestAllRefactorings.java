package org.refactoringminer.test;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestAllRefactorings extends LightJavaCodeInsightFixtureTestCase {

	private static final String REPOS = "tmp1";
	private List<RefactoringPopulator.Root> roots;
	private TestBuilder test;

	protected void setUp() throws Exception {
		super.setUp();
		this.roots = RefactoringPopulator.getFSERefactorings(Refactorings.All.getValue());
		this.test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), REPOS, Refactorings.All.getValue());
	}

	private void processRepositoryCommits(List<String> gitURL, int expectedTPs, int expectedFPs, int expectedFNs) throws Exception {
		for (RefactoringPopulator.Root root : roots) {
			if (gitURL.contains(root.repository))
				test.project(root.repository, "master").atCommit(root.sha1)
						.containsOnly(RefactoringPopulator.extractRefactorings(root.refactorings));
		}
		test.assertExpectations(getProject(), expectedTPs, expectedFPs, expectedFNs, false);
	}

	@Test
	public void testBatch1() throws Exception {
		String gitURLs[] = new String[] {
				"https://github.com/apache/drill.git",
				"https://github.com/ReactiveX/RxJava.git",
				"https://github.com/CyanogenMod/android_frameworks_base.git",
				"https://github.com/real-logic/Aeron.git",
				"https://github.com/gradle/gradle.git",
				"https://github.com/geometer/FBReaderJ.git",
				"https://github.com/spring-projects/spring-data-neo4j.git",
				"https://github.com/wildfly/wildfly.git",
				"https://github.com/fabric8io/fabric8.git",
				"https://github.com/joel-costigliola/assertj-core.git",
				"https://github.com/structr/structr.git",
				"https://github.com/ignatov/intellij-erlang.git",
				"https://github.com/geoserver/geoserver.git",
				"https://github.com/BuildCraft/BuildCraft.git",
				"https://github.com/HubSpot/Singularity.git",
				"https://github.com/katzer/cordova-plugin-local-notifications.git",
				"https://github.com/FasterXML/jackson-databind.git",
				"https://github.com/aws/aws-sdk-java.git",
				"https://github.com/linkedin/rest.li.git",
				"https://github.com/open-keychain/open-keychain.git",
				"https://github.com/baasbox/baasbox.git",
				"https://github.com/phishman3579/java-algorithms-implementation.git",
				"https://github.com/apache/cassandra.git",
				"https://github.com/JetBrains/intellij-community.git",
		};
		//TP: 2880  FP:  12  FN: 63
		processRepositoryCommits(Arrays.asList(gitURLs), 2880, 12, 63);
	}

	@Test
	public void testBatch2() throws Exception {
		String gitURLs[] = new String[]{
				"https://github.com/square/wire.git",
				"https://github.com/abarisain/dmix.git",
				"https://github.com/netty/netty.git",
				"https://github.com/HdrHistogram/HdrHistogram.git",
				"https://github.com/grails/grails-core.git",
				"https://github.com/cwensel/cascading.git",
				"https://github.com/JetBrains/intellij-plugins.git",
				"https://github.com/dropwizard/metrics.git",
				"https://github.com/google/guava.git",
				"https://github.com/apache/giraph.git",
				"https://github.com/siacs/Conversations.git",
				"https://github.com/Netflix/genie.git",
				"https://github.com/eclipse/vert.x.git",
				"https://github.com/Netflix/eureka.git",
				"https://github.com/scobal/seyren.git",
				"https://github.com/wicketstuff/core.git",
				"https://github.com/spring-projects/spring-integration.git",
				"https://github.com/orfjackal/retrolambda.git",
				"https://github.com/tomahawk-player/tomahawk-android.git",
				"https://github.com/raphw/byte-buddy.git",
				"https://github.com/liferay/liferay-plugins.git",
				"https://github.com/jenkinsci/workflow-plugin.git",
				"https://github.com/gwtproject/gwt.git",
				"https://github.com/google/truth.git",
				"https://github.com/antlr/antlr4.git",
				"https://github.com/koush/AndroidAsync.git",
				"https://github.com/thymeleaf/thymeleaf.git",
				"https://github.com/PhilJay/MPAndroidChart.git",
				"https://github.com/spring-projects/spring-boot.git",
				"https://github.com/brettwooldridge/HikariCP.git",
				"https://github.com/eucalyptus/eucalyptus.git",
				"https://github.com/dreamhead/moco.git",
				"https://github.com/neo4j/neo4j.git",
				"https://github.com/skylot/jadx.git",
				"https://github.com/vaadin/vaadin.git",
				"https://github.com/restlet/restlet-framework-java.git",
				"https://github.com/redsolution/xabber-android.git",
				"https://github.com/codinguser/gnucash-android.git",
				"https://github.com/spring-projects/spring-data-jpa.git",
				"https://github.com/loopj/android-async-http.git",
				"https://github.com/datastax/java-driver.git",
				"https://github.com/SimonVT/schematic.git",
				"https://github.com/jfinal/jfinal.git",
				"https://github.com/oblac/jodd.git",
				"https://github.com/realm/realm-java.git",
				"https://github.com/bennidi/mbassador.git",
				"https://github.com/Athou/commafeed.git",
				"https://github.com/hazelcast/hazelcast.git",
				"https://github.com/cbeust/testng.git",
				"https://github.com/querydsl/querydsl.git",
				"https://github.com/mockito/mockito.git",
				"https://github.com/mrniko/redisson.git",
				"https://github.com/clojure/clojure.git",
				"https://github.com/belaban/JGroups.git",
				"https://github.com/WhisperSystems/Signal-Android.git",
				"https://github.com/Activiti/Activiti.git",
				"https://github.com/kuujo/copycat.git",
				"https://github.com/jankotek/MapDB.git",
				"https://github.com/undertow-io/undertow.git",
				"https://github.com/jberkel/sms-backup-plus.git",
				"https://github.com/apache/tomcat.git",
				"https://github.com/facebook/buck.git",
				"https://github.com/jayway/rest-assured.git",
				"https://github.com/graphhopper/graphhopper.git",
		};
		//TP: 3420  FP:  16  FN: 158
		processRepositoryCommits(Arrays.asList(gitURLs), 3420, 16, 158);
	}

	@Test
	public void testBatch3() throws Exception {
		String gitURLs[] = new String[]{
				"https://github.com/xetorthio/jedis.git",
				"https://github.com/eclipse/jetty.project.git",
				"https://github.com/droolsjbpm/jbpm.git",
				"https://github.com/jOOQ/jOOQ.git",
				"https://github.com/AsyncHttpClient/async-http-client.git",
				"https://github.com/jeeeyul/eclipse-themes.git",
				"https://github.com/JetBrains/MPS.git",
				"https://github.com/codefollower/Lealone.git",
				"https://github.com/AdoptOpenJDK/jitwatch.git",
				"https://github.com/liferay/liferay-portal.git",
				"https://github.com/square/mortar.git",
				"https://github.com/infinispan/infinispan.git",
				"https://github.com/crashub/crash.git",
				"https://github.com/glyptodon/guacamole-client.git",
				"https://github.com/github/android.git",
				"https://github.com/square/javapoet.git",
				"https://github.com/elastic/elasticsearch.git",
				"https://github.com/hierynomus/sshj.git",
				"https://github.com/rackerlabs/blueflood.git",
				"https://github.com/jersey/jersey.git",
				"https://github.com/Alluxio/alluxio.git",
				"https://github.com/spring-projects/spring-data-rest.git",
				"https://github.com/NLPchina/ansj_seg.git",
				"https://github.com/apache/camel.git",
				"https://github.com/droolsjbpm/drools.git",
				"https://github.com/robovm/robovm.git",
				"https://github.com/bitcoinj/bitcoinj.git",
				"https://github.com/facebook/presto.git",
				"https://github.com/deeplearning4j/deeplearning4j.git",
				"https://github.com/crate/crate.git",
				"https://github.com/libgdx/libgdx.git",
				"https://github.com/hibernate/hibernate-orm.git",
				"https://github.com/Netflix/zuul.git",
				"https://github.com/ratpack/ratpack.git",
				"https://github.com/brianfrankcooper/YCSB.git",
				"https://github.com/QuantumBadger/RedReader.git",
				"https://github.com/addthis/hydra.git",
				"https://github.com/apache/pig.git",
		};
		//TP: 2996  FP:  7  FN: 61
		processRepositoryCommits(Arrays.asList(gitURLs), 2996, 7, 61);
	}

	@Test
	public void testBatch4() throws Exception {
		String gitURLs[] = new String[]{
				"https://github.com/apache/hive.git",
				"https://github.com/google/closure-compiler.git",
				"https://github.com/go-lang-plugin-org/go-lang-idea-plugin.git",
				"https://github.com/spring-projects/spring-framework.git",
				"https://github.com/greenrobot/greenDAO.git",
				"https://github.com/springfox/springfox.git",
				"https://github.com/alibaba/druid.git",
				"https://github.com/JoanZapata/android-iconify.git",
				"https://github.com/processing/processing.git",
				"https://github.com/spring-projects/spring-roo.git",
				"https://github.com/cgeo/cgeo.git",
				"https://github.com/plutext/docx4j.git",
				"https://github.com/checkstyle/checkstyle.git",
				"https://github.com/k9mail/k-9.git",
				"https://github.com/RoboBinding/RoboBinding.git",
				"https://github.com/selendroid/selendroid.git",
				"https://github.com/spring-projects/spring-data-mongodb.git",
				"https://github.com/google/auto.git",
				"https://github.com/Netflix/feign.git",
				"https://github.com/apache/zookeeper.git",
				"https://github.com/jMonkeyEngine/jmonkeyengine.git",
				"https://github.com/jboss-developer/jboss-eap-quickstarts.git",
				"https://github.com/AntennaPod/AntennaPod.git",
				"https://github.com/MovingBlocks/Terasology.git",
				"https://github.com/GoClipse/goclipse.git",
				"https://github.com/google/j2objc.git",
				"https://github.com/SonarSource/sonarqube.git",
				"https://github.com/Atmosphere/atmosphere.git",
				"https://github.com/slapperwan/gh4a.git",
				"https://github.com/square/okhttp.git",
				"https://github.com/opentripplanner/OpenTripPlanner.git",
				"https://github.com/facebook/facebook-android-sdk.git",
				"https://github.com/bumptech/glide.git",
				"https://github.com/languagetool-org/languagetool.git",
				"https://github.com/spring-projects/spring-hateoas.git",
				"https://github.com/rstudio/rstudio.git",
				"https://github.com/puniverse/quasar.git",
				"https://github.com/Jasig/cas.git",
				"https://github.com/cucumber/cucumber-jvm.git",
				"https://github.com/orientechnologies/orientdb.git",
				"https://github.com/spotify/helios.git",
				"https://github.com/SlimeKnights/TinkersConstruct.git",
				"https://github.com/Graylog2/graylog2-server.git",
				"https://github.com/SecUpwN/Android-IMSI-Catcher-Detector.git",
				"https://github.com/jline/jline2.git",
				"https://github.com/killbill/killbill.git",
				"https://github.com/VoltDB/voltdb.git",
				"https://github.com/mongodb/morphia.git",
				"https://github.com/reactor/reactor.git",
				"https://github.com/zeromq/jeromq.git",
				"https://github.com/mongodb/mongo-java-driver.git",
				"https://github.com/osmandapp/Osmand.git",
				"https://github.com/openhab/openhab.git",
				"https://github.com/BroadleafCommerce/BroadleafCommerce.git",
				"https://github.com/nutzam/nutz.git",
				"https://github.com/spring-projects/spring-security.git",
				"https://github.com/novoda/android-demos.git",
				"https://github.com/wordpress-mobile/WordPress-Android.git",
		};
		//TP: 1825  FP:  0  FN: 49
		processRepositoryCommits(Arrays.asList(gitURLs), 1823, 0, 49);
	}
/*
	@Test
	public void testAllRefactorings() throws Exception {
		GitHistoryRefactoringMinerImpl detector = new GitHistoryRefactoringMinerImpl();
		TestBuilder test = new TestBuilder(detector, REPOS, Refactorings.All.getValue());
		RefactoringPopulator.feedRefactoringsInstances(Refactorings.All.getValue(), Systems.FSE.getValue(), test);
		test.assertExpectations(getProject(), 11125, 29, 325, false);
	}
 */
}
