package io.xlate.edi.test;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

public class StaEDITestUtil {

    /**
     * Write a full segment to the output writer. Elements in the elements parameter
     * are written as simple elements or composite elements when they are an array.
     *
     * @param writer
     * @param segmentTag
     * @param elements
     * @throws EDIStreamException
     */
    public static void write(EDIStreamWriter writer, String segmentTag, Object... elements) throws EDIStreamException {
        writer.writeStartSegment(segmentTag);

        for (Object e : elements) {
            if (e instanceof String) {
                writer.writeElement((String) e);
            } else if (e instanceof String[]) {
                writer.writeStartElement();
                for (String c : (String[]) e) {
                    writer.writeComponent(c);
                }
                writer.endElement();
            }
        }

        writer.writeEndSegment();
    }

}
