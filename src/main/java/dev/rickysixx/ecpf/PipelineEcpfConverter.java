package dev.rickysixx.ecpf;

import de.intershop.pipeline._2010.Node;
import dev.rickysixx.ecpf.pipeline.NodeVisitor;
import dev.rickysixx.ecpf.pipeline.Pipeline;
import jakarta.xml.bind.JAXBException;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@CommandLine.Command(
    name = "icm-pipeline-ecpf-converter",
    usageHelpAutoWidth = true
)
public class PipelineEcpfConverter implements Callable<Integer>
{
    @CommandLine.Option(
        names = {"-n", "--start-nodes"},
        split = ",",
        description = "List of start nodes to generate the ECPF file for. " +
            "When used without the --only-common option, an ECPF file will be generated only for the " +
            "start nodes included in this list. " +
            "When used with the --only-common option, an ECPF file will be generated only for the " +
            "common start nodes which are also included in this list. " +
            "When both this and --only-common options are omitted, an ECPF file will be generated for " +
            "each start node in each pipeline file given.",
        paramLabel = "startNodeName"
    )
    private List<String> startNodeNames;

    @CommandLine.Option(
        names = {"-o", "--output-dir"},
        description = "Output directory for ECPF files. Default is the current directory. Must exist before invoking the program."
    )
    private File outputDirectory;

