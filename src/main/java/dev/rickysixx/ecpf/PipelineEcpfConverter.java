package dev.rickysixx.ecpf;

import de.intershop.pipeline._2010.Node;
import de.intershop.pipeline._2010.StartNode;
import dev.rickysixx.ecpf.pipeline.NodeVisitor;
import dev.rickysixx.ecpf.pipeline.Pipeline;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkArgument;

@CommandLine.Command(
    name = "icm-pipeline-ecpf-converter"
)
public class PipelineEcpfConverter implements Callable<Integer>
{
    @CommandLine.Parameters
    private File pipelineFile;

    @CommandLine.Option(names = {"-n", "--start-nodes"}, split = ",", description = "List of start nodes to generate the ECPF file for.")
    private List<String> startNodeNames;

    @CommandLine.Option(names = {"-o", "--output-dir"}, description = "Output directory for ECPF files. Default is the current directory. Must exist before invoking the program.")
    private File outputDirectory;

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

    private String getOutputFileName(Pipeline pipeline, int pipelinePositionInArgsList, StartNode startNode)
    {
        return pipeline.getName() + "_" + pipelinePositionInArgsList + "_" + startNode.getName() + ".ecpf";
    }

    private void ensureOutputDirectoryIsSet()
    {
        if (this.outputDirectory == null)
        {
            this.outputDirectory = FileSystems.getDefault().getPath("").toFile().getAbsoluteFile();
        }
        else
        {
            checkArgument(outputDirectory.exists() && outputDirectory.isDirectory(), "The given output directory [%s] does not exist or it's not a directory.", outputDirectory.getAbsolutePath());
        }
    }

    @Override
    public Integer call() throws Exception
    {
        ensureOutputDirectoryIsSet();

        Pipeline pipeline = new Pipeline(pipelineFile);
        Set<StartNode> startNodes = getStartNodesToProcess(pipeline);

        if (!startNodes.isEmpty())
        {
            for (StartNode startNode : startNodes)
            {
                File outputFile = new File(outputDirectory, getOutputFileName(pipeline, 1, startNode));

                try (PrintWriter outputWriter = new PrintWriter(outputFile))
                {
                    NodeVisitor visitor = new NodeVisitor(pipeline, outputWriter);
                    Iterator<Node> nodeIterator = pipeline.createIteratorFromStartNode(startNode);

                    while (nodeIterator.hasNext())
                    {
                        Node node = nodeIterator.next();

                        visitor.visit(node);
                        outputWriter.flush();
                    }
                }

            }
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
