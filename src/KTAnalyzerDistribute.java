package hoge;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import org.rosuda.JRclient.*;

public class KTAnalyzerDistribute extends WindowAdapter implements ActionListener{
	Frame frm = new Frame("KTAnalyzer");
	static TextArea ta;
	Choice ch1;
	TextField tf1, tf2, tf3;

	public static void main(String args[]) {
		KTAnalyzerDistribute win = new KTAnalyzerDistribute();
	}
	
	
	//center of mass calculator
	static double[][] getCenPos (double[][] pos, int nKTpairs, int nTime){

		double[][] cenPos = new double[2*nKTpairs*nTime][3];
		double[][] sumPos = new double[2*nKTpairs*nTime][3];
		int i;
		int j;
		int t;
		int emptySlot;

		for(i=0;i<2*nKTpairs*nTime;i++){
			for(j=0;j<3;j++){
				sumPos[i][j]=0;
			}
		}

		for(t=0;t<nTime;t++){
			emptySlot=0;
			for(i=0;i<2*nKTpairs;i++){
				if(!(Double.isNaN(pos[t+nTime*i][0]))){
					for(j=0;j<3;j++){
						sumPos[t][j] += pos[t+nTime*i][j];
					}
				}else{
					emptySlot++;
				}
			}
			for(j=0;j<3;j++){
				cenPos[t][j] = sumPos[t][j]/(nKTpairs*2-emptySlot);
			}
		}

		for(t=0;t<nTime;t++){
			for(i=0;i<2*nKTpairs;i++){
				for(j=0;j<3;j++){
					cenPos[t+i*nTime][j]=cenPos[t][j];
				}
			}
		}

		return cenPos;

	}

	static double[] getDisFromCenNucleolus(double[][] pos, int nTime, int nKTpairs, int nKTonNucleolus){
		int iniT=0;
		for(int t=0; t<nTime; t++){
			if(!Double.isNaN(pos[t][0])){
				iniT=t;
				t=nTime;
			}
		}

		double[] cenPosAtIniT = new double[3];

		for(int j=0; j<3; j++){
			for(int i=0; i<2*nKTpairs; i++){
				if(Double.isNaN(pos[iniT+i*nTime][0])){
					System.out.println("Error in getDisFromCenNucleolus");
					return null;
				}else{
					cenPosAtIniT[j] += pos[iniT+i*nTime][j];
				}
			}
			cenPosAtIniT[j] = cenPosAtIniT[j]/(2*nKTpairs);
		}

		double[][] regPosAtIniT = new double[2*nKTpairs][3];
		for(int j=0; j<3; j++){
			for(int i=0; i<2*nKTpairs; i++){
				regPosAtIniT[i][j] = pos[iniT+i*nTime][j]-cenPosAtIniT[j];
			}
		}

		double[] disFromCenAll = new double[2*nKTpairs];
		for(int i=0; i<2*nKTpairs; i++){
			disFromCenAll[i] = Math.sqrt(regPosAtIniT[i][0]*regPosAtIniT[i][0]+regPosAtIniT[i][1]*regPosAtIniT[i][1]+regPosAtIniT[i][2]*regPosAtIniT[i][2]);
		}

		int[] iOfKTonNucleolus = new int[nKTonNucleolus];

		for(int k=0; k<nKTonNucleolus; k++){
			double min = 100000000;
			int iMin=0;
			for(int i=0; i<2*nKTpairs; i++){
				if(disFromCenAll[i]<min){
					min = disFromCenAll[i];
					iMin = i;
				}
			}
			iOfKTonNucleolus[k]=iMin;
			disFromCenAll[iMin]=100000000;
		}

		double[] disFromCenNucleolus = new double[2*nKTpairs*nTime];
		double[][] cenNucleolus = new double[nTime][3];

		for(int t=0; t<nTime; t++){
			double counter=0;
			for(int i=0; i<nKTonNucleolus; i++){
				if(!Double.isNaN(pos[t+iOfKTonNucleolus[i]*nTime][0])){
					for(int j=0; j<3; j++){
						cenNucleolus[t][j] += pos[t+iOfKTonNucleolus[i]*nTime][j];
					}
					counter++;
				}
			}
			for(int j=0; j<3; j++){
				cenNucleolus[t][j] = cenNucleolus[t][j]/counter;
			}
		}



		for(int t=0; t<nTime; t++){
			for(int i=0; i<nKTonNucleolus; i++){
				double[] reg = new double[3];
				for(int j=0; j<3; j++){
					reg[j] = pos[t+iOfKTonNucleolus[i]*nTime][j] - cenNucleolus[t][j];
				}
				disFromCenNucleolus[t+iOfKTonNucleolus[i]*nTime] = Math.sqrt(reg[0]*reg[0]+reg[1]*reg[1]+reg[2]*reg[2]);
			}
		}
		return disFromCenNucleolus;
	}


	static double[] getDisFromCenNuc(double[][] pos, int nTime, int nKTpairs, int nKTonNuc){
		int iniT=0;
		for(int t=0; t<nTime; t++){
			if(!Double.isNaN(pos[t][0])){
				iniT=t;
				t=nTime;
			}
		}

		double[] cenPosAtIniT = new double[3];

		for(int j=0; j<3; j++){
			for(int i=0; i<2*nKTpairs; i++){
				if(Double.isNaN(pos[iniT+i*nTime][0])){
					System.out.println("Error in getDisFromCenNucleolus");
					return null;
				}else{
					cenPosAtIniT[j] += pos[iniT+i*nTime][j];
				}
			}
			cenPosAtIniT[j] = cenPosAtIniT[j]/(2*nKTpairs);
		}

		double[][] regPosAtIniT = new double[2*nKTpairs][3];
		for(int j=0; j<3; j++){
			for(int i=0; i<2*nKTpairs; i++){
				regPosAtIniT[i][j] = pos[iniT+i*nTime][j]-cenPosAtIniT[j];
			}
		}

		double[] disFromCenAll = new double[2*nKTpairs];
		for(int i=0; i<2*nKTpairs; i++){
			disFromCenAll[i] = Math.sqrt(regPosAtIniT[i][0]*regPosAtIniT[i][0]+regPosAtIniT[i][1]*regPosAtIniT[i][1]+regPosAtIniT[i][2]*regPosAtIniT[i][2]);
		}

		int[] iOfKTonNuc = new int[nKTonNuc];

		for(int k=0; k<nKTonNuc; k++){
			double max = 0;
			int iMax=0;
			for(int i=0; i<2*nKTpairs; i++){
				if(disFromCenAll[i]>max){
					max = disFromCenAll[i];
					iMax = i;
				}
			}
			iOfKTonNuc[k]=iMax;
			disFromCenAll[iMax]=0;
		}

		double[] disFromCenNuc = new double[2*nKTpairs*nTime];
		double[][] cenNuc = new double[nTime][3];


		for(int t=0; t<nTime; t++){
			double counter = 0;
			for(int i=0; i<nKTonNuc; i++){
				if(!Double.isNaN(pos[t+iOfKTonNuc[i]*nTime][0])){
					for(int j=0; j<3; j++){
						cenNuc[t][j] += pos[t+iOfKTonNuc[i]*nTime][j];
					}
					counter++;
				}
			}
			for(int j=0; j<3; j++){
				cenNuc[t][j] = cenNuc[t][j]/counter;
			}
		}

		for(int t=0; t<nTime; t++){
			for(int i=0; i<nKTonNuc; i++){
				double[] reg = new double[3];
				for(int j=0; j<3; j++){
					reg[j] = pos[t+iOfKTonNuc[i]*nTime][j] - cenNuc[t][j];
				}
				disFromCenNuc[t+iOfKTonNuc[i]*nTime] = Math.sqrt(reg[0]*reg[0]+reg[1]*reg[1]+reg[2]*reg[2]);
			}
		}
		return disFromCenNuc;
	}

	static double[] getVecArgAtT(double[][] KTt, int nKTpairs, int nTime){

		int c,i,j,k,l;
		int[] blnch = new int[nKTpairs];
		double[] p = new double[3];
		double dis=0;
		double[] vecMax= new double[3];
		double disMax=0;
		boolean bln=true;
		double[] vecArgAtT = new double[3];

		int nCase=1;
		for (i=0;i<KTt.length;i++){
			nCase = 2*nCase;
		}

		for(i = 0; i<KTt.length; i++) {
			blnch[i] = 1;
		}

		for(j=0;j<3;j++){
			p[j]=0;
		}

		for(j=0;j<3;j++){
			vecMax[j] = 0;
		}

		for(c=0;c<nCase/2;c++){
			for (i = 0; i<KTt.length; i++){
				if(!(Double.isNaN(KTt[i][0]))){
					for(j=0;j<3;j++){
						p[j] = p[j] + blnch[i]*KTt[i][j];
					}
				}
			}
			dis = p[0]*p[0]+p[1]*p[1]+p[2]*p[2];

			if (dis>disMax){
				for(j=0;j<3;j++){
					vecMax[j]=p[j];
				}
				disMax=dis;
			}

			for(j=0;j<3;j++){
				p[j]=0;
			}

			k= 0;
			bln =true;
			while (bln){
				if(k==KTt.length-1){
					bln = false;
				}else if (blnch[KTt.length-1-k] == 1){
					blnch[KTt.length-1-k] = -1;
					for (l=0; l<k; l++){
						blnch[KTt.length-1-l] =1;
					}
					bln = false;
				}else{
					k++;
				}
			}
		}

		for(j=0;j<3;j++){
			vecArgAtT[j] = vecMax[j]/Math.sqrt(vecMax[0]*vecMax[0]+vecMax[1]*vecMax[1]+vecMax[2]*vecMax[2]);
		}

		return vecArgAtT;
	}

	 static double[][] getVecArg (double[][] vec, int nKTpairs, int nTime){

		int i,j,t,emptySlot;
		double[][] vecArg = new double[2*nKTpairs*nTime][3];

		for(t = 0; t<nTime; t++){
			System.out.println("processing..t="+t);
			ta.append("processing..t="+t+"\n");
			emptySlot=0;

			for(i=0;i<nKTpairs;i++){
				if(vec[2*i*nTime+t][0]*vec[2*i*nTime+t][0]+vec[2*i*nTime+t][1]*vec[2*i*nTime+t][1]+vec[2*i*nTime+t][2]*vec[2*i*nTime+t][2] == 0 || Double.isNaN(vec[2*i*nTime+t][0])){
					emptySlot++;
				}
			}

			double[][] KTt = new double[nKTpairs-emptySlot][3];
			emptySlot=0;

			for(i=0;i<nKTpairs;i++){
				if(vec[2*i*nTime+t][0]*vec[2*i*nTime+t][0]+vec[2*i*nTime+t][1]*vec[2*i*nTime+t][1]+vec[2*i*nTime+t][2]*vec[2*i*nTime+t][2] == 0 || Double.isNaN(vec[2*i*nTime+t][0])){
					emptySlot++;
				}else{
					for(j=0;j<3;j++){
						KTt[i-emptySlot][j]=vec[2*i*nTime+t][j];
					}
				}
			}

			vecArg[t] = getVecArgAtT(KTt, nKTpairs, nTime);

		}

		double cosT=0;

		for (t = 1; t<nTime; t++) {
			if(!Double.isNaN(vecArg[t-1][0])){
				cosT = (vecArg[t-1][0]*vecArg[t][0]
					+vecArg[t-1][1]*vecArg[t][1]
					+vecArg[t-1][2]*vecArg[t][2])/Math.sqrt(
					(vecArg[t-1][0]*vecArg[t-1][0]
					+ vecArg[t-1][1]*vecArg[t-1][1]
					+ vecArg[t-1][2]*vecArg[t-1][2])*(vecArg[t][0]*vecArg[t][0]
					+ vecArg[t][1]*vecArg[t][1]
					+ vecArg[t][2]*vecArg[t][2])
					);
			}else if(t>2){
				cosT = (vecArg[t-2][0]*vecArg[t][0]
				             					+vecArg[t-2][1]*vecArg[t][1]
				             					+vecArg[t-2][2]*vecArg[t][2])/Math.sqrt(
				             					(vecArg[t-2][0]*vecArg[t-2][0]
				             					+ vecArg[t-2][1]*vecArg[t-2][1]
				             					+ vecArg[t-2][2]*vecArg[t-2][2])*(vecArg[t][0]*vecArg[t][0]
				             					+ vecArg[t][1]*vecArg[t][1]
				             					+ vecArg[t][2]*vecArg[t][2])
				             					);
			}

			if(cosT <0){
				for(j=0;j<3;j++){
					vecArg[t][j] = -1*vecArg[t][j];
				}
			}
		}

		for(i=0;i<nKTpairs*2;i++){
			for(t=0;t<nTime;t++){
				for(j=0;j<3;j++){
					vecArg[t+i*nTime][j]=vecArg[t][j];
				}
			}
		}

		return vecArg;
	}

