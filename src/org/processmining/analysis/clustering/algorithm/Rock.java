package org.processmining.analysis.clustering.algorithm;

import org.processmining.analysis.clustering.SimCalculator;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2004</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class Rock {

	int numOfProcess;
	int K; //K-means Algorithm에서의 cluster 수

	String[] proc;
	double[][] totalSimilarity;
	public Rock(SimCalculator sim, int K) {

		this.proc = sim.getProc();
		this.numOfProcess = proc.length;
		this.totalSimilarity = sim.getTotalSimilarity();

		this.K = K;
	}

	/*ROCK 알고리즘*/
	int[][] isNeighbor = new int[numOfProcess][numOfProcess]; //이웃 여부
	int[][] link = new int[numOfProcess][numOfProcess]; //프로세스간 링크
	double theta = 0.3; //한계값(threshold). theta보다 similarity가 크면, neighbor로 인정한다.
	double[][] goodness = new double[numOfProcess][numOfProcess]; //프로세스간 클러스터링 적합도
	double indexOnEstimation = 0.5; //클러스터링 평가계수

	double[][] q = new double[numOfProcess][numOfProcess - 1]; //로컬 최대히프(goodness)
	int[][] q1 = new int[numOfProcess][numOfProcess - 1]; //로컬 최대히프(상대프로세스의 인덱스)
	int[] heapSize = new int[numOfProcess]; //로컬 최대히프의 크기

	double[] Q = new double[numOfProcess]; //글로벌 최대히프(goodness)
	int[] Q1 = new int[numOfProcess]; //글로벌 최대히프(해당프로세스)
	int[] Q2 = new int[numOfProcess]; //글로벌 최대히프(상대프로세스)

	/*프로세스 클러스터링*/
	//int[] mergedProcess = new int[numOfProcess];	//merge되면 1로 바뀜.
	int numOfCluster = numOfProcess; //클러스터의 개수(현재 맞지 않음),
	int[] includingProcess = new int[numOfProcess]; //클러스터가 포함하고 있는 프로세스의 개수

	int[] belongedCluster = new int[numOfProcess]; //프로세스가 포함된 클러스터 인덱스{0,,,k-1}

/////////////////////////////////
//프로세스 이웃 찾아 링크(link) 계산 & 적합도(goodness) 계산
/////////////////////////////////
	public void findNeighbor() {

		for (int i = 0; i < numOfProcess; i++) {
			for (int j = 0; j < numOfProcess; j++) {

				if (totalSimilarity[i][j] > theta && i != j) {
					isNeighbor[i][j] = 1;
				} else {
					isNeighbor[i][j] = 0;
				}

			}
		}
	}

	public void countLink() {

		for (int i = 0; i < numOfProcess; i++) {
			for (int j = 0; j < numOfProcess; j++) {

				int count = 0;
				for (int m = 0; m < numOfProcess; m++) {
					if (isNeighbor[i][m] == 1 && isNeighbor[j][m] == 1 && m != i && m != j &&
							i != j) {
						count++;
					}
				}
				link[i][j] = count;
			}
		}
	}

	public void makeEachCluster() {

		for (int i = 0; i < numOfProcess; i++) {
			belongedCluster[i] = i; //각자 클러스터
			includingProcess[i] = 1;
		}
	}

	/*
	  private int countLinkOfTwoClusters(int cluster1, int cluster2) {

	 int count = 0;
	 int cluster1Size = 0;
	 int cluster2Size = 0;

	 for(int i=0; i<numOfProcess; i++) {
	  if(belongedCluster[i]==cluster1) {
	   cluster1Size++;

	   cluster2Size = 0;
	   for(int j=0; j<numOfProcess; j++) {
		if(belongedCluster[j]==cluster2) {
		 cluster2Size++;

		 count += link[i][j];
		}
	   }

	  }
	 }

	 includingProcess[cluster1] = cluster1Size;
	 includingProcess[cluster2] = cluster2Size;

	 return count;
	  }
	 */

	public void calculateGoodness() {

		for (int i = 0; i < numOfProcess; i++) {
			for (int j = 0; j < numOfProcess; j++) {

				if (i == j) {
					goodness[i][j] = 0;
					continue;
				}

				int iSize = includingProcess[i];
				int jSize = includingProcess[j];

				goodness[i][j] = link[i][j] /
								 (Math.pow(iSize + jSize, 1 + 2 * indexOnEstimation)
								 - Math.pow(iSize, 1 + 2 * indexOnEstimation)
								 - Math.pow(jSize, 1 + 2 * indexOnEstimation));
			}
		}
	}

	public void makeLocalHeap() {

		for (int i = 0; i < numOfProcess; i++) {

			int qSize = 0;
			for (int j = 0; j < numOfProcess; j++) {

				if (i != j) {
					insertLocalHeap(i, qSize, goodness[i][j], j);
					qSize++;
				}
			}
			heapSize[i] = qSize; // (numOfProcess -1)
		}

	}

	public void makeGlobalHeap() {

		int qSize = 0;

		for (int i = 0; i < numOfProcess; i++) {

			insertGlobalHeap(qSize, q[i][0], i, q1[i][0]); //(golbalQSize, goodness, process i, process j)
			qSize++;
		}

	}

	public void RockAlgorithm() {

		findNeighbor(); //유사성이 쎄타보다 큰 관계는 Neighbor 이다.
		//Simulation.printArrayInt("isNeighbor", isNeighbor[0]);

		countLink(); //두 프로세스의 공통된 Neighbor 개수가 Link이다.
		makeEachCluster(); //처음에는 각각이 독립된 클러스터이다.
		calculateGoodness(); //Goodness를 계산한다.

		makeLocalHeap();
		makeGlobalHeap();

		for (int i = 0; numOfCluster > K; i++) {
			mergeCluster();
		}
	}

	public void mergeCluster() {

		int u = Q1[0];
		int v = Q2[0];

		//u<v로 만들기..
		if (u > v) {
			int k = u;
			u = v;
			v = k;
		}

		for (int j = 0; j < numOfProcess; j++) {
			if (belongedCluster[j] == v) {
				belongedCluster[j] = u;
			}
		}
		numOfCluster--;
		System.out.println("Cluster " + v + "->" + u + " : " + Q[0]);
		System.out.println("Cluster " + numOfCluster);

		//두 링크를 합하여 작은 인수의 링크에 집어넣는다.
		for (int i = 0; i < numOfProcess; i++) {

			link[i][u] = link[i][u] + link[i][v];
			link[u][i] = link[u][i] + link[v][i];
			link[i][i] = 0;
		}

		includingProcess[u] = includingProcess[u] + includingProcess[v];
		includingProcess[v] = 0;

		//머지된 프로세스와 관련된 goodness를 local heap들에서 삭제한다.
		for (int i = 0; i < numOfProcess; i++) {

			if (i == u) { //u의 로컬히프를 새로 생성, makeLocalHeap()함수와 유사.
				int qSize = 0;
				for (int j = 0; j < numOfProcess; j++) { //u와 다른 모든 프로세스들에 대하여 다시 로컬 히프를 생성

					if (j != i && j != v) {
						int n1 = includingProcess[i];
						int n2 = includingProcess[j];
						if (n1 * n2 == 0) {
							goodness[i][j] = 0.0;
						} else {
							goodness[i][j] = link[i][j] /
											 (Math.pow(n1 + n2, 1 + 2 * indexOnEstimation)
											 - Math.pow(n1, 1 + 2 * indexOnEstimation)
											 - Math.pow(n2, 1 + 2 * indexOnEstimation));
						}

						insertLocalHeap(i, qSize, goodness[i][j], j);
						qSize++;
					}
				}
				heapSize[i] = qSize;
			} else { //다른 넘들의 로컬히프를 모두 수정.
				for (int j = 0; j < heapSize[i]; j++) { //현재 보유중인 로컬히프의 데이터들을 모두 훓어라.

					if (q1[i][j] == v) { //v에 관련된 데이터가 있으면, 삭제
						deleteLocalHeap(i, heapSize[i], j); //deleteLocalHeap(int i /*qNumber*/, int arraySize, int del /*deleted Index*/)
						heapSize[i]--;
					}
				}
				for (int j = 0; j < heapSize[i]; j++) { //현재 보유중인 로컬히프의 데이터들을 모두 훓어라.

					if (q1[i][j] == u) { //u에 관련된 데이터가 있으면, 업데이트
						int n1 = includingProcess[i];
						int n2 = includingProcess[u]; //이미 합쳐진 사이즈
						if (n1 * n2 == 0) {
							goodness[i][u] = 0.0;
						} else {
							goodness[i][u] = link[i][u] /
											 (Math.pow(n1 + n2, 1 + 2 * indexOnEstimation)
											 - Math.pow(n1, 1 + 2 * indexOnEstimation)
											 - Math.pow(n2, 1 + 2 * indexOnEstimation));
						}

						deleteLocalHeap(i, heapSize[i], j); //deleteLocalHeap(int i /*qNumber*/, int arraySize, int del /*deleted Index*/)
						heapSize[i]--;
						insertLocalHeap(i, heapSize[i], goodness[i][j], j);
						heapSize[i]++;
					}
				}
			}
		}
		makeGlobalHeap();

	}

/////////////////////////////////
//히프구조 처리 함수
/////////////////////////////////
	//프로세스 i의 로컬 히프에 자료 추가
	private void insertLocalHeap(int i, int arraySize, double data, int j) {

		//if(arraySize == q[i].length) {System.out.println("Local Heap is Full~~~~"); System.exit(0);}
		int k = 0;
		for (k = arraySize; true; ) {
			if (k == 0) {
				break;
			}
			if (data <= q[i][(k + 1) / 2 - 1]) {
				break;
			}
			q[i][k] = q[i][(k + 1) / 2 - 1];
			q1[i][k] = q1[i][(k + 1) / 2 - 1];
			k = (k + 1) / 2 - 1;
		}
		//arraySize++;

		q[i][k] = data;
		q1[i][k] = j;
	}

	//글로벌 히프 자료구조 추가
	private void insertGlobalHeap(int arraySize, double data, int i, int j) {
		//if(arraySize == Q.length) {System.out.println("Global Heap is Full~~~~"); System.exit(0);}
		int k = 0;
		for (k = arraySize; true; ) {
			if (k == 0) {
				break;
			}
			if (data <= Q[(k + 1) / 2 - 1]) {
				break;
			}
			Q[k] = Q[(k + 1) / 2 - 1];
			Q1[k] = Q1[(k + 1) / 2 - 1];
			Q2[k] = Q2[(k + 1) / 2 - 1];
			k = (k + 1) / 2 - 1;
		}
		//arraySize++;

		Q[k] = data;
		Q1[k] = i;
		Q2[k] = j;
	}

	//히프 자료구조 삭제
	private void deleteLocalHeap(int i /*qNumber*/, int arraySize, int del /*deleted Index*/) {
		if (del >= arraySize) {
			System.out.println("Heap is too small~~~~" + del + "~" + arraySize);
			System.exit(0);
		}
		int k = del;
		double data = q[i][arraySize - 1];
		int data1 = q1[i][arraySize - 1];

		for (int j = (del + 1) * 2 - 1; j < arraySize; ) {
			if (j < arraySize - 1) {
				if (q[i][j] < q[i][j + 1]) {
					j++;
				}
			}

			if (data >= q[i][j]) {
				break;
			}
			q[i][k] = q[i][j];
			q1[k] = q1[j];

			k = j;
			j = (j + 1) * 2 - 1;
		}

		q[i][k] = data;
		q1[i][k] = data1;
		q[i][arraySize - 1] = 0.0;
		q1[i][arraySize - 1] = 0;

		//return q[i];
	}

	//히프 자료구조 삭제
	private void deleteMaxGlobalHeap(int arraySize) {
		if (arraySize == 0) {
			System.out.println("Heap is Empty~~~~");
			System.exit(0);
		}
		int k = 0;
		double data = Q[arraySize - 1];
		int data1 = Q1[arraySize - 1];
		int data2 = Q2[arraySize - 1];

		for (int j = 1; j < arraySize; ) {
			if (j < arraySize - 1) {
				if (Q[j] < Q[j + 1]) {
					j++;
				}
			}

			if (data >= Q[j]) {
				break;
			}
			Q[k] = Q[j];
			Q1[k] = Q1[j];
			Q2[k] = Q2[j];

			k = j;
			j = (j + 1) * 2 - 1;
		}

		Q[k] = data;
		Q1[k] = data1;
		Q2[k] = data2;
		Q[arraySize - 1] = 0.0;
		Q[arraySize - 1] = 0;
		Q[arraySize - 1] = 0;

		//return Q;
	}

}
