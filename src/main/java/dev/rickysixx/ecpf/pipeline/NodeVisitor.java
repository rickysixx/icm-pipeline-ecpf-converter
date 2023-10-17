package dev.rickysixx.ecpf.pipeline;

import de.intershop.core._2010.NamedElement;
import de.intershop.core._2010.Parameter;
import de.intershop.core._2010.ReferenceableElement;
import de.intershop.pipeline._2010.Node;
import de.intershop.pipeline._2010.NodeSuccessor;
import de.intershop.pipeline._2010.StartNode;
import de.intershop.pipeline._2010.SuccessorNode;
import de.intershop.pipeline._2010.*;
import dev.rickysixx.ecpf.io.IndentingPrintWriter;

import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Comparator.comparing;

public class NodeVisitor
{
    private final Pipeline pipeline;
    private final IndentingPrintWriter outputWriter;

    private void visitConfigurationValue(ConfigurationValue configurationValue)
    {
        outputWriter.printf("name: [%s]\n", configurationValue.getName());
        outputWriter.printf("value: [%s]\n", configurationValue.getValue());
    }

    private void visitInConnector(PipelineNodeInConnector inConnector)
    {
        outputWriter.printf("name: [%s]\n", inConnector.getName());

        visitList("parameter_bindings", inConnector.getParameterBindings(), this::visitParameterBinding);
    }

    private void visitNodeSuccessor(NodeSuccessor successor)
    {
        Node successorNode = pipeline.getNodeByID(successor.getNext());

        outputWriter.printf("name: [%s]\n", successor.getName());
        outputWriter.printf("transaction_mode: [%s]\n", successor.getAction());
        outputWriter.printf("next_node_type: [%s]\n", successorNode.getClass().getSimpleName());

        if (successorNode instanceof PipelineNodeInConnector inConnector)
        {
            outputWriter.printf("in_connector_name: [%s]\n", inConnector.getName());
        }
        else if (successorNode instanceof LoopNodeEntry)
        {
            outputWriter.printf("is_loop_entry: [true]");
        }
    }

    private void visitOutConnector(PipelineNodeOutConnector outConnector)
    {
        outputWriter.printf("name: [%s]\n", outConnector.getName());

        visitList("successors", sortReferenceableElementList(outConnector.getNodeSuccessors()), this::visitNodeSuccessor);
        visitList("return_values", sortParameterList(outConnector.getReturnValues()), this::visitParameter);
        visitList("return_value_bindings", sortNamedElementList(outConnector.getReturnValueBindings()), this::visitParameterBinding);
    }

    private void visitParameter(Parameter parameter)
    {
        outputWriter.printf("name: [%s]\n", parameter.getName());
        outputWriter.printf("type: [%s]\n", parameter.getType());
        outputWriter.printf("is_optional: [%s]\n", parameter.isOptional());
    }

    private void visitParameterBinding(ParameterObjectPathBinding parameterBinding)
    {
        outputWriter.printf("name: [%s]\n", parameterBinding.getName());
        outputWriter.printf("constant: [%s]\n", parameterBinding.getConstant());
        outputWriter.printf("is_nullBinding: [%s]\n", parameterBinding.isNullBinding());
        outputWriter.printf("object_path: [%s]\n", parameterBinding.getObjectPath());
    }

    private <T> void visitList(String listName, List<T> list, Consumer<T> elementVisitor)
    {
        outputWriter.printf("%s [\n", listName);

        outputWriter.indentedBlock(() -> {
            list.forEach((element) -> {
                outputWriter.println("{");

                outputWriter.indentedBlock(() -> {
                    elementVisitor.accept(element);
                });

                outputWriter.println("}");
            });
        });

        outputWriter.println("]");
    }

    private void visitLoopNodeEntry(LoopNodeEntry entry)
    {
        outputWriter.println("entry: {");

        outputWriter.indentedBlock(() -> {
            outputWriter.printf("key: [%s]\n", entry.getKey());

            visitList("successors", sortReferenceableElementList(entry.getNodeSuccessors()), this::visitNodeSuccessor);
        });

        outputWriter.println("}");
    }