	static double[][] removeUnstablePlates (double[][] vecArg, int nKTpairs, int nTime, int tAna, double[][] pos){

		int t, j, i;
		double cosT;
		boolean bln = true;

		for(t=tAna; t<nTime; t++){
			for(j=0;j<3;j++){
				vecArg[t][j] = Double.parseDouble("NaN");
			}
		}

		for(t=tAna-1; t>0; t--){
			cosT = (vecArg[t-1][0]*vecArg[t][0]
			             					+vecArg[t-1][1]*vecArg[t][1]
			             					+vecArg[t-1][2]*vecArg[t][2])/Math.sqrt(
			             					(vecArg[t-1][0]*vecArg[t-1][0]
			             					+ vecArg[t-1][1]*vecArg[t-1][1]
			             					+ vecArg[t-1][2]*vecArg[t-1][2])*(vecArg[t][0]*vecArg[t][0]
			             					+ vecArg[t][1]*vecArg[t][1]
			             					+ vecArg[t][2]*vecArg[t][2])
			             					);
			if(Math.acos(cosT)>Math.PI/9 || Double.isNaN(cosT)){
				bln = false;
			}

			if(!bln){
				for(j=0;j<3;j++){
					vecArg[t-1][j] = Double.parseDouble("NaN");
				}
			}
		}


		for(i=0;i<nKTpairs*2;i++){
			for(t=0;t<nTime;t++){
				for(j=0;j<3;j++){
					vecArg[t+i*nTime][j]=vecArg[t][j];
				}
			}
		}


		return vecArg;
	}

	static double[][] removeUndefinedPlates (double[][] vecArg, double[][] weiByTimeVec, double[] stretchedSince, int nKTpairs, int nTime, int tAna, double[][] pos){

		int t, j, i;
		int tBorder =0;

		for(t=tAna; t<nTime; t++){
			for(j=0;j<3;j++){
				vecArg[t][j] = Double.parseDouble("NaN");
			}
		}

		for(t=tAna-1; t>-1; t--){
			int counter = 0;
			for(i=0;i<nKTpairs;i++){
				if(stretchedSince[t+i*2*nTime]>300){

					counter++;
				}
			}
			if(counter<2){
				tBorder = t;
				t=0;
			}
		}
		System.out.println("tBorder = "+tBorder);

		for(t=tBorder; t>-1; t--){
			for(j=0;j<3;j++){
				vecArg[t][j] = Double.parseDouble("NaN");
			}
		}

		for(i=0;i<nKTpairs*2;i++){
			for(t=0;t<nTime;t++){
				for(j=0;j<3;j++){
					vecArg[t+i*nTime][j]=vecArg[t][j];
				}
			}
		}


		return vecArg;
	}



	static double[][] getReversedVecArg(double[][] vec, double[] percInterKTdis, int nKTpairs, int nTime, int tAna){
		double[][] reversedVecArg = new double[2*nKTpairs*nTime][3];
		int i,j,t,emptySlot;
		int[] isBioriented = new int[2*nKTpairs*nTime];
		int[] wasBiorientedT = new int[nKTpairs];

		for(i=0; i<2*nKTpairs*nTime;i++){
			for(j=0;j<3;j++){
				reversedVecArg[i][j] = Double.parseDouble("NaN");
				isBioriented[i] = 0;
			}
		}

		double[][] KTt = new double[nKTpairs][3];
		double[] percInterKTdisT = new double[nKTpairs];


		for(i=0;i<nKTpairs;i++){
			for(j=0;j<3;j++){
				KTt[i][j]=vec[2*i*nTime+tAna-1][j];
			}
			isBioriented[tAna-1+2*i*nTime] = 1;
		}

		reversedVecArg[tAna-1] = getVecArgAtT(KTt, nKTpairs, nTime);


		for(t=tAna-2;t>-1;t--){
			emptySlot=0;

			for(i=0;i<nKTpairs;i++){
				for(j=0;j<3;j++){
					KTt[i][j]=vec[2*i*nTime+t][j];
					percInterKTdisT[i]=percInterKTdis[2*i*nTime+t];
				}
				wasBiorientedT[i] = isBioriented[t+1+2*i*nTime];

			}


			for(i=0;i<nKTpairs;i++){
				if(wasBiorientedT[i]==1){
					double cosT = (KTt[i][0]*reversedVecArg[t+1][0]
					             					+KTt[i][1]*reversedVecArg[t+1][1]
					             					+KTt[i][2]*reversedVecArg[t+1][2])/Math.sqrt(
					             					(KTt[i][0]*KTt[i][0]
					             					+KTt[i][1]*KTt[i][1]
					             					+KTt[i][2]*KTt[i][2])*(reversedVecArg[t+1][0]*reversedVecArg[t+1][0]
					             					+ reversedVecArg[t+1][1]*reversedVecArg[t+1][1]
					             					+ reversedVecArg[t+1][2]*reversedVecArg[t+1][2])
					             					);

					if(cosT>Math.cos(Math.PI/9) && percInterKTdisT[i] > 70){
						isBioriented[t+2*i*nTime] =1;
						for(j=0;j<3;j++){
							KTt[i][j]=KTt[i][j]*(percInterKTdisT[i]-70)/30;
						}
					}else if (cosT<-1*Math.cos(Math.PI/9) && percInterKTdisT[i] > 70){

						isBioriented[t+2*i*nTime] =1;
						for(j=0;j<3;j++){
							KTt[i][j]=-1*KTt[i][j]*(percInterKTdisT[i]-70)/30;
						}

					}else{

						isBioriented[t+2*i*nTime] =0;
						for(j=0;j<3;j++){
							KTt[i][j]=0;
						}
						emptySlot++;
					}

				}else if(wasBiorientedT[i]==0){
					double cosT = (KTt[i][0]*reversedVecArg[t+1][0]
									             					+KTt[i][1]*reversedVecArg[t+1][1]
									             					+KTt[i][2]*reversedVecArg[t+1][2])/Math.sqrt(
									             					(KTt[i][0]*KTt[i][0]
									             					+KTt[i][1]*KTt[i][1]
									             					+KTt[i][2]*KTt[i][2])*(reversedVecArg[t+1][0]*reversedVecArg[t+1][0]
									             					+ reversedVecArg[t+1][1]*reversedVecArg[t+1][1]
									             					+ reversedVecArg[t+1][2]*reversedVecArg[t+1][2])
									             					);
					if(cosT>Math.cos(Math.PI/9) && percInterKTdisT[i] > 70){
						isBioriented[t+2*i*nTime] =1;
						for(j=0;j<3;j++){
							KTt[i][j]=KTt[i][j]*(percInterKTdisT[i]-70)/30;
						}

					}else if (cosT<-1*Math.cos(Math.PI/9) && percInterKTdisT[i] > 70){

						isBioriented[t+2*i*nTime] =1;
						for(j=0;j<3;j++){
							KTt[i][j]=-1*KTt[i][j]*(percInterKTdisT[i]-70)/30;
						}

					}else{
						isBioriented[t+2*i*nTime] =0;
						for(j=0;j<3;j++){
							KTt[i][j]=0;
						}
						emptySlot++;
					}
				}else{
					System.out.println("error in reversedVecArg");
				}


			}



			if(emptySlot==nKTpairs){
			}else{
				for(j=0;j<3;j++){
					reversedVecArg[t][j]=0;
					for(i=0;i<nKTpairs;i++){
						reversedVecArg[t][j] += KTt[i][j];
					}

				}
			}

		}

		for(t=0;t<nTime;t++){
			double d = Math.sqrt(reversedVecArg[t][0]*reversedVecArg[t][0]+reversedVecArg[t][1]*reversedVecArg[t][1]+reversedVecArg[t][2]*reversedVecArg[t][2]);
			for(j=0;j<3;j++){
				reversedVecArg[t][j]=reversedVecArg[t][j]/d;
			}
		}



		for(i=0;i<nKTpairs*2;i++){
			for(t=0;t<nTime;t++){
				for(j=0;j<3;j++){
					reversedVecArg[t+i*nTime][j]=reversedVecArg[t][j];
				}
			}
		}


		return reversedVecArg;
	}

	static double[] isBioriented(double[][] reversedVecArg, double[][] vec, double[] percInterKTdis, int nKTpairs, int nTime, int tAna){
	//	double[][] reversedVecArg = new double[2*nKTpairs*nTime][3];
		int i,j,t,emptySlot;
		double[] isBioriented = new double[2*nKTpairs*nTime];
		double[] wasBiorientedT = new double[nKTpairs];

		for(i=0; i<2*nKTpairs*nTime;i++){
			for(j=0;j<3;j++){
		//		reversedVecArg[i][j] = Double.parseDouble("NaN");
				isBioriented[i] = 0;
			}
		}

		double[][] KTt = new double[nKTpairs][3];
		double[] percInterKTdisT = new double[nKTpairs];


		for(i=0;i<nKTpairs;i++){
			for(j=0;j<3;j++){
				KTt[i][j]=vec[2*i*nTime+tAna-1][j];
			}
			isBioriented[tAna-1+2*i*nTime] = 1;
		}

//		reversedVecArg[tAna-1] = getVecArgAtT(KTt, nKTpairs, nTime);


		for(i=0;i<nKTpairs;i++){
			for(t=tAna;t<nTime;t++){
				isBioriented[t+2*i*nTime] = Double.parseDouble("NaN");
			}
		}


		for(t=tAna-2;t>-1;t--){
			emptySlot=0;

			for(i=0;i<nKTpairs;i++){
				for(j=0;j<3;j++){
					KTt[i][j]=vec[2*i*nTime+t][j];
					percInterKTdisT[i]=percInterKTdis[2*i*nTime+t];
				}
				wasBiorientedT[i] = isBioriented[t+1+2*i*nTime];
			}



			for(i=0;i<nKTpairs;i++){
				if(wasBiorientedT[i]==1){
					double cosT = (KTt[i][0]*reversedVecArg[t+1][0]
					             					+KTt[i][1]*reversedVecArg[t+1][1]
					             					+KTt[i][2]*reversedVecArg[t+1][2])/Math.sqrt(
					             					(KTt[i][0]*KTt[i][0]
					             					+KTt[i][1]*KTt[i][1]
					             					+KTt[i][2]*KTt[i][2])*(reversedVecArg[t+1][0]*reversedVecArg[t+1][0]
					             					+ reversedVecArg[t+1][1]*reversedVecArg[t+1][1]
					             					+ reversedVecArg[t+1][2]*reversedVecArg[t+1][2])
					             					);
					if(cosT>Math.cos(Math.PI/9) && percInterKTdisT[i] > 70){
						isBioriented[t+2*i*nTime] =1;

					}else if (cosT<-1*Math.cos(Math.PI/9) && percInterKTdisT[i] > 70){

						isBioriented[t+2*i*nTime] =1;
						for(j=0;j<3;j++){
							KTt[i][j]=-1*KTt[i][j];
						}

					}else if(Double.isNaN(cosT)){
						isBioriented[t+2*i*nTime] = isBioriented[t+1+2*i*nTime];
						for(j=0;j<3;j++){
							KTt[i][j]=0;
						}
						emptySlot++;
					}else{

						isBioriented[t+2*i*nTime] =0;
						for(j=0;j<3;j++){
							KTt[i][j]=0;
						}
						emptySlot++;
					}


				}else if(wasBiorientedT[i]==0){
					double cosT = (KTt[i][0]*reversedVecArg[t+1][0]
									             					+KTt[i][1]*reversedVecArg[t+1][1]
									             					+KTt[i][2]*reversedVecArg[t+1][2])/Math.sqrt(
									             					(KTt[i][0]*KTt[i][0]
									             					+KTt[i][1]*KTt[i][1]
									             					+KTt[i][2]*KTt[i][2])*(reversedVecArg[t+1][0]*reversedVecArg[t+1][0]
									             					+ reversedVecArg[t+1][1]*reversedVecArg[t+1][1]
									             					+ reversedVecArg[t+1][2]*reversedVecArg[t+1][2])
									             					);
					if(cosT>Math.cos(Math.PI/9) && percInterKTdisT[i] > 70){
						isBioriented[t+2*i*nTime] =1;

					}else if (cosT<-1*Math.cos(Math.PI/9) && percInterKTdisT[i] > 70){

						isBioriented[t+2*i*nTime] =1;
						for(j=0;j<3;j++){
							KTt[i][j]=-1*KTt[i][j];
						}
					}else if(Double.isNaN(cosT)){
						isBioriented[t+2*i*nTime] = isBioriented[t+1+2*i*nTime];
						for(j=0;j<3;j++){
							KTt[i][j]=0;
						}
						emptySlot++;
					}else{
						isBioriented[t+2*i*nTime] =0;
						for(j=0;j<3;j++){
							KTt[i][j]=0;
						}
						emptySlot++;
					}
				}else{
					System.out.println("error in boolean");
				}



			}

		//	if(emptySlot==nKTpairs){
		//	}else{
		//		for(j=0;j<3;j++){
		//			reversedVecArg[t][j]=0;
		//			for(i=0;i<nKTpairs;i++){
		//				reversedVecArg[t][j] += KTt[i][j];
		//			}

		//		}
		//	}


		}


		for(i=0;i<nKTpairs;i++){
			for(t=0;t<nTime;t++){
				for(j=0;j<3;j++){
					isBioriented[t+2*i*nTime+nTime]=isBioriented[t+2*i*nTime];
				}
			}
		}


		return isBioriented;
	}

	static int getTimeOfAnaphase (double[] interKTdis, int nKTpairs, int nTime){
		int tAna=0;

		int i,t;

		double[] dInterKTdisSum = new double[nTime];

		dInterKTdisSum[0]=0;
		for (t=1; t<nTime; t++){
			dInterKTdisSum[t]=0;
			for (i=0; i<nKTpairs; i++){
				if(!(Double.isNaN(interKTdis[t+2*i*nTime]-interKTdis[t-1+2*i*nTime]))){
					dInterKTdisSum[t] += (interKTdis[t+2*i*nTime]-interKTdis[t-1+2*i*nTime]);
				}
			}
		}

		for(t=100;t<nTime;t++){
			if(dInterKTdisSum[t]>5){  //15 for control cells
				tAna=t;
				t=nTime;
			}
		}

		if(tAna==0){
			tAna=nTime-1;
		}

		return tAna;
	}

