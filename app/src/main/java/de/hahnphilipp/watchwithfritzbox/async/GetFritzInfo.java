package de.hahnphilipp.watchwithfritzbox.async;

import android.os.AsyncTask;
import android.util.Log;

import com.w3ma.m3u8parser.data.Playlist;
import com.w3ma.m3u8parser.exception.PlaylistParseException;
import com.w3ma.m3u8parser.parser.M3U8Parser;
import com.w3ma.m3u8parser.scanner.M3U8ItemScanner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.hahnphilipp.watchwithfritzbox.player.WatchWithFritzboxApplication;


public class GetFritzInfo extends AsyncTask<Void, Void, Void> {

    public String ip;
    public Runnable futureRunFinished;

    public boolean error = false;

    public Document doc;

    @Override
    protected Void doInBackground(Void... voids) {

        runFetch();
        if(futureRunFinished != null)
            futureRunFinished.run();

        return null;
    }

    public void runFetch() {
        try {
            //MOCK AN INVALID FRITZBOX CABLE FOR AMAZON TEST CENTER
            if(ip.contains("hahnphilipp.de")){
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.newDocument();
                Element e = doc.createElement("friendlyName");
                e.setTextContent("");
                doc.insertBefore(e, null);
                return;
            }
            URL url = new URL("http://"+ip+":49000/tr64desc.xml");
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(conn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            error = true;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            error = true;
        } catch (SAXException e) {
            e.printStackTrace();
            error = true;
        }
    }

}