    private void visitCallNode(CallNode node)
    {
        outputWriter.println("{");

        outputWriter.indentedBlock(() -> {
            outputWriter.printf("start_node: [%s]\n", node.getStartNode().getReferencedName());
            outputWriter.printf("call_mode_permissions: [%s]\n", Optional.ofNullable(node.getCallModePermissions()).map(CallModes::value).orElse(""));

            visitList("parameter_bindings", sortNamedElementList(node.getParameterBindings()), this::visitParameterBinding);
            visitList("return_value_bindings", sortNamedElementList(node.getReturnValueBindings()), this::visitParameterBinding);
        });

        outputWriter.println("}");
    }

    private void visitDecisionNode(DecisionNode node)
    {
        outputWriter.printf("condition_key: [%s]\n", node.getConditionKey());
        outputWriter.printf("operator: [%s]\n", node.getOperator());
        outputWriter.printf("condition_value: [%s]\n", node.getConditionValue());
        outputWriter.printf("condition_item: [%s]\n", node.getConditionItem());
    }

    private void visitEndNode(EndNode node)
    {
        outputWriter.println("{");

        outputWriter.indentedBlock(() -> {
            outputWriter.printf("name: [%s]\n", node.getName());
            outputWriter.printf("is_strict: [%s]\n", node.isStrict());

            visitList("return_values", sortParameterList(node.getReturnValues()), this::visitParameter);
        });

        outputWriter.println("}");
    }

    private void visitExtensionNode(ExtensionPointNode node)
    {
        outputWriter.printf("name: [%s]\n", node.getName());
        outputWriter.printf("is_strict: [%s]\n", node.isStrict());

        visitList("parameters", sortParameterList(node.getParameters()), this::visitParameter);
        visitList("parameter_bindings", sortNamedElementList(node.getParameterBindings()), this::visitParameterBinding);
        visitList("return_value_bindings", sortNamedElementList(node.getReturnValueBindings()), this::visitParameterBinding);
    }

    private void visitInteractionNode(InteractionNode node)
    {
        outputWriter.printf("template: [%s]\n", node.getTemplate().getReferencedName());
        outputWriter.printf("transaction_required: [%s]\n", node.isTransactionRequired());
        outputWriter.printf("interaction_processor: [%s]\n", node.getInteractionProcessor());
        outputWriter.printf("is_buffered: [%s]\n", node.isBuffered());
        outputWriter.printf("is_dynamic: [%s]\n", node.isDynamic());
    }

    private void visitJumpNode(JumpNode node)
    {
        outputWriter.printf("start_node: [%s]\n", node.getStartNode().getReferencedName());
        outputWriter.printf("call_mode_permissions: [%s]\n", Optional.ofNullable(node.getCallModePermissions()).map(CallModes::value).orElse(""));

        visitList("parameter_bindings", sortNamedElementList(node.getParameterBindings()), this::visitParameterBinding);
    }

    private void visitLoopNode(LoopNode node)
    {
        outputWriter.printf("loop: [%s]\n", node.getLoop());

        visitLoopNodeEntry(node.getEntry());
    }

    private void visitPipeletNode(PipeletNode node)
    {
        outputWriter.printf("pipelet: [%s]\n", node.getPipelet().getHref());

        visitList("configuration_values", sortNamedElementList(node.getConfigurationValues()), this::visitConfigurationValue);
        visitList("parameter_bindings", sortNamedElementList(node.getParameterBindings()), this::visitParameterBinding);
        visitList("return_value_bindings", sortNamedElementList(node.getReturnValueBindings()), this::visitParameterBinding);
    }

