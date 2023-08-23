package dev.rickysixx.ecpf.xml;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlTransient;

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
}
