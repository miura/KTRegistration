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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ImxParser {
	ArrayList<Integer> timepoints;
	ArrayList<double[]> positions;
	Integer mintime;
	Integer maxtime;
	int frames;
	boolean ref1found = false;
	boolean ref2found = false;	
	double[] ref1 = new double[3];
	double[] ref2 = new double[3];	
	
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
	// initial run to retireve spot information. 
	public void loadImaxInfo(String filepath){
		//ImxParser ip = new ImxParser();
		timepoints = new ArrayList<Integer>();
		positions = new ArrayList<double[]>();
		Document doc = this.parseImx(filepath);	
		NodeList nList = doc.getElementsByTagName("spot");
		for (int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			Element eElement = (Element) nNode;
			timepoints.add(Integer.parseInt(eElement.getAttribute("time")));
			String[] pos = eElement.getAttribute("position").split(" ");
			double[] elem = new double[4];
			for (int j = 0; j < 3; j++)
				elem[j] = Double.parseDouble(pos[j]);
			elem[3] = Double.parseDouble(eElement.getAttribute("time"));
			positions.add(elem);
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
	
	/**
	 * Collects track infromation for RegisterBack. 
	 * @param filepath
	 */
	public void loadImaxTracks(String filepath, int RefFNumber){
		Document doc = this.parseImx(filepath);	
		NodeList nl = doc.getElementsByTagName("bpTrack");
		NodeList nl2;
		Element e, e2;
		NamedNodeMap np = null;
		String posstr;
		String[] refstr;
		String refFrameStr = Integer.toString(RefFNumber-1);
		for (int i = 0; i < nl.getLength(); i++){
			e = (Element) nl.item(i);
			nl2 = e.getElementsByTagName("bpSurfaceComponent");
			for (int j = 0; j < nl2.getLength(); j++){
				e2 = (Element) nl2.item(j);
				np = getReferenceAttributes("reference1", e2, refFrameStr);
				if (np != null){
					posstr = np.getNamedItem("position").getNodeValue();
					System.out.println(posstr);
					System.out.println(np.getNamedItem("time"));
					ref1found = true;
					refstr = posstr.split(" ");
					for (int k = 0; k < 3; k++)
						ref1[k] = Double.parseDouble(refstr[k]);

				} 
				np = getReferenceAttributes("reference2", e2, refFrameStr);
				if (np != null){			
					posstr = np.getNamedItem("position").getNodeValue();
					System.out.println(posstr);
					System.out.println(np.getNamedItem("time"));
					ref2found = true;
					refstr = posstr.split(" ");
					for (int k = 0; k < 3; k++)
						ref2[k] = Double.parseDouble(refstr[k]);					
				}
			}
		}	
	}
		
	NamedNodeMap getReferenceAttributes(String tagname, Element e2, String refFrameStr){
		NodeList nl3, nl4;
		Element e3, e4;
		NamedNodeMap np = null;
		NamedNodeMap retnp = null;
		String thisname = getElementContent(e2, "name");
		if (thisname.equals(tagname)){
			System.out.println(thisname);
			nl3 = e2.getElementsByTagName("bpSurfaceComponentSpot");
			System.out.println("total Spots Number:" + Integer.toString(nl3.getLength()));
			for (int k = 0; k < nl3.getLength(); k++){
				e3 = (Element) nl3.item(k);
				nl4 = e3.getElementsByTagName("spot");
				Node n = nl4.item(0);
				np = n.getAttributes();
				if (np.getNamedItem("time").getNodeValue().equals(refFrameStr)){
					retnp = np;
				}
			}
		}
		return retnp;
	}
	
	
	static String getElementContent(Element e, String TagName) {
		NodeList nl = e.getElementsByTagName(TagName);
		Node n = nl.item(0);
		Node content = n.getFirstChild();
		return content.getNodeValue();
	}
	
	//for testing
	public static void main(String argv[]) {
		ImxParser ip = new ImxParser();
		//Document doc = ip.parseImx("/Volumes/D/Judith/data.imx");
		//ip.testParsing(doc);
		//ip.loadImaxInfo("/Volumes/D/Judith/data.imx");
		//ip.loadImaxInfo("/Volumes/D/Judith/780/780_3_TP_data.imx");
		
		// problem with the file below: no closing tag for </bpImageField>
		//ip.loadImaxInfo("/Volumes/D/Judith/spim/SPIM_3_TP_data.imx");

		//ip.loadImaxInfo("/Volumes/D/Judith/spim1/data1.imx");
		//ip.loadImaxInfo("/Volumes/D/Judith/spim2/data2.imx");
		
		//ip.loadImaxTracks("/Volumes/D/Judith/tracks.imx", 1);
		ip.loadImaxTracks("/Volumes/D/Judith/tracks.imx", 1);
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
