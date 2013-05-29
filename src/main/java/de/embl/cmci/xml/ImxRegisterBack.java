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
	
	//gui
	Button loadRegisterBack;
	
	public static void main(String args[]) {
		ImxRegisterBack win = new ImxRegisterBack();
	}
	
	//GUI
	public ImxRegisterBack() {
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
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == loadRegisterBack){
			registerBackMain();
		}

	}
	
	public void registerBackMain(){
		FileDialog fd = new FileDialog(frm , "Select the imx File" , FileDialog.LOAD);
		fd.setVisible(true);

		ta.setText(fd.getDirectory() + fd.getFile() + "\n");

		String folder = fd.getDirectory();
		String orif = fd.getFile();
		
		ImxParser ip = new ImxParser();
		ip.loadImaxInfo(folder + orif);
		//int nTime = 31;
		int nTime = ip.frames;
		
		String refTimepoint = "" + 1+(Integer.parseInt(ch1.getSelectedItem()));
		String cenPosF = "cenPos-data.imx.csv";
		
		// this probably should be fixed?
		String imx = loadImx(folder + orif);
		String cenPosStr = loadImx(folder + cenPosF);

		String[] rec = splitImxToString(imx);
		String[] cenPosRec = splitImxToString(cenPosStr);

		double cenPos[][] = loadPos(cenPosRec, nTime);

		double allSpotPos[][] = loadAllSpotPos(rec, nTime);

		double registerBackPos[][] = registerBackPos(allSpotPos, cenPos);

		String[] registeredBackRec = replaceIntoRegistered(rec, registerBackPos);

		double[][] spotPosAtFirst = loadSpotPosAtFrist(allSpotPos, refTimepoint);

		double[] axis = getReferenceAxis(rec, refTimepoint);

		int[] trackNumber = getTrackNumber(spotPosAtFirst, axis);

		String[] sortedRegisteredBackRec = giveTrackNumber(trackNumber, registeredBackRec, refTimepoint);

		//outputRegisteredImx(registeredBackRec, folder, orif);

		outputRegisteredImx(sortedRegisteredBackRec, folder, orif);

		ta.append("Done.");
	}

	public void windowClosing(WindowEvent e) {
		System.exit(0);
	}
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

		double[] axis = new double[3];
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

		for(int j=0; j<3; j++){
			axis[j] = (ref1[j]-ref2[j])/Math.sqrt((ref1[0]-ref2[0])*(ref1[0]-ref2[0])+(ref1[1]-ref2[1])*(ref1[1]-ref2[1])+(ref1[2]-ref2[2])*(ref1[2]-ref2[2]));
		}


		return axis;
	}



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



	private static double[][] loadSpotPosAtFrist(double[][] pos, String refTimepoint) {
		int i,n;
		n=0;

		for(i=0;i<pos.length;i++){
			if(pos[i][3]==Integer.parseInt(refTimepoint)){
				n++;
			}
		}


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
		System.out.println(s);
		String[] strrec = s.split(" ");

		return strrec;


	}

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