    @CommandLine.Option(
        names = {"--only-common"},
        description = "If specified without the --start-nodes option, ECPF files will be generated only " +
            "for start nodes in common between all the given pipeline files. " +
            "If specified together with the --start-nodes option, ECPF files will be generated only for " +
            "start nodes in common between all the given pipeline files which are also included in the " +
            "list given to the --start-nodes option."
    )
    private boolean onlyCommonStartNodes;

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Make the program more verbose."
    )
    private boolean verbose;

    @CommandLine.Option(
        names = {"-h", "--help"},
            usageHelp = true,
            description = "Display program's usage."
    )
    private boolean showUsage;

    @CommandLine.Option(
        names = {"--system-cartridges-dir"},
        description = "The path to the system cartridges directory. Required to properly handle the type of pipeline nodes.",
        required = true
    )
    private File systemCartridgesDir;

    @CommandLine.Parameters(
        arity = "1..n",
        description = "An Intershop pipeline file to process.",
        paramLabel = "pipelineFile"
    )
    private List<File> pipelineFiles;

    private List<Pipeline> pipelines;

    private Set<String> commonStartNodeNames;

    private static CommandLine cliInstance;

    private String getOutputFileName(Pipeline pipeline, int pipelinePositionInArgsList, Node startNode)
    {
        return pipeline.getName() + "_" + pipelinePositionInArgsList + "_" + startNode.getNameAsStartNode() + ".ecpf";
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

    private void createPipelineObjects() throws JAXBException
    {
        this.pipelines = new ArrayList<>(pipelineFiles.size());

        for (File pipelineFile : pipelineFiles)
        {
            this.pipelines.add(new Pipeline(pipelineFile));
        }
    }

    private Set<String> getCommonStartNodeNames()
    {
        printVerbose("Calculating common start nodes...");

        Iterator<Pipeline> pipelinesIterator = this.pipelines.iterator();
        Pipeline firstElement = pipelinesIterator.next();
        Set<String> commonStartNodeNames = firstElement.getAllStartNodesStream()
            .map(Node::getNameAsStartNode)
            .collect(Collectors.toCollection(HashSet::new));
        Supplier<String> commonStartNodeNamesJoiner = () -> String.join(", ", commonStartNodeNames);

        printVerbose("Initial set (start nodes of pipeline [%s]): [%s].",
            (Supplier<Path>) () -> firstElement.getFilePath().toAbsolutePath(),
            commonStartNodeNamesJoiner
        );

        while (pipelinesIterator.hasNext())
        {
            Pipeline pipeline = pipelinesIterator.next();
            Set<String> pipelineStartNodeNames = pipeline.getAllStartNodesStream()
                .map(Node::getNameAsStartNode)
                .collect(Collectors.toSet());

            commonStartNodeNames.retainAll(pipelineStartNodeNames);

            printVerbose("Visited pipeline [%s]. Common start node names: [%s].",
                (Supplier<Path>) () -> pipeline.getFilePath().toAbsolutePath(),
                commonStartNodeNamesJoiner
            );
        }

        printVerbose("Pipeline scan finished. Common start node names: [%s].", commonStartNodeNamesJoiner);

        return commonStartNodeNames;
    }

    private void processStartNode(Pipeline pipeline, int pipelinePositionInArgsList, Node startNode) throws FileNotFoundException
    {
        File outputFile = new File(outputDirectory, getOutputFileName(pipeline, pipelinePositionInArgsList, startNode));

        try (PrintWriter outputWriter = new PrintWriter(outputFile))
        {
            NodeVisitor visitor = new NodeVisitor(pipeline, outputWriter);
            Iterator<Node> nodesIterator = pipeline.createIteratorFromStartNode(startNode);

            while (nodesIterator.hasNext())
            {
                Node node = nodesIterator.next();

                visitor.visit(node);
                outputWriter.flush();
            }
        }
    }

    private void printVerbose(String message, Object... args)
    {
        if (this.verbose)
        {
            Object[] argList = Optional.ofNullable(args)
                .stream()
                .flatMap(Arrays::stream)
                .map((obj) -> {
                    if (obj instanceof Supplier<?> supplier)
                    {
                        return String.valueOf(supplier.get());
                    }

                    return String.valueOf(obj);
                }).toList().toArray(String[]::new);

            System.out.printf(message + "\n", argList);
        }
    }

    private static void printWarning(String message, Object... args)
    {
        System.err.printf("WARNING: " + message + "\n", args);
    }

    private Set<Node> getStartNodesToProcess(Pipeline pipeline)
    {
        Set<String> processingStartNodeNames = pipeline.getAllStartNodesStream()
            .map(Node::getNameAsStartNode)
            .collect(Collectors.toSet());

        if (onlyCommonStartNodes)
        {
            processingStartNodeNames.retainAll(this.commonStartNodeNames);
        }

        if (this.startNodeNames != null && !this.startNodeNames.isEmpty())
        {
            processingStartNodeNames.retainAll(this.startNodeNames);
        }

        Set<Node> startNodesToProcess = new HashSet<>(processingStartNodeNames.size());

        processingStartNodeNames.forEach((startNodeName) -> {
            Optional<Node> startNode = pipeline.tryGetStartNodeByName(startNodeName);

            if (startNode.isPresent())
            {
                startNodesToProcess.add(startNode.get());
            }
            else
            {
                printWarning("No start node with name [%s] found in pipeline [%s]. This start node will be ignored.", startNodeName, pipeline.getFilePath().toAbsolutePath());
            }
        });

        return Collections.unmodifiableSet(startNodesToProcess);
    }

    private void processPipelineList() throws FileNotFoundException
    {
        for (int i = 0; i < pipelines.size(); i++)
        {
            int currentPosition = i + 1;
            Pipeline pipeline = pipelines.get(i);
            Set<Node> startNodes = getStartNodesToProcess(pipeline);

            if (startNodes.isEmpty())
            {
                printWarning("No start nodes to process found for pipeline [%s].", pipeline.getFilePath().toAbsolutePath());
                continue;
            }

            for (Node startNode : startNodes)
            {
                processStartNode(pipeline, currentPosition, startNode);
            }
        }
    }

    @Override
    public Integer call() throws Exception
    {
        if (pipelineFiles.isEmpty())
        {
            printWarning("No pipeline file given. Exit immediately.");

            return 0;
        }

        createPipelineObjects();

        if (onlyCommonStartNodes)
        {
            this.commonStartNodeNames = getCommonStartNodeNames();

            if (this.commonStartNodeNames.isEmpty())
            {
                printWarning("No common start nodes found for the given pipeline list. Exit immediately.");

                return 0;
            }
        }

        ensureOutputDirectoryIsSet();
        processPipelineList();

        return 0;
    }

    public static void main(String[] args)
    {
        CommandLine cli = new CommandLine(new PipelineEcpfConverter());
        PipelineEcpfConverter.cliInstance = cli;
        int exitCode = cli.execute(args);

        System.exit(exitCode);
    }

    public static PipelineEcpfConverter getInstance()
    {
        return PipelineEcpfConverter.cliInstance.getCommand();
    }

    public File getSystemCartridgesDir()
    {
        return systemCartridgesDir;
    }
}
