package org.refactoringminer.astDiff.graph;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.*;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.diff.UMLModelDiff;
import java.util.*;
import javax.annotation.Nullable;
import org.refactoringminer.util.PathFileUtils;

/**
 * The statements only writing to a log, which give the callers of their member nothing: a call on a field holding a
 * logger of a logging library (see PlatformKnowledge), or an if with no else whose branch only logs.
 */
final class LogStatements {
  private final UMLModelDiff modelDiff;
  private final RMIndex rmIndex;
  private final ReceiverTypeResolver receiverTypeResolver;

  LogStatements(UMLModelDiff modelDiff, RMIndex rmIndex, ReceiverTypeResolver receiverTypeResolver) {
    this.modelDiff = modelDiff;
    this.rmIndex = rmIndex;
    this.receiverTypeResolver = receiverTypeResolver;
  }

  // Whether the change lies in a statement only logging (see isLogStatement): a log message edited, an argument added to
  // it, a log statement added or removed. It gives the callers of its member nothing, as no value they get or state they
  // read changes, so they are no usages of it (rocketmq_8685: the callers of closeChannel, about 85 lines, for the channel
  // ids added to its log messages); a reviewer judges the message itself, which the diff shows
  boolean logsOnly(Node change) {
    Tree tree = change.getTree();
    AbstractStatement statement = rmIndex.statementHolding(change.getSrcDst(), change.getPath(), tree);
    return statement != null && statement.getLocationInfo().getStartOffset() <= tree.getPos()
            && tree.getEndPos() <= statement.getLocationInfo().getEndOffset() && isLogStatement(change.getSrcDst(), statement);
  }

  // Whether the statement only logs: it is a call, its value unused, on a field holding a logger, an object of a logging
  // library (a type of a package of slf4j, log4j, logback, flogger, commons/java.util/jboss logging, or a logging package
  // of the repository, as netty's io.netty.util.internal.logging), or an if with no else whose branch only logs, as a
  // check whether a level is enabled (`if (tracerLog.isTraceEnabled() && shouldTraceAction(action))`, es_109236). A log
  // call writes to the log and gives its caller nothing: the values it formats change no state of the program, so neither
  // the logger's declaration (its 13-line javadoc in netty_14818), the methods computing a logged argument
  // (parseChannelRemoteAddr) nor the definitions of a local logged anew tell a reviewer anything of the change; whether
  // the placeholders and the arguments line up is in the diff. Knowing the logging libraries is knowledge of the
  // platform, as knowing the JDK
  boolean isLogStatement(SrcDst srcDst, @Nullable AbstractStatement statement) {
    if (statement instanceof CompositeStatementObject composite) {
      LocationInfo.CodeElementType type = composite.getLocationInfo().getCodeElementType();
      boolean guard = type.equals(LocationInfo.CodeElementType.IF_STATEMENT) && composite.getStatements().size() == 1;
      return (guard || type.equals(LocationInfo.CodeElementType.BLOCK)) && !composite.getStatements().isEmpty()
              && composite.getStatements().stream().allMatch(child -> isLogStatement(srcDst, child));
    }
    if (statement == null || !statement.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.EXPRESSION_STATEMENT)) {
      return false;
    }
    UMLAbstractClass context = rmIndex.ownerOf(srcDst, statement.getLocationInfo());
    if (context == null) {
      return false;
    }
    UMLModel umlModel = srcDst.equals(SrcDst.SRC) ? modelDiff.getParentModel() : modelDiff.getChildModel();
    String thisDot = PathFileUtils.getLang(statement.getLocationInfo().getFilePath()).THIS_DOT;
    // The call the statement only makes, as RM finds it with the statement termination of the language
    AbstractCall call = statement.invocationCoveringEntireFragment();
    if (call != null && call.getCoverage() == AbstractCall.StatementCoverageType.ONLY_CALL && call.getExpression() != null) {
      String receiver = call.getExpression();
      for (LeafExpression variable : statement.getVariables()) {
        String name = variable.getString();
        if (variable.getLocationInfo().getStartOffset() == call.getLocationInfo().getStartOffset()
                && (name.equals(receiver) || (thisDot + name).equals(receiver) || name.equals(thisDot + receiver))) {
          UMLAttribute field = receiverTypeResolver.resolveAccessTarget(variable, context, umlModel);
          return field != null && isLoggerType(field, umlModel);
        }
      }
    }
    return false;
  }

  // Whether the type of the field is one of a logging package: its qualified name as written, or by the single-type
  // import of the file declaring the field
  private static boolean isLoggerType(UMLAttribute field, UMLModel umlModel) {
    if (field.getType() == null) {
      return false;
    }
    String type = field.getType().getClassType();
    String qualified = type.contains(".") ? type : null;
    if (qualified == null) {
      for (UMLClass umlClass : umlModel.getClassList()) {
        if (!umlClass.getLocationInfo().getFilePath().equals(field.getLocationInfo().getFilePath())) {
          continue;
        }
        for (UMLImport umlImport : umlClass.getImportedTypes()) {
          if (!umlImport.isOnDemand() && umlImport.getName().endsWith("." + type)) {
            qualified = umlImport.getName();
          }
        }
      }
    }
    if (qualified == null) {
      return false;
    }
    String[] segments = qualified.split("\\.");
    for (int i = 0; i < segments.length - 1; i++) {
      if (PlatformKnowledge.LOGGING_PACKAGES.contains(segments[i])) {
        return true;
      }
    }
    return false;
  }
}
