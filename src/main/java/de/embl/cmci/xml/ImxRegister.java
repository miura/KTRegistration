package de.embl.cmci.xml;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

public class ImxRegister extends WindowAdapter implements ActionListener{
	JFrame frm = new JFrame("ImxRegister");
	TextArea ta, ta2;
	Choice ch1;	
	//gui
	Button loadDataImxButton;
	Button loadRegisterBack;
	ImxParser ip; 

	public double[][] centroids;
	
	public static void main(String args[]) {
		ImxRegister win = new ImxRegister();
		win.imxRegisterGUI();
	}
	
	public void imxRegisterGUI() {
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setSize(400 , 600);
		//frm.setLayout(new GridLayout(6, 1));
		frm.setLayout(new BoxLayout(frm.getContentPane(), BoxLayout.Y_AXIS));

		//first step, registration of the original
		JPanel panelstep1 = new JPanel();
		panelstep1.setLayout(new BoxLayout(panelstep1, BoxLayout.Y_AXIS));
		panelstep1.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		Label lb1 = new Label();
		lb1.setText("Step1: Registration");
		//frm.add(lb1);
		panelstep1.add(lb1);
		//1
		loadDataImxButton = new Button("Load Original Imx");
		//frm.add("North", loadDataImxButton);
		//frm.add(loadDataImxButton);
		panelstep1.add(loadDataImxButton);
		loadDataImxButton.addActionListener(this);
		frm.add(panelstep1);    
		//2
		//ta = (TextArea)frm.add("Center", new TextArea());
		ta = (TextArea)frm.add(new TextArea());
		ta.setSize(200 , 550);

		// second step, originally in the Register back
		//
		JPanel panelstep2 = new JPanel();
		panelstep2.setLayout(new BoxLayout(panelstep2, BoxLayout.Y_AXIS));
		panelstep2.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		frm.add(new JSeparator(SwingConstants.HORIZONTAL));
		Label lb2 = new Label();
		lb2.setText("Step2: Inverse Registration");
		panelstep2.add(lb2);

		Label lb = new Label();
		lb.setText("Representative interKT axis");
		panelstep2.add(lb);
		Panel p1 = new Panel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
		// 4
		JLabel lb4 = new JLabel("Time:", JLabel.CENTER);
		lb4.setVerticalAlignment(JLabel.CENTER);
		//frm.add(lb1);
		p1.add(lb4);
		// 5
		ch1 = new Choice();
		for(int i=0; i<31; i++){
			ch1.add(""+(i+1));
		}
		//frm.add(ch1);
		p1.add(ch1);
		panelstep2.add(p1);
		//6
		loadRegisterBack = new Button("Load Registered & \n annotated Imx file");
		panelstep2.add(loadRegisterBack);
		loadRegisterBack.addActionListener(this);

		//7
		ta2 = new TextArea();
		panelstep2.add("Center", ta2);
		ta2.setSize(200 , 550);
		frm.add(panelstep2);

		frm.setVisible(true);
		frm.addWindowListener(this);
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == loadDataImxButton){
			registerMain();
		}
    if (e.getSource() == loadRegisterBack){
  		//ImxRegisterBack regback = new ImxRegisterBack(ImxRegisterBack.NO_GUI);    
			if (registerBackMain() == false){
				System.out.println("aborted.");
			}
		}

	}
	public void registerMain(){
		FileDialog fd = new FileDialog(frm , "Select the imx File" , FileDialog.LOAD);
		fd.setVisible(true);		
		String folder = fd.getDirectory();
		String orif = fd.getFile();
		registerMain(folder, orif);
	}
	
	public void registerMain(String folder, String orif){
		
		String fullpath = folder + orif;
		ouputInfo(fullpath);
		
		ImxParser ip = new ImxParser();
		ip.loadImaxInfo(fullpath);
		
		//int nTime = 31;
		int nTime = ip.frames;
		
		// ------ spots <spot>
		double spotsPos[][] = ip.convertSpotPos();
		
		String spotsinfo = "\n === Spots Info === \n";
		String placeholder = "t=%d: %-10.8f %-10.8f %-10.8f%n";
		for (int i = 0; i < spotsPos.length; i++)
			spotsinfo += String.format(placeholder, 
					(int) spotsPos[i][3], spotsPos[i][0], spotsPos[i][1], spotsPos[i][2]); 
		ouputInfo(spotsinfo);

		
		//------ centroid --------
		double cenPos[][] = calcCenPos(spotsPos, nTime);
		String centroidinfo = "\n\n=== Spot Centroids ===\n";	
		placeholder = "t=%d: %-10.8f %-10.8f %-10.8f%n";
		for(int i=0;i<cenPos.length;i++)
			centroidinfo += String.format(placeholder, i, cenPos[i][0], cenPos[i][1], cenPos[i][2]);
		ouputInfo(centroidinfo);
		this.centroids = cenPos;

		double registeredPos[][] = registerPos(spotsPos, cenPos);
		
		ip.setSpotListReg(registeredPos);

		// === writing files ===
		outputCenPos(cenPos, folder, orif);
		
		String newfilepath = folder + "registered-"+ orif;
		ip.writeUpdatedImx(newfilepath);
		
		// === keep things in the active memory ====
		ip.rootpath = folder;
		this.ip = ip;		
		
		ouputInfo("\nRegisterd File: " + newfilepath + "\n");
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

	/**
	 * Writes centorid positions in a file. 
	 * @author Tomo
	 * @param cenPos
	 * @param folder
	 * @param f
	 */
	private static void outputCenPos(double[][] cenPos, String folder, String f) {
		try{
			File file2 = new File(folder + "cenPos-"+f+".csv");
			PrintWriter pw2
				= new PrintWriter(new BufferedWriter(new FileWriter(file2)));

			for(int i=0;i<cenPos.length;i++){
				for(int j=0;j<3;j++){
					//pw2.print(cenPos[i][j]+" ");
					pw2.print(Double.toString(cenPos[i][j])+" ");

				}
			}
			pw2.close();
		}catch(IOException e){
			System.out.println(e);
		}

	}

	/**
	 * Registers spots by centroid of all spots / time frame. 
	 * @author Tomo
	 * @param allSpotPos
	 * @param cenPos
	 * @return
	 */
	private static double[][] registerPos(double[][] allSpotPos,
			double[][] cenPos) {
		int i,j;
		double[][] registeredPos = new double[allSpotPos.length][5];

		for(i=0;i<allSpotPos.length;i++){
			int t = (int)allSpotPos[i][3];
			for(j=0;j<3;j++){
				registeredPos[i][j] = allSpotPos[i][j] - cenPos[t][j] + cenPos[0][j];

			}
			for(j=3;j<5;j++){
				registeredPos[i][j] = allSpotPos[i][j];
			}
		}

		return registeredPos;
	}
	
	/**
	 * Computes centorid / time frame
	 * @author Tomo
	 * @param pos
	 * @param nTime
	 * @return
	 */
	private static double[][] calcCenPos(double[][] pos, int nTime) {
		double[][] cenPos = new double[nTime][3];
		int i=0;
		int n=0;
		int t=0;
		int j=0;

		for(t=0;t<nTime;t++){
			n=0;
			j=0;
			//System.out.println(pos.length);
			for(i=0;i<pos.length;i++){

				if((int)pos[i][3] == t){
					n++;
				}
			}
		//	for(i=0;i<cenPos.length;i++){
	//		for(j=0;j<3;j++){
	//				System.out.print(cenPos[i][j]);
		//		}
		//	}
			double[][] posAtT = new double[n][4];
			for(i=0;i<pos.length;i++){
				if((int)pos[i][3] == t){
					posAtT[j] = pos[i];
					j++;
				}
			}
			for(j=0;j<n;j++){
				for(i=0;i<3;i++){
					cenPos[t][i] = cenPos[t][i] + posAtT[j][i];
				}
			}
			for(i=0;i<3;i++){
				cenPos[t][i] = cenPos[t][i]/n;
			}
		}
		return cenPos;
	}

	public boolean registerBackMain(){
		// is this OK to correct???
		//int reftime = 1 + (Integer.parseInt(ch1.getSelectedItem())); 
		int reftime = (Integer.parseInt(ch1.getSelectedItem())) -1;
		return registerBackMain(reftime);
	}
	
	//uses file dialog
	public boolean registerBackMain(int reftime){
		FileDialog fd = new FileDialog(frm , "Select the imx File" , FileDialog.LOAD);
		fd.setVisible(true);

		String folder = fd.getDirectory();
		String orif = fd.getFile();
		return registerBackMain(folder, orif, reftime);
	}	
	
	/**
	 * moved from ImxRegisterBack
	 * 
	 * @author Tomo
	 * @author Kota
	 * @param folder
	 * @param orif
	 * @param reftime
	 */
	public boolean registerBackMain(String folder, String orif, int reftime){
		
		boolean isNewImaxParser = false;
		boolean useCentroidInMemory = false;
		//prints out the selected directory.
		ouputInfo2(folder + orif + "\n");

		//retrieve user-selected timepoint. 
		
		String refTimepoint = Integer.toString(reftime) ;
		ouputInfo2("Reference Time Index: "+ refTimepoint + "\n");
		ImxParser ip;		
		if (this.ip == null) {
			ip = new ImxParser();
			isNewImaxParser = true;
			this.ip = ip;
			ouputInfo2("Imx Parser newly created." + "\n");
		} else {
			ouputInfo2("Imx Parser found." + "\n");
			ip = this.ip;	
			if (this.ip.rootpath.equals(folder))
				useCentroidInMemory = true;
		}
		
		// === load Imx file and parse information ===
		ip.loadImaxInfo(folder + orif);
		//int nTime = 31;
		int nTime = ip.frames;
		ouputInfo2("Time Points:"+Integer.toString(nTime) + "\n");
		
		// === retrieve Centroid data from either memory or a file in the root. ===
		double cenPos[][];		
		if (!useCentroidInMemory){
			//this file name seems to be pretty static. 
			//the file containing centroid positions. 
			String cenPosF = "cenPos-data.imx.csv";

			// this loads the centroid position file. 
			String cenPosStr = loadImx(folder + cenPosF);

			//splits Imx content by spaces. 
			String[] cenPosRec = splitImxToString(cenPosStr);

			//loads the position file	
			cenPos = loadPos(cenPosRec, nTime);
			ouputInfo2("Centroid file loded, Imx Parser created." + "\n");
		} else {
			cenPos = this.centroids;
			ouputInfo2("Centroid in memory used." + "\n");
		}
		
		// === Evaluate Kinetochore pairs and rename tracks. 
		String evalstate = ip.evaluateTrackPairs(refTimepoint);		
		if (!evalstate.equals("done")){
			ouputInfo2(evalstate + "\n");
			return false;
		} else {
			ArrayList<String> tinfo = ip.trackpairinfo;
			for (String s : tinfo){
				ouputInfo2(s + "\n");
			}
		}

		// === restore original positions (register-back) ===
		// ------ <spots><spot>
		double spotsPos[][] = ip.convertSpotPos();
		//cancels back registration for <spots><spot>
		double registerBackPos[][] = registerBackPos(spotsPos, cenPos);
		ip.setSpotListRegBack(registerBackPos);
				
		//------ <bpTrack><spot>
		double trackspotsPos[][] = ip.convertTrackSpotPos();
		double registerBackTrackPos[][] = registerBackPos(trackspotsPos, cenPos);		
		ip.setTrackSpotListRegBack(registerBackTrackPos);
		
		// === writing update imx file ===
		
		String newfilepath = folder + "registeredback-"+ orif;
		ip.writeUpdatedImx(newfilepath);
		
		// === keep things in the active memory ====
		ip.rootpath = folder;
		this.ip = ip;		
		
		ouputInfo2("\nRegisterdBack File: " + newfilepath + "\n");		
		ouputInfo2("Done.");
		return true;
	}
	void ouputInfo2(String info){
		if ( ta2 != null)
			ta2.append(info);
		else
			System.out.println(info);		
	}
	// a vector between selected pair.  
	static double[] calculateRefAxis(double[] ref1, double[] ref2){
		double[] axis = new double[3];
		for(int j=0; j<3; j++){
			axis[j] = (ref1[j]-ref2[j])/Math.sqrt((ref1[0]-ref2[0])*(ref1[0]-ref2[0])+(ref1[1]-ref2[1])*(ref1[1]-ref2[1])+(ref1[2]-ref2[2])*(ref1[2]-ref2[2]));
		}
		return axis;		
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

	/**
	 * Loads a file and returns the content as a single String object. 
	 * @author Tomo
	 * @param filename
	 * @return
	 */
	private static String loadImx(String filename) {
		String s = null;
		try {
			File f = new File(filename);
			byte[] b = new byte[(int) f.length()];
			FileInputStream fi = new FileInputStream(f);
			fi.read(b);
			s = new String(b);
			fi.close();
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return s;
	}
	/**
	 * Converts a String to an String[] array, separator is " ".
	 * @author Tomo
	 * @param s
	 * @return
	 */
	private static String[] splitImxToString(String s) {
		String[] strrec = s.split(" ");
		return strrec;
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

}
