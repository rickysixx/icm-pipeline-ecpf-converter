package dev.rickysixx.ecpf.pipeline;

import de.intershop.core._2010.Parameter;
import de.intershop.pipeline._2010.Node;
import de.intershop.pipeline._2010.StartNode;
import dev.rickysixx.ecpf.io.IndentingPrintWriter;

import java.io.*;
import java.util.List;

public class NodeVisitor
{
    private final Pipeline pipeline;
    private final IndentingPrintWriter outputWriter;

    private void dispatchVisit(Node node)
    {
        throw new UnsupportedOperationException(String.format("Visit method for node type [%s] has not been implemented yet.", node.getClass().getSimpleName()));
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
        });

        outputWriter.println("}");
    }
}
