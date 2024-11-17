/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.bpm.bar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.ValidationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.bonitasoft.engine.bpm.bar.xml.ActorDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ActorInitiatorDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.AutomaticTaskDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.BoundaryEventDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.BusinessDataDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.CallActivityDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.CallableElementBinding;
import org.bonitasoft.engine.bpm.bar.xml.CallableElementVersionBinding;
import org.bonitasoft.engine.bpm.bar.xml.CatchErrorEventTriggerDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.CatchMessageEventTriggerDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.CatchSignalEventTriggerDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ConditionalExpressionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ConnectorDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ConnectorDefinitionInputBinding;
import org.bonitasoft.engine.bpm.bar.xml.ConstraintDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ContextDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ContextEntryBinding;
import org.bonitasoft.engine.bpm.bar.xml.ContractDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ContractInputBinding;
import org.bonitasoft.engine.bpm.bar.xml.CorrelationBinding;
import org.bonitasoft.engine.bpm.bar.xml.CorrelationKeyBinding;
import org.bonitasoft.engine.bpm.bar.xml.CorrelationValueBinding;
import org.bonitasoft.engine.bpm.bar.xml.DataDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.DataInputOperationBinding;
import org.bonitasoft.engine.bpm.bar.xml.DataOutputOperationBinding;
import org.bonitasoft.engine.bpm.bar.xml.DefaultTransitionDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.DefaultValueBinding;
import org.bonitasoft.engine.bpm.bar.xml.DisplayDescriptionAfterCompletionExpressionBinding;
import org.bonitasoft.engine.bpm.bar.xml.DisplayDescriptionExpressionBinding;
import org.bonitasoft.engine.bpm.bar.xml.DisplayNameExpressionBinding;
import org.bonitasoft.engine.bpm.bar.xml.DocumentDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.DocumentListDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.EndEventDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ExpressionBinding;
import org.bonitasoft.engine.bpm.bar.xml.FlowElementBinding;
import org.bonitasoft.engine.bpm.bar.xml.GatewayDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.IncomingTransitionRefBinding;
import org.bonitasoft.engine.bpm.bar.xml.InputDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.IntermediateCatchEventBinding;
import org.bonitasoft.engine.bpm.bar.xml.IntermediateThrowEventDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.LeftOperandBinding;
import org.bonitasoft.engine.bpm.bar.xml.LoopConditionBinding;
import org.bonitasoft.engine.bpm.bar.xml.LoopMaxBinding;
import org.bonitasoft.engine.bpm.bar.xml.ManualTaskDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.MultiInstanceCompletionConditionBinding;
import org.bonitasoft.engine.bpm.bar.xml.MultiInstanceLoopCardinalityBinding;
import org.bonitasoft.engine.bpm.bar.xml.MultiInstanceLoopCharacteristicsBinding;
import org.bonitasoft.engine.bpm.bar.xml.OperationBinding;
import org.bonitasoft.engine.bpm.bar.xml.OutgoingTransitionRefBinding;
import org.bonitasoft.engine.bpm.bar.xml.ParameterDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ProcessDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ReceiveTaskDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.RightOperandBinding;
import org.bonitasoft.engine.bpm.bar.xml.SendTaskDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.StandardLoopCharacteristicsBinding;
import org.bonitasoft.engine.bpm.bar.xml.StartEventDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.StringIndexBinding;
import org.bonitasoft.engine.bpm.bar.xml.SubProcessDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.TargetFlowNodeBinding;
import org.bonitasoft.engine.bpm.bar.xml.TargetProcessBinding;
import org.bonitasoft.engine.bpm.bar.xml.TerminateEventTriggerDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.TextDataDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ThrowErrorEventTriggerDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ThrowMessageEventTriggerDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.ThrowSignalEventTriggerDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.TimerEventTriggerDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.TransitionDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.UserFilterDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.UserTaskDefinitionBinding;
import org.bonitasoft.engine.bpm.bar.xml.XMLProcessDefinition;
import org.bonitasoft.engine.bpm.flownode.FlowElementContainerDefinition;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.io.IOUtil;
import org.bonitasoft.engine.io.xml.ElementBinding;
import org.bonitasoft.engine.io.xml.XMLHandler;
import org.bonitasoft.engine.io.xml.XMLNode;
import org.bonitasoft.engine.io.xml.XMLParseException;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 */
public class ProcessDefinitionBARContribution implements BusinessArchiveContribution {

    public static final String PROCESS_DEFINITION_XML = "process-design.xml";

    public static final String PROCESS_INFOS_FILE = "process-infos.txt";

    private XMLHandler handler;

