package p3;

/**
 * This class contains a lot of public variables that can be updated
 * by other classes during a simulation, to collect information about
 * the run.
 */
public class Statistics
{
	/** The number of processes that have exited the system */
	public long nofCompletedProcesses = 0;
	/** The number of processes that have entered the system */
	public long nofCreatedProcesses = 0;
	/** The total time that all completed processes have spent waiting for memory */
	public long totalTimeSpentWaitingForMemory = 0;
	/** The time-weighted length of the memory queue, divide this number by the total time to get average queue length */
	public long memoryQueueLengthTime = 0;
	/** The largest memory queue length that has occurred */
	public long memoryQueueLargestLength = 0;
	/** The time-weighted length of the memory queue, divide this number by the total time to get average queue length */
	public long ioQueueLengthTime = 0;
	/** The largest memory queue length that has occurred */
	public long ioQueueLargestLength = 0;
	/** The time-weighted length of the memory queue, divide this number by the total time to get average queue length */
	public long readyQueueLengthTime = 0;
	/** The largest memory queue length that has occurred */
	public long readyQueueLargestLength = 0;
	
	public long nofProcessSwitches = 0;
	public long nofProcessedIoOperations = 0;
	public long avgThroughput = 0;
	public long totCpuTime = 0;
	public long totalTimeSpentInSystem = 0;
	public long totalTimeSpentWaitingForCpu = 0;
	public long totalTimeSpentInCpu = 0;
	public long totalTimeSpentWaitingForIo = 0;
	public long totalTimeSpentInIo = 0;
	public long totalNofTimesPlacedInReadyQueue = 0;
	public long totalNofTimesPlacedInIoQueue = 0;
	public long totActiveCpuTime = 0;
	public long totIdleCpuTime = 0;
    
	/**
	 * Prints out a report summarizing all collected data about the simulation.
	 * @param simulationLength	The number of milliseconds that the simulation covered.
	 */
	public void printReport(long simulationLength) {
		calcTotalSystemTime();
		System.out.println();
		System.out.println("Simulation statistics:");
		System.out.println();
		System.out.println("Number of completed processes:                                "+nofCompletedProcesses);
		System.out.println("Number of created processes:                                  "+nofCreatedProcesses);
		System.out.println("Number of (forced) process switches:                          "+nofProcessSwitches);
		System.out.println("Number of processed I/O operations:                           "+nofProcessedIoOperations);
		System.out.println("Average throughput (processes per second):                    "+
		(float)nofCompletedProcesses/((float)totCpuTime/1000));
		System.out.println();
		System.out.println("Total CPU time spent processing:                              "+totActiveCpuTime+" ms");
		System.out.println("Fraction of CPU time spent process                            "+
		((float)totActiveCpuTime/(float)totCpuTime)*100+"%");
		System.out.println("Total CPU time spent waiting:                                 "+totIdleCpuTime);
		System.out.println("Fraction of CPU time spent waiting                            "+
		((float)totIdleCpuTime/(float)totCpuTime)*100+"%");
		System.out.println();
		System.out.println("Largest occuring memory queue length:                         "+memoryQueueLargestLength);
		System.out.println("Average memory queue length:                                  "+(float)memoryQueueLengthTime/simulationLength);
		System.out.println("Largest occuring cpu queue length:                            "+readyQueueLargestLength);
		System.out.println("Average memory cpu length:                                    "+(float)readyQueueLengthTime/simulationLength);
		System.out.println("Largest occuring I/O queue length:                            "+ioQueueLargestLength);
		System.out.println("Average I/O queue length:                                     "+(float)ioQueueLengthTime/simulationLength);
		if(nofCompletedProcesses > 0) {
			System.out.println("Average # of times a process has been placed in memory queue:   "+1);
			System.out.println("Average # of times a process has been placed in cpu queue:      "+
				(float)totalNofTimesPlacedInReadyQueue/nofCompletedProcesses);
			System.out.println("Average # of times a process has been placed in I/O queue:      "+
				(float)totalNofTimesPlacedInIoQueue/nofCompletedProcesses);
			System.out.println();
			System.out.println("Average time spent in system per process:                       "+
				totalTimeSpentInSystem/nofCompletedProcesses+" ms");
			System.out.println("Average time spent waiting for memory per process:              "+
				totalTimeSpentWaitingForMemory/nofCompletedProcesses+" ms");
			System.out.println("Average time spent waiting for cpu per process:                 "+
				totalTimeSpentWaitingForCpu/nofCompletedProcesses+" ms");
			System.out.println("Average time spent processing per process:                      "+
				totalTimeSpentInCpu/nofCompletedProcesses+" ms");
			System.out.println("Average time spent waiting for I/O per process:                 "+
				totalTimeSpentWaitingForIo/nofCompletedProcesses+" ms");
			System.out.println("Average time spent in I/O per process:                          "+
				totalTimeSpentInIo/nofCompletedProcesses+" ms");
		}
	}

	private void calcTotalSystemTime() {
		totalTimeSpentInSystem += totalTimeSpentInCpu+totalTimeSpentInIo
				+totalTimeSpentWaitingForCpu+totalTimeSpentWaitingForIo+totalTimeSpentWaitingForMemory;
		
	}
}
