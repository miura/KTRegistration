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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class ImxParser {
	ArrayList<Integer> timepoints;
	ArrayList<double[]> positions;
	ArrayList<double[]> bpTrackPositions;
	Integer mintime;
	Integer maxtime;
	int frames;
	boolean ref1found = false;
	boolean ref2found = false;	
	double[] ref1 = new double[3];
	double[] ref2 = new double[3];
	
	/**
	 * current <spots><spot> Nodes 
	 */
	NodeList spotlist;
	
	/**
	 * current <bpTrack>...<spot> Nodes
	 * for the step 1, this will be empty and unused. 
	 */
	NodeList tspotlist;
	
	// following arraylists are for future implementation probably, 
	// undoing things. 
	/**
	 *  the initial list of spots. (cloned, to keep data) 
	 */
	ArrayList<Node> spotlistOrg;
	
	/**
	 *  the list of registered spots.  (cloned, to keep data) 
	 */	
	ArrayList<Node> spotlistReg;
	
	/**
	 * the list of registered back spots (not containing track-spots)  (cloned, to keep data) 
	 */
	ArrayList<Node> spotlistRegBack;
	
	/**
	 * the list of spots within tracks, before register-back  (cloned, to keep data) 
	 */
	ArrayList<Node> spotlistTracks;

	/**
	 * the list of spots within tracks, after register-back  (cloned, to keep data) 
	 */
	ArrayList<Node> spotlistRegBackTracks;
	
	/**
	 * the list of <bpTrack>s under <bpSurfaceComponents>
	 */
	NodeList trackList;
	/**
	 * a clone of the loaded xml, to be over written and saved as a new xml file. 
	 */
	public Document doc;
	/**
	 * true if the loaded file seems to be spot only file (the first step)
	 */
	public boolean isSpotfile;
	/**
	 * true if the loaded file seems to be already tracked file (the second step)
	 */
	public boolean isTrackedFile;
	public String rootpath;
	
	/**
	 * name of the reference tracks for pair vector calculation. 
	 */
	private final String reference1string = "reference1";
	private final String reference2string = "reference2";
	public ArrayList<String> trackpairinfo;
	
	/** Document Loader
	 *  see http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
	 * @param filepath
	 * @return
	 */
	public Document parseImx(String filepath){
		Document doc = null;
		try {
			File imxFile = new File(filepath);
			this.rootpath = imxFile.getParent();
			
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
		this.doc = null;
		//ImxParser ip = new ImxParser();
		timepoints = new ArrayList<Integer>();
		//positions = new ArrayList<double[]>();
		Document docorg = this.parseImx(filepath);
		Document doc = (Document) docorg.cloneNode(true);
		this.doc = doc;
		
		boolean hasSpotList = false;
		boolean hasTrackSpots = false;
		
		//get spots under <spots>, spots without tracking. 
		NodeList spots = getSpots(doc); //spots under <spots>
		System.out.println("spot number:" + spots.getLength());
		if (spots.getLength() > 0)
			hasSpotList = true;
		
		Node tracksContainer = null;
		NodeList trackspots = null;
		NodeList trackList = null;
		tracksContainer = getBPTracks(doc);
		if (tracksContainer != null){
			trackspots = getSpots( (Element) tracksContainer);
			trackList = getTracks((Element) tracksContainer);
			if (trackspots != null){
				System.out.println("track spot number:" + trackspots.getLength());
				hasTrackSpots = true;
			} else
				System.out.println("No spots in tracks");
		} else {
			System.out.println("No Tracks file");
		}
		if (hasSpotList && !hasTrackSpots)
			isSpotfile = true;
		else if (hasSpotList && hasTrackSpots)
			isSpotfile = false; // track containing file

		//step1
		if (isSpotfile){
			this.spotlist = spots;
			this.spotlistOrg = nodeList2ArrayList(spots); //to keep
			parseSpotsList(spots);
			this.trackList = null;
			
		//step2	
		} else {
			this.spotlist = spots;
			this.tspotlist = trackspots;
			this.spotlistReg = nodeList2ArrayList(spots); //to keep
			this.spotlistTracks = nodeList2ArrayList(trackspots); // to keep
			this.trackList = trackList;
			parseSpotsList(spots);
			parseTrackSpotsList(trackspots);
		}
			
		
	}
	ArrayList<Node> nodeList2ArrayList(NodeList src){
		ArrayList<Node> al = new ArrayList<Node>();
		for (int i = 0; i < src.getLength(); i ++)
			al.add(src.item(i).cloneNode(false)); 
		return al;
	}
	/**
	 * parse time points and get min and max of time points. 
	 * store positions as a String array in the order of the appearance of the <spot>
	 * @param spots
	 */
	void parseSpotsList(NodeList spots){
		this.positions = spotNodeList2ArrayList(spots);
		for (double[] spot : this.positions) 
			timepoints.add( (int) spot[3]);		
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
	
	ArrayList<double[]> spotNodeList2ArrayList(NodeList spots){
		ArrayList<double[]> positions = new ArrayList<double[]>();
		for (int i = 0; i < spots.getLength(); i++) {
			Node nNode = spots.item(i);
			Element eElement = (Element) nNode;
			String[] pos = eElement.getAttribute("position").split(" ");
			double[] elem = new double[5];
			for (int j = 0; j < 3; j++)
				elem[j] = Double.parseDouble(pos[j]);
			elem[3] = Double.parseDouble(eElement.getAttribute("time"));
			positions.add(elem);
		}
		return positions;
	}
	
	void parseTrackSpotsList(NodeList trackspots){
		this.bpTrackPositions = spotNodeList2ArrayList(trackspots);	
	}

	double[][] convertSpotPos (){
		// index = 4 in the original ImxRegister is index within string array (rec)
		// but for here, a dummy (will be 0 for all)
		double[][] pos = new double[this.positions.size()][5];
		for (int i = 0; i < pos.length; i++)
			pos[i] = this.positions.get(i);
		return pos;
	}

	double[][] convertTrackSpotPos (){
		// index = 4 in the original ImxRegister is index within string array (rec)
		// but for here, a dummy (will be 0 for all)
		double[][] pos = new double[this.bpTrackPositions.size()][5];
		for (int i = 0; i < pos.length; i++)
			pos[i] = this.positions.get(i);
		return pos;
	}
	
	/**
	 * Using the double[][] (Tomo's format of positions) passed to the argument, 
	 * updates the doc content, <spots><spot> lines.  
	 * @param pos
	 * @param spotlist
	 * @return
	 */
	boolean setSpotList(double[][] pos, NodeList spotlist){
		if (pos.length != spotlist.getLength()){
			System.out.println("Given double[][] has different lenght as spotlistReg");
			return false;
		}
		String placeholder = "%-10.8f %-10.8f %-10.8f";
		for (int i = 0; i < pos.length; i++){
			String posstring = String.format(placeholder, pos[i][0], pos[i][1], pos[i][2]);
			Node spot = spotlist.item(i);
			NamedNodeMap attrs = spot.getAttributes();
			Node attpos = attrs.getNamedItem("position");
			attpos.setTextContent(posstring);
		}
		return true;
	}
	
	/** 
	 * for step1. registered spot positions will be overwritten. 
	 * @param pos
	 * @return
	 */
	public boolean setSpotListReg(double[][] pos){
		if (setSpotList(pos, this.spotlist)){
			this.spotlistReg = nodeList2ArrayList(this.spotlist); // this is just to keep data. 
			return true;
		} else
			return false;
	}

	/** 
	 * for step2. 
	 * registered back spot positions will be written over registered spot positions. 
	 * This method updates only the spots under <spots><spot>
	 * @param pos
	 * @return
	 */
	public boolean setSpotListRegBack(double[][] pos){
		if (setSpotList(pos, this.spotlist)){
			this.spotlistRegBack = nodeList2ArrayList(this.spotlist); // this is just to keep data. 
			return true;
		} else
			return false;
	}

	/** 
	 * for step2. 
	 * registered back track spot positions will be written over registered track spot positions. 
	 * This method updates only the spots under <boTrack> ... <spot>
	 * @param pos
	 * @return
	 */
	public boolean setTrackSpotListRegBack(double[][] pos){
		if (setSpotList(pos, this.tspotlist)){
			this.spotlistRegBackTracks = nodeList2ArrayList(this.tspotlist); // this is just to keep data. 
			return true;
		} else
			return false;
	}		
	
	
//	/**
//	 * Collects track information for RegisterBack.
//	 * Input file should be the Imx file after registration, tracking and 
//	 * manual annotation in Imaris 
//	 * @param filepath
//	 */
//	public boolean loadImaxTracks(String filepath, int RefFNumber){
//		//Document doc = this.parseImx(filepath);	
//		Document doc = this.doc;
//		NodeList nodebpTrack = doc.getElementsByTagName("bpTrack");
//		NodeList nl2;
//		Element e, e2;
//		NamedNodeMap np = null;
//		String posstr;
//		String[] refstr;
//		String refFrameStr = Integer.toString(RefFNumber-1);
//		for (int i = 0; i < nodebpTrack.getLength(); i++){
//			e = (Element) nodebpTrack.item(i);
//			nl2 = e.getElementsByTagName("bpSurfaceComponent");
//			for (int j = 0; j < nl2.getLength(); j++){
//				e2 = (Element) nl2.item(j);
//				np = getReferenceAttributes("reference1", e2, refFrameStr);
//				if (np != null){
//					posstr = np.getNamedItem("position").getNodeValue();
//					System.out.println(posstr);
//					System.out.println(np.getNamedItem("time"));
//					ref1found = true;
//					refstr = posstr.split(" ");
//					for (int k = 0; k < 3; k++)
//						ref1[k] = Double.parseDouble(refstr[k]);
//
//				} 
//				np = getReferenceAttributes("reference2", e2, refFrameStr);
//				if (np != null){			
//					posstr = np.getNamedItem("position").getNodeValue();
//					System.out.println(posstr);
//					System.out.println(np.getNamedItem("time"));
//					ref2found = true;
//					refstr = posstr.split(" ");
//					for (int k = 0; k < 3; k++)
//						ref2[k] = Double.parseDouble(refstr[k]);					
//				}
//			}
//		}
//		if (ref1found && ref2found){
//			return true;
//		} else
//			return false;
//		
//	}
		
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
	
	public String evaluateTrackPairs(String reftime){
		if (this.trackList == null){
			return "no trackList! pair evaluation terminated.";
		}
		// spot nodes at reference time point
		ArrayList<Node> refspotnodes = new ArrayList<Node>();
		ArrayList<double[]> refspotpositions = new ArrayList<double[]>();
		
		// array of track name attribute node
		ArrayList<Node> trackNameNodes = new ArrayList<Node>();

		//for reporting
		ArrayList<String> trackinfo = new ArrayList<String>();

		int ntracks = this.trackList.getLength();
		// go through <bpTrack>s
		for (int i=0; i < ntracks; i++){
			boolean isRef1 = false;
			boolean isRef2 = false;
			String trackid = "no name";
			Element atrack = (Element) this.trackList.item(i);
			//trackid = atrack.getFirstChild().getFirstChild().getNodeValue();
			Node trackbps =  atrack.getElementsByTagName("bpSurfaceComponent").item(0);
			Node trackbpsName = ((Element) trackbps).getElementsByTagName("name").item(0);
			if (trackbpsName != null) {
				trackid = trackbpsName.getTextContent();
				trackNameNodes.add(trackbpsName);
				if (trackid.equals(this.reference1string))
					isRef1 = true;
				if (trackid.equals(this.reference2string))
					isRef2 = true;				
			}	
			NodeList spots = atrack.getElementsByTagName("spot");
			int nspots = spots.getLength();
			trackinfo.add("===" + trackid + "::   spots number:" + Integer.toString(nspots));
			
			// extract spots at reference time points.
			// if this track is a reference track, then stored as field value. 
			for (int j = 0; j < nspots; j++){
				Node aspot = spots.item(j);
				String stime = aspot.getAttributes().getNamedItem("time").getNodeValue();
				if (stime.equals(reftime)){
					String spos = aspot.getAttributes().getNamedItem("position").getNodeValue();
					double [] pos = new double[3];
					Scanner in = new Scanner(spos);
					for (int k=0; k<3; k++) pos[k] = in.nextDouble();
					//System.out.println(stime);
					//for (int k=0; k<3; k++) System.out.println(pos[k]);
					refspotpositions.add(pos);
					if (isRef1){
						this.ref1 = pos;
						this.ref1found = true;
					}
					if (isRef2){
						this.ref2 = pos;
						this.ref2found = true;
					}
				}
			}
		}
		// === pairing core part ===
		double[] axis = null;
		int[] renamedTrackIDs = null;
		if (this.ref1found && this.ref2found){
			axis = calculateRefAxis(this.ref1, this.ref2);
			renamedTrackIDs = getTrackNumber(refspotpositions, axis);
			if (renamedTrackIDs.length != trackNameNodes.size())
				return "track array length dose not match with renamedID array lenght. Terminates";
			for (int i = 0; i< renamedTrackIDs.length; i++){
				String newid = Integer.toString(renamedTrackIDs[i]);
				String newname = "Track " + newid;
				Node atrackName = trackNameNodes.get(i);
				atrackName.setTextContent(newname);
			}
				
		} else {
			return "No Reference Tracks found! Pair vaector cannot be calculated.";
		}
		//ArrayList<String> newtrackinfo = new ArrayList<String>();
		// check
		for (int i=0; i < ntracks; i++){
			String trackid = "no name";
			Element atrack = (Element) this.trackList.item(i);
			//trackid = atrack.getFirstChild().getFirstChild().getNodeValue();
			Node trackbps =  atrack.getElementsByTagName("bpSurfaceComponent").item(0);
			Node trackbpsName = ((Element) trackbps).getElementsByTagName("name").item(0);
			if (trackbpsName != null) {
				trackid = trackbpsName.getTextContent();
				//System.out.println("New ID:" + trackid);
				trackinfo.set(i, trackinfo.get(i) + " >> new id: " + trackid);
			}
			this.trackpairinfo = trackinfo;
		}		
		return "done";
	}
	
	// compute a vector between selected reference pair.  
	double[] calculateRefAxis(double[] ref1, double[] ref2){
		double[] axis = new double[3];
		for(int j=0; j<3; j++){
			axis[j] = (ref1[j]-ref2[j])/
					Math.sqrt(
							(ref1[0]-ref2[0])*(ref1[0]-ref2[0]) + 
							(ref1[1]-ref2[1])*(ref1[1]-ref2[1]) + 
							(ref1[2]-ref2[2])*(ref1[2]-ref2[2])
							);
		}
		return axis;		
	}

	/**
	 *  calculate the pairs. Partner is searched by the minimum of cross product length.
	 *  returned value is an array with spot array length containing updated track id. 
	 * @author Tomo
	 * @author Kota / modified for ImxParser 
	 * @param spot
	 * @param axis
	 * @return
	 */
	// 
	private int[] getTrackNumber(ArrayList<double[]> spot, double[] axis) {

		int nSpots = spot.size();
		int[] partner = new int[nSpots];
		System.out.println("Spot Number in Reference Frame:" + nSpots);
		for(int i = 0; i < nSpots; i++){
			double minD = Double.MAX_VALUE;
			partner[i] = 99;
			//System.out.println("spot.length" + nSpots);
			for(int j = 0; j < nSpots; j++){
				if(i!=j){
					double sx = spot.get(i)[0] - spot.get(j)[0];
					double sy = spot.get(i)[1] - spot.get(j)[1];
					double sz = spot.get(i)[2] - spot.get(j)[2];

					double dx = sy*axis[2]-sz*axis[1];
					double dy = sz*axis[0]-sx*axis[2];
					double dz = sx*axis[1]-sy*axis[0];

					double D = Math.sqrt(dx*dx+dy*dy+dz*dz);

					if(D<minD){
						minD = D;
						partner[i] = j;
					}
				}
			}

		}

		int trackNumber[] = new int[nSpots];

		int nUnpaired=0;
		int nPaired=0;

		// re-numbering in ordered way.
		for(int i = 0; i < nSpots; i++){
			int p = partner[i];
			if(p==99){
				trackNumber[i] = 99;
				nUnpaired++;
			}else if(partner[p] == i){
				if(i < p){
					trackNumber[i] = nPaired;
					trackNumber[p] = nPaired+1;
					nPaired = nPaired+2;
				}else{
				}

			}else{
				trackNumber[i] = 99;
				nUnpaired++;
			}
		}

		if(nPaired+nUnpaired != nSpots){System.out.println("Error");}

		return trackNumber;
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
		//ip.loadImaxTracks("/Volumes/D/Judith/tracks.imx", 1);
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
	
	Node getbpSurfaceComponent(Document doc){
		Node bpSurfaceComponent = null;
		NodeList nl = doc.getElementsByTagName("bpSurfaceComponent");
		if (nl.getLength() != 1)
			System.out.println("<bpSurfaceComponent> tags =" + Integer.toString(nl.getLength()));
		else {
			bpSurfaceComponent = nl.item(0);
		}		
		return bpSurfaceComponent;		
	}
	
	// returns the node bpSurfaceComponent just above bpTracks
	Node getBPTracks(Document doc){
		NodeList bpTrack = doc.getElementsByTagName("bpTrack");
		System.out.println("<bpTrack> tags =" + Integer.toString(bpTrack.getLength()));
		Node trackContainer = null;
		if (bpTrack.getLength() > 0) 
			trackContainer = bpTrack.item(0).getParentNode();
		return trackContainer;
	}
	//returns list of <spot> nodes
	NodeList getSpots(Document doc){
		NodeList spotsnode  = doc.getElementsByTagName("spots");
		NodeList spots = null;
		if ((spotsnode.getLength() < 1) || (spotsnode == null) ){
			System.out.println(" no <spots> found>");
		} else if (spotsnode.getLength() > 1){
			System.out.println(" more than one <spots> nodes found>");
		} else {
			Element spotelement = (Element) spotsnode.item(0);
			spots = getSpots(spotelement);
		}
		return spots;
	}	

	//returns a NodeList of <spot> nodes
	NodeList getSpots(Element e){
		NodeList spots  = e.getElementsByTagName("spot");
		if ((spots.getLength() < 1) || (spots == null) ){
			System.out.println(" no <spot> found>");
		} else {
			System.out.println(" more than one <spot> nodes found>");
		}
		return spots;
	}
	//list of <bpTrack>
	NodeList getTracks(Element e){
		NodeList tracks  = e.getElementsByTagName("bpTrack");
		if ((tracks.getLength() < 1) || (tracks == null) ){
			System.out.println(" no <bpTrack> found>");
		} else {
			System.out.println("<bpTrack>tags:" + Integer.toString(tracks.getLength()));
		}
		return tracks;
	}
	
	/**
	 * writes updated xml as a new file. 
	 * http://www.mkyong.com/java/how-to-modify-xml-file-in-java-dom-parser/
	 * @param filepath fullpath to a new file to be written.
	 */
	public void writeUpdatedImx(String filepath){
		try {
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filepath));
			transformer.transform(source, result);
			System.out.println("File Written: " + filepath);
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
		
	}
}
