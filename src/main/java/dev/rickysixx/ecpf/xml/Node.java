package dev.rickysixx.ecpf.xml;

import de.intershop.pipeline._2010.*;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlTransient;

import java.util.List;
import java.util.stream.Stream;

@XmlTransient
public abstract class Node
{
    @XmlTransient
    private Node parent;

    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent)
    {
        if (parent instanceof Node node)
        {
            this.parent = node;
        }
    }

    public Node getParent()
    {
        return parent;
    }

    /**
     * This method is a wrapper for the auto-generated {@link SuccessorNode#getNodeSuccessors()} which returns the <i>real</i> successors of a pipeline node
     * (e.g. the nodes you would like to find as successors when visiting a pipeline like a graph).
     */
    public Stream<NodeSuccessor> getNodeSuccessorStream()
    {
        if (this instanceof LoopNode loopNode)
        {
            return Stream.concat(
                loopNode.getEntry().getNodeSuccessors().stream(),
                loopNode.getNodeSuccessors().stream()
            );
        }
        else if (this instanceof PipeletNode pipeletNode)
        {
            // PipeletNode is not a subclass of NodeSuccessor, so its type must be handled explicitly
            return pipeletNode.getNodeSuccessors().stream();
        }
        else if (this instanceof PipelineNodeNode pipelineNode)
        {
            // combine in a single Stream all successors of all out connectors
            return pipelineNode.getOutConnectors()
               .stream()
               .map(PipelineNodeOutConnector::getNodeSuccessors)
               .flatMap(List::stream);
        }
        else if (this instanceof SuccessorNode successorNode)
        {
            return successorNode.getNodeSuccessors().stream();
        }

        // other types of Node do not have successors
        return Stream.empty();
    }
}
