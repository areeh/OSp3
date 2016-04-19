package p3;

/**
 * This class implements the I/O device for the simulated system.
 * 
 * @author Are
 *
 */

public class IO {
	
	private Queue ioQueue;
	
	private Statistics statistics;
	
	private Process activeProcess;
	
	private long avgIoTime;
	
	private long nextIoTime;

	public IO(Queue ioQueue, long avgIoTime, Statistics statistics) {
		this.ioQueue = ioQueue;
		this.statistics = statistics;
		this.avgIoTime = avgIoTime;
	}
	
	public void insertProcess(Process p) {
		ioQueue.insert(p);
	}
	
	public Process getActiveProcess() {
		return activeProcess;
	}
	
	public Process activateNext(long clock) {
		if (activeProcess != null) {
			activeProcess.leftIO(clock);	
			activeProcess = null;
		}
		if (!ioQueue.isEmpty()) {
			activeProcess = (Process)ioQueue.removeNext();
			activeProcess.leftIoQueue(clock);
			return activeProcess;
		}
		return null;
	}
	
	public Process activateProcess(long clock) {
		activeProcess = null;
		if (!ioQueue.isEmpty()) {
			activeProcess = (Process)ioQueue.removeNext();
			activeProcess.leftIoQueue(clock);
		}
		return activeProcess;
	}
	
	public void setActiveProcess(Process p) {
		activeProcess = p;
	}
	
	public long getNextIoTime(long clock) {
		return nextIoTime;
	}
	
	public void updateNextIoTime(long clock) {
		nextIoTime = clock + 1 + (long)(2*Math.random()*avgIoTime);
		System.out.println("Printing next IO time");
		System.out.println(nextIoTime);
	}
	
	public boolean ioQueueIsEmpty() {
		return ioQueue.isEmpty();
	}
	
	
	
	public void timePassed(long timePassed) {
		statistics.ioQueueLengthTime += ioQueue.getQueueLength()*timePassed;
		if (ioQueue.getQueueLength() > statistics.ioQueueLargestLength) {
			statistics.ioQueueLargestLength = ioQueue.getQueueLength(); 
		}
    }


}
