package org.refactoringminer.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.utils.RefactoringRelationship;
import org.refactoringminer.utils.RefactoringSet;

public class Arqsoft16Dataset {

  public final RefactoringSet atmosphere_cc2b3f1 = new RefactoringSet("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1");
  public final RefactoringSet clojure_17217a1 = new RefactoringSet("https://github.com/aserg-ufmg/clojure.git", "17217a1");
  public final RefactoringSet guava_79767ec = new RefactoringSet("https://github.com/aserg-ufmg/guava.git", "79767ec");
  public final RefactoringSet metrics_276d5e4 = new RefactoringSet("https://github.com/aserg-ufmg/metrics.git", "276d5e4");
  public final RefactoringSet orientdb_b213aaf = new RefactoringSet("https://github.com/aserg-ufmg/orientdb.git", "b213aaf");
  public final RefactoringSet retrofit_f13f317 = new RefactoringSet("https://github.com/aserg-ufmg/retrofit.git", "f13f317");
  public final RefactoringSet springBoot_48e893a = new RefactoringSet("https://github.com/aserg-ufmg/spring-boot.git", "48e893a");
  
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
    // Abstract method getUrlTarget already exists in BaseTest
    atmosphere_cc2b3f1.add(item("Pull Up Method", "org.atmosphere.jersey.QueryStringTest#getUrlTarget(int) \norg.atmosphere.jersey.MappingResourceTest#getUrlTarget(int)", "org.atmosphere.jersey.BaseTest#getUrlTarget(int)"));
    atmosphere_cc2b3f1.add(item("Pull Up Method", "org.atmosphere.container.BlockingIOCometSupport#suspend(Action, AtmosphereRequest, AtmosphereResponse)", "org.atmosphere.cpr.AsynchronousProcessor#suspend(Action, AtmosphereRequest, AtmosphereResponse)"));
    atmosphere_cc2b3f1.add(item("Push Down Method", "org.atmosphere.websocket.WebSocket#attachment()", "org.atmosphere.container.EmbeddedWebSocketHandler.ArrayBaseWebSocket#attachment()\norg.atmosphere.container.version.Grizzly2WebSocket#attachment()\norg.atmosphere.container.version.GrizzlyWebSocket#attachment()\norg.atmosphere.container.version.JBossWebSocket#attachment()\norg.atmosphere.container.version.JSR356WebSocket#attachment()\norg.atmosphere.container.version.Jetty8WebSocket#attachment()\norg.atmosphere.container.version.Jetty9WebSocket#attachment()\norg.atmosphere.annotation.ManagedAtmosphereHandlerTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.container.version.TomcatWebSocket#attachment()\norg.atmosphere.annotation.path.PathTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.cpr.TrackMessageSizeInterceptorTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.container.version.WebLogicWebSocket#attachment()\norg.atmosphere.cpr.WebSocketHandlerTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.cpr.WebSocketProcessorTest.ArrayBaseWebSocket#attachment()\norg.atmosphere.cpr.WebSocketStreamingHandlerTest.ArrayBaseWebSocket#attachment()"));
    atmosphere_cc2b3f1.add(item("Push Down Method", "org.atmosphere.cpr.AtmosphereServlet#newAtmosphereFramework()", "org.atmosphere.cpr.MeteorServlet#newAtmosphereFramework()\norg.atmosphere.cpr.AtmosphereFrameworkTest.MyAtmosphereServlet#newAtmosphereFramework()"));
    atmosphere_cc2b3f1.add(item("Push Down Method", "org.atmosphere.websocket.WebSocket#attachment(Object)", "org.atmosphere.container.EmbeddedWebSocketHandler.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.container.version.Grizzly2WebSocket#attachment(Object)\norg.atmosphere.container.version.GrizzlyWebSocket#attachment(Object)\norg.atmosphere.container.version.JBossWebSocket#attachment(Object)\norg.atmosphere.container.version.JSR356WebSocket#attachment(Object)\norg.atmosphere.container.version.Jetty8WebSocket#attachment(Object)\norg.atmosphere.container.version.Jetty9WebSocket#attachment(Object)\norg.atmosphere.annotation.ManagedAtmosphereHandlerTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.container.version.TomcatWebSocket#attachment(Object)\norg.atmosphere.annotation.path.PathTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.cpr.TrackMessageSizeInterceptorTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.container.version.WebLogicWebSocket#attachment(Object)\norg.atmosphere.cpr.WebSocketStreamingHandlerTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.cpr.WebSocketHandlerTest.ArrayBaseWebSocket#attachment(Object)\norg.atmosphere.cpr.WebSocketProcessorTest.ArrayBaseWebSocket#attachment(Object)"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.config.managed.ManagedServiceInterceptor#mapAnnotatedService(boolean, String, AtmosphereRequest, AtmosphereFramework.AtmosphereHandlerWrapper)", "org.atmosphere.config.managed.ManagedServiceInterceptor#configureAnnotatedProxyIfSingleton(AtmosphereFramework.AtmosphereHandlerWrapper, AnnotatedProxy)"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.jersey.AtmosphereFilter.Filter#filter(ContainerRequest, ContainerResponse)", "org.atmosphere.jersey.AtmosphereFilter#getContainerResponseWriter(String)"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.jersey.AtmosphereFilter#create(AbstractMethod)", "org.atmosphere.jersey.AtmosphereFilter#getFilterForAnnotation(AbstractMethod, Class[])"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.annotation.AtmosphereHandlerServiceProcessor#handle(AtmosphereFramework, Class<AtmosphereHandler>)", "org.atmosphere.annotation.AtmosphereHandlerServiceProcessor#getListAtmosphereInterceptor(AtmosphereFramework, AtmosphereHandlerService)"));
    atmosphere_cc2b3f1.add(item("Extract Method", "org.atmosphere.cache.UUIDBroadcasterCache#addToCache(String, String, BroadcastMessage)", "org.atmosphere.cache.UUIDBroadcasterCache#configureCacheMessage(String, String, boolean, CacheMessage)"));
    atmosphere_cc2b3f1.add(item("Inline Method", "org.atmosphere.annotation.AnnotationUtil#checkDefault(Class<? extends AtmosphereInterceptor>)", "org.atmosphere.annotation.AnnotationUtil#interceptorsForManagedService(AtmosphereFramework, List<Class<? extends AtmosphereInterceptor>>, List<AtmosphereInterceptor>, boolean)"));
    // This refactoring was not performed
    //atmosphere_cc2b3f1.add(item("Inline Method", "org.atmosphere.cpr.AtmosphereServlet#configureFramework(ServletConfig, boolean)", "org.atmosphere.cpr.AtmosphereServlet#configureFramework(ServletConfig)"));
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
    
    retrofit_f13f317.add(item("Move Class", "retrofit2.converter.protobuf.ProtoConverterFactory", "retrofit2.converter.protobuf.main.ProtoConverterFactory"));
    retrofit_f13f317.add(item("Move Class", "retrofit2.converter.protobuf.ProtoRequestBodyConverter", "retrofit2.converter.protobuf.main.ProtoRequestBodyConverter"));
    retrofit_f13f317.add(item("Move Class", "retrofit2.converter.protobuf.ProtoResponseBodyConverter", "retrofit2.converter.protobuf.main.ProtoResponseBodyConverter"));
    retrofit_f13f317.add(item("Move Class", "retrofit2.converter.scalars.ScalarRequestBodyConverter", "retrofit2.converter.scalars.main.ScalarRequestBodyConverter"));
    retrofit_f13f317.add(item("Move Class", "retrofit2.converter.scalars.ScalarResponseBodyConverters", "retrofit2.converter.scalars.main.ScalarResponseBodyConverters"));
    retrofit_f13f317.add(item("Move Class", "retrofit2.converter.scalars.ScalarsConverterFactory", "retrofit2.converter.scalars.main.ScalarsConverterFactory"));
    retrofit_f13f317.add(item("Rename Class", "retrofit2.adapter.rxjava.Result", "retrofit2.adapter.rxjava.RequestResult"));
    retrofit_f13f317.add(item("Rename Class", "retrofit2.converter.moshi.MoshiRequestBodyConverter", "retrofit2.converter.moshi.MoshiRequestConverter"));
    retrofit_f13f317.add(item("Rename Class", "retrofit2.converter.moshi.MoshiResponseBodyConverter", "retrofit2.converter.moshi.MoshiResponseConverter"));
    retrofit_f13f317.add(item("Rename Class", "retrofit2.converter.jackson.JacksonRequestBodyConverter", "retrofit2.converter.jackson.JacksonRequestConverter"));
    retrofit_f13f317.add(item("Rename Class", "retrofit2.converter.jackson.JacksonResponseBodyConverter", "retrofit2.converter.jackson.JacksonResponseConverter"));
    retrofit_f13f317.add(item("Extract Superclass", "retrofit2.Utils", "retrofit2.SuperUtils"));
    retrofit_f13f317.add(item("Extract Interface", "retrofit2.adapter.guava.HttpException", "retrofit2.adapter.AdapterException"));
    retrofit_f13f317.add(item("Rename Method", "retrofit2.adapter.java8.HttpException#code()", "retrofit2.adapter.java8.HttpException#getCode()"));
    retrofit_f13f317.add(item("Rename Method", "retrofit2.adapter.java8.HttpException#response()", "retrofit2.adapter.java8.HttpException#getResponse()"));
    retrofit_f13f317.add(item("Rename Method", "retrofit2.RequestBuilder#addHeader(String, String)", "retrofit2.RequestBuilder#addHeaderToRequestBuilder(String, String)"));
    retrofit_f13f317.add(item("Rename Method", "retrofit2.RequestBuilder#addPathParam(String, String, boolean)", "retrofit2.RequestBuilder#addPathParamNameAndValue(String, String, boolean)"));
    retrofit_f13f317.add(item("Rename Method", "retrofit2.RequestBuilder#addQueryParam(String, String, boolean)", "retrofit2.RequestBuilder#addQueryParamNameAndValue(String, String, boolean)"));
    // Extract and move
    //retrofit_f13f317.add(item("Move Method", "retrofit2.Retrofit.Builder#createCallBackExecutor(Platform)", "retrofit2.Platform#createCallBackExecutor(Builder)"));
    //retrofit_f13f317.add(item("Move Method", "retrofit2.Retrofit.Builder#createCallFactory()", "retrofit2.Platform#createCallFactory(Builder)"));
    retrofit_f13f317.add(item("Move Method", "retrofit2.ServiceMethod.Builder#createCallAdapter()", "retrofit2.Retrofit#createCallAdapter(Builder)"));
    retrofit_f13f317.add(item("Move Method", "retrofit2.OkHttpCall#createRawCall()", "retrofit2.ServiceMethod#createRawCall(OkHttpCall)"));
    retrofit_f13f317.add(item("Move Method", "retrofit2.Utils#indexOf(Object[], Object)", "retrofit2.IndexSearch#indexOf(Object[], Object)"));
    // Redundant: Extract superclass
    //retrofit_f13f317.add(item("Pull Up Method", "retrofit2.Utils#typeToString(Type)", "retrofit2.SuperUtils#typeToString(Type)"));
    //retrofit_f13f317.add(item("Pull Up Method", "retrofit2.Utils#equals(Type, Type)", "retrofit2.SuperUtils#equals(Type, Type)"));
    //retrofit_f13f317.add(item("Pull Up Method", "retrofit2.Utils#equal(Object, Object)", "retrofit2.SuperUtils#equal(Object, Object)"));
    retrofit_f13f317.add(item("Extract Method", "retrofit2.Retrofit.Builder#build()", "retrofit2.Platform#createCallFactory(Builder)"));
    retrofit_f13f317.add(item("Extract Method", "retrofit2.Retrofit.Builder#build()", "retrofit2.Platform#createCallBackExecutor(Builder)"));
    retrofit_f13f317.add(item("Extract Method", "retrofit2.RequestBuilder.build()", "retrofit2.RequestBuilder#createURL()"));
    retrofit_f13f317.add(item("Extract Method", "retrofit2.ParameterHandler.Body#apply(RequestBuilder, T)", "retrofit2.ParameterHandler.Body#createBody(RequestBuilder, T)"));
    retrofit_f13f317.add(item("Extract Method", "retrofit2.ParameterHandler.Part#apply(RequestBuilder, T)", "retrofit2.ParameterHandler.Part#createBody(RequestBuilder, T)"));
    retrofit_f13f317.add(item("Inline Method", "retrofit2.Utils#checkNotPrimitive(Type)", "retrofit2.Utils.ParameterizedTypeImpl#(Type, Type, Type[])\nretrofit2.Utils.WildcardTypeImpl#(Type[], Type[])"));
    retrofit_f13f317.add(item("Inline Method", "retrofit2.Utils#validateServiceInterface(Class)", "retrofit2.Retrofit#create(Class)"));
    retrofit_f13f317.add(item("Inline Method", "retrofit2.Utils#getCallResponseType(Type)", "retrofit2.DefaultCallAdapterFactory#get(Type, Annotation[], Retrofit)\nretrofit2.ExecutorCallAdapterFactory#get(Type, Annotation[], Retrofit)"));
    retrofit_f13f317.add(item("Inline Method", "retrofit2.Utils#hashCodeOrZero(Object)", "retrofit2.Utils.ParameterizedTypeImpl#hashCode()"));
    retrofit_f13f317.add(item("Inline Method", "retrofit2.Utils#declaringClassOf(TypeVariable)", "retrofit2.Utils#resolveTypeVariable(Type, Class, TypeVariable)"));
    retrofit_f13f317.add(item("Move Field", "retrofit2.converter.simplexml.SimpleXmlRequestBodyConverter#CHARSET", "retrofit2.converter.simplexml.SimpleXmlConverterFactory#CHARSET"));
    retrofit_f13f317.add(item("Move Field", "retrofit2.converter.simplexml.SimpleXmlRequestBodyConverter#MEDIA_TYPE", "retrofit2.converter.simplexml.SimpleXmlConverterFactory#MEDIA_TYPE"));
    retrofit_f13f317.add(item("Move Field", "retrofit2.converter.gson.GsonRequestBodyConverter#MEDIA_TYPE", "retrofit2.converter.gson.GsonConverterFactory#MEDIA_TYPE"));
    retrofit_f13f317.add(item("Move Field", "retrofit2.converter.gson.GsonRequestBodyConverter#UTF_8", "retrofit2.converter.gson.GsonConverterFactory#UTF_8"));
    retrofit_f13f317.add(item("Move Field", "retrofit2.converter.jackson.JacksonRequestBodyConverter#MEDIA_TYPE", "retrofit2.converter.jackson.JacksonConverterFactory#MEDIA_TYPE"));
    retrofit_f13f317.add(item("Pull Up Field", "retrofit2.ParameterHandler.Path#name\nretrofit2.ParameterHandler.Query#name\nretrofit2.ParameterHandler.Field#name", "retrofit2.ParameterHandler#name"));
    retrofit_f13f317.add(item("Pull Up Field", "retrofit2.ParameterHandler.Part#headers", "retrofit2.ParameterHandler#headers"));
    retrofit_f13f317.add(item("Pull Up Field", "retrofit2.ParameterHandler.PartMap#transferEncoding", "retrofit2.ParameterHandler#transferEncoding"));
    retrofit_f13f317.add(item("Push Down Field", "retrofit2.Platform#PLATFORM", "retrofit2.Platform.Android#PLATFORM\nretrofit2.Platform.IOS#PLATFORM\nretrofit2.Platform.Java8#PLATFORM"));
    
    clojure_17217a1.add(item("Rename Class", "clojure.lang.Obj", "clojure.lang.ClojureObject"));
    clojure_17217a1.add(item("Rename Class", "clojure.lang.Ref", "clojure.lang.Reference"));
    clojure_17217a1.add(item("Rename Class", "clojure.lang.Var", "clojure.lang.Variable"));
    clojure_17217a1.add(item("Rename Class", "clojure.lang.BigInt", "clojure.lang.ClojureBigInteger"));
    clojure_17217a1.add(item("Rename Class", "clojure.lang.IObj", "clojure.lang.IClojureObject"));
    clojure_17217a1.add(item("Move Class", "clojure.lang.IFn", "clojure.lang.interfaces.IFn"));
    clojure_17217a1.add(item("Move Class", "clojure.lang.IProxy", "clojure.lang.interfaces.IProxy"));
    clojure_17217a1.add(item("Move Class", "clojure.lang.IType", "clojure.lang.interfaces.IType"));
    clojure_17217a1.add(item("Move Class", "clojure.lang.ISeq", "clojure.lang.interfaces.ISeq"));
    clojure_17217a1.add(item("Move Class", "clojure.lang.IRef", "clojure.lang.interfaces.IRef"));
    clojure_17217a1.add(item("Extract Superclass", "clojure.asm.AnnotationVisitor\nclojure.asm.ClassVisitor", "clojure.asm.ObjectVisitor"));
    clojure_17217a1.add(item("Extract Interface", "clojure.asm.commons.InstructionAdapter", "clojure.asm.commons.IStackablle"));
    clojure_17217a1.add(item("Rename Method", "clojure.asm.commons.GeneratorAdapter.pop2()", "clojure.asm.commons.GeneratorAdapter.popLongDouble()"));
    clojure_17217a1.add(item("Rename Method", "clojure.lang.AReference.meta()", "clojure.lang.AReference.getMeta()"));
    clojure_17217a1.add(item("Rename Method", "clojure.lang.ASeq.equiv(Object)", "clojure.lang.ASeq.isEquivalent(Object)"));
    clojure_17217a1.add(item("Rename Method", "clojure.lang.ExceptionInfo.getData()", "clojure.lang.ExceptionInfo.getExceptionData()"));
    clojure_17217a1.add(item("Rename Method", "clojure.asm.Attribute.put(ClassWriter, byte[], int, int, int, ByteVector)", "clojure.asm.Attribute.writeAllAtributes(ClassWriter, byte[], int, int, int, ByteVector)"));
    clojure_17217a1.add(item("Move Method", "clojure.lang.Numbers.reduceBigInt(BigInt)", "clojure.lang.ClojureBigInteger.reduceBigInt(ClojureBigInteger)"));
    clojure_17217a1.add(item("Move Method", "clojure.lang.Var.intern(Namespace, Symbol)", "clojure.lang.Namespace.intern(Namespace, Symbol)"));
    clojure_17217a1.add(item("Move Method", "clojure.lang.Var.intern(Namespace, Symbol, Object)", "clojure.lang.Namespace.intern(Namespace, Symbol, Object)"));
    clojure_17217a1.add(item("Move Method", "clojure.lang.Var.intern(Namespace, Symbol, Object, boolean)", "clojure.lang.Namespace.intern(Namespace, Symbol, Object, boolean)"));
    clojure_17217a1.add(item("Move Method", "clojure.lang.Var.find(Symbol)", "clojure.lang.Symbol.find(Symbol)"));
    // The method is not moved, @Override added
    clojure_17217a1.add(item("Pull Up Method", "clojure.lang.AMapEntry.nth(int)", "clojure.lang.Indexed.nth(int)"));
    // This refactoring was not performed
    clojure_17217a1.add(item("Pull Up Method", "clojure.lang.Volatile.reset(Object)", "clojure.lang.IDeref.reset(Object)"));
    clojure_17217a1.add(item("Extract Method", "clojure.lang.Compiler.FnMethod.parse(ObjExpr, ISeq, Object)", "clojure.lang.Compiler.FnMethod.getFnMethod(ObjExpr)"));
    clojure_17217a1.add(item("Extract Method", "clojure.lang.Compiler.NewInstanceExpr.build(IPersistentVector, IPersistentVector, Symbol, String, Symbol, Symbol, ISeq, Object, IPersistentMap)", "clojure.lang.Compiler.NewInstanceExpr.getInstanceExpr(IPersistentVector, Symbol, Symbol, Object, IPersistentMap)"));
    clojure_17217a1.add(item("Extract Method", "clojure.lang.Compiler.ObjExpr.emitValue(Object, GeneratorAdapter)", "clojure.lang.Compiler.ObjExpr.defineGeneratoAdaptaer(Object, GeneratorAdapter, boolean)"));
    clojure_17217a1.add(item("Extract Method", "clojure.lang.Compiler.ObjExpr.compile(String, String[], boolean)", "clojure.lang.Compiler.ObjExpr.getStringMap(String, int, int)"));
    clojure_17217a1.add(item("Extract Method", "clojure.lang.Compiler.FnMethod.doEmitStatic(ObjExpr, ClassVisitor)", "clojure.lang.Compiler.FnMethod.generatePrimInvoke(ClassVisitor, Method)"));
    clojure_17217a1.add(item("Inline Method", "clojure.lang.AMapEntry.asVector()", "clojure.lang.AMapEntry.assocN(int, Object)"));
    clojure_17217a1.add(item("Inline Method", "clojure.lang.ASeq.reify()", "clojure.lang.ASeq.subList(int, int)"));
    clojure_17217a1.add(item("Inline Method", "clojure.lang.LazySeq.reify()", "clojure.lang.LazySeq.subList(int, int)"));
    clojure_17217a1.add(item("Inline Method", "clojure.lang.MultiFn.isA(Object, Object)", "clojure.lang.MultiFn.dominates(Object, Object)"));
    clojure_17217a1.add(item("Inline Method", "clojure.lang.Util.dohasheq(IHashEq)", "clojure.lang.Util.hasheq(Object)"));
    clojure_17217a1.add(item("Move Field", "", ""));
    clojure_17217a1.add(item("Move Field", "", ""));
    clojure_17217a1.add(item("Move Field", "", ""));
    clojure_17217a1.add(item("Move Field", "", ""));
    clojure_17217a1.add(item("Move Field", "", ""));
    clojure_17217a1.add(item("Pull Up Field", "", ""));
    clojure_17217a1.add(item("Pull Up Field", "", ""));
    clojure_17217a1.add(item("Pull Up Field", "", ""));
    clojure_17217a1.add(item("Push Down Field", "", ""));
    clojure_17217a1.add(item("Push Down Field", "", ""));
    clojure_17217a1.add(item("Push Down Field", "", ""));
    
    metrics_276d5e4.add(item("Rename Class", "io.dropwizard.metrics.benchmarks.ReservoirBenchmark", "io.dropwizard.metrics.benchmarks.ReservoirBenchmarkData"));
    metrics_276d5e4.add(item("Rename Class", "io.dropwizard.metrics.benchmarks.MeterBenchmark", "io.dropwizard.metrics.benchmarks.MeterBenchmarkData"));
    metrics_276d5e4.add(item("Rename Class", "io.dropwizard.metrics.benchmarks.CounterBenchmark", "io.dropwizard.metrics.benchmarks.CounterBenchmarkData"));
    metrics_276d5e4.add(item("Rename Class", "io.dropwizard.metrics.ConsoleReporter", "io.dropwizard.metrics.ConsolePrinter"));
    metrics_276d5e4.add(item("Rename Class", "io.dropwizard.metrics.Counter", "io.dropwizard.metrics.CounterMetric"));
    metrics_276d5e4.add(item("Move Class", "io.dropwizard.metrics.jdbi.strategies.BasicSqlNameStrategy", "io.dropwizard.metrics.jdbi.BasicSqlNameStrategy"));
    metrics_276d5e4.add(item("Move Class", "io.dropwizard.metrics.health.HealthCheck", "io.dropwizard.metrics.health.jvm.HealthCheck"));
    metrics_276d5e4.add(item("Move Class", "io.dropwizard.metrics.health.SharedHealthCheckRegistries", "io.dropwizard.metrics.health.jvm.SharedHealthCheckRegistries"));
    metrics_276d5e4.add(item("Move Class", "com.codahale.metrics.MetricSet", "com.codahale.metrics.health.MetricSet"));
    metrics_276d5e4.add(item("Move Class", "com.codahale.metrics.Snapshot", "com.codahale.metrics.health.Snapshot"));
    metrics_276d5e4.add(item("Extract SuperClass", "io.dropwizard.metrics.UniformReservoir", "io.dropwizard.metrics.ReservoirNextLong"));
    metrics_276d5e4.add(item("Extract Interface", "io.dropwizard.metrics.httpclient.InstrumentedHttpRequestExecutor", "io.dropwizard.metrics.httpclient.HttpExecutor"));
    metrics_276d5e4.add(item("Extract Method", "com.codahale.metrics.MetricRegistry#registerAll(MetricName, MetricSet)", "com.codahale.metrics.MetricRegistry#registerLoop(MetricName, MetricSet)"));
    metrics_276d5e4.add(item("Extract Method", "io.dropwizard.metrics.graphite.GraphiteReporter#reportTimer(MetricName, Timer, long)", "io.dropwizard.metrics.graphite.GraphiteReporter#sendDataToGraphite(MetricName, long, Snapshot)"));
    metrics_276d5e4.add(item("Extract Method", "io.dropwizard.metrics.graphite.PickledGraphite#pickleMetrics(List<MetricTuple>)", "io.dropwizard.metrics.graphite.PickledGraphite#populateWithTupleValues(List<MetricTuple>, Writer)"));
    metrics_276d5e4.add(item("Extract Method", "io.dropwizard.metrics.health.SharedHealthCheckRegistries#getOrCreate(String)", "io.dropwizard.metrics.health.jvm.SharedHealthCheckRegistries#createRegistry(String)"));
    metrics_276d5e4.add(item("Extract Method", "io.dropwizard.metrics.jetty9.InstrumentedHandler#updateResponses(HttpServletRequest, HttpServletResponse, long)", "io.dropwizard.metrics.jetty9.InstrumentedHandler#updateRequest(HttpServletRequest, long)"));
    metrics_276d5e4.add(item("Inline Method", "io.dropwizard.metrics.jdbi.InstrumentedTimingCollector#getTimer(StatementContext)", "io.dropwizard.metrics.jdbi.InstrumentedTimingCollector#collect(long, StatementContext)"));
    metrics_276d5e4.add(item("Inline Method", "io.dropwizard.metrics.FixedNameCsvFileProvider#sanitize(MetricName)", "io.dropwizard.metrics.FixedNameCsvFileProvider#getFile(File, MetricName)"));
    metrics_276d5e4.add(item("Inline Method", "io.dropwizard.metrics.ScheduledReporter#calculateRateUnit(TimeUnit)", "io.dropwizard.metrics.ScheduledReporter.ScheduledReporter(MetricRegistry, MetricFilter, TimeUnit, TimeUnit, ScheduledExecutorService)"));
    metrics_276d5e4.add(item("Inline Method", "io.dropwizard.metrics.SlidingTimeWindowReservoir#trim()", "io.dropwizard.metrics.SlidingTimeWindowReservoir#getSnapshot()\nio.dropwizard.metrics.SlidingTimeWindowReservoir#update(long)\nio.dropwizard.metrics.SlidingTimeWindowReservoir#size()"));
    metrics_276d5e4.add(item("Inline Method", "io.dropwizard.metrics.CsvReporter.reportHistogram(long, MetricName, Histogram)", "io.dropwizard.metrics.CsvReporter#report(SortedMap<MetricName, Gauge>, SortedMap<MetricName, Counter>, SortedMap<MetricName, Histogram>, SortedMap<MetricName, Meter>, SortedMap<MetricName, Timer>)"));
    metrics_276d5e4.add(item("Move Method", "io.dropwizard.metrics.SlidingTimeWindowReservoir#getTick()", "io.dropwizard.metrics.Clock#getTick(SlidingTimeWindowReservoir)"));
    metrics_276d5e4.add(item("Move Method", "io.dropwizard.metrics.Meter#tickIfNecessary()", "io.dropwizard.metrics.Clock#tickIfNecessary(Meter)"));
    metrics_276d5e4.add(item("Move Method", "io.dropwizard.metrics.Timer#update(long)", "io.dropwizard.metrics.Histogram#update(Timer, long)"));
    metrics_276d5e4.add(item("Move Method", "io.dropwizard.metrics.ConsoleReporter#printMeter(Meter)", "io.dropwizard.metrics.Meter#printMeter(ConsolePrinter)"));
    metrics_276d5e4.add(item("Move Method", "io.dropwizard.metrics.Slf4jReporter#prefix(MetricName, String[])", "io.dropwizard.metrics.MetricName#prefix(Slf4jReporter, String[])"));
    metrics_276d5e4.add(item("Rename Method", "io.dropwizard.metrics.graphite.Graphite#send(String, String, long)", "io.dropwizard.metrics.graphite.Graphite#sendData(String, String, long)"));
    metrics_276d5e4.add(item("Rename Method", "io.dropwizard.metrics.graphite.Graphite#flush()", "io.dropwizard.metrics.graphite.Graphite#flushData()"));
    metrics_276d5e4.add(item("Rename Method", "io.dropwizard.metrics.health.HealthCheck#execute()", "io.dropwizard.metrics.health.jvm.HealthCheck#executeCheck()"));
    metrics_276d5e4.add(item("Rename Method", "io.dropwizard.metrics.UnsafeStriped64#internalReset(long)", "io.dropwizard.metrics.UnsafeStriped64#internalResetData(long)"));
    // Rename before pull up
    //metrics_276d5e4.add(item("Rename Method", "io.dropwizard.metrics.servlet.InstrumentedFilter#createMeterNamesByStatusCode()", "io.dropwizard.metrics.servlet.InstrumentedFilter#createByStatusCode()"));
    metrics_276d5e4.add(item("Pull Up Method", "io.dropwizard.metrics.servlet.InstrumentedFilter#createMeterNamesByStatusCode()", "io.dropwizard.metrics.servlet.AbstractInstrumentedFilter#createByStatusCode()"));
    metrics_276d5e4.add(item("Pull Up Method", "io.dropwizard.metrics.graphite.GraphiteReporter#format(long)", "io.dropwizard.metrics.ScheduledReporter#format(long)"));
    metrics_276d5e4.add(item("Pull Up Method", "io.dropwizard.metrics.graphite.GraphiteReporter#format(double)", "io.dropwizard.metrics.ScheduledReporter#format(double)"));
    metrics_276d5e4.add(item("Push Down Method", "io.dropwizard.metrics.jdbi.strategies.DelegatingStatementNameStrategy#registerStrategies(StatementNameStrategy[])", "io.dropwizard.metrics.jdbi.BasicSqlNameStrategy#registerStrategies(StatementNameStrategy[])\n io.dropwizard.metrics.jdbi.strategies.ContextNameStrategy#registerStrategies(StatementNameStrategy[])\n io.dropwizard.metrics.jdbi.strategies.NaiveNameStrategy#registerStrategies(StatementNameStrategy[])\n io.dropwizard.metrics.jdbi.strategies.ShortNameStrategy#registerStrategies(StatementNameStrategy[])\n io.dropwizard.metrics.jdbi.strategies.SmartNameStrategy#registerStrategies(StatementNameStrategy[])"));
    metrics_276d5e4.add(item("Push Down Method", "io.dropwizard.metrics.ScheduledReporter#convertRate(double)", "io.dropwizard.metrics.ConsolePrinter#convertRate(double)\n io.dropwizard.metrics.CsvReporter#convertRate(double)\n io.dropwizard.metrics.graphite.GraphiteReporter#convertRate(double)\n io.dropwizard.metrics.influxdb.InfluxDbReporter#convertRate(double)\n io.dropwizard.metrics.Slf4jReporter#convertRate(double)"));
    metrics_276d5e4.add(item("Push Down Method", "io.dropwizard.metrics.ScheduledReporter#convertDuration(double)", "io.dropwizard.metrics.ConsolePrinter#convertDuration(double)\n io.dropwizard.metrics.CsvReporter#convertDuration(double)\n io.dropwizard.metrics.graphite.GraphiteReporter#convertDuration(double)\n io.dropwizard.metrics.influxdb.InfluxDbReporter#convertDuration(double)\n io.dropwizard.metrics.Slf4jReporter#convertDuration(double)"));
    metrics_276d5e4.add(item("Move Field", "io.dropwizard.metrics.CsvReporter#UTF_8", "io.dropwizard.metrics.CsvReporterTest#UTF_8"));
    metrics_276d5e4.add(item("Move Field", "io.dropwizard.metrics.jvm.GarbageCollectorMetricSet#WHITESPACE", "io.dropwizard.metrics.jvm.GarbageCollectorMetricSetTest#WHITESPACE"));
    metrics_276d5e4.add(item("Move Field", "io.dropwizard.metrics.jvm.MemoryUsageGaugeSet#WHITESPACE", "io.dropwizard.metrics.jvm.MemoryUsageGaugeSetTest#WHITESPACE"));
    metrics_276d5e4.add(item("Move Field", "io.dropwizard.metrics.jvm.ThreadDeadlockDetector#MAX_STACK_TRACE_DEPTH", "io.dropwizard.metrics.jvm.ThreadDeadlockDetectorTest#MAX_STACK_TRACE_DEPTH"));
    metrics_276d5e4.add(item("Move Field", "io.dropwizard.metrics.jvm.ThreadDump#UTF_8", "io.dropwizard.metrics.jvm.ThreadDumpTest#UTF_8"));
    metrics_276d5e4.add(item("Pull Up Field", "io.dropwizard.metrics.servlet.InstrumentedFilter#CREATED", "io.dropwizard.metrics.servlet.AbstractInstrumentedFilter#CREATED"));
    metrics_276d5e4.add(item("Pull Up Field", "io.dropwizard.metrics.servlet.InstrumentedFilter#NO_CONTENT", "io.dropwizard.metrics.servlet.AbstractInstrumentedFilter#NO_CONTENT"));
    metrics_276d5e4.add(item("Pull Up Field", "io.dropwizard.metrics.servlet.InstrumentedFilter#BAD_REQUEST", "io.dropwizard.metrics.servlet.AbstractInstrumentedFilter#BAD_REQUEST"));
    metrics_276d5e4.add(item("Push Down Field", "io.dropwizard.metrics.ScheduledReporter#durationUnit", "io.dropwizard.metrics.ConsolePrinter#durationUnit\nio.dropwizard.metrics.CsvReporter#durationUnit\nio.dropwizard.metrics.graphite.GraphiteReporter#durationUnit\nio.dropwizard.metrics.influxdb.InfluxDbReporter#durationUnit\nio.dropwizard.metrics.Slf4jReporter#durationUnit"));
    metrics_276d5e4.add(item("Push Down Field", "io.dropwizard.metrics.ScheduledReporter#rateFactor", "io.dropwizard.metrics.ConsolePrinter#rateFactor\nio.dropwizard.metrics.CsvReporter#rateFactor\nio.dropwizard.metrics.graphite.GraphiteReporter#rateFactor\nio.dropwizard.metrics.influxdb.InfluxDbReporter#rateFactor\nio.dropwizard.metrics.Slf4jReporter#rateFactor"));
    metrics_276d5e4.add(item("Push Down Field", "io.dropwizard.metrics.ScheduledReporter#durationFactor", "io.dropwizard.metrics.ConsolePrinter#durationFactor\nio.dropwizard.metrics.CsvReporter#durationFactor\nio.dropwizard.metrics.graphite.GraphiteReporter#durationFactor\nio.dropwizard.metrics.influxdb.InfluxDbReporter#durationFactor\nio.dropwizard.metrics.Slf4jReporter#durationFactor"));
    
    guava_79767ec.add(item("Rename Class", "com.google.common.eventbus.DeadEvent", "com.google.common.eventbus.GhostEvent"));
    guava_79767ec.add(item("Rename Class", "com.google.common.io.CountingInputStream", "com.google.common.io.CountableInputStream"));
    guava_79767ec.add(item("Rename Class", "com.google.common.io.CountingOutputStream", "com.google.common.io.CountableOutputStream"));
    guava_79767ec.add(item("Rename Class", "com.google.common.base.MoreObjects", "com.google.common.base.ObjectsExtension"));
    guava_79767ec.add(item("Rename Class", "com.google.common.collect.Collections2", "com.google.common.collect.CollectionsExtension"));
    guava_79767ec.add(item("Move Class", "com.google.common.base.Ascii", "com.google.common.primitives.Ascii"));
    guava_79767ec.add(item("Move Class", "com.google.common.base.Charsets", "com.google.common.primitives.Charsets"));
    guava_79767ec.add(item("Move Class", "com.google.common.base.Defaults", "com.google.common.primitives.Defaults"));
    guava_79767ec.add(item("Move Class", "com.google.common.base.Strings", "com.google.common.primitives.Strings"));
    guava_79767ec.add(item("Move Class", "com.google.common.xml.XmlEscapers", "com.google.common.escape.XmlEscapers"));
    guava_79767ec.add(item("Extract Superclass", "com.google.common.base.Utf8", "com.google.common.base.Encoding"));
    guava_79767ec.add(item("Extract Interface", "com.google.common.base.Joiner", "com.google.common.base.Joinable"));
    guava_79767ec.add(item("Rename Method", "com.google.common.io.MultiReader#advance()", "com.google.common.io.MultiReader#openNextStream()"));
    guava_79767ec.add(item("Rename Method", "com.google.common.net.HostSpecifier#from(String specifier)", "com.google.common.net.HostSpecifier#create(String specifier)"));
    guava_79767ec.add(item("Rename Method", "com.google.common.io.ReaderInputStream#grow(CharBuffer buf)", "com.google.common.io.ReaderInputStream#growTwice(CharBuffer buf)"));
    guava_79767ec.add(item("Rename Method", "com.google.common.hash.HashFunction#bits", "com.google.common.hash.HashFunction#numberOfBits"));
    guava_79767ec.add(item("Rename Method", "com.google.common.math.BigIntegerMath#binomial(int n, int k)", "com.google.common.math.BigIntegerMath#binomialCoefficient(int n, int k)"));
    guava_79767ec.add(item("Move Method", "com.google.common.io.CharStreams#copy(Readable from, Appendable to)", "com.google.common.io.CharSource#copy(Readable from, Appendable to) "));
    guava_79767ec.add(item("Move Method", "com.google.common.math.Quantiles#interpolate(double lower, double upper, double remainder, double scale)", "com.google.common.math.Quantiles.ScaleAndIndex#interpolate(double lower, double upper, double remainder, double scale)"));
    guava_79767ec.add(item("Move Method", "com.google.common.math.Quantiles#checkIndex(int index, int scale)", "com.google.common.math.Quantiles.ScaleAndIndex#checkIndex(int index, int scale)"));
    guava_79767ec.add(item("Move Method", "com.google.common.math.Quantiles#containsNaN(double[] dataset)", "com.google.common.math.Quantiles.ScaleAndIndex#containsNaN(double[] dataset)"));
    guava_79767ec.add(item("Move Method", "com.google.common.math.Quantiles#selectInPlace(int required, double[] array, int from, int to)", "com.google.common.math.Quantiles.ScaleAndIndex#selectInPlace(int required, double[] array, int from, int to)"));
    guava_79767ec.add(item("Pull Up Method", "com.google.common.escape.CharEscaper#growBuffer(char[] dest, int index, int size)\ncom.google.common.escape.UnicodeEscaper#growBuffer(char[] dest, int index, int size)", "com.google.common.escape.Escaper#growBuffer(char[] dest, int index, int size)"));
    guava_79767ec.add(item("Pull Up Method", "com.google.common.base.Utf8#encodedLengthGeneral(CharSequence sequence, int start) ", "com.google.common.base.Encoding#encodedLengthGeneral(CharSequence sequence, int start) "));
    // Pull Up to interface
    guava_79767ec.add(item("Pull Up Method", "com.google.common.cache.LongAdder#fn(long v, long x)", "com.google.common.cache.LongAddable#fn(long v, long x)\ncom.google.common.cache.LongAddables#fn(long v, long x)"));
    guava_79767ec.add(item("Push Down Method", "com.google.common.escape.Escaper#asFunction()", "com.google.common.escape.CharEscaper#asFunction()\ncom.google.common.escape.UnicodeEscaper#asFunction()"));
    // Not a push down, but move
    guava_79767ec.add(item("Move Method", "com.google.common.io.LineBuffer#add(char[] cbuf, int off, int len)", "com.google.common.io.LineReader#add(char[] cbuf, int off, int len)"));
    guava_79767ec.add(item("Move Method", "com.google.common.io.LineBuffer#finishLine(boolean sawNewline)", "com.google.common.io.LineReader#finishLine(boolean sawNewline)"));
    guava_79767ec.add(item("Extract Method", "com.google.common.math.BigIntegerMath#sqrt(BigInteger x, RoundingMode mode)", "com.google.common.math.BigIntegerMath#sqrtMode(BigInteger x, RoundingMode mode)"));
    guava_79767ec.add(item("Extract Method", "com.google.common.math.IntMath#binomial(int n, int k) ", "com.google.common.math.IntMath#calculateK(int n, int k)"));
    guava_79767ec.add(item("Extract Method", "com.google.common.net.InetAddresses#convertDottedQuadToHex(String ipString)", "com.google.common.net.InetAddresses#concatLastTerms(String initialPart, byte[] quad)"));
    guava_79767ec.add(item("Extract Method", "com.google.common.net.InetAddresses#getCoercedIPv4Address(InetAddress ip) ", "com.google.common.net.InetAddresses#coercedInet6(InetAddress ip) "));
    guava_79767ec.add(item("Extract Method", "com.google.common.io.Files#simplifyPath(String pathname)", "com.google.common.io.Files#joinPath(String pathname, List<String> path)"));
    guava_79767ec.add(item("Inline Method", "com.google.common.io.CharSequenceReader#hasRemaining()", "com.google.common.io.CharSequenceReader#read(CharBuffer target)\ncom.google.common.io.CharSequenceReader#read()\ncom.google.common.io.CharSequenceReader#read(char[] cbuf, int off, int len)"));
    guava_79767ec.add(item("Inline Method", "com.google.common.net.HostAndPort#hasPort()", "com.google.common.net.HostAndPort#getPort()\ncom.google.common.net.HostAndPort#getPortOrDefault(int defaultPort)\ncom.google.common.net.HostAndPort#fromParts(String host, int port) \ncom.google.common.net.HostAndPort#fromHost(String host)\ncom.google.common.net.HostAndPort#withDefaultPort(int defaultPort)\ncom.google.common.net.HostAndPort#toString()\ncom.google.common.net.HostSpecifier#fromValid(String specifier)"));
    guava_79767ec.add(item("Inline Method", "com.google.common.util.concurrent.ThreadFactoryBuilder#format(String format, Object[] args)", "com.google.common.util.concurrent.ThreadFactoryBuilder#setNameFormat(String nameFormat)\ncom.google.common.util.concurrent.ThreadFactoryBuilder#build(ThreadFactoryBuilder builder)\ncom.google.common.util.concurrent.ThreadFactoryBuilder#newThread(Runnable runnable)"));
    guava_79767ec.add(item("Inline Method", "com.google.thirdparty.publicsuffix.TrieParser#reverse(CharSequence s)", "com.google.thirdparty.publicsuffix.TrieParser#doParseTrieToBuilder(List<CharSequence> stack, CharSequence encoded, ImmutableMap.Builder<String,PublicSuffixType> builder)"));
    guava_79767ec.add(item("Inline Method", "com.google.common.math.IntMath#sqrtFloor(int)", "com.google.common.math.IntMath#sqrt(int x, RoundingMode mode)"));
    guava_79767ec.add(item("Move Field", "com.google.common.net.InternetDomainName#DOTS_MATCHER", "com.google.common.net.InetAddresses#DOTS_MATCHER"));
    guava_79767ec.add(item("Move Field", "com.google.common.net.InternetDomainName#DOT_SPLITTER", "com.google.common.net.InetAddresses#DOT_SPLITTER"));
    guava_79767ec.add(item("Move Field", "com.google.common.net.InternetDomainName#DOT_JOINER", "com.google.common.net.InetAddresses#DOT_JOINER"));
    guava_79767ec.add(item("Move Field", "com.google.common.net.InternetDomainName#DOT_REGEX", "com.google.common.net.HostAndPort#DOT_REGEX"));
    guava_79767ec.add(item("Move Field", "com.google.common.net.MediaType#CHARSET_ATTRIBUTE", "com.google.common.net.HostAndPort#CHARSET_ATTRIBUTE"));
    guava_79767ec.add(item("Pull Up Field", "com.google.common.escape.CharEscaper#DEST_PAD_MULTIPLIER", "com.google.common.escape.Escaper#DEST_PAD_MULTIPLIER"));
    guava_79767ec.add(item("Pull Up Field", "com.google.common.base.SmallCharMatcher#C1", "com.google.common.base.CharMatcher.NamedFastMatcher#C1"));
    guava_79767ec.add(item("Pull Up Field", "com.google.common.base.SmallCharMatcher#C2", "com.google.common.base.CharMatcher.NamedFastMatcher#C2"));
    guava_79767ec.add(item("Push Down Field", "com.google.common.escape.Escaper#asFunction", "com.google.common.escape.CharEscaper#asFunction\ncom.google.common.escape.UnicodeEscaper#asFunction"));
    // Not a push down, but move
    guava_79767ec.add(item("Move Field", "com.google.common.io.LineBuffer#line", "com.google.common.io.LineReader#line"));
    guava_79767ec.add(item("Move Field", "com.google.common.io.LineBuffer#sawReturn", "com.google.common.io.LineReader#sawReturn"));
    
    springBoot_48e893a.add(item("Rename Class", "org.springframework.boot.yaml.ArrayDocumentMatcher", "org.springframework.boot.yaml.ArrayDocumentMatcherNew"));
    springBoot_48e893a.add(item("Rename Class", "org.springframework.boot.logging.log4j2.ColorConverter", "org.springframework.boot.logging.log4j2.ColorConverterRenamed"));
    springBoot_48e893a.add(item("Rename Class", "org.springframework.boot.logging.log4j2.WhitespaceThrowablePatternConverter", "org.springframework.boot.logging.log4j2.WhitespaceThrowablePatternConverterRenamed"));
    springBoot_48e893a.add(item("Rename Class", "org.springframework.boot.yaml.DefaultProfileDocumentMatcher", "org.springframework.boot.yaml.DefaultProfileDocumentMatcherNew"));
    springBoot_48e893a.add(item("Rename Class", "org.springframework.boot.web.servlet.ServletComponentScanRegistrar", "org.springframework.boot.web.servlet.ServletComponentScanRegistrarRenamed"));
    springBoot_48e893a.add(item("Move Class", "org.springframework.boot.context.config.AnsiOutputApplicationListener", "org.springframework.boot.context.AnsiOutputApplicationListener"));
    springBoot_48e893a.add(item("Move Class", "org.springframework.boot.actuate.endpoint.jmx.DataEndpointMBean", "org.springframework.boot.actuate.endpoint.DataEndpointMBean"));
    springBoot_48e893a.add(item("Move Class", "org.springframework.boot.actuate.endpoint.mvc.DocsMvcEndpoint", "org.springframework.boot.actuate.endpoint.DocsMvcEndpoint"));
    springBoot_48e893a.add(item("Move Class", "org.springframework.boot.actuate.endpoint.mvc.LogFileMvcEndpoint", "org.springframework.boot.actuate.endpoint.LogFileMvcEndpoint"));
    springBoot_48e893a.add(item("Move Class", "org.springframework.boot.actuate.metrics.aggregate.AggregateMetricReader", "org.springframework.boot.actuate.metrics.AggregateMetricReader"));
    springBoot_48e893a.add(item("Extract Superclass", "org.springframework.boot.ant.FindMainClass", "org.springframework.boot.ant.MainClass"));
    springBoot_48e893a.add(item("Extract Interface", "org.springframework.boot.actuate.health.Health", "org.springframework.boot.actuate.health.HealthInterface"));
    springBoot_48e893a.add(item("Rename Method", "org.springframework.boot.bind.RelaxedDataBinder#cleanNamePrefix(String)", "org.springframework.boot.bind.RelaxedDataBinder#cleanNamePrefixRenamed(String)"));
    springBoot_48e893a.add(item("Rename Method", "org.springframework.boot.bind.RelaxedDataBinder#setIgnoreNestedProperties(boolean)", "org.springframework.boot.bind.RelaxedDataBinder#setIgnoreNestedPropertiesRenamed(boolean)"));
    springBoot_48e893a.add(item("Rename Method", "org.springframework.boot.bind.RelaxedDataBinder#setNameAliases(Map<String, List<String>>)", "org.springframework.boot.bind.RelaxedDataBinder#setNameAliasesRenamed(Map<String, List<String>>)"));
    springBoot_48e893a.add(item("Rename Method", "org.springframework.boot.builder.ParentContextCloserApplicationListener#maybeInstallListenerInParent(ConfigurableApplicationContext)", "org.springframework.boot.builder.ParentContextCloserApplicationListener#maybeInstallListenerInParentNew(ConfigurableApplicationContext)"));
    springBoot_48e893a.add(item("Rename Method", "org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor#addWithPrefix(Properties, Properties, String)", "org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor#addWithPrefixNew(Properties, Properties, String)"));
    springBoot_48e893a.add(item("Move Method", "org.springframework.boot.SpringApplication#handleRunFailure(ConfigurableApplicationContext, SpringApplicationRunListeners, Throwable)", "org.springframework.boot.SpringApplicationRunListeners#handleRunFailure(ConfigurableApplicationContext, SpringApplication, Throwable)"));
    springBoot_48e893a.add(item("Move Method", "org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter#buildMessage(FailureAnalysis failureAnalysis)", "org.springframework.boot.diagnostics.FailureAnalysis#buildMessage( )"));
    springBoot_48e893a.add(item("Move Method", "org.springframework.boot.context.config.ConfigFileApplicationListener#onApplicationPreparedEvent(ApplicationEvent)", "org.springframework.boot.logging.DeferredLog#onApplicationPreparedEvent(ConfigFileApplicationListener, ApplicationEvent)"));
    springBoot_48e893a.add(item("Move Method", "org.springframework.boot.context.embedded.AbstractConfigurableEmbeddedServletContainer#shouldRegisterJspServlet()", "org.springframework.boot.context.embedded.Compression#shouldRegisterJspServlet(AbstractConfigurableEmbeddedServletContainer)"));
    springBoot_48e893a.add(item("Move Method", "org.springframework.boot.logging.LoggingApplicationListener#registerShutdownHookIfNecessary(Environment, LoggingSystem)", "org.springframework.boot.logging.LoggingSystem#registerShutdownHookIfNecessary(Environment, LoggingApplicationListener)"));
    springBoot_48e893a.add(item("Pull Up Method", "org.springframework.boot.actuate.security.AuthorizationAuditListener#onAuthenticationCredentialsNotFoundEvent(AuthenticationCredentialsNotFoundEvent)", "org.springframework.boot.actuate.security.AbstractAuthorizationAuditListener#onAuthenticationCredentialsNotFoundEvent(AuthenticationCredentialsNotFoundEvent)"));
    springBoot_48e893a.add(item("Pull Up Method", "org.springframework.boot.actuate.security.AuthorizationAuditListener#onAuthorizationFailureEvent(AuthorizationFailureEvent)", "org.springframework.boot.actuate.security.AbstractAuthorizationAuditListener#onAuthorizationFailureEvent(AuthorizationFailureEvent) "));
    springBoot_48e893a.add(item("Pull Up Method", "org.springframework.boot.autoconfigure.condition.OnClassCondition#getAttributes(AnnotatedTypeMetadata, Class<?>)", "org.springframework.boot.autoconfigure.condition.SpringBootCondition#getAttributes(AnnotatedTypeMetadata, Class<?>)"));
    springBoot_48e893a.add(item("Push Down Method", "org.springframework.boot.context.embedded.RegistrationBean#setEnabled(boolean)", "org.springframework.boot.context.embedded.AbstractFilterRegistrationBean#setEnabled(boolean)\norg.springframework.boot.context.embedded.ServletListenerRegistrationBean#setEnabled(boolean)\norg.springframework.boot.context.embedded.ServletRegistrationBean#setEnabled(boolean)"));
    springBoot_48e893a.add(item("Push Down Method", "org.springframework.boot.context.embedded.RegistrationBean#isEnabled()", "org.springframework.boot.context.embedded.AbstractFilterRegistrationBean#isEnabled()\norg.springframework.boot.context.embedded.ServletListenerRegistrationBean#isEnabled()\norg.springframework.boot.context.embedded.ServletRegistrationBean#isEnabled()"));
    springBoot_48e893a.add(item("Push Down Method", "org.springframework.boot.actuate.cache.AbstractJmxCacheStatisticsProvider#getMBeanServer()", "org.springframework.boot.actuate.cache.InfinispanCacheStatisticsProvider#getMBeanServer()\norg.springframework.boot.actuate.cache.JCacheCacheStatisticsProvider#getMBeanServer()"));
    springBoot_48e893a.add(item("Push Down Method", "org.springframework.boot.actuate.cache.AbstractJmxCacheStatisticsProvider#getAttribute(ObjectName, String, Class<T>)", "org.springframework.boot.actuate.cache.InfinispanCacheStatisticsProvider#getAttribute(ObjectName, String, Class<T>)\norg.springframework.boot.actuate.cache.JCacheCacheStatisticsProvider#getAttribute(ObjectName, String, Class<T>)"));
    springBoot_48e893a.add(item("Push Down Method", "org.springframework.boot.context.embedded.AbstractConfigurableEmbeddedServletContainer#setDisplayName(String)", "org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory#setDisplayName(String)\norg.springframework.boot.context.web.ErrorPageFilter#setDisplayName(String)"));
    springBoot_48e893a.add(item("Push Down Method", "org.springframework.boot.context.embedded.AbstractConfigurableEmbeddedServletContainer#getDisplayName()", "org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory#getDisplayName()\norg.springframework.boot.context.web.ErrorPageFilter#getDisplayName()"));
    // This refactoring was not performed
    //springBoot_48e893a.add(item("Extract Method", "org.springframework.boot.ansi.AnsiOutput#buildEnabled(StringBuilder, Object[])", "org.springframework.boot.ansi.AnsiOutput#auxBuildEnabled(StringBuilder, Object[], boolean, boolean)"));
    springBoot_48e893a.add(item("Extract Method", "org.springframework.boot.cli.SpringCli#main(String[])", "org.springframework.boot.cli.SpringCli#runner()"));
    springBoot_48e893a.add(item("Extract Method", "org.springframework.boot.cli.command.run.RunCommand.RunOptionHandler#run(OptionSet)", "org.springframework.boot.cli.command.run.RunCommand.RunOptionHandler#aux(OptionSet)"));
    springBoot_48e893a.add(item("Extract Method", "org.springframework.boot.cli.command.test.TestRunner.RunThread#run()", "org.springframework.boot.cli.command.test.TestRunner.RunThread#verify() "));
    springBoot_48e893a.add(item("Extract Method", "org.springframework.boot.actuate.metrics.buffer.Buffers<B extends Buffer<?>>#doWith(String, Consumer<B>)", "org.springframework.boot.actuate.metrics.buffer.Buffers<B extends Buffer<?>>#test(String) "));
    springBoot_48e893a.add(item("Inline Method", "org.springframework.boot.ansi.AnsiOutput#buildDisabled(StringBuilder, Object[])", "org.springframework.boot.ansi.AnsiOutput#toString(Object[]) "));
    springBoot_48e893a.add(item("Inline Method", "org.springframework.boot.autoconfigure.AutoConfigurationSorter#sortByAnnotation(AutoConfigurationClasses, List<String>)", "org.springframework.boot.autoconfigure.AutoConfigurationSorter#getInPriorityOrder(Collection<String>)"));
    springBoot_48e893a.add(item("Inline Method", "org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor#extractPropertiesFromApplication(Properties, Map<String, Object>)", "org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor#getPropertiesFromApplication(Environment)"));
    springBoot_48e893a.add(item("Inline Method", "org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelector.ConfigurationPropertiesBeanRegistrar#collectClasses(List<Object>)", "org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelector.ConfigurationPropertiesBeanRegistrar#registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)"));
    springBoot_48e893a.add(item("Inline Method", "org.springframework.boot.actuate.cache.EhCacheStatisticsProvider#cacheHitRatio(StatisticsGateway)", "org.springframework.boot.actuate.cache.EhCacheStatisticsProvider#getCacheStatistics(CacheManager, EhCacheCache)"));
    springBoot_48e893a.add(item("Move Field", "Não foi possível realizar a refatoração ", ""));
    springBoot_48e893a.add(item("Pull Up Field", "org.springframework.boot.actuate.autoconfigure.OnEnabledEndpointElementCondition#prefix", "org.springframework.boot.autoconfigure.condition.SpringBootCondition#prefix"));
    springBoot_48e893a.add(item("Pull Up Field", "org.springframework.boot.actuate.autoconfigure.OnEnabledEndpointElementCondition#annotationType", "org.springframework.boot.autoconfigure.condition.SpringBootCondition#annotationType"));
    springBoot_48e893a.add(item("Pull Up Field", "org.springframework.boot.loader.ExecutableArchiveLauncher#archive", "org.springframework.boot.loader.Launcher#archive"));
    springBoot_48e893a.add(item("Push Down Field", "org.springframework.boot.context.embedded.RegistrationBean#enabled", "org.springframework.boot.context.embedded.AbstractFilterRegistrationBean#enabled\norg.springframework.boot.context.embedded.ServletListenerRegistrationBean#enabled\norg.springframework.boot.context.embedded.ServletRegistrationBean#enabled"));
    springBoot_48e893a.add(item("Push Down Field", "org.springframework.boot.actuate.cache.AbstractJmxCacheStatisticsProvider#mBeanServer", "org.springframework.boot.actuate.cache.InfinispanCacheStatisticsProvider#mBeanServer\norg.springframework.boot.actuate.cache.JCacheCacheStatisticsProvider#mBeanServer"));
    springBoot_48e893a.add(item("Push Down Field", "org.springframework.boot.context.embedded.AbstractConfigurableEmbeddedServletContainer#displayName", "org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory#displayName\norg.springframework.boot.context.web.ErrorPageFilter#displayName"));
    
    orientdb_b213aaf.add(item("Rename Class", "com.orientechnologies.orient.client.remote.OSBTreeCollectionManagerRemote", "com.orientechnologies.orient.client.remote.OSBTreeCollectionManagerAdim"));
    orientdb_b213aaf.add(item("Rename Class", "com.orientechnologies.orient.client.remote.OSBTreeBonsaiRemote", "com.orientechnologies.orient.client.remote.OSBTreeRemote"));
//    orientdb_b213aaf.add(item("Rename Class", "com.orientechnologies.orient.client.remote.OstorageRemote", "com.orientechnologies.orient.client.remote.OstorageRemote")); // errado
    orientdb_b213aaf.add(item("Rename Class", "com.orientechnologies.orient.client.remote.OStorageRemoteAsynchEventListener", "com.orientechnologies.orient.client.remote.OStorageRemEventLis"));
    orientdb_b213aaf.add(item("Rename Class", "com.orientechnologies.orient.client.remote.OStorageRemoteConfiguration", "com.orientechnologies.orient.client.remote.OStorageConfingRem"));
//    orientdb_b213aaf.add(item("Extract Class", "com.orientechnologies.orient.server.Oserver", "com.orientechnologies.orient.server.OserverConfigData"));
//    orientdb_b213aaf.add(item("Extract Class", "com.orientechnologies.orient.server.network.protocol.http.OhttpResponse", "com.orientechnologies.orient.server.network.protocol.http.OhttpResponseData"));
//    orientdb_b213aaf.add(item("Extract Class", "com.orientechnologies.orient.server.network.protocol.http.command.post.OserverCommandPostUploadSingleFile", "com.orientechnologies.orient.server.network.protocol.http.command.post.OserverCommandData"));
//    orientdb_b213aaf.add(item("Extract Class", "com.orientechnologies.orient.server.plugin.OserverPluginManager", "com.orientechnologies.orient.server.plugin.OserverPluginManagerData"));
//    orientdb_b213aaf.add(item("Extract Class", "com.orientechnologies.orient.server.plugin.mail.OmailPlugin", "com.orientechnologies.orient.server.plugin.mail.OmailPluginData"));
    orientdb_b213aaf.add(item("Extract Superclass", "com.orientechnologies.orient.etl.extractor.OJDBCExtractor\ncom.orientechno.OETLStubRandomExtractor", "com.orientechnologies.orient.etl.extractor.OextractorSuper"));
    orientdb_b213aaf.add(item("Extract Interface", "com.orientechnologies.orient.jdbc.OrientBlob", "com.orientechnologies.orient.jdbc.OrientBlobInterface"));
//    orientdb_b213aaf.add(item("Rename Method", "com.orientechnologies.orient.jdbc.OrientDataCode#orientDataSource", "com.orientechnologies.orient.jdbc.OrientDataCode#orientDataCode")); // errado
    orientdb_b213aaf.add(item("Rename Class", "com.orientechnologies.orient.jdbc.OrientDataSource", "com.orientechnologies.orient.jdbc.OrientDataCoude")); // certo
    
    orientdb_b213aaf.add(item("Rename Method", "com.orientechnologies.orient.client.db.ODatabaseHelper#releaseDatabase(ODatabase)", "com.orientechnologies.orient.client.db.ODatabaseHelper#releaseDb(ODatabase)"));
    orientdb_b213aaf.add(item("Rename Method", "com.orientechnologies.orient.client.db.ODatabaseHelper#freezeDatabase(ODatabase)", "com.orientechnologies.orient.client.db.ODatabaseHelper#freezeDb(ODatabase)"));
    orientdb_b213aaf.add(item("Rename Method", "com.orientechnologies.orient.client.db.ODatabaseHelper#existsDatabase(String)", "com.orientechnologies.orient.client.db.ODatabaseHelper#existsDb(String)"));
    orientdb_b213aaf.add(item("Rename Method", "com.orientechnologies.orient.client.db.ODatabaseHelper#dropDatabase(ODatabase, String, String)", "com.orientechnologies.orient.client.db.ODatabaseHelper#dropDb(ODatabase, String, String)"));
    // Not sure, pull up to interface but
    orientdb_b213aaf.add(item("Pull Up Method", "com.orientechnologies.orient.client.remote.OStorageRemoteAsynchEventListener#getStorage()", "com.orientechonologies.orient.enterprise.chamennel.binary.ORemoteServerEventListener#getStorage()"));
//    orientdb_b213aaf.add(item("Pull Up Method", "com.orientechnologies.orient.graph.handler.OgraphServerHandler#getname()", "com.orientechonologies.orient.core.command.script.Oscriptlnjection#getName()")); ??
//    orientdb_b213aaf.add(item("Pull Up Method", "com.orientechnologies.orient.client.remote.OengineRemote#NAME", "com.orientechnologies.orient.core.engine.OengineAbstract#NAME")); // errado
    orientdb_b213aaf.add(item("Pull Up Field", "com.orientechnologies.orient.client.remote.OEngineRemote#NAME", "com.orientechnologies.orient.core.engine.OEngineAbstract#NAME")); // certo
    orientdb_b213aaf.add(item("Pull Up Field", "com.orientechnologies.orient.client.remote.OStorageRemoteConfiguration#networkRecordSerializer", "com.orientechnologies.orient.core.config.OStorageConfiguration#networkRecordSerializer"));
//    orientdb_b213aaf.add(item("Pull Up Field", "com.orientechnologies.orient.etl.source.OfileSource#input", "com.orientechnologies.orient.etl.source.OabstractSouce#input")); ??
    
  }

  public RefactoringSet[] all() {
      return new RefactoringSet[]{
          atmosphere_cc2b3f1,
          clojure_17217a1,
          guava_79767ec,
          metrics_276d5e4,
          orientdb_b213aaf,
          retrofit_f13f317,
          springBoot_48e893a
      };
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
