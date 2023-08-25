package dev.rickysixx.ecpf.pipeline;

import de.intershop.core._2010.Parameter;
import de.intershop.core._2010.ReferenceableElement;
import de.intershop.pipeline._2010.*;
import dev.rickysixx.ecpf.io.IndentingPrintWriter;

import java.io.PrintWriter;
import java.util.List;

import static java.util.Comparator.comparing;

public class NodeVisitor
{
    private final Pipeline pipeline;
    private final IndentingPrintWriter outputWriter;

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
        if (node instanceof StartNode startNode)
        {
            visitStartNode(startNode);
        }
        else
        {
            throw new UnsupportedOperationException(String.format("Visit method for node type [%s] has not been implemented yet.", node.getClass().getSimpleName()));
        }
    }

    private static List<Parameter> sortParameterList(List<Parameter> parameters)
    {
        return parameters.stream().sorted(comparing(Parameter::getName)).toList();
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
