package de.embl.cmci.xml;


import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ImxRegisterBack extends WindowAdapter implements ActionListener{
	Frame frm = new Frame("ImxRegisterBack");
	TextArea ta;
	Choice ch1;
	public static final boolean NO_GUI = false;
	public static final boolean WITH_GUI = true;
	//gui
	Button loadRegisterBack;

	public static void main(String args[]) {
		ImxRegisterBack win = new ImxRegisterBack(WITH_GUI);
	}
	
	//GUI
  public ImxRegisterBack(boolean GUI) {
    if (GUI) {
      frm.setSize(200 , 600);
      frm.setLayout(new FlowLayout());

      Label lb = new Label();
      lb.setText("Representative interKT axis");
      frm.add(lb);

      Label lb1 = new Label("Time:");
      frm.add(lb1);

      ch1 = new Choice();
      for(int i=0; i<31; i++){
        ch1.add(""+(i+1));
      }
      frm.add(ch1);

      loadRegisterBack = new Button("Load imx file");
      frm.add(loadRegisterBack);
      loadRegisterBack.addActionListener(this);

      ta = (TextArea)frm.add("Center", new TextArea());
      ta.setSize(200 , 550);

      frm.setVisible(true);
      frm.addWindowListener(this);
    }
  }
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == loadRegisterBack){
			int reftime = 1+(Integer.parseInt(ch1.getSelectedItem()));
			registerBackMain(reftime);
		}

	}

	public void registerBackMain(TextArea ta, Choice ch1){
		this.ta = ta;
		this.ch1 = ch1;
		int reftime = 1+(Integer.parseInt(ch1.getSelectedItem()));
		registerBackMain(reftime);
	}
	
	//uses file dialog
	public void registerBackMain(int reftime){
		FileDialog fd = new FileDialog(frm , "Select the imx File" , FileDialog.LOAD);
		fd.setVisible(true);

		String folder = fd.getDirectory();
		String orif = fd.getFile();
		registerBackMain(folder, orif, reftime);
	}
	
	public void registerBackMain(String folder, String orif, int reftime){

		//prints out the selected directory.
		ouputInfo(folder + orif + "\n");

		//retrieve user-selected timepoint. 
		
		String refTimepoint = Integer.toString(reftime) ;

		ImxParser ip = new ImxParser();
		ip.loadImaxInfo(folder + orif);
		//int nTime = 31;
		int nTime = ip.frames;
		ouputInfo("Time Points:"+Integer.toString(nTime) + "\n");

//		ip.loadImaxTracks(folder + orif, reftime);

		//this file name seems to be pretty static. 
		//the file containing centroid positions. 
		String cenPosF = "cenPos-data.imx.csv";

		// this loads the centroid position file. 
		String cenPosStr = loadImx(folder + cenPosF);

		//splits Imx content by spaces. 
		String[] cenPosRec = splitImxToString(cenPosStr);

		//loads the position file	
		double cenPos[][] = loadPos(cenPosRec, nTime);

		// this probably should be fixed?
		// loading registered, tracked then reference annotated file. 
		String imx = loadImx(folder + orif);		
		String[] rec = splitImxToString(imx);		
		// collect all spot positions in 2D array. 
		double allSpotPos[][] = loadAllSpotPos(rec, nTime);
		for (int i = 0; i < allSpotPos.length; i++)
			System.out.println(allSpotPos[i][0]);

		//this restores what it should be the original positions of all the spots. 
		double registerBackPos[][] = registerBackPos(allSpotPos, cenPos);

		// prepare string array that corresponds to the Imx content. 
		String[] registeredBackRec = replaceIntoRegistered(rec, registerBackPos);

		// returns spots in tracks at reference timepoint
		double[][] spotPosAtFirst = loadSpotPosAtFrist(allSpotPos, refTimepoint);

		//double[] axis = getReferenceAxis(rec, refTimepoint);
		// a vector between selected pair (annotated).
		double[] axis = calculateRefAxis(ip.ref1, ip.ref2);

		// find a partner with the minimum cross product
		//	rename tracks according to pairing. 
		int[] trackNumber = getTrackNumber(spotPosAtFirst, axis);


		String[] sortedRegisteredBackRec = giveTrackNumber(trackNumber, registeredBackRec, refTimepoint);

		//outputRegisteredImx(registeredBackRec, folder, orif);

		outputRegisteredImx(sortedRegisteredBackRec, folder, orif);
		
		ouputInfo("Done.");
	}
	void ouputInfo(String info){
		if ( ta != null)
			ta.append(info);
		else
			System.out.println(info);		
	}

	public void windowClosing(WindowEvent e) {
		System.exit(0);
	}
	
	// calculate the pairs. Partner is searched by the minimum of cross product length.
	// returned value is an array with spot array length containing updated track id. 
	private static int[] getTrackNumber(double[][] spot,
			double[] axis) {

		int[] partner = new int[spot.length];

		for(int i=0; i<spot.length; i++){
			double minD = Double.MAX_VALUE;
			partner[i] = 99;
			System.out.println("spot.length"+spot.length);
			for(int j=0; j<spot.length; j++){
				if(i!=j){
					double sx = spot[i][0]-spot[j][0];
					double sy = spot[i][1]-spot[j][1];
					double sz = spot[i][2]-spot[j][2];

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

		int trackNumber[] = new int[spot.length];

		int nUnpaired=0;
		int nPaired=0;

		// re-numbering in ordered way.
		for(int i=0; i<spot.length; i++){
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

		if(nPaired+nUnpaired!=spot.length){System.out.println("Error");}

		return trackNumber;
	}


	private static double[] getReferenceAxis(String[] rec, String refTimepoint) {

		double[] ref1 = new double[3];
		double[] ref2 = new double[3];


		for(int i=0; i<rec.length; i++){
			if(rec[i].contains("reference1")){
				int j=0;
				while(!(rec[i+j].equals("<spot") && rec[i+j+4].equals("time=\""+refTimepoint+"\""))){
					j++;
				}

				ref1[0] = xFromStr(rec[i+j+1]);
				ref1[1] = yFromStr(rec[i+j+2]);
				ref1[2] = zFromStr(rec[i+j+3]);
			}
			if(rec[i].contains("reference2")){
				int j=0;
				while(!(rec[i+j].equals("<spot") && rec[i+j+4].equals("time=\""+refTimepoint+"\""))){
					j++;
				}

				ref2[0] = xFromStr(rec[i+j+1]);
				ref2[1] = yFromStr(rec[i+j+2]);
				ref2[2] = zFromStr(rec[i+j+3]);
				System.out.println("ref2"+ref2[0]);
			}

		}

//		for(int j=0; j<3; j++){
//			axis[j] = (ref1[j]-ref2[j])/Math.sqrt((ref1[0]-ref2[0])*(ref1[0]-ref2[0])+(ref1[1]-ref2[1])*(ref1[1]-ref2[1])+(ref1[2]-ref2[2])*(ref1[2]-ref2[2]));
//		}
		//return axis;
		return calculateRefAxis(ref1, ref2);
	}
	
	// a vector between selected pair.  
	static double[] calculateRefAxis(double[] ref1, double[] ref2){
		double[] axis = new double[3];
		for(int j=0; j<3; j++){
			axis[j] = (ref1[j]-ref2[j])/Math.sqrt((ref1[0]-ref2[0])*(ref1[0]-ref2[0])+(ref1[1]-ref2[1])*(ref1[1]-ref2[1])+(ref1[2]-ref2[2])*(ref1[2]-ref2[2]));
		}
		return axis;		
	}

	// rename all track numbers. 
	private static String[] giveTrackNumber(int[] trackNumber,
			String[] rec, String refTimepoint) {
		String[] gaveRec = rec;
		int n=0;
		boolean isTrack = false;
		for(int i=0; i<rec.length; i++){
			if(rec[i].contains("bpTrack")){
				isTrack = true;
			}
			if(rec[i].equals("<spot") && isTrack && rec[i+4].equals("time=\""+refTimepoint+"\"")){

				int j=0;
				while(!(rec[i-j].equals("<name>Track") || rec[i-j].contains("reference"))){

					j++;
				}
				System.out.println(j);
				System.out.println(i-j);
				System.out.println(trackNumber.length);
				gaveRec[i-j] = "<name>"+trackNumber[n]+"</name>";

				gaveRec[i-j+1] = "\n";
				n++;
			}

		}

		return gaveRec;
	}


	// among all the points, last half of the 
	// array is searched for those which are at the reference time point
	// reference time point corresponds to the frame where reference pair was annotated.
	// compared to the original spot data file, there are two times more spot position
	// element because of spots + spots within tracks. Spots from last half are those from tracks.  
	private static double[][] loadSpotPosAtFrist(double[][] pos, String refTimepoint) {
		int i,n;
		n=0;

		for(i=0;i<pos.length;i++){
			if(pos[i][3]==Integer.parseInt(refTimepoint)){
				n++;
			}
		}

    // why n/2??
		double[][] firstPos = new double[n/2][5];

		int k=0;
		for(i=pos.length/2;i<pos.length;i++){
			if(pos[i][3]==Integer.parseInt(refTimepoint)){
				firstPos[k] = pos[i];
				k++;
			}
		}

		// TODO Auto-generated method stub
		return firstPos;
	}


  // extract all spot positions. 
  // lines starting with <spot are the spots. 
  // extract x, y, z, t, and the line index. This index is 
  // not same as the original file number as separation
  // chractor is " ".
	private static double[][] loadAllSpotPos(String[] rec, int time) {
		int i,n;
		n=0;

		for(i=0;i<rec.length;i++){
			if(rec[i].equals("<spot")){
				n++;
				i = i+4;
			}
		}

		double[][] pos = new double[n][5];

		int k=0;
		for(i=0;i<rec.length;i++){
			if(rec[i].equals("<spot")){
				pos[k][0] = xFromStr(rec[i+1]);
				pos[k][1] = yFromStr(rec[i+2]);
				pos[k][2] = zFromStr(rec[i+3]);
				pos[k][3] = timeFromStr(rec[i+4]);
				pos[k][4] = i;
				i = i+4;
				k++;
			}
		}

		return pos;
	}

	private static void outputRegisteredImx(String[] registeredRec, String folder, String f) {
		try{
			File file = new File(folder + "registerback-"+f);
			PrintWriter pw
				= new PrintWriter(new BufferedWriter(new FileWriter(file)));
			for(int i=0;i<registeredRec.length;i++){
				pw.print(registeredRec[i]+" ");
			}
			pw.close();
		}catch(IOException e){
			System.out.println(e);
		}

	}

	private static String[] replaceIntoRegistered(String[] rec,
			double[][] registeredPos) {
		int i;
		String[] registeredRec = rec;
		for(i=0;i<registeredPos.length;i++){
			int j = (int)registeredPos[i][4];
			registeredRec[j+1] = "position=\"" + registeredPos[i][0];
			registeredRec[j+2] = ""+ registeredPos[i][1];
			registeredRec[j+3] = registeredPos[i][2] + "\"";
		}
		return registeredRec;
	}

  // add centroid of that time point - the centroid of the first point.  
	private static double[][] registerBackPos(double[][] allSpotPos,
      double[][] cenPos) {
		int i,j;
		double[][] registeredBackPos = new double[allSpotPos.length][5];

		for(i=0;i<allSpotPos.length;i++){
			int t = (int)allSpotPos[i][3];
			for(j=0;j<3;j++){
				registeredBackPos[i][j] = allSpotPos[i][j] + cenPos[t][j] - cenPos[0][j];

			}
			for(j=3;j<5;j++){
				registeredBackPos[i][j] = allSpotPos[i][j];
			}
		}

		return registeredBackPos;
	}

  //loads the position file generated by registering. 
  private static double[][] loadPos(String[] rec, int nTime) {
		int i;
		double[][] pos = new double[nTime][3];

		for(i=0;i<nTime;i++){
			pos[i][0] = Double.parseDouble(rec[3*i]);
			pos[i][1] = Double.parseDouble(rec[3*i+1]);
			pos[i][2] = Double.parseDouble(rec[3*i+2]);
		}

		return pos;
	}


	private static double xFromStr(String str) {
		int i;
		double x;
		char[] strChar = str.toCharArray();
		char[] tChar = new char[strChar.length-10];
		for(i=10;i<strChar.length; i++){
			tChar[i-10]=strChar[i];
		}
		String nStr = new String(tChar);
		x = Double.parseDouble(nStr);
		return x;
	}
	private static double yFromStr(String str) {
		int i;
		double y;
		char[] strChar = str.toCharArray();
		char[] tChar = new char[strChar.length];
		for(i=0;i<strChar.length; i++){
			tChar[i]=strChar[i];
		}
		String nStr = new String(tChar);
		y = Double.parseDouble(nStr);
		return y;
	}

	private static double zFromStr(String str) {
		int i;
		double z;
		char[] strChar = str.toCharArray();
		char[] tChar = new char[strChar.length-1];
		for(i=0;i<strChar.length-1; i++){
			tChar[i]=strChar[i];
		}
		String nStr = new String(tChar);
		z = Double.parseDouble(nStr);
		return z;
	}



	private static int timeFromStr(String str) {
		int i,t;
		char[] strChar;
		strChar = str.toCharArray();
		char[] tChar = new char[strChar.length-7];
		for(i=6;i<strChar.length-1; i++){
			tChar[i-6]=strChar[i];
		}
		String nStr = new String(tChar);
		t = Integer.parseInt(nStr);
		return t;
	}

	private static String[] splitImxToString(String s) {
		//System.out.println(s);
		String[] strrec = s.split(" ");

		return strrec;


	}

  //returns Imx file as a string. 
	private static String loadImx(String filename) {
		String s = null;
		try
		{
			File f = new File(filename);
			byte[] b = new byte[(int) f.length()];
			FileInputStream fi = new FileInputStream(f);
			fi.read(b);
			s = new String(b);
			fi.close();
		}
		catch(Exception e)
		{
			return null;

		}
		if ( s	==	null )
		{
			return null;
		}

		return s;


	}

}
