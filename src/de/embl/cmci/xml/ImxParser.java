package de.embl.cmci.xml;

/**
 * Kota Miura
 * 20130528
 * 
 * Tomo's registration class extended with xml parsing
 * to readout timepoints, generalize the protocol. 
 * 
 * Currently only for getting time point minimum and max, but eventually
 * registration itself is better be done using 3D vectors. 
 * 
 */
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ImxParser {
	ArrayList timepoints;
	ArrayList positions;
	Integer mintime;
	Integer maxtime;
	private int frames;
	
	public Document parseImx(String filepath){
		Document doc = null;
		try {
			File imxFile = new File(filepath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(imxFile);
			doc.getDocumentElement().normalize();
			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
				
		} catch (Exception e) {
			e.printStackTrace();
	    }
		return doc;
	}
	
	public void loadImaxInfo(String filepath){
		//ImxParser ip = new ImxParser();
		timepoints = new ArrayList<Integer>();
		positions = new ArrayList<String>();
		Document doc = this.parseImx(filepath);	
		NodeList nList = doc.getElementsByTagName("spot");
		for (int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			Element eElement = (Element) nNode;
			timepoints.add(Integer.parseInt(eElement.getAttribute("time")));
			positions.add(eElement.getAttribute("position"));
		}
		Object maxT = Collections.max(timepoints);
		Object minT = Collections.min(timepoints);
		maxtime = (Integer) maxT;
		mintime = (Integer) minT;
		if (maxtime > mintime)
			frames =maxtime - mintime + 1;
		else
			System.out.println(" something wrong with imx file: timepoint min >= max");
		System.out.println("minT:" + Integer.toString(mintime));
		System.out.println("maxT:" + Integer.toString(maxtime));
		
	}
	
	//for testing
	public static void main(String argv[]) {
		ImxParser ip = new ImxParser();
		//Document doc = ip.parseImx("/Volumes/D/Judith/data.imx");
		//ip.testParsing(doc);
		ip.loadImaxInfo("/Volumes/D/Judith/data.imx");
		ip.loadImaxInfo("/Volumes/D/Judith/780/780_3_TP_data.imx");
		// problem with the file below: no closing tag for </bpImageField>
		ip.loadImaxInfo("/Volumes/D/Judith/spim/SPIM_3_TP_data.imx");

	}	
	public void testParsing(Document doc){
		NodeList nList = doc.getElementsByTagName("spot");	
		System.out.println("----------------------------");
		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = nList.item(temp);

			System.out.println("\nCurrent Element :" + nNode.getNodeName());

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				System.out.println("Time: " + eElement.getAttribute("time"));
				System.out.println("Radius: " + eElement.getAttribute("radius"));					
				//System.out.println("First Name : " + eElement.getElementsByTagName("firstname").item(0).getTextContent());
			}
		}		
	}
	
}
