package dev.rickysixx.ecpf.pipeline;

import de.intershop.core._2010.Parameter;
import de.intershop.pipeline._2010.StartNode;
import dev.rickysixx.ecpf.io.IndentingPrintWriter;

import java.io.*;
import java.util.List;

public class NodeVisitor
{
    private final Pipeline pipeline;
    private final IndentingPrintWriter outputWriter;

    public NodeVisitor(Pipeline pipeline, PrintWriter outputWriter)
    {
        this.pipeline = pipeline;
        this.outputWriter = new IndentingPrintWriter(outputWriter);
    }
}
