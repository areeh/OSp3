package p3;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * This class implements the CPU for the simulated
 * system.
 * 
 * @author Are
 *
 */

public class CPU {
	
	private Queue readyQueue;
	private PriorityQueue<Process> pQ;
	
	private Statistics statistics;
	
	private Process activeProcess;

	public CPU(Queue readyQueue, long maxCpuTime, Statistics statistics) {
		this.readyQueue = readyQueue;
		this.pQ = new PriorityQueue<Process>();
		this.statistics = statistics;
	}
	
	public void insertProcess(Process p) {
		readyQueue.insert(p);
	}
	
	public void insertPqProcess(Process p) {
		pQ.add(p);
	}
	
	public Process switchProcess(long clock) {
		if (activeProcess != null) {
			statistics.nofProcessSwitches++;
			activeProcess.leftCPU(clock);
			readyQueue.insert(activeProcess);
			activeProcess = null;
			Process p = (Process)readyQueue.getNext();
			p.leftReadyQueue(clock);
			activeProcess = (Process)readyQueue.removeNext();
			if (activeProcess == null) {
				System.err.println("Did not activate swapped away process");
			}
			return activeProcess;
		}
		return null;
	}
	
	public Process switchProcessPrioritized(long clock) {
		System.out.println("Ran prioritized version of sp");
		if (activeProcess != null) {
			statistics.nofProcessSwitches++;
			activeProcess.leftCPU(clock);
			readyQueue.insert(activeProcess);
			activeProcess = null;
			Process p = (Process)pQ.poll();
			p.leftReadyQueue(clock);
			activeProcess = (Process)readyQueue.removeSpecified(p);
			if (activeProcess == null) {
				System.err.println("Did not activate swapped away process");
			}
			return activeProcess;
		}
		return null;
	}
	
	public void endProcess(long clock) {
		activeProcess.leftCPU(clock);
	}
	
	public Process activateNext(long clock) {
		if (activeProcess != null) {
			activeProcess.leftCPU(clock);
			activeProcess = null;
		}
		if (!readyQueue.isEmpty()) {
			Process p = (Process)readyQueue.getNext();
			p.leftReadyQueue(clock);
			activeProcess = (Process)readyQueue.removeNext();
			return activeProcess;
		}
		return null;
	}
	
	public Process activateNextPrioritized(long clock) {
		System.out.println("Ran prioritized version of an");
		if (activeProcess != null) {
			activeProcess.leftCPU(clock);
			activeProcess = null;
		}
		if (!readyQueue.isEmpty() && !pQ.isEmpty()) {
			Process p = (Process)pQ.poll();
			p.leftReadyQueue(clock);
			activeProcess = (Process)readyQueue.removeSpecified(p);
			return activeProcess;
		}
		return null;
	}
	
	public Process activateProcess(long clock) {
		activeProcess = null;
		if (!readyQueue.isEmpty()) {
			Process p = (Process)readyQueue.getNext();
			p.leftReadyQueue(clock);
			activeProcess = (Process)readyQueue.removeNext();
		}
		return activeProcess;
	}
	
	public Process activateProcessPrioritized(long clock) {
		System.out.println("Ran prioritized version of ap");
		activeProcess = null;
		if (!readyQueue.isEmpty() && !pQ.isEmpty()) {
			Process p = (Process)pQ.poll();
			p.leftReadyQueue(clock);
			activeProcess = (Process)readyQueue.removeSpecified(p);
		}
		return activeProcess;
	}
	
	public Process getActiveProcess() {
		return activeProcess;
	}
	
	public void setActiveProcess(Process p) {
		this.activeProcess = p;
	}
	
	public boolean readyQueueIsEmpty() {
		return readyQueue.isEmpty();
	}
	
	/**
	 * 
	 * @return nof items added
	 */
	
	public int addReadyQueueToPq() {
		System.out.println("Called addReadyQueue to pq");
		for (Object o : readyQueue.getContent()) {
			Process p = (Process)o; 
			pQ.add(p);
		}
		return pQ.size();
	}
	
	
	public void timePassed(long timePassed) {
		statistics.readyQueueLengthTime += readyQueue.getQueueLength()*timePassed;
		if (readyQueue.getQueueLength() > statistics.readyQueueLargestLength) {
			statistics.readyQueueLargestLength = readyQueue.getQueueLength(); 
		}
		if (activeProcess == null) {
			statistics.totIdleCpuTime += timePassed;
		} else {
			statistics.totActiveCpuTime += timePassed;
		}
		statistics.totCpuTime += timePassed;
	}
	
}

