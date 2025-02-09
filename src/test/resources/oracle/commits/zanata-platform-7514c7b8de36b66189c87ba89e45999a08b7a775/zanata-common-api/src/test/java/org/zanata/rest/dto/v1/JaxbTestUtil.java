package org.zanata.rest.dto.v1;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class JaxbTestUtil {

    @SuppressWarnings("unchecked")
    public static <T> T roundTripXml(T obj, Class<?>... classes)
            throws JAXBException {

        int index = -1;
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] == obj.getClass()) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            classes = Arrays.copyOf(classes, classes.length + 1);
            classes[classes.length - 1] = obj.getClass();
        }
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(classes);
        context.createMarshaller().marshal(obj, writer);
        writer.flush();
        // System.out.println(writer.toString());
        obj =
                (T) context.createUnmarshaller().unmarshal(
                        new StringReader(writer.toString()));
        return obj;
    }

}