    private void visitPipelineNode(PipelineNodeNode node)
    {
        outputWriter.println("{");

        outputWriter.indentedBlock(() -> {
            outputWriter.printf("type: [%s]\n", node.getClass().getSimpleName());
            outputWriter.printf("pipelet: [%s]\n", node.getPipelet().getHref());

            visitList("configuration_values", sortNamedElementList(node.getConfigurationValues()), this::visitConfigurationValue);
            visitList("in_connectors", sortConnectorList(node.getInConnectors()), this::visitInConnector);
            visitList("out_connectors", sortConnectorList(node.getOutConnectors()), this::visitOutConnector);
        });

        outputWriter.println("}");
    }

    private void visitStartNode(StartNode node)
    {
        outputWriter.printf("name: [%s]\n", node.getName());
        outputWriter.printf("is_strict: [%s]\n", node.isStrict());
        outputWriter.printf("call_mode: [%s]\n", node.getCallMode());
        outputWriter.printf("visibility: [%s]\n", node.getVisibility());
        outputWriter.printf("is_secure: [%s]\n", node.isSecure());
        outputWriter.printf("session_mode: [%s]\n", node.getSessionMode());

        visitList("parameters", sortParameterList(node.getParameters()), this::visitParameter);
    }

    private void dispatchVisit(Node node)
    {
        if (node instanceof CallNode callNode)
        {
            visitCallNode(callNode);
        }
        else if (node instanceof DecisionNode decisionNode)
        {
            visitDecisionNode(decisionNode);
        }
        else if (node instanceof EndNode endNode)
        {
            visitEndNode(endNode);
        }
        else if (node instanceof ExtensionPointNode n)
        {
            visitExtensionNode(n);
        }
        else if (node instanceof InteractionNode interactionNode)
        {
            visitInteractionNode(interactionNode);
        }
        else if (node instanceof JoinNode joinNode)
        {
            // nothing to do for join node; all work has been done by the visitNode method
        }
        else if (node instanceof JumpNode n)
        {
            visitJumpNode(n);
        }
        else if (node instanceof LoopNode n)
        {
            visitLoopNode(n);
        }
        else if (node instanceof PipeletNode n)
        {
            visitPipeletNode(n);
        }
        else if (node instanceof PipelineNodeNode pipelineNode)
        {
            visitPipelineNode(pipelineNode);
        }
        else if (node instanceof StartNode startNode)
        {
            visitStartNode(startNode);
        }
        else
        {
            throw new UnsupportedOperationException(String.format("Visit method for node type [%s] has not been implemented yet. Current pipeline file is [%s].", node.getClass().getSimpleName(), pipeline.getFilePath().toAbsolutePath()));
        }
    }

    private static <T extends NamedElement> List<T> sortNamedElementList(List<T> elements)
    {
        return elements.stream().sorted(comparing(NamedElement::getName)).toList();
    }

    private static List<Parameter> sortParameterList(List<Parameter> parameters)
    {
        return parameters.stream().sorted(comparing(Parameter::getName)).toList();
    }

    private static <T extends PipelineNodeConnector> List<T> sortConnectorList(List<T> connectors)
    {
        return connectors.stream().sorted(comparing(PipelineNodeConnector::getName)).toList();
    }

    private static <T extends ReferenceableElement> List<T> sortReferenceableElementList(List<T> elements)
    {
        return elements.stream().sorted(comparing(ReferenceableElement::getName)).toList();
    }

    public NodeVisitor(Pipeline pipeline, PrintWriter outputWriter)
    {
        this.pipeline = pipeline;
        this.outputWriter = new IndentingPrintWriter(outputWriter);
    }

    public void visit(Node node)
    {
        outputWriter.println("{");

        outputWriter.indentedBlock(() -> {
            outputWriter.printf("type: [%s]\n", node.getClass().getSimpleName());

            dispatchVisit(node);

            if (node instanceof SuccessorNode successorNode)
            {
                visitList("successors", sortReferenceableElementList(successorNode.getNodeSuccessors()), this::visitNodeSuccessor);
            }
            else if (node instanceof PipeletNode n)
            {
                visitList("successors", sortReferenceableElementList(n.getNodeSuccessors()), this::visitNodeSuccessor);
            }
        });

        outputWriter.println("}");
    }
}
