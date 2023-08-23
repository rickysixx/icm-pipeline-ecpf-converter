package dev.rickysixx.ecpf;

import de.intershop.pipeline._2010.Node;
import de.intershop.pipeline._2010.StartNode;
import dev.rickysixx.ecpf.pipeline.NodeVisitor;
import dev.rickysixx.ecpf.pipeline.Pipeline;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "icm-pipeline-ecpf-converter"
)
public class PipelineEcpfConverter implements Callable<Integer>
{
    @CommandLine.Parameters
    private File pipelineFile;

    @CommandLine.Option(names = {"-n", "--start-nodes"}, split = ",", description = "List of start nodes to generate the ECPF file for.")
    private List<String> startNodeNames;

    private Set<StartNode> getStartNodesToProcess(Pipeline pipeline)
    {
        if (startNodeNames != null && !startNodeNames.isEmpty())
        {
            Set<StartNode> startNodes = new HashSet<>(startNodeNames.size());

            startNodeNames.forEach((startNodeName) -> {
                Optional<StartNode> startNode = pipeline.tryGetStartNodeByName(startNodeName);

                if (startNode.isPresent())
                {
                    startNodes.add(startNode.get());
                }
                else
                {
                    System.err.printf("WARNING: No start node with name [%s] found in pipeline file [%s]. This start node will be ignored.", startNodeName, pipelineFile.getPath());
                }
            });

            return Collections.unmodifiableSet(startNodes);
        }

        return pipeline.getAllStartNodes();
    }

    @Override
    public Integer call() throws Exception
    {
        Pipeline pipeline = new Pipeline(pipelineFile);
        Set<StartNode> startNodes = getStartNodesToProcess(pipeline);

        if (!startNodes.isEmpty())
        {
            PrintWriter writer = new PrintWriter(System.out);
            NodeVisitor visitor = new NodeVisitor(pipeline, writer);

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
        }
        else
        {
            System.err.println("WARNING: No start node to process found.");
        }

        return 0;
    }

    public static void main(String[] args)
    {
        int exitCode = new CommandLine(new PipelineEcpfConverter()).execute(args);

        System.exit(exitCode);
    }
}
