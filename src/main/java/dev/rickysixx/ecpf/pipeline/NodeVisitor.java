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

import static java.util.Comparator.comparing;

public class NodeVisitor
{
    private final Pipeline pipeline;
    private final IndentingPrintWriter outputWriter;

    private void visitConfigurationValueList(List<ConfigurationValue> configurationValues)
    {
        outputWriter.println("configuration_values: [");

        outputWriter.indentedBlock(() -> {
            configurationValues.forEach((configurationValue) -> {
                outputWriter.println("{");

                outputWriter.indentedBlock(() -> {
                    outputWriter.printf("name: [%s]\n", configurationValue.getName());
                    outputWriter.printf("value: [%s]\n", configurationValue.getValue());
                });

                outputWriter.println("}");
            });
        });

        outputWriter.println("]");
    }

    private void visitInConnectorList(List<PipelineNodeInConnector> inConnectors)
    {
        outputWriter.println("in_connectors: [");

        outputWriter.indentedBlock(() -> {
            inConnectors.forEach((inConnector) -> {
                outputWriter.println("{");

                outputWriter.indentedBlock(() -> {
                    outputWriter.printf("name: [%s]\n", inConnector.getName());

                    visitParameterBindingList("parameter_bindings", inConnector.getParameterBindings());
                });

                outputWriter.println("}");
            });
        });

        outputWriter.println("]");
    }

    private void visitOutConnectorList(List<PipelineNodeOutConnector> outConnectors)
    {
        outputWriter.println("out_connectors: [");

        outputWriter.indentedBlock(() -> {
            outConnectors.forEach((outConnector) -> {
                outputWriter.println("{");

                outputWriter.indentedBlock(() -> {
                    outputWriter.printf("name: [%s]\n", outConnector.getName());

                    visitSuccessorList(sortReferenceableElementList(outConnector.getNodeSuccessors()));
                    visitParameterList("return_values", sortParameterList(outConnector.getReturnValues()));
                    visitParameterBindingList("return_value_bindings", sortNamedElementList(outConnector.getReturnValueBindings()));
                });
            });
        });

        outputWriter.println("]");
    }

    private void visitParameterList(String listName, List<Parameter> parameters)
    {
        outputWriter.printf("%s: [", listName);

        outputWriter.indentedBlock(() -> {
            parameters.forEach((parameter) -> {
                outputWriter.println("{");

                outputWriter.indentedBlock(() -> {
                    outputWriter.printf("name: [%s]\n", parameter.getName());
                    outputWriter.printf("type: [%s]\n", parameter.getType());
                    outputWriter.printf("is_optional: [%s]\n", parameter.isOptional());
                });

                outputWriter.println("}");
            });
        });

        outputWriter.println("]");
    }

    private void visitParameterBindingList(String listName, List<ParameterObjectPathBinding> parameterBindings)
    {
        outputWriter.printf("%s: [\n", listName);

        outputWriter.indentedBlock(() -> {
            parameterBindings.forEach((parameterBinding) -> {
                outputWriter.println("{");

                outputWriter.indentedBlock(() -> {
                    outputWriter.printf("name: [%s]\n", parameterBinding.getName());
                    outputWriter.printf("constant: [%s]\n", parameterBinding.getConstant());
                    outputWriter.printf("is_nullBinding: [%s]\n", parameterBinding.isNullBinding());
                    outputWriter.printf("object_path: [%s]\n", parameterBinding.getObjectPath());
                });

                outputWriter.println("}");
            });
        });

        outputWriter.println("]");
    }

    private void visitSuccessorList(List<NodeSuccessor> successors)
    {
        outputWriter.println("successors: [");

        outputWriter.indentedBlock(() -> {
            successors.forEach((successor) -> {
                outputWriter.println("{");

                outputWriter.indentedBlock(() -> {
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
                });

                outputWriter.println("}");
            });
        });

        outputWriter.println("]");
    }

    private void visitCallNode(CallNode node)
    {
        outputWriter.println("{");

        outputWriter.indentedBlock(() -> {
            outputWriter.printf("start_node: [%s]\n", node.getStartNode().getReferencedName());
            outputWriter.printf("call_mode_permissions: [%s]\n", Optional.ofNullable(node.getCallModePermissions()).map(CallModes::value).orElse(""));

            visitParameterBindingList("parameter_bindings", sortNamedElementList(node.getParameterBindings()));
            visitParameterBindingList("return_value_bindings", sortNamedElementList(node.getReturnValueBindings()));
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

    private void visitPipelineNode(PipelineNodeNode node)
    {
        outputWriter.println("{");

        outputWriter.indentedBlock(() -> {
            outputWriter.printf("type: [%s]\n", node.getClass().getSimpleName());
            outputWriter.printf("pipelet: [%s]\n", node.getPipelet().getHref());

            visitConfigurationValueList(sortNamedElementList(node.getConfigurationValues()));
            visitInConnectorList(sortConnectorList(node.getInConnectors()));
            visitOutConnectorList(sortConnectorList(node.getOutConnectors()));
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

        visitParameterList("parameters", sortParameterList(node.getParameters()));
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
        else if (node instanceof StartNode startNode)
        {
            visitStartNode(startNode);
        }
        else
        {
            throw new UnsupportedOperationException(String.format("Visit method for node type [%s] has not been implemented yet.", node.getClass().getSimpleName()));
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
                visitSuccessorList(sortReferenceableElementList(successorNode.getNodeSuccessors()));
            }
        });

        outputWriter.println("}");
    }
}