	// get final interKT distance (mean interkinetochore distance of -25~-5 timepoints from anaphase)
								//(or maximum interkinetochore distance)
	static double[] getFinalInterKTdis(double[] interKTdis, int nKTpairs, int nTime, int timepointOfProphaseEnd){

		int i,t;

		int tAna = getTimeOfAnaphase(interKTdis, nKTpairs, nTime);
		System.out.println("tAna"+tAna);

		double[] finalInterKTdis = new double[2*nKTpairs*nTime];
		int emptySlot;

		for (i=0;i<nKTpairs;i++){
			finalInterKTdis[2*i*nTime]=0;
		}

		for (i=0;i<nKTpairs;i++){
			double max = 0;

			for(t=timepointOfProphaseEnd; t<tAna-5; t++){
				if(interKTdis[t+2*i*nTime]>max){
					max = interKTdis[t+2*i*nTime];
				}
			}
			finalInterKTdis[2*i*nTime] = max;
		}


		/*
		for (i=0;i<nKTpairs;i++){
			emptySlot=0;
			for (t=tAna-25;t<tAna-5;t++){
				if(!(Double.isNaN(interKTdis[t+2*i*nTime]))){
					finalInterKTdis[2*i*nTime] += interKTdis[t+2*i*nTime];
				}else{
					emptySlot++;
				}
			}
			finalInterKTdis[2*i*nTime]=finalInterKTdis[2*i*nTime]/(20-emptySlot);
		}
		*/
		for(i=0;i<nKTpairs;i++){
			for(t=0;t<nTime;t++){
				finalInterKTdis[t+2*i*nTime]=finalInterKTdis[2*i*nTime];
				finalInterKTdis[t+2*i*nTime+nTime]=finalInterKTdis[2*i*nTime];
			}
		}

		return finalInterKTdis;
	}

	// calculate EigenvalueDecomposition
	static EigenvalueDecomposition[] getEd(double[][] pos, int nKT, int nTime){
		int i, t, m, n;
		double[][][] gyr = new double[nTime][3][3];
		int emptySlot;

		for (t=0; t<nTime; t++){
			for (m=0; m<3; m++){
				for (n=0; n<3; n++){
					gyr[t][m][n]=0;
				}
			}

			double[] cenPos = new double[3];

			emptySlot=0;
			for(i=0;i<nKT;i++){
				if(!(Double.isNaN(pos[t+nTime*i][0]))){
					for(int j=0;j<3;j++){
						cenPos[j] += pos[t+nTime*i][j];
					}
				}else{
					emptySlot++;
				}
			}
			for(int j=0;j<3;j++){
				cenPos[j] = cenPos[j]/(nKT-emptySlot);
			}


			emptySlot=0;



			for (m=0; m<3; m++){
				for (n=0; n<3; n++){
					emptySlot=0;
					for (i=0; i<nKT; i++){
						if(!(Double.isNaN(pos[t+nTime*i][0]))){
							gyr[t][m][n]+= (pos[t+i*nTime][m]-cenPos[m])*(pos[t+i*nTime][n]-cenPos[n]);
						}else{
							emptySlot++;
						}

					}
					if(!(emptySlot==nKT))
						gyr[t][m][n] = gyr[t][m][n]/(nKT-emptySlot);

				}
			}
		}

		Matrix[] gyrMtrx = new Matrix[nTime];
		EigenvalueDecomposition[] ed = new EigenvalueDecomposition[nTime];
		for (t=0; t<nTime; t++){
			gyrMtrx[t] = new Matrix(gyr[t]);
			ed[t] = gyrMtrx[t].eig();
		}

		return ed;
	}



	//asymmetry
	static double getAsym(Matrix eigenValMtrx){

		double eigenVal1=Math.sqrt(eigenValMtrx.get(0,0));
		double eigenVal2=Math.sqrt(eigenValMtrx.get(1,1));
		double eigenVal3=Math.sqrt(eigenValMtrx.get(2,2));

		double asym = (
						(eigenVal1*eigenVal1-eigenVal2*eigenVal2)*(eigenVal1*eigenVal1-eigenVal2*eigenVal2)
						+(eigenVal1*eigenVal1-eigenVal3*eigenVal3)*(eigenVal1*eigenVal1-eigenVal3*eigenVal3)
						+(eigenVal2*eigenVal2-eigenVal3*eigenVal3)*(eigenVal2*eigenVal2-eigenVal3*eigenVal3)
			)/(2*(eigenVal1*eigenVal1+eigenVal2*eigenVal2+eigenVal3*eigenVal3)*(eigenVal1*eigenVal1+eigenVal2*eigenVal2+eigenVal3*eigenVal3));

		return asym;
	}

	//use R, get spline smoothing
	static double[] getSpline (double x[], double[] y, int nTime, Rconnection re){
		int t;
		int emptySlot=0;
		double[] latyComp = new double[nTime];
		int startTime=0;
		int endTime=0;

		for(t=0;t<nTime;t++){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
				startTime=t;
				break;
			}
		}

