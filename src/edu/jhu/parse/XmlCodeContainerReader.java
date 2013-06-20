package edu.jhu.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlCodeContainerReader {

    private Map<String,String> codeMap = new HashMap<String,String>();
    
    public XmlCodeContainerReader() {
    }

    /**
     * Gets a code snippet with the specified id
     */
    public String getCodeSnippet(Object id) {
        return codeMap.get(id.toString());
    }
    
    public void loadZimplCodeFromResource(String resourceName) {
        InputStream inputStream = this.getClass().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new RuntimeException("Unable to find resource: " + resourceName);
        }
        loadZimplCodeFromInputStream(inputStream);
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
            
    public void loadZimplCodeFromInputStream(InputStream inputStream) {
            DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = builderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
            Document document;
            try {
                    document = builder.parse(inputStream);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
                    
            loadZimplCodeFromXml(document);
    }


    public void loadZimplCodeFromXml(Document document) {
        NodeList codeNodes = document.getElementsByTagName("code");
        for (int i=0; i<codeNodes.getLength(); i++) {
            Node codeNode = codeNodes.item(i);
            Node idNode = codeNode.getAttributes().getNamedItem("id");
            String id = idNode.getNodeValue();
            
            NodeList codeChildren = codeNode.getChildNodes();
            for (int j=0; j<codeChildren.getLength(); j++) {
                Node codeChild = codeChildren.item(j);
                if (codeChild.getNodeType() == Node.CDATA_SECTION_NODE) {
                    String zimplCode = codeChild.getNodeValue();
                    
                    // Put the <id, zimplCode> pair into the codeMap
                    codeMap.put(id, zimplCode);
                }
            }
        }
    }
}
