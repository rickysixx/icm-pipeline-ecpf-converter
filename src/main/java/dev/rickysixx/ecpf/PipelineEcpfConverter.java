package dev.rickysixx.ecpf;

import de.intershop.pipeline._2010.Node;
import de.intershop.pipeline._2010.StartNode;
import dev.rickysixx.ecpf.io.IndentingPrintWriter;
import dev.rickysixx.ecpf.pipeline.NodeVisitor;
import dev.rickysixx.ecpf.pipeline.Pipeline;
import org.checkerframework.checker.units.qual.N;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "icm-pipeline-ecpf-converter"
)
public class PipelineEcpfConverter implements Callable<Integer>
{
    @CommandLine.Parameters
    private File pipelineFile;

    @Override
    public Integer call() throws Exception
    {
        Pipeline pipeline = new Pipeline(pipelineFile);
        PrintWriter writer = new PrintWriter(System.out);
        NodeVisitor visitor = new NodeVisitor(pipeline, writer);
        Set<StartNode> startNodes = pipeline.getAllStartNodes();

        startNodes.forEach((startNode) -> {
            System.out.printf("ECPF of [%s]:\n\n\n", startNode.getName());

            Iterator<Node> nodeIterator = pipeline.createIteratorFromStartNode(startNode);

            while (nodeIterator.hasNext())
            {
                Node node = nodeIterator.next();

                visitor.visit(node);
                writer.flush();
            }

            System.out.println("-".repeat(20));
        });

        return 0;
    }

    public static void main(String[] args)
    {
        int exitCode = new CommandLine(new PipelineEcpfConverter()).execute(args);

        System.exit(exitCode);
    }
}
