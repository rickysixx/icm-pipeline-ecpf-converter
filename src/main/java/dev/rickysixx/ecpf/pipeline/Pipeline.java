package dev.rickysixx.ecpf.pipeline;

import de.intershop.pipeline._2010.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.DepthFirstIterator;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Pipeline
{
    private final Graph<Node, DefaultEdge> graph;

    private final File file;

    private final de.intershop.pipeline._2010.Pipeline xmlNode;

    private void addEdgeToParentNode(Node source, Node childNode)
    {
        Node parentNode = (Node) childNode.getParent();

        graph.addEdge(source, parentNode);
    }

    private void populateGraph()
    {
        xmlNode.getNodes().forEach(graph::addVertex);

        graph.vertexSet()
            .forEach((node) -> {
                node.getNodeSuccessorStream()
                    .map(NodeSuccessor::getNext)
                    .map(this::getNodeByID)
                    .forEach((successor) -> {
                        if (successor instanceof LoopNodeEntry || successor instanceof PipelineNodeInConnector)
                        {
                            addEdgeToParentNode(node, successor);
                        }
                        else
                        {
                            graph.addEdge(node, successor);
                        }
                    });
            });
    }

    private Node getLoopNodeEntryByID(String nodeID, int indexOf_Entry)
    {
        checkArgument(indexOf_Entry != -1, "Node ID [%s] does not end with [_Entry].", nodeID);

        LoopNode loopNode = (LoopNode) getNodeByID(nodeID.substring(0, indexOf_Entry));

        return loopNode.getEntry();
    }

    private Node getPipelineNodeInConnectorByID(String nodeID, int lastUnderscoreIndex)
    {
        PipelineNodeNode pipelineNode = (PipelineNodeNode) getNodeByID(nodeID.substring(0, lastUnderscoreIndex));
        String connectorName = nodeID.substring(lastUnderscoreIndex + 1);

        return pipelineNode.getInConnectors()
                .stream()
                .filter((inConnector) -> inConnector.getName().equals(connectorName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("No pipeline node inConnector found for name [%s] (node ID: [%s]).", connectorName, nodeID)));
    }

    @SuppressWarnings("unchecked")
    private static de.intershop.pipeline._2010.Pipeline parsePipelineFile(File file) throws JAXBException
    {
        JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<de.intershop.pipeline._2010.Pipeline> element = (JAXBElement<de.intershop.pipeline._2010.Pipeline>) unmarshaller.unmarshal(file);

        return element.getValue();
    }

    public Pipeline(File file) throws JAXBException
    {
        this.file = checkNotNull(file, "file cannot be null.");
        this.xmlNode = parsePipelineFile(this.file);
        this.graph = new DirectedMultigraph<>(DefaultEdge.class);

        populateGraph();
    }

    public Stream<Node> getAllStartNodesStream()
    {
        return graph.vertexSet()
            .stream()
            .filter(Node::isStartNode);
    }

    public Optional<Node> tryGetStartNodeByName(String name)
    {
        return getAllStartNodesStream()
            .filter((node) -> node.getNameAsStartNode().equals(name))
            .findFirst();
    }

    public Node getStartNodeByName(String name)
    {
        return tryGetStartNodeByName(name)
            .orElseThrow(() -> new NoSuchElementException(String.format("No start node found for name [%s] in pipeline [%s].", name, xmlNode.getName())));
    }

    public Node getNodeByID(String nodeID)
    {
        int lastUnderscoreIndex = nodeID.lastIndexOf('_');

        if (lastUnderscoreIndex >= 0 && nodeID.substring(lastUnderscoreIndex).equals("_Entry"))
        {
            return getLoopNodeEntryByID(nodeID, lastUnderscoreIndex);
        }

        Optional<Node> n = graph.vertexSet()
            .stream()
            .filter((node) -> !(node instanceof LoopNodeEntry))
            .filter((node) -> !(node instanceof PipelineNodeInConnector))
            .filter((node) -> node.getNodeID().equals(nodeID))
            .findFirst();

        return n.orElseGet(() -> getPipelineNodeInConnectorByID(nodeID, lastUnderscoreIndex));
    }

    public Iterator<Node> createIteratorFromStartNode(String startNodeName)
    {
        Node startNode = getStartNodeByName(startNodeName);

        return createIteratorFromStartNode(startNode);
    }

    public Iterator<Node> createIteratorFromStartNode(Node startNode)
    {
        return new DepthFirstIterator<>(graph, startNode);
    }

    public Set<Node> getAllStartNodes()
    {
        return getAllStartNodesStream().collect(Collectors.toUnmodifiableSet());
    }

    public String getName()
    {
        return xmlNode.getName();
    }

    public Path getFilePath()
    {
        return file.toPath();
    }
}
