package dev.rickysixx.ecpf.xml;

import com.intershop.pipeline.ObjectFactory;
import com.intershop.pipeline.PipelineNodeDefinition;
import com.intershop.pipeline.PipelineNodeTypes;
import de.intershop.pipeline._2010.*;
import dev.rickysixx.ecpf.PipelineEcpfConverter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlTransient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@XmlTransient
public abstract class Node
{
    @XmlTransient
    private Node parent;

    @XmlTransient
    private PipelineNodeDefinition pipelineNodeDefinition;

    @SuppressWarnings("unchecked")
    public PipelineNodeDefinition getPipelineNodeDefinition()
    {
        if (this instanceof PipelineNodeNode pipelineNode)
        {
            String[] splitHref = pipelineNode.getPipelet().getHref().split("/");
            String cartridgeName = splitHref[1];
            String pipelineNodeDefinitionQualifiedName = splitHref[3];
            int lastIndexOfDot = pipelineNodeDefinitionQualifiedName.lastIndexOf('.');
            String pipelineNodeDefinitionPath = pipelineNodeDefinitionQualifiedName.substring(0, lastIndexOfDot)
               .replaceAll("\\.", "/")
               .concat(pipelineNodeDefinitionQualifiedName.substring(lastIndexOfDot));
            File systemCartridgesDir = PipelineEcpfConverter.getInstance().getSystemCartridgesDir();

            try (ZipFile cartridgeJarFile = new ZipFile(Paths.get(systemCartridgesDir.getPath(), cartridgeName, "release", "lib", cartridgeName + ".jar").toFile()))
            {
                ZipEntry pipelineNodeDefinitionEntry = cartridgeJarFile.getEntry(pipelineNodeDefinitionPath);

                try (InputStream pipelineNodeDefinition = cartridgeJarFile.getInputStream(pipelineNodeDefinitionEntry))
                {
                    JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    JAXBElement<PipelineNodeDefinition> xmlElement = (JAXBElement<PipelineNodeDefinition>) unmarshaller.unmarshal(pipelineNodeDefinition);

                    return xmlElement.getValue();
                }
                catch (JAXBException e)
                {
                    throw new RuntimeException(e);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private void afterUnmarshal(Unmarshaller unmarshaller, Object parent)
    {
        if (parent instanceof Node node)
        {
            this.parent = node;
            this.pipelineNodeDefinition = getPipelineNodeDefinition();
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
