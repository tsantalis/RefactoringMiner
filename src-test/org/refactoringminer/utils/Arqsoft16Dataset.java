package org.refactoringminer.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.refactoringminer.api.RefactoringType;

public class Arqsoft16Dataset {

  public final RefactoringSet atmosphere_cc2b3f1 = new RefactoringSet("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1");
  
  public Arqsoft16Dataset() {
    atmosphere_cc2b3f1.add(item("Move Class", "org.atmosphere.util.annotation.InputStreamIterator", "org.atmosphere.util.annotation.iterator.InputStreamIterator"));
    atmosphere_cc2b3f1.add(item("Move Class", "org.atmosphere.util.CookieUtil", "org.atmosphere.util.tools.CookieUtil"));
    atmosphere_cc2b3f1.add(item("Move Class", "org.atmosphere.util.IntrospectionUtils", "org.atmosphere.util.tools.IntrospectionUtils"));
    atmosphere_cc2b3f1.add(item("Move Class", "org.atmosphere.util.StringEscapeUtils", "org.atmosphere.util.tools.StringEscapeUtils"));
    atmosphere_cc2b3f1.add(item("Move Class", "org.atmosphere.util.annotation.FileIterator", "org.atmosphere.util.annotation.iterator.FileIterator"));
    atmosphere_cc2b3f1.add(item("Rename Class", "org.atmosphere.cpr.ExcludeSessionBroadcasterTest", "org.atmosphere.cpr.SessionExcludeBroadcasterTest"));
    atmosphere_cc2b3f1.add(item("Rename Class", "org.atmosphere.jersey.TestResource", "org.atmosphere.jersey.ResourceTest"));
    atmosphere_cc2b3f1.add(item("Rename Class", "org.atmosphere.websocket.protocol.EchoProtocol", "org.atmosphere.websocket.protocol.ProtocolEcho"));
    atmosphere_cc2b3f1.add(item("Rename Class", "org.atmosphere.websocket.protocol.SimpleHttpProtocol", "org.atmosphere.websocket.protocol.ProtocolSimpleHttp"));
    atmosphere_cc2b3f1.add(item("Rename Class", "org.atmosphere.websocket.protocol.StreamingHttpProtocol", "org.atmosphere.websocket.protocol.ProtocolStreamingHttp"));
    atmosphere_cc2b3f1.add(item("Extract Superclass", "org.atmosphere.config.FrameworkConfiguration\norg.atmosphere.config.ApplicationConfiguration", "org.atmosphere.config.AtmosphereConfigurationManager"));
    atmosphere_cc2b3f1.add(item("Extract Interface", "org.atmosphere.cpr.AtmosphereFrameworkInitializer", "org.atmosphere.cpr.FrameworkInitializer"));
    atmosphere_cc2b3f1.add(item("Rename Method", "org.atmosphere.util.uri.UriTemplate#match(CharSequence, Map<String, String>)", "org.atmosphere.util.uri.UriTemplate#matchURIAndTemplate(CharSequence, Map<String, String>)"));
    atmosphere_cc2b3f1.add(item("Rename Method", "org.atmosphere.util.IOUtils#readEntirely(AtmosphereResource)", "org.atmosphere.util.IOUtils#readEntirelyBody(AtmosphereResource)"));
    atmosphere_cc2b3f1.add(item("Rename Method", "org.atmosphere.util.IOUtils#readEntirelyAsString(AtmosphereResource)", "org.atmosphere.util.IOUtils#readEntirelyBodyAsString(AtmosphereResource)"));
    atmosphere_cc2b3f1.add(item("Rename Method", "org.atmosphere.util.Utils#pathInfo(AtmosphereRequest)", "org.atmosphere.util.Utils#getPathInfo(AtmosphereRequest)"));
    atmosphere_cc2b3f1.add(item("Rename Method", "org.atmosphere.util.WebDotXmlReader#parse(Document)", "org.atmosphere.util.WebDotXmlReader#parseDocumentXML(Document)"));
    atmosphere_cc2b3f1.add(item("Move Method", "org.atmosphere.util.Utils#getInheritedPrivateMethod(Class<?>)", "org.atmosphere.inject.InjectableObjectFactory#getInheritedPrivateMethod(Class<?>)"));
    atmosphere_cc2b3f1.add(item("Move Method", "org.atmosphere.util.IOUtils#deliver(Object, DeliverTo, DELIVER_TO, AtmosphereResource)", "org.atmosphere.config.managed.ManagedAtmosphereHandler#deliver(Object, DeliverTo, DELIVER_TO, AtmosphereResource)"));
    atmosphere_cc2b3f1.add(item("Move Method", "org.atmosphere.util.IOUtils#isAtmosphere(String)", "org.atmosphere.cpr.ContainerInitializer#isAtmosphere(String)"));
    atmosphere_cc2b3f1.add(item("Move Method", "org.atmosphere.util.Utils#firefoxWebSocketEnabled(HttpServletRequest)", "org.atmosphere.websocket.DefaultWebSocketProcessor#firefoxWebSocketEnabled(HttpServletRequest)"));
    atmosphere_cc2b3f1.add(item("Move Method", "org.atmosphere.util.Utils#properProtocol(HttpServletRequest)", "org.atmosphere.cpr.AsynchronousProcessor#properProtocol(HttpServletRequest)"));
    atmosphere_cc2b3f1.add(item("Pull Up Method", "org.atmosphere.jersey.BroadcasterInjector#isValidType(Type)", "org.atmosphere.jersey.BaseInjectableProvider#isValidType(Type)"));
    atmosphere_cc2b3f1.add(item("Pull Up Method", "org.atmosphere.jersey.QueryStringTest#getUrlTarget(int) \norg.atmosphere.jersey.MappingResourceTest#getUrlTarget(int)", "org.atmosphere.jersey.BaseTest#getUrlTarget(int)"));
    atmosphere_cc2b3f1.add(item("Pull Up Method", "org.atmosphere.container.BlockingIOCometSupport#suspend(Action, AtmosphereRequest, AtmosphereResponse)", "org.atmosphere.cpr.AsynchronousProcessor#suspend(Action, AtmosphereRequest, AtmosphereResponse)"));
    atmosphere_cc2b3f1.add(item("Push Down Method", "org.atmosphere.websocket.WebSocket#attachment()", "org.atmosphere.container.EmbeddedWebSocketHandler.ArrayBaseWebSocket#attachment()\norg.atmosphere.container.version.Grizzly2WebSocket#attachment()\norg.atmosphere.container.version.GrizzlyWebSocket#attachment()\norg.atmosphere.container.version.JBossWebSocket#attachment()\norg.atmosphere.container.version.JSR356WebSocket#attachment()\norg.atmosphere.container.version.Jetty8WebSocket#attachment()\norg.atmosphere.container.version.Jetty9WebSocket#attachment()\norg.atmosphere.annotation.ManagedAtmosphereHandlerTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.container.version.TomcatWebSocket#attachment()\norg.atmosphere.annotation.path.PathTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.cpr.TrackMessageSizeInterceptorTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.container.version.WebLogicWebSocket#attachment()\norg.atmosphere.cpr.WebSocketHandlerTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.cpr.WebSocketProcessorTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.cpr.WebSocketStreamingHandlerTest.ArrayBaseWebSocket#attachment()"));
    atmosphere_cc2b3f1.add(item("Push Down Method", "org.atmosphere.cpr.AtmosphereServlet#newAtmosphereFramework()", "org.atmosphere.cpr.MeteorServlet#newAtmosphereFramework()\norg.atmosphere.cpr.AtmosphereFrameworkTest.MyAtmosphereServlet#newAtmosphereFramework()"));
    atmosphere_cc2b3f1.add(item("Push Down Method", "org.atmosphere.websocket.WebSocket#attachment(Object)", "org.atmosphere.container.EmbeddedWebSocketHandler.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.container.version.Grizzly2WebSocket#attachment(Object)\norg.atmosphere.container.version.GrizzlyWebSocket#attachment(Object)\norg.atmosphere.container.version.JBossWebSocket#attachment(Object)\norg.atmosphere.container.version.JSR356WebSocket#attachment(Object)\norg.atmosphere.container.version.Jetty8WebSocket#attachment(Object)\norg.atmosphere.container.version.Jetty9WebSocket#attachment(Object)\norg.atmosphere.annotation.ManagedAtmosphereHandlerTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.container.version.TomcatWebSocket#attachment(Object)\norg.atmosphere.annotation.path.PathTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.cpr.TrackMessageSizeInterceptorTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.container.version.WebLogicWebSocket#attachment(Object)\norg.atmosphere.cpr.WebSocketStreamingHandlerTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.cpr.WebSocketHandlerTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.cpr.WebSocketProcessorTest.ArrayBaseWebSocket#attachment(Object)"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.config.managed.ManagedServiceInterceptor#mapAnnotatedService(boolean, String, AtmosphereRequest, AtmosphereFramework.AtmosphereHandlerWrapper)", "org.atmosphere.config.managed.ManagedServiceInterceptor#configureAnnotatedProxyIfSingleton(AtmosphereFramework.AtmosphereHandlerWrapper, AnnotatedProxy)"));
    atmosphere_cc2b3f1.add(item("Extract Method", "com.sun.jersey.api.JResponseAsResponse.AtmosphereFilter#filter(ContainerRequest, ContainerResponse)", "com.sun.jersey.api.JResponseAsResponse.AtmosphereFilter#getContainerResponseWriter(String)"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.jersey.AtmosphereFilter#create(AbstractMethod)", "org.atmosphere.jersey.AtmosphereFilter#getFilterForAnnotation(AbstractMethod, Class[])"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.annotation.AtmosphereHandlerServiceProcessor#handle(AtmosphereFramework, Class<AtmosphereHandler>)", "org.atmosphere.annotation.AtmosphereHandlerServiceProcessor#getListAtmosphereInterceptor(AtmosphereFramework, AtmosphereHandlerService)"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.cache.UUIDBroadcasterCache#addToCache(String, String, BroadcastMessage)", "org.atmosphere.cache.UUIDBroadcasterCache#configureCacheMessage(String, String, boolean, CacheMessage)"));
    atmosphere_cc2b3f1.add(item("Inline Method", "org.atmosphere.annotation.AnnotationUtil#checkDefault(Class<? extends AtmosphereInterceptor>)", "org.atmosphere.annotation.AnnotationUtil#interceptorsForManagedService(AtmosphereFramework, List<Class<? extends AtmosphereInterceptor>>, List<AtmosphereInterceptor>, boolean)"));
    atmosphere_cc2b3f1.add(item("Inline Method", "org.atmosphere.cpr.AtmosphereServlet#configureFramework(ServletConfig, boolean)", "org.atmosphere.cpr.AtmosphereServlet#configureFramework(ServletConfig)"));
    atmosphere_cc2b3f1.add(item("Inline Method", "org.atmosphere.websocket.protocol.ProtocolUtil#attributes(WebSocket, AtmosphereRequest)", "org.atmosphere.websocket.protocol.ProtocolUtil#constructRequest(WebSocket, String, String, String, String, boolean)"));
    atmosphere_cc2b3f1.add(item("Inline Method", "org.atmosphere.jersey.BaseTest#stopServer()", "org.atmosphere.jersey.BaseTest#unsetAtmosphereHandler()"));
    atmosphere_cc2b3f1.add(item("Inline Method", "org.atmosphere.jersey.BaseTest.configureCometSupport()", "org.atmosphere.jersey.BaseTest#setUpGlobal()"));
    atmosphere_cc2b3f1.add(item("Move Field", "org.atmosphere.interceptor.HeartbeatInterceptor#HEARTBEAT_FUTURE", "org.atmosphere.interceptor.IdleResourceInterceptor#HEARTBEAT_FUTURE"));
    atmosphere_cc2b3f1.add(item("Move Field", "org.atmosphere.websocket.WebSocket#WEBSOCKET_ACCEPT_DONE", "org.atmosphere.container.JettyWebSocketUtil#WEBSOCKET_ACCEPT_DONE"));
    atmosphere_cc2b3f1.add(item("Move Field", "org.atmosphere.util.VoidServletConfig#ATMOSPHERE_SERVLET", "org.atmosphere.util.IOUtils#ATMOSPHERE_SERVLET"));
    atmosphere_cc2b3f1.add(item("Move Field", "org.atmosphere.util.VoidExecutorService#VOID", "org.atmosphere.websocket.DefaultWebSocketProcessor#VOID"));
    atmosphere_cc2b3f1.add(item("Move Field", "org.atmosphere.cpr.DefaultBroadcaster#POLLING_DEFAULT", "org.atmosphere.cpr.AtmosphereFramework#POLLING_DEFAULT"));
    atmosphere_cc2b3f1.add(item("Pull Up Field", "org.atmosphere.handler.ReflectorServletProcessor#config\norg.atmosphere.config.managed.ManagedAtmosphereHandler#config", "org.atmosphere.handler.AbstractReflectorAtmosphereHandler#config"));
    atmosphere_cc2b3f1.add(item("Pull Up Field", "org.atmosphere.config.managed.ServiceInterceptor#config\norg.atmosphere.util.Utils.closeMessage.OnDisconnectInterceptor#config\norg.atmosphere.interceptor.JSONPAtmosphereInterceptor#config\norg.atmosphere.interceptor.IdleResourceInterceptor#config\norg.atmosphere.interceptor.HeartbeatInterceptor#config", "org.atmosphere.cpr.AtmosphereInterceptorAdapter#config"));
    atmosphere_cc2b3f1.add(item("Pull Up Field", "org.atmosphere.interceptor.TrackMessageSizeB64Interceptor#OUT_ENCODING\norg.atmosphere.client.TrackMessageSizeInterceptor#OUT_ENCODING", "org.atmosphere.cpr.AtmosphereInterceptorAdapter#OUT_ENCODING"));
    atmosphere_cc2b3f1.add(item("Push Down Field", "", ""));
    atmosphere_cc2b3f1.add(item("Push Down Field", "org.atmosphere.websocket.WebSocket#attachment", "org.atmosphere.container.EmbeddedWebSocketHandler.ArrayBaseWebSocket#attachment\norg.atmosphere.container.version.Grizzly2WebSocket#attachment\norg.atmosphere.container.version.GrizzlyWebSocket#attachment\norg.atmosphere.container.version.JBossWebSocket#attachment\norg.atmosphere.container.version.Jetty8WebSocket#attachment\norg.atmosphere.container.version.JSR356WebSocket#attachment\norg.atmosphere.container.version.Jetty9WebSocket#attachment\norg.atmosphere.annotation.ManagedAtmosphereHandlerTest.ArrayBaseWebSocket#attachment\norg.atmosphere.container.version.TomcatWebSocket#attachment\norg.atmosphere.annotation.path.PathTest.ArrayBaseWebSocket#attachment\norg.atmosphere.cpr.TrackMessageSizeInterceptorTest.ArrayBaseWebSocket#attachment\norg.atmosphere.container.version.WebLogicWebSocket#attachment\norg.atmosphere.cpr.WebSocketStreamingHandlerTest.ArrayBaseWebSocket#attachment\norg.atmosphere.cpr.WebSocketProcessorTest.ArrayBaseWebSocket#attachment\norg.atmosphere.cpr.WebSocketHandlerTest.ArrayBaseWebSocket#attachment"));
    atmosphere_cc2b3f1.add(item("Push Down Field", "", ""));
  }

  private List<RefactoringRelationship> item(String refTypeS, String before, String after) {
    if (before.trim().isEmpty() || after.trim().isEmpty()) {
      return Collections.emptyList();
    }
    String[] beforeA = before.trim().split("\n");
    String[] afterA = after.trim().split("\n");
    RefactoringType rt = RefactoringType.fromName(refTypeS.replace("Field", "Attribute"));
    
    List<RefactoringRelationship> list = new ArrayList<>();
    for (String b : beforeA) {
      for (String a : afterA) {
        if (!b.trim().isEmpty() && !a.trim().isEmpty()) {
          list.add(new RefactoringRelationship(rt, b, a));
        }
      }
    }
    return list;
  }

}
