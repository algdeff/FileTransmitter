package XmlMonitor.Utils;

import XmlMonitor.Logic.db.DatabaseManager;
import org.jdom2.*;
import org.jdom2.filter.ContentFilter;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class XmlUtil {

    public XmlUtil() {
    }

    public void createXMLDocument(String filename) {

        double numEntries = Math.random() * 10 + 1;

        Document xmlDoc = new Document();
        Element root = new Element("Entries");
        xmlDoc.setRootElement(root);

        for (int entryNumber = 1; entryNumber < numEntries; entryNumber++) {
            Element entry = new Element("Entry");
            entry.setAttribute("id", String.valueOf(entryNumber));

            entry.addContent(new Comment("max string lenght 1024 symbols, " + String.valueOf(entryNumber)));
            Element content = new Element("content");
            content.addContent("DATA: " + generateRandomString());
            entry.addContent(content);

            Date date = new Date();
            //System.out.println(date.getTime() + " / " + date.toString() + " / " + date.getHours());

            entry.addContent(new Comment("date of record creation, " + String.valueOf(entryNumber)));
            Element creationDate = new Element("creationDate");

            LocalDateTime datetime = LocalDateTime.of(LocalDate.now(), LocalTime.of(13, new Random().nextInt(59), 22));
            Timestamp timestamp = Timestamp.valueOf(datetime);  //"2007-12-23 09:01:06.000000003");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss");
            creationDate.addContent(datetime.format(formatter));
            entry.addContent(creationDate);
            root.addContent(entry);
        }

        try {
            Format fmt = Format.getPrettyFormat();
            XMLOutputter serializer = new XMLOutputter(fmt);
            serializer.output(xmlDoc, System.out);
            serializer.output(xmlDoc, new FileOutputStream(new File(filename)));
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private String generateRandomString() {
        return String.valueOf(Math.random() * 999999999);
    }

}