    public ProcessDefinitionBARContribution() {
        final List<Class<? extends ElementBinding>> bindings = new ArrayList<Class<? extends ElementBinding>>();
        bindings.add(ProcessDefinitionBinding.class);
        bindings.add(ActorDefinitionBinding.class);
        bindings.add(ActorInitiatorDefinitionBinding.class);
        bindings.add(UserTaskDefinitionBinding.class);
        bindings.add(ManualTaskDefinitionBinding.class);
        bindings.add(AutomaticTaskDefinitionBinding.class);
        bindings.add(ReceiveTaskDefinitionBinding.class);
        bindings.add(SendTaskDefinitionBinding.class);
        bindings.add(TransitionDefinitionBinding.class);
        bindings.add(GatewayDefinitionBinding.class);
        bindings.add(DefaultTransitionDefinitionBinding.class);
        bindings.add(ConnectorDefinitionBinding.class);
        bindings.add(ConnectorDefinitionInputBinding.class);
        bindings.add(UserFilterDefinitionBinding.class);
        bindings.add(ParameterDefinitionBinding.class);
        bindings.add(StartEventDefinitionBinding.class);
        bindings.add(StringIndexBinding.class);
        bindings.add(IntermediateCatchEventBinding.class);
        bindings.add(BoundaryEventDefinitionBinding.class);
        bindings.add(TimerEventTriggerDefinitionBinding.class);
        bindings.add(EndEventDefinitionBinding.class);
        bindings.add(ExpressionBinding.class);
        bindings.add(ConditionalExpressionBinding.class);
        bindings.add(DataDefinitionBinding.class);
        bindings.add(BusinessDataDefinitionBinding.class);
        bindings.add(TextDataDefinitionBinding.class);
        bindings.add(DocumentDefinitionBinding.class);
        bindings.add(DocumentListDefinitionBinding.class);
        bindings.add(DefaultValueBinding.class);
        bindings.add(DisplayDescriptionAfterCompletionExpressionBinding.class);
        bindings.add(DisplayDescriptionExpressionBinding.class);
        bindings.add(DisplayNameExpressionBinding.class);
        bindings.add(OutgoingTransitionRefBinding.class);
        bindings.add(IncomingTransitionRefBinding.class);
        bindings.add(CatchMessageEventTriggerDefinitionBinding.class);
        bindings.add(OperationBinding.class);
        bindings.add(ContractInputBinding.class);
        bindings.add(RightOperandBinding.class);
        bindings.add(LeftOperandBinding.class);
        bindings.add(ThrowMessageEventTriggerDefinitionBinding.class);
        bindings.add(CatchSignalEventTriggerDefinitionBinding.class);
        bindings.add(ThrowSignalEventTriggerDefinitionBinding.class);
        bindings.add(IntermediateThrowEventDefinitionBinding.class);
        bindings.add(CatchErrorEventTriggerDefinitionBinding.class);
        bindings.add(ThrowErrorEventTriggerDefinitionBinding.class);
        bindings.add(CorrelationBinding.class);
        bindings.add(CorrelationKeyBinding.class);
        bindings.add(CorrelationValueBinding.class);
        bindings.add(StandardLoopCharacteristicsBinding.class);
        bindings.add(MultiInstanceLoopCharacteristicsBinding.class);
        bindings.add(LoopConditionBinding.class);
        bindings.add(LoopMaxBinding.class);
        bindings.add(MultiInstanceLoopCardinalityBinding.class);
        bindings.add(MultiInstanceCompletionConditionBinding.class);
        bindings.add(CallActivityDefinitionBinding.class);
        bindings.add(DataInputOperationBinding.class);
        bindings.add(DataOutputOperationBinding.class);
        bindings.add(CallableElementBinding.class);
        bindings.add(CallableElementVersionBinding.class);
        bindings.add(TerminateEventTriggerDefinitionBinding.class);
        bindings.add(TargetProcessBinding.class);
        bindings.add(TargetFlowNodeBinding.class);
        bindings.add(SubProcessDefinitionBinding.class);
        bindings.add(FlowElementBinding.class);
        bindings.add(ContractDefinitionBinding.class);
        bindings.add(ContextDefinitionBinding.class);
        bindings.add(ContextEntryBinding.class);
        bindings.add(InputDefinitionBinding.class);
        bindings.add(ConstraintDefinitionBinding.class);

        //        final InputStream schemaStream = ProcessDefinitionBARContribution.class.getResourceAsStream("ProcessDefinition.xsd");
        //        try {
        //            handler = new XMLHandler(bindings, schemaStream);
        //        } catch (final Exception e) {
        //            throw new BonitaRuntimeException(e);
        //        } finally {
        //            try {
        //                schemaStream.close();
        //            } catch (final IOException e) {
        //                throw new BonitaRuntimeException(e);
        //            }
        //        }

        final URL schemaUrl = ProcessDefinitionBARContribution.class.getResource("ProcessDefinition.xsd");
        try {
            handler = new XMLHandler(bindings, schemaUrl);
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
    }

    @Override
    public boolean isMandatory() {
        return true;
    }

    @Override
    public boolean readFromBarFolder(final BusinessArchive businessArchive, final File barFolder) throws IOException, InvalidBusinessArchiveFormatException {
        final File file = new File(barFolder, PROCESS_DEFINITION_XML);
        if (!file.exists()) {
            return false;
        }
        final DesignProcessDefinition processDefinition = deserializeProcessDefinition(file);
        businessArchive.setProcessDefinition(processDefinition);
        checkProcessInfos(barFolder, processDefinition);
        return true;
    }

    protected void checkProcessInfos(final File barFolder, final DesignProcessDefinition processDefinition) throws InvalidBusinessArchiveFormatException {
        final String processInfos = getProcessInfos(generateInfosFromDefinition(processDefinition));
        String fileContent;
        try {
            fileContent = IOUtil.read(new File(barFolder, PROCESS_INFOS_FILE));
            if (!processInfos.equals(fileContent.trim())) {
                throw new InvalidBusinessArchiveFormatException("Invalid Business Archive format");
            }
        } catch (final IOException e) {
            throw new InvalidBusinessArchiveFormatException("Invalid Business Archive format");
        }
    }

    public DesignProcessDefinition deserializeProcessDefinition(final File file) throws IOException, InvalidBusinessArchiveFormatException {
        try {
            handler.validate(file);
            final Object objectFromXML = handler.getObjectFromXML(file);

            if (!(objectFromXML instanceof DesignProcessDefinition)) {
                throw new InvalidBusinessArchiveFormatException("The file did not contain a process, but: " + objectFromXML);
            }
            return (DesignProcessDefinition) objectFromXML;
        } catch (final XMLParseException e) {
            throw new InvalidBusinessArchiveFormatException(e);
        } catch (final ValidationException e) {
            checkVersion(IOUtil.read(file));
            throw new InvalidBusinessArchiveFormatException(e);
        }
    }

    void checkVersion(final String content) throws InvalidBusinessArchiveFormatException {
        final Pattern pattern = Pattern.compile("http://www\\.bonitasoft\\.org/ns/process/client/6.([0-9])");
        final Matcher matcher = pattern.matcher(content);
        final boolean find = matcher.find();
        if (!find) {
            throw new InvalidBusinessArchiveFormatException("There is no bonitasoft process namespace declaration");
        }
        final String group = matcher.group(1);
        if (!group.equals("3")) {
            throw new InvalidBusinessArchiveFormatException("Wrong version of your process definition, 6." + group
                    + " namespace is not compatible with your current version. Use the studio to update it.");
        }
    }

    @Override
    public void saveToBarFolder(final BusinessArchive businessArchive, final File barFolder) throws IOException {
        final DesignProcessDefinition processDefinition = businessArchive.getProcessDefinition();
        serializeProcessDefinition(barFolder, processDefinition);
    }

    public void serializeProcessDefinition(final File barFolder, final DesignProcessDefinition processDefinition) throws IOException {
        try {
            try (FileOutputStream outputStream = new FileOutputStream(new File(barFolder, PROCESS_DEFINITION_XML))) {
                handler.write(getXMLNode(processDefinition), outputStream);
            }
            final String infos = generateInfosFromDefinition(processDefinition);
            IOUtil.writeContentToFile(getProcessInfos(infos), new File(barFolder, PROCESS_INFOS_FILE));
        } catch (final FileNotFoundException e) {
            throw new IOException(e);
        }
    }

    public String getProcessDefinitionContent(DesignProcessDefinition processDefinition) throws IOException {
        final StringWriter writer = new StringWriter();
        handler.write(getXMLNode(processDefinition), writer);
        return writer.toString();
    }

    public DesignProcessDefinition read(String content) throws IOException, XMLParseException {
        return (DesignProcessDefinition) handler.getObjectFromXML(new StringReader(content));
    }

    XMLNode getXMLNode(DesignProcessDefinition processDefinition) {
        return new XMLProcessDefinition().getXMLProcessDefinition(processDefinition);
    }

    protected String generateInfosFromDefinition(final DesignProcessDefinition processDefinition) {
        final FlowElementContainerDefinition processContainer = processDefinition.getFlowElementContainer();
        return new StringBuilder("key1:").append(processDefinition.getActorsList().size()).append(",key2:").append(processContainer.getTransitions().size())
                .append(",key3:").append(processContainer.getActivities().size()).toString();
    }

    protected String getProcessInfos(final String infos) {
        return Base64.encodeBase64String(DigestUtils.md5(infos)).trim();
    }

    @Override
    public String getName() {
        return "Process design";
    }

}