		for(t=nTime-1;t>-1;t--){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
				endTime=t;
				break;
			}
		}


		for(t=0;t<nTime;t++){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
			}else{
				emptySlot++;
			}
		}


		if(emptySlot>nTime-4){
			for(t=0;t<nTime;t++){
				latyComp[t]=Double.parseDouble("NaN");
			}
			return latyComp;

		}else{

			double[] regPosOfKTt = new double[nTime-emptySlot];
			double[] tStampsOfKTt = new double[nTime-emptySlot];

			emptySlot=0;

			for(t=0;t<nTime;t++){
				if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
					tStampsOfKTt[t-emptySlot] = x[t];
					regPosOfKTt[t-emptySlot] = y[t];
				}else{
					emptySlot++;
				}
			}


			try{

				re.assign("t", tStampsOfKTt);
				re.assign("xpos", regPosOfKTt);
				re.assign("predictAt", x);
				RList l = new RList();
				l = re.eval("predict(smooth.spline(x=t,y=xpos), x=predictAt)").asList();


				double[] latx = l.at("x").asDoubleArray();
				double[] laty = l.at("y").asDoubleArray();


				double[] latxComp = new double[nTime];



				emptySlot=0;
				for(t=0;t<nTime;t++){
					if(t<startTime || t>endTime){
						latxComp[t] = Double.parseDouble("NaN");
						latyComp[t] = Double.parseDouble("NaN");
					}else{
						latxComp[t]=latx[t-emptySlot];
						latyComp[t]=laty[t-emptySlot];
					}

				}




			} catch (RSrvException e) {
				if(e.toString().equals("unhandled type: 23")){
				}else{
					e.printStackTrace();
				}
			}

			return latyComp;
		}

	}

	static double[] getLowess(double x[], double[] y, int nTime, Rconnection re, double f){
		int t;
		int emptySlot=0;
		double[] latyComp = new double[nTime];

		for(t=0;t<nTime;t++){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
			}else{
				emptySlot++;
			}
		}
		if(emptySlot>nTime-4){
			for(t=0;t<nTime;t++){
				latyComp[t]=Double.parseDouble("NaN");
			}
			return latyComp;
		}else{
			double[] regPosOfKTt = new double[nTime-emptySlot];
			double[] tStampsOfKTt = new double[nTime-emptySlot];

			emptySlot=0;
			boolean[] emptySlotBln = new boolean[nTime];

			for(t=0;t<nTime;t++){
				if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
					tStampsOfKTt[t-emptySlot] = x[t];
					regPosOfKTt[t-emptySlot] = y[t];
					emptySlotBln[t]=false;
				}else{
					emptySlot++;
					emptySlotBln[t]=true;
				}
			}


			try{

				re.assign("t", tStampsOfKTt);
				re.assign("xpos", regPosOfKTt);
				RList l = new RList();
				l = re.eval("lowess(x=t, y=xpos, f="+f+")").asList();


				double[] latx = l.at("x").asDoubleArray();
				double[] laty = l.at("y").asDoubleArray();


				double[] latxComp = new double[nTime];



				emptySlot=0;
				for(t=0;t<nTime;t++){
					if(emptySlotBln[t]){
						latxComp[t]=Double.parseDouble("NaN");
						latyComp[t]=Double.parseDouble("NaN");
						emptySlot++;
					}else{
						latxComp[t]=latx[t-emptySlot];
						latyComp[t]=laty[t-emptySlot];
					}
				}




			} catch (RSrvException e) {
				if(e.toString().equals("unhandled type: 23")){
				}else{
					e.printStackTrace();
				}
			}

			return latyComp;
		}

	}

	static double[] getSpline_Vel(double x[], double[] y, int nTime, Rconnection re){
		int t;
		int emptySlot=0;
		double[] latyComp=new double[nTime];

		int startTime=0;
		int endTime=0;

		for(t=0;t<nTime;t++){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
				startTime=t;
				break;
			}
		}

		for(t=nTime-1;t>-1;t--){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
				endTime=t;
				break;
			}
		}

		for(t=0;t<nTime;t++){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
			}else{
				emptySlot++;
			}
		}


		double[] regPosOfKTt = new double[nTime-emptySlot];
		double[] tStampsOfKTt = new double[nTime-emptySlot];

		emptySlot=0;

		for(t=0;t<nTime;t++){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
				tStampsOfKTt[t-emptySlot] = x[t];
				regPosOfKTt[t-emptySlot] = y[t];
			}else{
				emptySlot++;
			}
		}


		try{
			re.assign("t", tStampsOfKTt);
			re.assign("xpos", regPosOfKTt);
			re.assign("predictAt", x);
			RList l = new RList();
			l = re.eval("predict(smooth.spline(x=t,y=xpos), x=predictAt, deriv=1)").asList();

			double[] latx = l.at("x").asDoubleArray();
			double[] laty = l.at("y").asDoubleArray();




			double[] latxComp = new double[nTime];
			latyComp = new double[nTime];


			emptySlot=0;
			for(t=0;t<nTime;t++){
				if(t<startTime || t>endTime){
					latxComp[t] = Double.parseDouble("NaN");
					latyComp[t] = Double.parseDouble("NaN");
				}else{
					latxComp[t]=latx[t-emptySlot];
					latyComp[t]=laty[t-emptySlot];
				}

			}

		} catch (RSrvException e) {
			if(e.toString().equals("unhandled type: 23")){
			}else{
				e.printStackTrace();
			}
		}

		return latyComp;
	}


	static double[][] getSpline_txyz (double[] tStamps, double pos[][], int nTime, int nKTpairs, Rconnection re){

		double[][] splinedPos = new double[nTime*nKTpairs*2][3];
		double[] ktPosOverT = new double[nTime];
		double[] tStampsOverT = new double[nTime];
		double[] splinedPosOverT = new double[nTime];

		int i,j,t;
		for(j=0;j<3;j++){
			for(i=0;i<2*nKTpairs;i++){
				for(t=0;t<nTime;t++){
					ktPosOverT[t] = pos[t+i*nTime][j];
					tStampsOverT[t] = tStamps[t];
				}

				splinedPosOverT = getSpline(tStampsOverT, ktPosOverT, nTime, re);
				for(t=0;t<nTime;t++){
					splinedPos[t+i*nTime][j] = splinedPosOverT[t];
				}
			}
		}

		return splinedPos;

	}

	static double[] getSpline_ty(double[] tStamps, double y[], int nTime, int nKTpairs, Rconnection re){
		double[] splinedY = new double[nTime*nKTpairs*2];
		double[] yOverT = new double[nTime];
		double[] tStampsOverT = new double[nTime];
		double[] splinedYOverT = new double[nTime];

		int i,t;

		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				yOverT[t] = y[t+i*nTime];
				tStampsOverT[t] = tStamps[t];
			}

			splinedYOverT = getSpline(tStampsOverT, yOverT, nTime, re);
			for(t=0;t<nTime;t++){
				splinedY[t+i*nTime] = splinedYOverT[t];
			}

		}


		return splinedY;
	}

	static double[] getLowess_ty(double[] tStamps, double y[], int nTime, int nKTpairs, Rconnection re, double f){
		double[] lowessedY = new double[nTime*nKTpairs*2];
		double[] yOverT = new double[nTime];
		double[] tStampsOverT = new double[nTime];
		double[] lowessedYOverT = new double[nTime];

		int i,t;

		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				yOverT[t] = y[t+i*nTime];
				tStampsOverT[t] = tStamps[t];
			}

			lowessedYOverT = getLowess(tStampsOverT, yOverT, nTime, re, f);
			for(t=0;t<nTime;t++){
				lowessedY[t+i*nTime] = lowessedYOverT[t];
			}

		}


		return lowessedY;
	}



	static double[][] getSplinedVel_txyz (double[] tStamps, double pos[][], int nTime, int nKTpairs, Rconnection re){

		double[][] splinedPos = new double[nTime*nKTpairs*2][3];
		double[] ktPosOverT = new double[nTime];
		double[] tStampsOverT = new double[nTime];
		double[] splinedPosOverT = new double[nTime];

		int i,j,t;
		for(j=0;j<3;j++){
			for(i=0;i<2*nKTpairs;i++){
				for(t=0;t<nTime;t++){
					ktPosOverT[t] = pos[t+i*nTime][j];
					tStampsOverT[t] = tStamps[t];
				}

				splinedPosOverT = getSpline_Vel(tStampsOverT, ktPosOverT, nTime, re);
				for(t=0;t<nTime;t++){
					splinedPos[t+i*nTime][j] = splinedPosOverT[t];
				}
			}
		}

		return splinedPos;

	}

	static double[][] getLowess_txyz(double[] tStamps, double pos[][], int nTime, int nKTpairs, Rconnection re, double f){
		double[][] lowessedPos = new double[nTime*nKTpairs*2][3];
		double[] posOverT = new double[nTime];
		double[] tStampsOverT = new double[nTime];
		double[] lowessedPosOverT = new double[nTime];

		int i,j,t;

		for(j=0;j<3;j++){
			for(i=0;i<2*nKTpairs;i++){
				for(t=0;t<nTime;t++){
					posOverT[t] = pos[t+i*nTime][j];
					tStampsOverT[t] = tStamps[t];
				}

				lowessedPosOverT = getLowess(tStampsOverT, posOverT, nTime, re, f);
				for(t=0;t<nTime;t++){
					lowessedPos[t+i*nTime][j] = lowessedPosOverT[t];
				}
			}
		}

		return lowessedPos;

	}

	static double[][] getRunningAverage_txyz(double[] tStamps, double pos[][], int nTime, int nKTpairs, Rconnection re, double tWind){
		double[][] averagedPos = new double[nTime*nKTpairs*2][3];

		int i,j,t;


		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				double counter=0;
				for(int tt=0; tt<nTime; tt++){
					if(Math.abs(tStamps[t]-tStamps[tt]) < tWind){
						if(!Double.isNaN(pos[tt+i*nTime][0])){
							for(j=0;j<3;j++){
								averagedPos[t+i*nTime][j] += pos[tt+i*nTime][j];
							}
							counter++;
						}

					}
				}
				for(j=0;j<3;j++){
					averagedPos[t+i*nTime][j] = averagedPos[t+i*nTime][j]/counter;
				}
			}


			for(t=0;t<nTime;t++){
				averagedPos[t+i*nTime] = averagedPos[t];
			}
		}


		return averagedPos;

	}

	static double[] getSplinedVel_ty(double[] tStamps, double y[], int nTime, int nKTpairs, Rconnection re){
		double[] splinedY = new double[nTime*nKTpairs*2];
		double[] yOverT = new double[nTime];
		double[] tStampsOverT = new double[nTime];
		double[] splinedYOverT = new double[nTime];

		int i,t;

		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				yOverT[t] = y[t+i*nTime];
				tStampsOverT[t] = tStamps[t];
			}

			splinedYOverT = getSpline_Vel(tStampsOverT, yOverT, nTime, re);

			for(t=0;t<nTime;t++){
				splinedY[t+i*nTime] = splinedYOverT[t];
			}

		}


		return splinedY;
	}

	static double[][] getNeiVec(double[][] normVec, int nKTpairs, int nTime, double[] percInterKTdisSplined){
		double[][] neiVec = new double[2*nKTpairs*nTime][3];
		int i,ii,t,j,counter;
		double cosT=0;

		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				if(percInterKTdisSplined[t+2*i*nTime]>70){
					for(j=0;j<3;j++){
						neiVec[t+2*i*nTime][j]=normVec[t+2*i*nTime][j];
						neiVec[t+2*i*nTime+nTime][j]=normVec[t+2*i*nTime+nTime][j];
					}
				}
			}
		}

		for (t=0; t<nTime; t++){
			for (i=0; i<nKTpairs; i++){
				counter = 0;
				for (ii=0; ii<nKTpairs; ii++){
					cosT = (neiVec[t+2*i*nTime][0]*neiVec[t+2*ii*nTime][0]
					        +neiVec[t+2*i*nTime][1]*neiVec[t+2*ii*nTime][1]
							+neiVec[t+2*i*nTime][2]*neiVec[t+2*ii*nTime][2])/Math.sqrt(
							(neiVec[t+2*i*nTime][0]*neiVec[t+2*i*nTime][0]
							+ neiVec[t+2*i*nTime][1]*neiVec[t+2*i*nTime][1]
							+ neiVec[t+2*i*nTime][2]*neiVec[t+2*i*nTime][2])*(neiVec[t+2*ii*nTime][0]*neiVec[t+2*ii*nTime][0]
							+ neiVec[t+2*ii*nTime][1]*neiVec[t+2*ii*nTime][1]
							+ neiVec[t+2*ii*nTime][2]*neiVec[t+2*ii*nTime][2])
							);
					if(cosT<0) cosT = -1*cosT;

					if(Math.acos(cosT)<Math.PI/9){
						counter++;
					}
				}
				for (j=0; j<3; j++){
					neiVec[t+2*i*nTime][j] = counter*neiVec[t+2*i*nTime][j];
				}
			}
		}

		return neiVec;
	}

	static double[] getNumberOfClusters(double[][] pos, int nKTpairs, int nTime, double thr){
		double[] nCluster = new double[2*nKTpairs*nTime];
		double[] clusterGroup = new double[2*nKTpairs];

		for(int t=0; t<nTime; t++){
			for(int i=0; i<2*nKTpairs; i++){
				clusterGroup[i] = i;
			}
			for(int i=0; i<2*nKTpairs; i++){
				if(Double.isNaN(pos[t+i*nTime][0])){
					clusterGroup[i] = Double.parseDouble("NaN");
				}
				for(int j=i+1; j<2*nKTpairs; j++){
					double d = Math.sqrt((pos[t+i*nTime][0]-pos[t+j*nTime][0])*(pos[t+i*nTime][0]-pos[t+j*nTime][0])+(pos[t+i*nTime][1]-pos[t+j*nTime][1])*(pos[t+i*nTime][1]-pos[t+j*nTime][1])+(pos[t+i*nTime][2]-pos[t+j*nTime][2])*(pos[t+i*nTime][2]-pos[t+j*nTime][2]));
					if(d<thr){
						if(clusterGroup[i]<clusterGroup[j]){
							clusterGroup[j] = clusterGroup[i];
						}else if(clusterGroup[i] == clusterGroup[j]){
						}else{
							clusterGroup[i] = clusterGroup[j];
							j = 2*nKTpairs-1;
							i = -1;
						}
					}
				}
			}

			nCluster[t]=0;
			int i=0;
			while(i<2*nKTpairs){
				for(int j=0; j<2*nKTpairs; j++){
					if(clusterGroup[j] == (double)i){
						nCluster[t] = nCluster[t]+1;
						j=2*nKTpairs;
					}
				}
				i = i+1;
			}
		}

		for(int i=0;i<2*nKTpairs;i++){
			for(int t=0;t<nTime;t++){
				nCluster[t+nTime*i]=nCluster[t];
			}
		}

		return nCluster;
	}

	static int getTimeOfMetaphase(double[] numOfStretched, int tAna, int i) {
		int tMeta=0;
		for(int t=tAna; t>0; t--){
			if(numOfStretched[t]<i){
				tMeta = t+1;
				t=0;
			}
		}
		return tMeta;
	}

	static int getTimeOfMetaphaseStable(double[] numOfEstablished, int nTime, int i) {
		int tMetaStable=0;
		for(int t=0; t<nTime; t++){
			if(numOfEstablished[t]>=i){
				tMetaStable = t;
				break;
			}
		}
		return tMetaStable;
	}


	static int getTimeOfOrientedMovement(double[] ratio) {
		int tOM=0;
		for(int t=0; t<ratio.length; t++){
			if(ratio[t]>3){
				tOM = t;
				t=ratio.length;
			}
		}
		return tOM;
	}

	static double[][] getVecArgPredictedByMeta(double[][] vecArg, int tMeta, int nKTpairs, int nTime){
		double[] vecMeta = new double[3];

		double[][] vecArg1 = new double[nKTpairs*nTime*2][3];

		for(int j=0; j<3; j++){
			double sum =0;
			int counter =0;
			for(int t=tMeta+1; t<tMeta+11;t++){
				if(!Double.isNaN(vecArg[t][j])){
					sum = sum+vecArg[t][j];
					counter++;
				}
				vecMeta[j] = sum/counter;
			}
		}

		for(int t=0; t<tMeta; t++){
			for(int j=0; j<3; j++){
				vecArg1[t][j] = vecMeta[j];
			}
		}

		for(int t=tMeta; t<nTime; t++){
			for(int j=0; j<3; j++){
				vecArg1[t][j] = vecArg[t][j];
			}
		}


		for(int i=0; i<2*nKTpairs; i++){
			for(int t=0; t<nTime; t++){
				vecArg1[t+i*nTime] = vecArg1[t];
			}
		}

		return vecArg1;
	}

	static double[][] getVecArgByMovementOrientation(double[][] vec, int tMovOri, int nKTpairs, int nTime){
		double[] vecMeta = new double[3];
		double[][] vecArg1 = new double[nKTpairs*nTime*2][3];


		for(int j=0; j<3; j++){
			double sum =0;
			int counter =0;
			for(int t=tMovOri+1; t<tMovOri+11;t++){
				if(!Double.isNaN(vec[t][j])){
					sum = sum+vec[t][j];
					counter++;
				}
				vecMeta[j] = sum/counter;
			}
		}

		for(int t=0; t<tMovOri; t++){
			for(int j=0; j<3; j++){
				vecArg1[t][j] = vecMeta[j];
			}
		}

		for(int t=tMovOri; t<nTime; t++){
			for(int j=0; j<3; j++){
				vecArg1[t][j] = vec[t][j];
			}
		}


		for(int i=0; i<2*nKTpairs; i++){
			for(int t=0; t<nTime; t++){
				vecArg1[t+i*nTime] = vecArg1[t];
			}
		}

		return vecArg1;
	}


	static double[][] getVecArgRemovedBeforeMeta(double[][] vecArg, int tMeta, int nKTpairs, int nTime){

		double[][] vecArg1 = new double[nKTpairs*nTime*2][3];

		for(int t=0; t<tMeta; t++){
			for(int j=0; j<3; j++){
				vecArg1[t][j] = Double.parseDouble("NaN");
			}
		}

		for(int t=tMeta; t<nTime; t++){
			for(int j=0; j<3; j++){
				vecArg1[t][j] = vecArg[t][j];
			}
		}


		for(int i=0; i<2*nKTpairs; i++){
			for(int t=0; t<nTime; t++){
				vecArg1[t+i*nTime] = vecArg1[t];
			}
		}

		return vecArg1;
	}

	static double getSTDEV(double[] array){
		double m=0;
		int counter=0;
		for(int i=0; i<array.length; i++){
			if(!Double.isNaN(array[i])){
				m = m + array[i];
				counter++;
			}
		}
		m = m/counter;

		double sum=0;

		for(int i=0; i<array.length; i++){
			if(!Double.isNaN(array[i])){
				sum += (array[i]-m)*(array[i]-m);
			}
		}

		double stdev=0;
		if(counter==0 || counter==1){
			stdev = Double.parseDouble("NaN");
			return stdev;
		}else{
			stdev = Math.sqrt(sum/(counter-1));
			return stdev;
		}
	}

	static double getAverage(double[] array){
		double m=0;
		int counter=0;
		for(int i=0; i<array.length; i++){
			if(!Double.isNaN(array[i])){
				m = m + array[i];
				counter++;
			}
		}
		if(counter==0){
			m = Double.parseDouble("NaN");
			return m;
		}else{
			m = m/counter;
			return m;
		}
	}


	static double[] getPredicted(double[] x, double[] y, int nTime, double[] predictTime, Rconnection re){
		int t;
		int emptySlot=0;

		for(t=0;t<nTime;t++){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
			}else{
				emptySlot++;
			}
		}

		double[] yy = new double[nTime-emptySlot];
		double[] xx = new double[nTime-emptySlot];

		emptySlot=0;
		boolean[] emptySlotBln = new boolean[nTime];

		for(t=0;t<nTime;t++){
			if(!Double.isNaN(x[t]) && !Double.isNaN(y[t])){
				xx[t-emptySlot] = x[t];
				yy[t-emptySlot] = y[t];
				emptySlotBln[t]=false;
			}else{
				emptySlot++;
				emptySlotBln[t]=true;
			}
		}

		try{
			re.assign("t", xx);
			re.assign("xpos", yy);
			re.assign("predictTime", predictTime);
			RList l = new RList();
			l = re.eval("predict(smooth.spline(x=t,y=xpos),x=predictTime)").asList();
			double[] laty = l.at("y").asDoubleArray();
			return laty;

		}catch (RSrvException e) {
			e.printStackTrace();
			return null;
		}
	}



	static double[] getPredicted_ty(double[] tStamps, double[] y, int nTime, int nKTpairs, double[] predictTime, Rconnection re) {
		double[] predictedY = new double[predictTime.length*nKTpairs*2];
		double[] yOverT = new double[nTime];
		double[] tStampsOverT = new double[nTime];
		double[] predictedYOverT = new double[nTime];

		int i,t;
		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				yOverT[t] = y[t+i*nTime];
				tStampsOverT[t] = tStamps[t];
			}

			predictedYOverT = getPredicted(tStampsOverT, yOverT, nTime, predictTime, re);
			for(t=0;t<predictTime.length;t++){
				predictedY[t+i*predictTime.length] = predictedYOverT[t];
			}
		}


		return predictedY;

	}

	static double[][] getPredicted_txyz(double[] tStamps, double[][] pos, int nTime, int nKTpairs, double[] predictTime, Rconnection re) {
		double[][] predictedPos = new double[predictTime.length*nKTpairs*2][3];
		double[] ktPosOverT = new double[nTime];
		double[] tStampsOverT = new double[nTime];
		double[] predictedPosOverT = new double[nTime];

		int i,j,t;
		for(j=0;j<3;j++){
			for(i=0;i<2*nKTpairs;i++){
				for(t=0;t<nTime;t++){
					ktPosOverT[t] = pos[t+i*nTime][j];
					tStampsOverT[t] = tStamps[t];
				}

				predictedPosOverT = getPredicted(tStampsOverT, ktPosOverT, nTime, predictTime, re);
				for(t=0;t<predictTime.length;t++){
					predictedPos[t+i*predictTime.length][j] = predictedPosOverT[t];
				}
			}
		}

		return predictedPos;

	}

	static double[] makePredictTime(double startTime, double endTime, double tInterval){
		int t;
		int nT = (int)((endTime-startTime)/tInterval);
		double[] predictTime = new double[nT];

		for(t=0;t<nT;t++){
			predictTime[t] = startTime+t*tInterval;
		}
		return predictTime;
	}

	static double[][] getVecArgPlusChrEigAxis(double[][] vecArg, double[][] chrEigenVec, int tMeta, int tMetaPlate, int nKTpairs, int nTime) {

		double[][] createdVecArg = new double[nKTpairs*nTime*2][3];
		double[] vecMetaPlate = new double[3];

		for(int j=0; j<3; j++){
			double sum =0;
			int counter =0;
			for(int t=tMetaPlate+1; t<tMetaPlate+11;t++){
				if(!Double.isNaN(vecArg[t][j])){
					sum = sum+vecArg[t][j];
					counter++;
				}
				vecMetaPlate[j] = sum/counter;
			}
		}

		for(int t=0; t<tMetaPlate; t++){
			for(int j=0; j<3; j++){
				createdVecArg[t][j] = vecMetaPlate[j];
			}
		}

		for(int t=tMetaPlate; t<tMeta; t++){
			for(int j=0; j<3; j++){
				createdVecArg[t][j] = chrEigenVec[t][j];
			}
		}

		for(int t=tMeta; t<nTime; t++){
			for(int j=0; j<3; j++){
				createdVecArg[t][j] = vecArg[t][j];
			}
		}

		for(int i=0; i<2*nKTpairs; i++){
			for(int t=0; t<nTime; t++){
				createdVecArg[t+i*nTime] = createdVecArg[t];
			}
		}

		return createdVecArg;
	}


	static int getTimeOfMetaphasePlate(double[] aRatioChrEig, int timepointOfProphaseEnd, double thr, int nTime) {
		int p = 0;

		for(int t=timepointOfProphaseEnd; t<nTime; t++){
			if(aRatioChrEig[t]<thr){
				p = t;
				t = nTime;
			}
		}

		return p;
	}

	static double[][] correctFlips(double[][] axis){
		double cosT=0;

		for (int t = 1; t<axis.length; t++) {
			if(!Double.isNaN(axis[t-1][0])){
				cosT = (axis[t-1][0]*axis[t][0]
					+axis[t-1][1]*axis[t][1]
					+axis[t-1][2]*axis[t][2])/Math.sqrt(
					(axis[t-1][0]*axis[t-1][0]
					+ axis[t-1][1]*axis[t-1][1]
					+ axis[t-1][2]*axis[t-1][2])*(axis[t][0]*axis[t][0]
					+ axis[t][1]*axis[t][1]
					+ axis[t][2]*axis[t][2])
					);
			}else if(t>2 && !Double.isNaN(axis[t-2][0])){
				cosT = (axis[t-2][0]*axis[t][0]
				             					+axis[t-2][1]*axis[t][1]
				             					+axis[t-2][2]*axis[t][2])/Math.sqrt(
				             					(axis[t-2][0]*axis[t-2][0]
				             					+ axis[t-2][1]*axis[t-2][1]
				             					+ axis[t-2][2]*axis[t-2][2])*(axis[t][0]*axis[t][0]
				             					+ axis[t][1]*axis[t][1]
				             					+ axis[t][2]*axis[t][2])
				             					);
			}else if(t>3 && !Double.isNaN(axis[t-3][0])){
				cosT = (axis[t-3][0]*axis[t][0]
				             					+axis[t-3][1]*axis[t][1]
				             					+axis[t-3][2]*axis[t][2])/Math.sqrt(
				             					(axis[t-3][0]*axis[t-3][0]
				             					+ axis[t-3][1]*axis[t-3][1]
				             					+ axis[t-3][2]*axis[t-3][2])*(axis[t][0]*axis[t][0]
				             					+ axis[t][1]*axis[t][1]
				             					+ axis[t][2]*axis[t][2])
				             					);
			}else if(t>4 && !Double.isNaN(axis[t-4][0])){
				cosT = (axis[t-4][0]*axis[t][0]
				             					+axis[t-4][1]*axis[t][1]
				             					+axis[t-4][2]*axis[t][2])/Math.sqrt(
				             					(axis[t-4][0]*axis[t-4][0]
				             					+ axis[t-4][1]*axis[t-4][1]
				             					+ axis[t-4][2]*axis[t-4][2])*(axis[t][0]*axis[t][0]
				             					+ axis[t][1]*axis[t][1]
				             					+ axis[t][2]*axis[t][2])
				             					);
			}

			if(cosT <0){
				for(int j=0;j<3;j++){
					axis[t][j] = -1*axis[t][j];
				}
			}
		}

		return axis;
	}

	static double[][] getRotatedPos(double[][] axis, double[][] regPos, int nTime, int nKTpairs){
		double[][] rotPos = new double[2*nKTpairs*nTime][3];
		double[] x2 = new double[2*nKTpairs];
		double[] y2=  new double[2*nKTpairs];
		double[] z2=  new double[2*nKTpairs];
		for(int t=0; t<nTime; t++){
			double rotY;
			double rotZ;
			double ax = axis[t][0];
			double ay = axis[t][1];
			double az = axis[t][2];
			if(az<0){
				rotY = Math.acos(ax/Math.sqrt(ax*ax+az*az));
			}else{
				rotY = -1*Math.acos(ax/Math.sqrt(ax*ax+az*az));
			}
			if(ay<0){
				rotZ = Math.acos(Math.sqrt(ax*ax+az*az)/Math.sqrt(ax*ax+ay*ay+az*az));
			}else{
				rotZ = -1*Math.acos(Math.sqrt(ax*ax+az*az)/Math.sqrt(ax*ax+ay*ay+az*az));
			}

			for(int i=0; i<2*nKTpairs; i++){
				double x = regPos[t+i*nTime][0];
				double y = regPos[t+i*nTime][1];
				double z = regPos[t+i*nTime][2];

				double x1 = x*Math.cos(rotY) - z*Math.sin(rotY);
				double z1 = x*Math.sin(rotY) + z*Math.cos(rotY);
				double y1 = y;

				x2[i] = x1*Math.cos(rotZ) - y1*Math.sin(rotZ);
				y2[i] = x1*Math.sin(rotZ) + y1*Math.cos(rotZ);
				z2[i] = z1;

				rotPos[t+i*nTime][0] = x2[i];
				rotPos[t+i*nTime][1] = y2[i];
				rotPos[t+i*nTime][2] = z2[i];

			}

			/*if(t!=0){
				double d=0;
				double minD = Double.MAX_VALUE;
				double minTheta=0;
				for(double theta=0; theta<360; theta++){
					d=0;
					for(int i=0; i<2*nKTpairs; i++){
						double y3 = y2[i]*Math.cos(theta/180.0*Math.PI) - z2[i]*Math.sin(theta/180.0*Math.PI);
						double z3 = y2[i]*Math.sin(theta/180.0*Math.PI) + z2[i]*Math.cos(theta/180.0*Math.PI);

						double yp = rotPos[t-1+i*nTime][1];
						double zp = rotPos[t-1+i*nTime][2];

						d += Math.sqrt((y3 - yp)*(y3 - yp)
								+ (z3 - zp)*(z3 - zp));
					}
					if(d<minD){
						minD=d;
						minTheta=theta;
					}
					//  x = xcos-ysin
					//  y = xsin+ycos
				}
				System.out.println(minTheta);
				for(int i=0; i<2*nKTpairs; i++){
					rotPos[t+i*nTime][0] = x2[i];
					rotPos[t+i*nTime][1] = y2[i]*Math.cos(minTheta/180.0*Math.PI) - z2[i]*Math.sin(minTheta/180.0*Math.PI);
					rotPos[t+i*nTime][2] = y2[i]*Math.sin(minTheta/180.0*Math.PI) + z2[i]*Math.cos(minTheta/180.0*Math.PI);

				}
			}*/
		}
		return rotPos;
	}

	static double[][] getVecArgByFixedAxis(double x, double y, double z, int nKTpairs, int nTime){
		double[][] vecArg1 = new double[nKTpairs*nTime*2][3];

		for(int t=0; t<nTime; t++){
			vecArg1[t][0] = x;
			vecArg1[t][1] = y;
			vecArg1[t][2] = z;
			for(int i =0; i<2*nKTpairs; i++){
				vecArg1[t+i*nKTpairs] = vecArg1[t];
			}
		}

		return vecArg1;


	}

	static ArrayList<Double> getRunningAverageWithTimeWindow(
			double[] tStamps, double[] value, double timeWindow, int nTime, int tMeta) {

		ArrayList<Double> valueFromMeta = new ArrayList<Double>();

		int block = (int)Math.floor((tStamps[0] - tStamps[tMeta])/(timeWindow));
		int preBlock = block;
		double v=0;
		int counter=0;

		for(int t=0; t<nTime; t++){
			block = (int)Math.floor((tStamps[t] - tStamps[tMeta])/(timeWindow));
			if(block == preBlock){
				if(!Double.isNaN(value[t])){
					v += value[t];
					counter++;
				}
			}else if(counter!=0){
				valueFromMeta.add(v/(double)counter);
				t=t-1;
				preBlock = block;
				v=0;
				counter=0;
			}else if(counter==0){
				valueFromMeta.add(Double.parseDouble("NaN"));
				t=t-1;
				preBlock = block;
				v=0;
				counter=0;
			}


		}

		return valueFromMeta;
	}

	static ArrayList<Double> getRunningAverageWithTimeWindowTStamps(
			double[] tStamps, double timeWindow, int nTime, int tMeta) {

		ArrayList<Double> tStampFromMeta = new ArrayList<Double>();

		int block = (int)Math.floor((tStamps[0] - tStamps[tMeta])/(timeWindow));
		int preBlock = block;

		for(int t=0; t<nTime; t++){
			block = (int)Math.floor((tStamps[t] - tStamps[tMeta])/(timeWindow));
			if(block == preBlock){
			}else{
				tStampFromMeta.add((double)preBlock*timeWindow+0.5*timeWindow);
				t=t-1;
				preBlock = block;
			}
		}

		return tStampFromMeta;
	}

	public KTAnalyzerDistribute() {
			frm.setSize(600 , 600);
			frm.setLayout(new FlowLayout());
			
			Label lb1 = new Label();
			lb1.setText("Analyze from con");
			frm.add(lb1);
			
			tf1 = new TextField();
			tf1.setText("1");
			frm.add(tf1);
			
			Label lb2 = new Label("to con");
			frm.add(lb2);
			
			tf2 = new TextField();
			tf2.setText("12");
			frm.add(tf2);
			
			Label lb5 = new Label(".txt");
			frm.add(lb5);
			
			Label lb3 = new Label("Algorithm for spindle axis estimation");
			frm.add(lb3);
			
			ch1 = new Choice();
			ch1.add("By interKT axis");
			ch1.add("By chromosome arrangement");
			frm.add(ch1);
			
			Label lb4 = new Label("For spindle axis estimation, ignore timepoints before");
			frm.add(lb4);
			
			tf3 = new TextField();
			tf3.setText("0");
			frm.add(tf3);
			
			Button load = (Button)frm.add(new Button("Load con.txt files"));
			load.addActionListener(this);

			ta = (TextArea)frm.add(new TextArea());
			ta.setSize(300 , 400);

			frm.setVisible(true);
			frm.addWindowListener(this);
	}
	
	public void actionPerformed(ActionEvent e) {
		FileDialog fd = new FileDialog(frm , "Select one of the \"con\" files" , FileDialog.LOAD);
		fd.setVisible(true);

		ta.setText(fd.getDirectory() + fd.getFile() + "\n");

		String folder = fd.getDirectory();

		int i,j,t;
		int nKTpairs=20;
		int nTimePF=31;
		int startFile = Integer.parseInt(tf1.getText());
		int endFile = Integer.parseInt(tf2.getText());
		int nTime = (endFile-startFile+1)*(nTimePF-1);
		String tStampFile = "timestamps.txt";
		int vecArgMode;
		if(ch1.getSelectedIndex()==0)
			vecArgMode=1;
		else if(ch1.getSelectedIndex()==1)
			vecArgMode=5;
		else
			vecArgMode=1;
		
		// 0 -> remove before metaphase, 1 -> fill before metaphase by early metaphase plate
							// 2 -> combination of average KT orientation and chromosome eigen values
							// 3 -> input fixed values manually
							// 4 -> by movement orientation
							// 5 -> by chrEigenValue

		int timepointOfProphaseEnd= Integer.parseInt(tf3.getText());
		
		double tInterval =1; // predict Time interval
		//double timeWindow = 300; // timeWindowAnalyzer, length of time window (sec)

		int nLines =0;
		Vector rec = null;
		double[][] pos = null;

		//load data
		System.out.println("data");
		ta.append("data\n");
		pos = CsvMultiConAssemble.read(folder, nTimePF, nKTpairs, startFile, endFile);
		System.out.println("data loaded");
		ta.append("data loaded\n");
		nLines = pos.length;
		if(nLines!=nTime*nKTpairs*2){
			System.out.println("!Error!");
			ta.append("!Error!\n");
		}
		

		// load timestamps into tStamps

		Vector rec1 = Csv.read(folder+tStampFile);
		if (rec1 == null){
			System.out.print("nullfile-timestapms");
			ta.append("nullfile-timestapms\n");
			return;
		}

		double[] tStamps = new double[nLines];


		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				String[] cell = (String[])rec1.elementAt(t+1);
				tStamps[t+nTime*i]=Double.parseDouble(cell[0]);
			}
		}

		System.out.println("timestamp loaded");
		ta.append("timestamp loaded\n");
		
		int nOfUndetected=0;

		for(t=0; t<nTime; t++){
			for(i=0; i<2*nKTpairs; i++){
				if(Double.isNaN(pos[t+i*nTime][0])){
					nOfUndetected++;
					double[] posPt = new double[4];
					double[] posAt = new double[4];
					int pt=0;
					int at=0;

					for(j=0; j<4; j++){
						posPt[j] = Double.parseDouble("NaN");
						posAt[j] = Double.parseDouble("NaN");
					}

					for(pt=0; t-pt>0; pt++){
						if(!Double.isNaN(pos[t-pt+i*nTime][0])){
							posPt = pos[t-pt+i*nTime];
							break;
						}
					}

					for(at=0; t+at<nTime; at++){
						if(!Double.isNaN(pos[t+at+i*nTime][0])){
							posAt = pos[t+at+i*nTime];
							break;
						}
					}

					double[] visibleKT = new double[2*nKTpairs];

					if(t-pt!=0 && t+at!=nTime){
						for(int tt=0; tt<pt+at+1; tt++){
							for(int ii=0; ii<2*nKTpairs; ii++){
								if(!Double.isNaN(visibleKT[ii])){
									if(Double.isNaN(pos[t-pt+tt+ii*nTime][0])){
										visibleKT[ii] = Double.parseDouble("NaN");
									}
								}
							}
						}

						double[][] cenVisible = new double[pt+at+1][3];

						for(int tt=0; tt<pt+at+1; tt++){
							double counter=0;
							for(int ii=0; ii<2*nKTpairs; ii++){
								if(!Double.isNaN(visibleKT[ii])){
									for(j=0; j<3; j++){
										cenVisible[tt][j] += pos[t-pt+tt+ii*nTime][j];
									}
									counter++;
								}
							}

							for(j=0; j<3; j++){
								if(counter==0){
									cenVisible[tt][j] = Double.parseDouble("NaN");
								}else{
									cenVisible[tt][j] = cenVisible[tt][j]/counter;
								}
							}
						}

						for(int tt=1; tt<pt+at; tt++){
							for(j=0; j<3; j++){
								pos[t-pt+tt+i*nTime][j] = cenVisible[tt][j] + ((pos[t-pt+i*nTime][j]-cenVisible[0][j])*(double)(pt+at-tt)+(pos[t+at+i*nTime][j]-cenVisible[pt+at][j])*(double)tt)/(double)(pt+at);
							}
							pos[t-pt+tt+i*nTime][3] = Double.parseDouble("NaN");
						}
					}
				}
			}
		}

		System.out.println("Number of undetected kinetochores; "+nOfUndetected);
		ta.append("Number of undetected kinetochores; "+nOfUndetected+"\n");
		//calculate number of clusters

		double[] nCluster = new double[nLines];
		nCluster = getNumberOfClusters(pos, nKTpairs, nTime, 1);


		//calculate center of Mass

		double[][] cenPos = new double[nLines][3];
		cenPos = getCenPos(pos, nKTpairs, nTime);

		//calculate Normalized positions of KTs (relative to center of Mass position)

		double[][] regPos = new double[nLines][3];

		for(i=0;i<nKTpairs*2;i++){
			for(t=0;t<nTime;t++){
				for(j=0;j<3;j++){
					regPos[t+i*nTime][j] = pos[t+i*nTime][j]*pos[t+i*nTime][3] - cenPos[t][j];
				}
			}
		}

		// spline smoothing of kt positions

		double[][] splinedPos = new double[nTime*nKTpairs*2][3];
		//Rengine re = new Rengine(new String[]{"--no-save"}, false, null);
		Rconnection re = null;
		try{
			re=new Rconnection();
		} catch (RSrvException ee) {
			if(ee.toString().equals("unhandled type: 23")){
			}else{
				ee.printStackTrace();
			}
		}
		splinedPos = getSpline_txyz(tStamps, regPos, nTime, nKTpairs, re);

		double[][] allRegPos = new double[nLines][3];

		for(i=0;i<nKTpairs*2;i++){
			for(t=0;t<nTime;t++){
				for(j=0;j<3;j++){
					allRegPos[t+i*nTime][j] = pos[t+i*nTime][j] - cenPos[t][j];
				}
			}
		}


		//velocity (from splined)

		double[][] splinedVelVec = new double[nTime*nKTpairs*2][3];
		double[] splinedVel = new double[nTime*nKTpairs*2];

		splinedVelVec = getSplinedVel_txyz(tStamps, allRegPos, nTime, nKTpairs, re);
		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				splinedVel[t+i*nTime]=Math.sqrt(splinedVelVec[t+i*nTime][0]*splinedVelVec[t+i*nTime][0]
				                                +splinedVelVec[t+i*nTime][1]*splinedVelVec[t+i*nTime][1]
				                                +splinedVelVec[t+i*nTime][2]*splinedVelVec[t+i*nTime][2]);


			}
		}

		double[][] splinedVelNormVec = new double[nTime*nKTpairs*2][3];

		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				for(j=0;j<3;j++){
					splinedVelNormVec[t+i*nTime][j] = splinedVelVec[t+i*nTime][j]/splinedVel[t+i*nTime];
				}
			}
		}



		//calculate Mass center - KT distance

		double[] disFromCen = new double[nLines];

		for(i=0;i<nTime*nKTpairs*2;i++){
			disFromCen[i] = Math.sqrt(regPos[i][0]*regPos[i][0]+regPos[i][1]*regPos[i][1]+regPos[i][2]*regPos[i][2]);
		}


		//calculate Mass center - KT distance (splined)
		double[] disFromCenSplined = new double[nLines];
		disFromCenSplined = getSpline_ty(tStamps, disFromCen, nTime, nKTpairs, re);

		//calculate vector connecting KT pairs

		double[][] vec = new double[nLines][3];

		for (i = 0; i<nKTpairs; i++) {
			for(t=0; t<nTime; t++){
				for(j=0;j<3;j++){
					vec[2*i*nTime+t][j] = pos[2*i*nTime+t][j]*pos[2*i*nTime+t][3]-pos[2*i*nTime+t+nTime][j]*pos[2*i*nTime+t+nTime][3];
					vec[2*i*nTime+t+nTime][j] = vec[2*i*nTime+t][j];
				}
			}
		}

		//calculate vector connecting KT pairs (Splined)

		double[][] splinedVec = new double[nLines][3];

		for (i = 0; i<nKTpairs; i++) {
			for(t=0; t<nTime; t++){
				for(j=0;j<3;j++){
					splinedVec[2*i*nTime+t][j] = splinedPos[2*i*nTime+t][j]-splinedPos[2*i*nTime+t+nTime][j];
					splinedVec[2*i*nTime+t+nTime][j] = splinedVec[2*i*nTime+t][j];
				}
			}
		}


		//interKT distance
		double[] interKTdis = new double[nLines];

		for (i = 0; i<2*nKTpairs; i++) {
			for(t=0; t<nTime; t++){
				interKTdis[t+i*nTime]=Math.sqrt(vec[t+i*nTime][0]*vec[t+i*nTime][0]+vec[t+i*nTime][1]*vec[t+i*nTime][1]+vec[t+i*nTime][2]*vec[t+i*nTime][2]);
			}
		}

		//interKT distance (Splined)
		double[] interKTdisSplined = new double[nLines];
		interKTdisSplined = getSpline_ty(tStamps, interKTdis, nTime, nKTpairs, re);


		// calculate normalized vector

		double[][] normVec = new double[nLines][3];

		for (i = 0; i<nLines; i++) {
			for(j=0;j<3;j++){
				normVec[i][j] = vec[i][j]/Math.sqrt(vec[i][0]*vec[i][0]+vec[i][1]*vec[i][1]+vec[i][2]*vec[i][2]);
			}
		}

		// calculate normalized vector, splined

		double[][] normVecSplined = new double[nLines][3];

		for (i = 0; i<nLines; i++) {
			for(j=0;j<3;j++){
				normVecSplined[i][j] = splinedVec[i][j]/Math.sqrt(splinedVec[i][0]*splinedVec[i][0]+splinedVec[i][1]*splinedVec[i][1]+splinedVec[i][2]*splinedVec[i][2]);
			}
		}

	   //	vecArg = getVecArg(normVec,nKTpairs,nTime);




		//get final inter-kinetochore distance or maximum interKT distance

		double[] finalInterKTdis = new double[nLines];
		finalInterKTdis = getFinalInterKTdis(interKTdisSplined,nKTpairs, nTime, timepointOfProphaseEnd);

		System.out.println("interKTdisSplined");

		//calculate % interKT distance
		double[] percInterKTdis = new double[nLines];
		for (i=0; i<2*nKTpairs; i++){
			for (t=0; t<nTime; t++){
				percInterKTdis[t+i*nTime] = interKTdis[t+i*nTime]/finalInterKTdis[t+i*nTime]*100;
			}
		}

		//calculate % interKT distance (Splined)
		double[] percInterKTdisSplined = new double[nLines];
		for (i=0; i<2*nKTpairs; i++){
			for (t=0; t<nTime; t++){
				percInterKTdisSplined[t+i*nTime] = interKTdisSplined[t+i*nTime]/finalInterKTdis[t+i*nTime]*100;
			}
		}



		//calculate timing of anaphase
		int tAna = getTimeOfAnaphase(interKTdisSplined, nKTpairs, nTime);
	//	tAna = 161;
		System.out.println("Anaphase timing; " + tAna);
		ta.append("Anaphase timing: "+tAna+"\n");

		// calculate middle position of paired kinetochores (chromosome position)

		double[][] chrPos=new double[nLines][3];
		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				for (j=0; j<3; j++){
					chrPos[t+2*i*nTime][j]=(regPos[t+2*i*nTime][j]+regPos[t+2*i*nTime+nTime][j])/2;
				}
			}
		}

		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				for (j=0; j<3; j++){
					chrPos[t+2*i*nTime+nTime][j]=chrPos[t+2*i*nTime][j];
				}
			}
		}

		// calculate middle position of paired kinetochores (chromosome position)

		double[][] allChrPos=new double[nLines][3];
		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				for (j=0; j<3; j++){
					allChrPos[t+2*i*nTime][j]=(allRegPos[t+2*i*nTime][j]+allRegPos[t+2*i*nTime+nTime][j])/2;
				}
			}
		}

		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				for (j=0; j<3; j++){
					allChrPos[t+2*i*nTime+nTime][j]=allChrPos[t+2*i*nTime][j];
				}
			}
		}

		// calculate middle position of paired kinetochores (chromosome position), Splined

		double[][] chrPosSplined=new double[nLines][3];

		chrPosSplined = getSpline_txyz(tStamps, chrPos, nTime, nKTpairs, re);

		double[][] chrPosSplinedVelVec = new double[nLines][3];
		chrPosSplinedVelVec = getSplinedVel_txyz(tStamps, chrPos, nTime, nKTpairs, re);

		double[] chrPosSplinedVel = new double[nLines];

		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				chrPosSplinedVel[t+i*nTime]=Math.sqrt(chrPosSplinedVelVec[t+i*nTime][0]*chrPosSplinedVelVec[t+i*nTime][0]
				                                +chrPosSplinedVelVec[t+i*nTime][1]*chrPosSplinedVelVec[t+i*nTime][1]
				                                +chrPosSplinedVelVec[t+i*nTime][2]*chrPosSplinedVelVec[t+i*nTime][2]);


			}
		}



		//calculate asymmetry with chromosome positions

		double[][] allChrPosNReduced=new double[nKTpairs*nTime][3];
		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				for (j=0; j<3; j++){
					allChrPosNReduced[t+i*nTime][j]=allChrPos[t+2*i*nTime][j];
				}
			}
		}

		double[] chrEigenVal1=new double[nLines];
		double[] chrEigenVal2=new double[nLines];
		double[] chrEigenVal3=new double[nLines];
		double[] chrAsym=new double[nLines];
		double[][] chrEigenVec1=new double[nLines][3];
		double[][] chrEigenVec2=new double[nLines][3];
		double[][] chrEigenVec3=new double[nLines][3];

		EigenvalueDecomposition[] chrEd = new EigenvalueDecomposition[nTime];
		chrEd = getEd(allChrPosNReduced, nKTpairs, nTime);

		Matrix[] chrEigenValMtrx = new Matrix[nTime];
		Matrix[] chrEigenVecMtrx = new Matrix[nTime];

		for (t=0; t<nTime; t++){

			chrEigenValMtrx[t] = chrEd[t].getD();
			chrEigenVecMtrx[t] = chrEd[t].getV();

			chrEigenVal1[t]=Math.sqrt(chrEigenValMtrx[t].get(0,0));
			chrEigenVal2[t]=Math.sqrt(chrEigenValMtrx[t].get(1,1));
			chrEigenVal3[t]=Math.sqrt(chrEigenValMtrx[t].get(2,2));

			chrAsym[t]=getAsym(chrEigenValMtrx[t]);

			for (j=0; j<3; j++){
				chrEigenVec1[t][j]=chrEigenVecMtrx[t].get(j,0);
				chrEigenVec2[t][j]=chrEigenVecMtrx[t].get(j,1);
				chrEigenVec3[t][j]=chrEigenVecMtrx[t].get(j,2);
			}

		}

		for(t=0;t<nTime;t++){
			for(i=0;i<2*nKTpairs;i++){
				chrEigenVal1[t+i*nTime]=chrEigenVal1[t];
				chrEigenVal2[t+i*nTime]=chrEigenVal2[t];
				chrEigenVal3[t+i*nTime]=chrEigenVal3[t];
				chrAsym[t+i*nTime]=chrAsym[t];

				for(j=0;j<3;j++){
					chrEigenVec1[t+i*nTime][j]=chrEigenVec1[t][j];
					chrEigenVec2[t+i*nTime][j]=chrEigenVec2[t][j];
					chrEigenVec3[t+i*nTime][j]=chrEigenVec3[t][j];
				}
			}
		}

		double[] aRatioChrEig = new double[nLines];
		for(t=0;t<nTime;t++){
			for(i=0;i<2*nKTpairs;i++){
				aRatioChrEig[t+i*nTime] = chrEigenVal1[t]/(chrEigenVal2[t]+chrEigenVal3[t])*2;
			}
		}







		//calculate average vector at time

		//One way to do is caluculate weighted vector by % interKT distance

		double[][] weightedVec=new double[nLines][3];

		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				if(percInterKTdisSplined[t+2*i*nTime]>70){
					for(j=0;j<3;j++){
						weightedVec[t+2*i*nTime][j]=normVec[t+2*i*nTime][j]*(percInterKTdisSplined[t+2*i*nTime]-70)/30;
						weightedVec[t+2*i*nTime+nTime][j]=normVec[t+2*i*nTime+nTime][j]*(percInterKTdisSplined[t+2*i*nTime+nTime]-70)/30;
					}
				}
			}
		}




		double[][] weightedVecArg = new double[nLines][3];
		weightedVecArg = getVecArg(weightedVec,nKTpairs,nTime);

		// get number of kinetochores that are stretched

		double[] numOfStretched = new double[nLines];
		double[][] noWeightedVec=new double[nLines][3];

		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				if(percInterKTdisSplined[t+2*i*nTime]>70){
					numOfStretched[t]=numOfStretched[t]+1;
					for(j=0;j<3;j++){
						noWeightedVec[t+2*i*nTime][j]=normVec[t+2*i*nTime][j];
						noWeightedVec[t+2*i*nTime+nTime][j]=normVec[t+2*i*nTime+nTime][j];
					}
				}else if(Double.isNaN(percInterKTdisSplined[t+2*i*nTime])){
					numOfStretched[t] = Double.parseDouble("NaN");
				}



			}
		}




		// get time of metaphase

		int tMeta = getTimeOfMetaphase(numOfStretched, tAna, 10);
		int tMetaPlate = getTimeOfMetaphasePlate(aRatioChrEig, timepointOfProphaseEnd, 0.5, nTime);

		double[][] vecArg = new double[nLines][3];
		if(vecArgMode == 1){

			vecArg = getVecArgPredictedByMeta(weightedVecArg, tMeta, nKTpairs, nTime);
		}else if (vecArgMode == 5){
			vecArg = chrEigenVec1;
		}

		vecArg = correctFlips(vecArg);

		// spline average vector




		double[][] vecArgSplined = new double[nLines][3];
		vecArgSplined = getSpline_txyz(tStamps, vecArg, nTime, nKTpairs, re);



		// center of metaphase plate = center of mass of all kinetochores

		double[][] metaphasePlateCen = new double[nLines][3];
		for (i=0; i<nLines; i++){
			for (j=0; j<3; j++){
				metaphasePlateCen[i][j] = 0;
			}
		}


		// calculate angle between average vector and a KT vector. if >90deg, flip it.

		double[] ang = new double[nLines];
		double[] ang180 = new double[nLines];

		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				ang[t+2*i*nTime] = Math.acos(
									(vec[t+2*i*nTime][0]*vecArg[t][0]
									+vec[t+2*i*nTime][1]*vecArg[t][1]
									+vec[t+2*i*nTime][2]*vecArg[t][2])/Math.sqrt(
									(vec[t+2*i*nTime][0]*vec[t+2*i*nTime][0]
									+ vec[t+2*i*nTime][1]*vec[t+2*i*nTime][1]
									+ vec[t+2*i*nTime][2]*vec[t+2*i*nTime][2])*(vecArg[t][0]*vecArg[t][0]
									+ vecArg[t][1]*vecArg[t][1]
									+ vecArg[t][2]*vecArg[t][2])
									)
								);
				ang180[t+2*i*nTime] = ang[t+2*i*nTime];
			}
		}

		for (i=0; i<nKTpairs; i++){
			if(ang180[tAna-3+2*i*nTime] > Math.PI/2){
				for (t=0; t<nTime; t++){
					ang180[t+2*i*nTime] = Math.PI-ang[t+2*i*nTime];
				}
			}
		}

		for (i=0; i<nKTpairs; i++){
			for (t=0; t<nTime; t++){
				if(ang[t+2*i*nTime]>Math.PI/2)
					ang[t+2*i*nTime]=Math.PI-ang[t+2*i*nTime];

				ang[t+(2*i+1)*nTime]=ang[t+2*i*nTime];
				ang180[t+(2*i+1)*nTime]=ang180[t+2*i*nTime];
			}
		}

		// calculate angle between average vector and a KT vector. if >90deg, flip it. (Splined)

		double[] angSplined = new double[nLines];
		angSplined = getSpline_ty(tStamps, ang, nTime, nKTpairs, re);

		double[] ang180Splined = new double[nLines];
		ang180Splined = getSpline_ty(tStamps, ang180, nTime, nKTpairs, re);



		//metaphase plate is ax+by+cz+d=0, where a=xVecAverage, b=yVecAverage, c=zVecAverage, d=-ax0-by0-cz0, (x0,y0,z0=metaphasePlateCen)
		//if you think of NormalizedKTpositions.
		//distance from metaphase plate is defined as dis=(a*xNormalizedPosition+b*yNormalizedPosition+c*xNormalizedPosition+d)/sqrt(a*a+b*b+c*c)

		double[] disFromPlate = new double[nLines];

		for (i=0; i<2*nKTpairs; i++){
			for (t=0; t<nTime; t++){
				disFromPlate[t+i*nTime] = (vecArg[t][0]*regPos[t+i*nTime][0]
									+vecArg[t][1]*regPos[t+i*nTime][1]
									+vecArg[t][2]*regPos[t+i*nTime][2]
									-vecArg[t][0]*metaphasePlateCen[t][0]
									-vecArg[t][1]*metaphasePlateCen[t][1]
									-vecArg[t][2]*metaphasePlateCen[t][2])/Math.sqrt(vecArg[t][0]*vecArg[t][0]+vecArg[t][1]*vecArg[t][1]+vecArg[t][2]*vecArg[t][2]);
			}
		}

		//distance from plate, Splined

		double[] disFromPlateSplined = new double[nLines];

		disFromPlateSplined = getSpline_ty(tStamps, disFromPlate, nTime, nKTpairs, re);

		//distance from the axis.

		double[] disFromAxis = new double[nLines];

		for (i=0; i<2*nKTpairs; i++){
			for (t=0; t<nTime; t++){
				disFromAxis[t+i*nTime] = Math.sqrt(disFromCen[t+i*nTime]*disFromCen[t+i*nTime]-disFromPlate[t+i*nTime]*disFromPlate[t+i*nTime]);
			}
		}

		//distance from the axis, splined.

		double[] disFromAxisSplined = new double[nLines];

		disFromAxisSplined = getSpline_ty(tStamps, disFromAxis, nTime, nKTpairs, re);


		// chromosome distance from center,

		double[] chrDisFromCen = new double[nLines];

		for(i=0;i<nTime*nKTpairs*2;i++){
			chrDisFromCen[i] = Math.sqrt(chrPos[i][0]*chrPos[i][0]+chrPos[i][1]*chrPos[i][1]+chrPos[i][2]*chrPos[i][2]);
		}


		//calculate Mass center - chromosome distance (splined)
		double[] chrDisFromCenSplined = new double[nLines];
		chrDisFromCenSplined = getSpline_ty(tStamps, chrDisFromCen, nTime, nKTpairs, re);

		//distance from the plate (chromosome).

		double[] chrDisFromPlate = new double[nLines];

		for (i=0; i<2*nKTpairs; i++){
			for (t=0; t<nTime; t++){
				chrDisFromPlate[t+i*nTime] = (vecArg[t][0]*chrPos[t+i*nTime][0]
									+vecArg[t][1]*chrPos[t+i*nTime][1]
									+vecArg[t][2]*chrPos[t+i*nTime][2]
									-vecArg[t][0]*metaphasePlateCen[t][0]
									-vecArg[t][1]*metaphasePlateCen[t][1]
									-vecArg[t][2]*metaphasePlateCen[t][2])/Math.sqrt(vecArg[t][0]*vecArg[t][0]+vecArg[t][1]*vecArg[t][1]+vecArg[t][2]*vecArg[t][2]);
			}
		}

		//distance from the plate (chromosome), splined

		double[] chrDisFromPlateSplined = new double[nLines];
		chrDisFromPlateSplined = getSpline_ty(tStamps, chrDisFromPlate, nTime, nKTpairs, re);

		//absolute of distance from the plate (chromosome)

		double[] chrDisFromPlateAbs = new double[nLines];

		for (i=0; i<2*nKTpairs; i++){
			for (t=0; t<nTime; t++){
				chrDisFromPlateAbs[t+i*nTime] = Math.abs((vecArg[t][0]*chrPos[t+i*nTime][0]
									+vecArg[t][1]*chrPos[t+i*nTime][1]
									+vecArg[t][2]*chrPos[t+i*nTime][2]
									-vecArg[t][0]*metaphasePlateCen[t][0]
									-vecArg[t][1]*metaphasePlateCen[t][1]
									-vecArg[t][2]*metaphasePlateCen[t][2])/Math.sqrt(vecArg[t][0]*vecArg[t][0]+vecArg[t][1]*vecArg[t][1]+vecArg[t][2]*vecArg[t][2]));
			}
		}

		//chromosome distance from the axis.

		double[] chrDisFromAxis = new double[nLines];

		for (i=0; i<2*nKTpairs; i++){
			for (t=0; t<nTime; t++){
				chrDisFromAxis[t+i*nTime] = Math.sqrt(chrDisFromCen[t+i*nTime]*chrDisFromCen[t+i*nTime]-chrDisFromPlateAbs[t+i*nTime]*chrDisFromPlateAbs[t+i*nTime]);
			}
		}

		//chromosome distance from the axis, splined
		double[] chrDisFromAxisSplined = new double[nLines];
		chrDisFromAxisSplined = getSpline_ty(tStamps, chrDisFromAxis, nTime, nKTpairs, re);


		//Length of the path, Splined
		double[] ktPath = new double[nTime*nKTpairs*2];

		for(i=0;i<2*nKTpairs;i++){
			for(t=0;t<nTime;t++){
				ktPath[t+i*nTime] =0;
			}
		}

		//Average length of the path, Splined

		double[] averageKtPath = new double[nTime*nKTpairs*2];

		for(i=0;i<2*nKTpairs;i++){
			int emptySlot=0;

			for(t=1;t<tAna-2;t++){
				if(Double.isNaN(ktPath[t+i*nTime])){
					emptySlot++;
				}
			}
			averageKtPath[i*nTime] =  ktPath[tAna-3+i*nTime]/(tAna-3-emptySlot);
		}

		for(t=0;t<nTime;t++){
			for(i=0;i<2*nKTpairs;i++){
				averageKtPath[t+i*nTime] = averageKtPath[i*nTime];
			}
		}


		double[] nStretched = new double[nLines];

		for(i=0; i<nLines; i++){
			nStretched[i] = (double)numOfStretched[i];
		}

		double[] nIndividualizedKT = new double[nLines];
		double[] clusterCount = new double[nLines];

		for(i=0; i<2*nKTpairs; i++){
			for(t=0; t<nTime; t++){
				clusterCount[t+i*nTime]=0;
				for(int k=0; k<2*nKTpairs; k++){
					double d = Math.sqrt( (pos[t+i*nTime][0]-pos[t+k*nTime][0])*(pos[t+i*nTime][0]-pos[t+k*nTime][0])
							+(pos[t+i*nTime][1]-pos[t+k*nTime][1])*(pos[t+i*nTime][1]-pos[t+k*nTime][1])
							+(pos[t+i*nTime][2]-pos[t+k*nTime][2])*(pos[t+i*nTime][2]-pos[t+k*nTime][2]));
					if(d<1){
						clusterCount[t+i*nTime]++;
					}
				}
			}
		}

		for(t=0;t<nTime;t++){
			for(i=0; i<2*nKTpairs; i++){
				if(clusterCount[t+i*nTime]==1){
					nIndividualizedKT[t]++;
				}
			}
			for(i=0; i<2*nKTpairs; i++){
				nIndividualizedKT[t+i*nTime] = nIndividualizedKT[t];
			}
		}

		//output to result.txt

		String title = " \t"+					//1
		"KT\t"+
		"Timestamps\t"+
		"Position X\t"+
		"Position Y\t"+
		"Position Z\t"+
		"xCenterPosition\t"+
		"yCenterPosition\t"+
		"zCenterPosition\t"+
		"xRegisteredPosition\t"+
		"yRegisteredPosition\t"+
		"zRegisteredPosition\t"+
		"xSplinedPosition\t"+
		"ySplinedPosition\t"+
		"zSplinedPosition\t"+
		
		"speed\t"+
		"DistanceFromCen\t"+
		"DistanceFromCenSplined\t"+
		"Plate-KT distance\t"+
		"Plate-KT distanceSplined\t"+
		"Axis-KT distance\t"+
		"Axis-KT distanceSplined\t"+
		"nCluster\t"+
		"nIndividualizedKT\t"+
		
		"RegisteredChromosomePosx\t"+
		"RegisteredChromosomePosy\t"+
		"RegisteredChromosomePosz\t"+
		"SplinedChromosomePosx\t"+
		"SplinedChromosomePosy\t"+
		"SplinedChromosomePosz\t"+
		"chromosome speed\t"+
		"Cen-chr distance\t"+
		"Cen-chr distance Splined\t"+
		"Plate-Chr distance\t"+
		"Plate-Chr distanceSplined\t"+
		"Axis-Chr distance\t"+
		"Axis-Chr distanceSplined\t"+	
		
		"interKTdis\t"+
		"interKTdisSplined\t"+
		"finalInterKTdis\t"+
		"%interKTdis\t"+
		"%interKTdisSplined\t"+
		"NumOfStretched\t"+

		"angleWithSpindleAxis\t"+
		"angleWithSpindleAxisSplined\t"+
		"angleWithSpindleAxis180\t"+
		"angleWithSpindleAxisSplined180\t"+
		
		"xSpindleAxis\t"+
		"ySpindleAxis\t"+
		"zSpindleAxis\t"+

		"chrEigenVal1\t"+
		"chrEigenVal2\t"+
		"chrEigenVal3\t"+
		"aspectRatioChrEig\t"+
		
		"LengthOfKtPath\t"+
		"AverageLengthOfKtPath\t"+

		"tStampAtTAna\t";


		try{
			File file = new File(folder +"result"+vecArgMode+".txt");
			PrintWriter pw
				= new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw.println(title);

			System.out.println("title written");
			ta.append("title written\n");

			for(i=0;i<nLines;i++){
				pw.println((i%nTime+1)+"\t"						//1
							+(Math.floor(i/nTime)+1)+"\t"
							+tStamps[i]+"\t"
							+pos[i][0]+"\t"
							+pos[i][1]+"\t"
							+pos[i][2]+"\t"
							+cenPos[i][0]+"\t"
							+cenPos[i][1]+"\t"
							+cenPos[i][2]+"\t"
							+regPos[i][0]+"\t"
							+regPos[i][1]+"\t"
							+regPos[i][2]+"\t"
							+splinedPos[i][0]+"\t"
							+splinedPos[i][1]+"\t"
							+splinedPos[i][2]+"\t"
							+splinedVel[i]+"\t"
							+disFromCen[i]+"\t"
							+disFromCenSplined[i]+"\t"
							+disFromPlate[i]+"\t"
							+disFromPlateSplined[i]+"\t"
							+disFromAxis[i]+"\t"
							+disFromAxisSplined[i]+"\t"
							+nCluster[i]+"\t"
							+nIndividualizedKT[i]+"\t"
							
							+chrPos[i][0]+"\t"
							+chrPos[i][1]+"\t"
							+chrPos[i][2]+"\t"
							+chrPosSplined[i][0]+"\t"
							+chrPosSplined[i][1]+"\t"
							+chrPosSplined[i][2]+"\t"
							+chrPosSplinedVel[i]+"\t"
							+chrDisFromCen[i]+"\t"
							+chrDisFromCenSplined[i]+"\t"
							+chrDisFromPlate[i]+"\t"
							+chrDisFromPlateSplined[i]+"\t"						
							+chrDisFromAxis[i]+"\t"
							+chrDisFromAxisSplined[i]+"\t"
							
							+interKTdis[i]+"\t"
							+interKTdisSplined[i]+"\t"
							+finalInterKTdis[i]+"\t"
							+percInterKTdis[i]+"\t"
							+percInterKTdisSplined[i]+"\t"
							+numOfStretched[i]+"\t"
							
							+ang[i]+"\t"
							+angSplined[i]+"\t"
							+ang180[i]+"\t"
							+ang180Splined[i]+"\t"
										
							+vecArg[i][0]+"\t"
							+vecArg[i][1]+"\t"
							+vecArg[i][2]+"\t"

							+chrEigenVal1[i]+"\t"
							+chrEigenVal2[i]+"\t"
							+chrEigenVal3[i]+"\t"
							+aRatioChrEig[i]+"\t"
							
							+ktPath[i]+"\t"
							+averageKtPath[i]+"\t"

							+tStamps[tAna]+"\t"
							);
			}
			System.out.println("values written");
			ta.append("values written\n");
			pw.close();

		}catch(IOException e1){
			System.out.println(e1);
		}

		// output to "tblock.txt"

		try{
			File file = new File(folder +"tblock"+vecArgMode+".txt");
			PrintWriter pw3
				= new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw3.print("#");
			for(i=0;i<150;i++){
				pw3.print((i+1)+"\t");
			}
			pw3.println();
			pw3.println(title);

			System.out.println("title written");
			ta.append("title written\n");

			for(t=0;t<nTime;t++){
				for(i=0;i<2*nKTpairs;i++){

					pw3.println((t+1)+"\t"						//1
							+(i+1)+"\t"
							+tStamps[t+i*nTime]+"\t"
							+pos[t+i*nTime][0]+"\t"
							+pos[t+i*nTime][1]+"\t"
							+pos[t+i*nTime][2]+"\t"
							+cenPos[t+i*nTime][0]+"\t"
							+cenPos[t+i*nTime][1]+"\t"
							+cenPos[t+i*nTime][2]+"\t"
							+regPos[t+i*nTime][0]+"\t"
							+regPos[t+i*nTime][1]+"\t"
							+regPos[t+i*nTime][2]+"\t"
							+splinedPos[t+i*nTime][0]+"\t"
							+splinedPos[t+i*nTime][1]+"\t"
							+splinedPos[t+i*nTime][2]+"\t"
							+splinedVel[t+i*nTime]+"\t"
							+disFromCen[t+i*nTime]+"\t"
							+disFromCenSplined[t+i*nTime]+"\t"
							+disFromPlate[t+i*nTime]+"\t"
							+disFromPlateSplined[t+i*nTime]+"\t"
							+disFromAxis[t+i*nTime]+"\t"
							+disFromAxisSplined[t+i*nTime]+"\t"
							+nCluster[t+i*nTime]+"\t"
							+nIndividualizedKT[t+i*nTime]+"\t"
							
							+chrPos[t+i*nTime][0]+"\t"
							+chrPos[t+i*nTime][1]+"\t"
							+chrPos[t+i*nTime][2]+"\t"
							+chrPosSplined[t+i*nTime][0]+"\t"
							+chrPosSplined[t+i*nTime][1]+"\t"
							+chrPosSplined[t+i*nTime][2]+"\t"
							+chrPosSplinedVel[t+i*nTime]+"\t"
							+chrDisFromCen[t+i*nTime]+"\t"
							+chrDisFromCenSplined[t+i*nTime]+"\t"
							+chrDisFromPlate[t+i*nTime]+"\t"
							+chrDisFromPlateSplined[t+i*nTime]+"\t"						
							+chrDisFromAxis[t+i*nTime]+"\t"
							+chrDisFromAxisSplined[t+i*nTime]+"\t"
							
							+interKTdis[t+i*nTime]+"\t"
							+interKTdisSplined[t+i*nTime]+"\t"
							+finalInterKTdis[t+i*nTime]+"\t"
							+percInterKTdis[t+i*nTime]+"\t"
							+percInterKTdisSplined[t+i*nTime]+"\t"
							+numOfStretched[t+i*nTime]+"\t"
							
							+ang[t+i*nTime]+"\t"
							+angSplined[t+i*nTime]+"\t"
							+ang180[t+i*nTime]+"\t"
							+ang180Splined[t+i*nTime]+"\t"
										
							+vecArg[t+i*nTime][0]+"\t"
							+vecArg[t+i*nTime][1]+"\t"
							+vecArg[t+i*nTime][2]+"\t"

							+chrEigenVal1[t+i*nTime]+"\t"
							+chrEigenVal2[t+i*nTime]+"\t"
							+chrEigenVal3[t+i*nTime]+"\t"
							+aRatioChrEig[t+i*nTime]+"\t"
							
							+ktPath[t+i*nTime]+"\t"
							+averageKtPath[t+i*nTime]+"\t"

							+tStamps[tAna]+"\t"
							);
				}
				pw3.println();
				pw3.println();

			}
			System.out.println("values written");
			ta.append("values written\n");
			pw3.close();


		}catch(IOException e1){
			System.out.println(e1);
		}

		// output to "ktblock.txt"

		try{
			File file = new File(folder + "ktblock"+vecArgMode+".txt");
			PrintWriter pw4
				= new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw4.print("#");
			for(i=0;i<150;i++){
				pw4.print((i+1)+"\t");
			}
			pw4.println();
			pw4.println(title);
			System.out.println("title written");
			ta.append("title written\n");


			for(i=0;i<2*nKTpairs;i++){
				for(t=0;t<nTime;t++){
					pw4.println((t+1)+"\t"						//1
							+(i+1)+"\t"
							+tStamps[t+i*nTime]+"\t"
							+pos[t+i*nTime][0]+"\t"
							+pos[t+i*nTime][1]+"\t"
							+pos[t+i*nTime][2]+"\t"
							+cenPos[t+i*nTime][0]+"\t"
							+cenPos[t+i*nTime][1]+"\t"
							+cenPos[t+i*nTime][2]+"\t"
							+regPos[t+i*nTime][0]+"\t"
							+regPos[t+i*nTime][1]+"\t"
							+regPos[t+i*nTime][2]+"\t"
							+splinedPos[t+i*nTime][0]+"\t"
							+splinedPos[t+i*nTime][1]+"\t"
							+splinedPos[t+i*nTime][2]+"\t"
							+splinedVel[t+i*nTime]+"\t"
							+disFromCen[t+i*nTime]+"\t"
							+disFromCenSplined[t+i*nTime]+"\t"
							+disFromPlate[t+i*nTime]+"\t"
							+disFromPlateSplined[t+i*nTime]+"\t"
							+disFromAxis[t+i*nTime]+"\t"
							+disFromAxisSplined[t+i*nTime]+"\t"
							+nCluster[t+i*nTime]+"\t"
							+nIndividualizedKT[t+i*nTime]+"\t"
							
							+chrPos[t+i*nTime][0]+"\t"
							+chrPos[t+i*nTime][1]+"\t"
							+chrPos[t+i*nTime][2]+"\t"
							+chrPosSplined[t+i*nTime][0]+"\t"
							+chrPosSplined[t+i*nTime][1]+"\t"
							+chrPosSplined[t+i*nTime][2]+"\t"
							+chrPosSplinedVel[t+i*nTime]+"\t"
							+chrDisFromCen[t+i*nTime]+"\t"
							+chrDisFromCenSplined[t+i*nTime]+"\t"
							+chrDisFromPlate[t+i*nTime]+"\t"
							+chrDisFromPlateSplined[t+i*nTime]+"\t"						
							+chrDisFromAxis[t+i*nTime]+"\t"
							+chrDisFromAxisSplined[t+i*nTime]+"\t"
							
							+interKTdis[t+i*nTime]+"\t"
							+interKTdisSplined[t+i*nTime]+"\t"
							+finalInterKTdis[t+i*nTime]+"\t"
							+percInterKTdis[t+i*nTime]+"\t"
							+percInterKTdisSplined[t+i*nTime]+"\t"
							+numOfStretched[t+i*nTime]+"\t"
							
							+ang[t+i*nTime]+"\t"
							+angSplined[t+i*nTime]+"\t"
							+ang180[t+i*nTime]+"\t"
							+ang180Splined[t+i*nTime]+"\t"
										
							+vecArg[t+i*nTime][0]+"\t"
							+vecArg[t+i*nTime][1]+"\t"
							+vecArg[t+i*nTime][2]+"\t"

							+chrEigenVal1[t+i*nTime]+"\t"
							+chrEigenVal2[t+i*nTime]+"\t"
							+chrEigenVal3[t+i*nTime]+"\t"
							+aRatioChrEig[t+i*nTime]+"\t"
							
							+ktPath[t+i*nTime]+"\t"
							+averageKtPath[t+i*nTime]+"\t"

							+tStamps[tAna]+"\t");
				}
				pw4.println();
				pw4.println();

			}
			System.out.println("values written - end");
			ta.append("values written - end\n");
			pw4.close();


		}catch(IOException e1){
			System.out.println(e1);
		}


		try{
			File file = new File(folder + "eachChromosome-controller.plt");
			PrintWriter pw6
				= new PrintWriter(new BufferedWriter(new FileWriter(file)));
			pw6.println("reset");
			pw6.println("set autoscale");
			pw6.println("set xrange[0:9]");
			pw6.println("unset clip");
			pw6.println("set nokey");
			pw6.println("set terminal png size 1280,960");

			pw6.println("set format y \"\"");
			pw6.println("set format x \"\"");

			for(i=0;i<nKTpairs;i++){
				pw6.println("set output \'eachChromosome/Chr-"+i+".png\'");
				pw6.println("i="+i);
				pw6.println("load \'eachChromosome-forOutput.plt\'");

			}
			pw6.println("unset format");
			pw6.close();

		}catch(IOException e1){
			System.out.println(e1);
		}

	}
	public void windowClosing(WindowEvent e) {
		System.exit(0);
	}





}