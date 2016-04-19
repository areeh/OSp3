package p3;

import java.io.*;

/**
 * The main class of the P3 exercise. This class is only partially complete.
 */
public class Simulator implements Constants
{
	/** The queue of events to come */
    private EventQueue eventQueue;
	/** Reference to the memory unit */
    private Memory memory;
    /** Reference to the CPU */
    private CPU cpu;
    /** Reference to the I/O device */
    private IO io;
	/** Reference to the GUI interface */
	private Gui gui;
	/** Reference to the statistics collector */
	private Statistics statistics;
	/** The global clock */
    private long clock;
	/** The length of the simulation */
	private long simulationLength;
	/** The average length between process arrivals */
	private long avgArrivalInterval;
	/** The maximum time quant used by the RR algorithm */
	private long maxCpuTime;
	/** phase number used for priority-based RR */
	private int priorityRrPhase;
	/** number of items in phase left to be processed before next */
	private int itemsInPhase;
	/** true to use priority RR */
	private boolean priorityRR;

	/**
	 * Constructs a scheduling simulator with the given parameters.
	 * @param memoryQueue			The memory queue to be used.
	 * @param cpuQueue				The CPU queue to be used.
	 * @param ioQueue				The I/O queue to be used.
	 * @param memorySize			The size of the memory.
	 * @param maxCpuTime			The maximum time quant used by the RR algorithm.
	 * @param avgIoTime				The average length of an I/O operation.
	 * @param simulationLength		The length of the simulation.
	 * @param avgArrivalInterval	The average time between process arrivals.
	 * @param gui					Reference to the GUI interface.
	 */
	public Simulator(Queue memoryQueue, Queue cpuQueue, Queue ioQueue, long memorySize,
			long maxCpuTime, long avgIoTime, long simulationLength, long avgArrivalInterval, Gui gui) {
		this.simulationLength = simulationLength;
		this.avgArrivalInterval = avgArrivalInterval;
		this.maxCpuTime = maxCpuTime;
		this.gui = gui;
		statistics = new Statistics();
		eventQueue = new EventQueue();
		memory = new Memory(memoryQueue, memorySize, statistics);
		cpu = new CPU(cpuQueue, maxCpuTime, statistics);
		io = new IO(ioQueue, avgIoTime, statistics);
		clock = 0;
		// Add code as needed
		
		itemsInPhase = 0;
		priorityRR = false;
		priorityRrPhase = 0;
    }

    /**
	 * Starts the simulation. Contains the main loop, processing events.
	 * This method is called when the "Start simulation" button in the
	 * GUI is clicked.
	 */
	public void simulate() {
		System.out.print("Simulating...");
		// Generate the first process arrival event
		eventQueue.insertEvent(new Event(NEW_PROCESS, 0, clock));
		// Process events until the simulation length is exceeded:
		while (clock < simulationLength && !eventQueue.isEmpty()) {
			// Find the next event
			Event event = eventQueue.getNextEvent();
			// Find out how much time that passed...
			long timeDifference = event.getTime()-clock;				
			// ...and update the clock.
			clock = event.getTime();
			// Let the memory unit and the GUI know that time has passed
			
			if (timeDifference < 0) {
				System.out.println(event.getTime());
			}
			
			
			memory.timePassed(timeDifference);
			cpu.timePassed(timeDifference);
			io.timePassed(timeDifference);
			gui.timePassed(timeDifference);
			// Deal with the event
			if (clock < simulationLength) {
				processEvent(event);
			}

			// Note that the processing of most events should lead to new
			// events being added to the event queue!

		}
		System.out.println("..done.");
		// End the simulation by printing out the required statistics
		statistics.printReport(simulationLength);
	}

	/**
	 * Processes an event by inspecting its type and delegating
	 * the work to the appropriate method.
	 * @param event	The event to be processed.
	 */
	private void processEvent(Event event) {
		switch (event.getType()) {
			case NEW_PROCESS:
				createProcess();
				break;
			case SWITCH_PROCESS:
				if (priorityRR) {
					switchProcessPriority();
				} else {
					switchProcess();					
				}
				break;
			case END_PROCESS:
				if (priorityRR) {
					endProcessPriority();
				} else {
					endProcess();					
				}
				break;
			case IO_REQUEST:
				if (priorityRR) {
					processIoRequestPriority();
				} else {
					processIoRequest();					
				}
				break;
			case END_IO:
				if (priorityRR) {
					endIoOperationPriority();
				} else {
					endIoOperation();					
				}
				break;
		}
	}
	
	/**
	 * Transfers processes from the memory queue to the ready queue as long as there is enough
	 * memory for the processes.
	 */
	private void flushMemoryQueue() {
		Process p = memory.checkMemory(clock);
		// As long as there is enough memory, processes are moved from the memory queue to the cpu queue
		while(p != null) {
			
			if (cpu.readyQueueIsEmpty() && cpu.getActiveProcess() == null) {
				cpu.insertProcess(p);
				runProcessCpu();
			} else {
				cpu.insertProcess(p);
			}

			// Check for more free memory
			p = memory.checkMemory(clock);
		}
	}

	private void runProcessCpu() {
		if (cpu.getActiveProcess() == null) {
			Process p = cpu.activateProcess(clock);
			if (p == null) {
				System.err.println("Tried to run process when readyQueue was empty");
				gui.setCpuActive(null);
			} else {
				gui.setCpuActive(p);
				endSwitchOrIo(p);					
			}
		} else {
			System.err.println("fill empty cpu when cpu was filled");
		}
	}
	
	private void runProcessIo() {
		if (io.getActiveProcess() == null) {
			Process p = io.activateProcess(clock);
			if (p == null) {
				System.err.println("Tried to run process when ioQueue was empty");
				gui.setIoActive(null);
			} else {
				gui.setIoActive(p);
				io.updateNextIoTime(clock);
				eventQueue.insertEvent(new Event(END_IO, io.getNextIoTime(clock), clock));
			}
		} else {
			System.err.println("Tried to fill empty io when io was filled");
		}
	}

	/**
	 * Simulates a process arrival/creation.
	 */
	private void createProcess() {
		// Create a new process
		Process newProcess = new Process(memory.getMemorySize(), clock);
		memory.insertProcess(newProcess);
		if (priorityRR) {
			flushMemoryQueuePriority();
		} else {
			flushMemoryQueue();			
		}
		// Add an event for the next process arrival
		long nextArrivalTime = clock + 1 + (long)(2*Math.random()*avgArrivalInterval);
		eventQueue.insertEvent(new Event(NEW_PROCESS, nextArrivalTime, clock));
		// Update statistics
		statistics.nofCreatedProcesses++;
    }
	
	/**
	 * Simulates a process switch.
	 */
	private void switchProcess() {
		//swap processes
		if (cpu.getActiveProcess() == null) {
			System.err.println("Tried to swap without active process");
		}
		
		Process p = cpu.switchProcess(clock);


		//set marker if no process could be set active (should not happen at switch,
		//should switch in the same process again if so)

		if (p == null) {
			if (cpu.getActiveProcess() != null) {
				System.err.println("error in switchProcess");
			}
			cpu.setActiveProcess(null);
			gui.setCpuActive(null);
		} else {
			gui.setCpuActive(p);
			endSwitchOrIo(p);					
		}
	}
	

	/**
	 * Ends the active process, and deallocates any resources allocated to it.
	 */
	private void endProcess() {
		System.out.println("Started endProcess");
		//deallocate resources
		Process p = cpu.getActiveProcess();
		if (cpu.getActiveProcess() == null) {
			System.err.println("Attempted end with no active process");
		} else {
			memory.processCompleted(p);			
		}
		// Try to use the freed memory:
		flushMemoryQueue();			
		// Update statistics
		p.updateStatistics(statistics);
		//Activate next process
		Process next = cpu.activateNext(clock);
		//set marker if no process could be set active (empty queue)
		if (next == null) {
			if (cpu.getActiveProcess() != null) {
				System.err.println("error in endprocess");
			}
			cpu.setActiveProcess(null);
			gui.setCpuActive(null);
		} else {
			gui.setCpuActive(next);
			endSwitchOrIo(p);					
		}
		System.out.println("Ended endProcess");
		
	}

	/**
	 * Processes an event signifying that the active process needs to
	 * perform an I/O operation.
	 */
	private void processIoRequest() {
		//insert active cpu process into ioQueue
		boolean runIo = false;
		if (cpu.getActiveProcess() != null) {		
			//if activeProcess is empty (and ioQueue), add event to immediately run the inserted process
			if (io.ioQueueIsEmpty() && io.getActiveProcess() == null) {
				runIo = true;	
			}
			io.insertProcess(cpu.getActiveProcess());
		} else {
			System.err.println("IOrequest with empty cpu device");
		}
		
		//activate next process in readyQueue
		Process	p = cpu.activateNext(clock);
		if (runIo) {
			runProcessIo();
		}
		
		//set marker if no process could be activated (empty queue)
		if (p == null) {
			if (cpu.getActiveProcess() != null) {
				System.err.println("Error in processIO");
			}
			cpu.setActiveProcess(null);
			gui.setCpuActive(null);
		} else {
			gui.setCpuActive(p);
			endSwitchOrIo(p);					
		}
		
	}

	/**
	 * Processes an event signifying that the process currently doing I/O
	 * is done with its I/O operation.
	 */
	private void endIoOperation() {
		boolean runCpu = false;
		statistics.nofProcessedIoOperations++;
		//Inserting finished process back into cpu queue
		if (cpu.readyQueueIsEmpty() && cpu.getActiveProcess() == null) {
			runCpu = true;
		}
		cpu.insertProcess(io.getActiveProcess());
		//Activating the next process in the ioQueue
		Process	p = io.activateNext(clock);
		if (runCpu) {
			runProcessCpu();
		}

		
		//Set a marker if no process could be activated (empty queue)
		if (p == null) {
			if (io.getActiveProcess() != null) {
				System.err.println("Error in endIo");
			}
			io.setActiveProcess(null);
			gui.setIoActive(null);
		} else {
			gui.setIoActive(p);
			io.updateNextIoTime(clock);
			eventQueue.insertEvent(new Event(END_IO, io.getNextIoTime(clock), clock));
		}
	}
	
	/**
	 * Adds an event depending on what is next for the process activated in the CPU:
	 * Finished process
	 * Time quant over, switch process
	 * Time to I/O over, request I/O
	 * 
	 * @param p the process to be activated
	 */
	private void endSwitchOrIo(Process p) {
			if (p.getCpuTimeNeeded() < p.getTimeToNextIoOperation() && p.getCpuTimeNeeded() < maxCpuTime) {
				eventQueue.insertEvent(new Event(END_PROCESS, clock + p.getCpuTimeNeeded(), clock));
			} else if (p.getTimeToNextIoOperation() < p.getCpuTimeNeeded() && p.getTimeToNextIoOperation() < maxCpuTime) {
				eventQueue.insertEvent(new Event(IO_REQUEST, clock + p.getTimeToNextIoOperation(), clock));
			} else {
				eventQueue.insertEvent(new Event(SWITCH_PROCESS, clock + maxCpuTime, clock));
			}
	}
	/** 
	 * Priority RR version
	 * 
	 * @param p
	 * @param timeSlice
	 */
	
	private void endSwitchOrIo(Process p, long timeSlice) {
			if (p.getCpuTimeNeeded() < p.getTimeToNextIoOperation() && p.getCpuTimeNeeded() < timeSlice) {
				eventQueue.insertEvent(new Event(END_PROCESS, clock + p.getCpuTimeNeeded(), clock));
			} else if (p.getTimeToNextIoOperation() < p.getCpuTimeNeeded() && p.getTimeToNextIoOperation() < timeSlice) {
				eventQueue.insertEvent(new Event(IO_REQUEST, clock + p.getTimeToNextIoOperation(), clock));
			} else {
				eventQueue.insertEvent(new Event(SWITCH_PROCESS, clock + timeSlice, clock));
			}
	}
	
	private void flushMemoryQueuePriority() {
		Process p = memory.checkMemory(clock);
		// As long as there is enough memory, processes are moved from the memory queue to the cpu queue
		while(p != null) {
			
			if (priorityRrPhase == 0) {
				cpu.insertPqProcess(p);
				itemsInPhase++;
				priorityRrPhase = 1;
			}
			if (cpu.readyQueueIsEmpty() && cpu.getActiveProcess() == null) {
				cpu.insertProcess(p);
				runProcessCpuPriority();
			} else {
				cpu.insertProcess(p);
			}

			// Check for more free memory
			p = memory.checkMemory(clock);
		}
	}
	
	private void switchProcessPriority() {
		//swap processes
		if (cpu.getActiveProcess() == null) {
			System.err.println("Tried to swap without active process");
		}
		
		Process p;
		if (priorityRrPhase == 1) {
			p = cpu.switchProcess(clock);			
		} else if (priorityRrPhase == 2) {
			p = cpu.switchProcessPrioritized(clock);
		} else {
			p = cpu.switchProcess(clock);
		}

		//set marker if no process could be set active (should not happen at switch,
		//should switch in the same process again if so)

		if (p == null) {
			if (cpu.getActiveProcess() != null) {
				System.err.println("error in switchProcess");
			}
			cpu.setActiveProcess(null);
			gui.setCpuActive(null);
		} else {
			itemsInPhase--;
			if (itemsInPhase <= 0) {
				itemsInPhase = cpu.addReadyQueueToPq();
				if (priorityRrPhase == 1) {
					priorityRrPhase = 2;
				} else if (priorityRrPhase == 2) {
					priorityRrPhase = 1;
				}
			}
			gui.setCpuActive(p);
			if (priorityRrPhase == 1) {
				endSwitchOrIo(p, maxCpuTime);
			} else if (priorityRrPhase == 2){
				endSwitchOrIo(p, p.getCpuTimeNeeded());
			}
		}
	}
	
	private void endProcessPriority() {
		//deallocate resources
		Process p = cpu.getActiveProcess();
		if (cpu.getActiveProcess() == null) {
			System.err.println("Attempted end with no active process");
		} else {
			memory.processCompleted(p);			
		}
		// Try to use the freed memory:
		flushMemoryQueuePriority();
		// Update statistics
		p.updateStatistics(statistics);
		//Activate next process
		Process next;
		if (priorityRrPhase == 1) {
			next = cpu.activateNext(clock);
		} else if (priorityRrPhase == 2) {
			next = cpu.activateNextPrioritized(clock);
		} else {
			next = cpu.activateNext(clock);
		}
		//set marker if no process could be set active (empty queue)
		if (next == null) {
			if (cpu.getActiveProcess() != null) {
				System.err.println("error in endprocess");
			}
			cpu.setActiveProcess(null);
			gui.setCpuActive(null);
		} else {
			itemsInPhase--;
			if (itemsInPhase <= 0) {
				itemsInPhase = cpu.addReadyQueueToPq();
				if (priorityRrPhase == 1) {
					priorityRrPhase = 2;
				} else if (priorityRrPhase == 2) {
					priorityRrPhase = 1;
				}
			}
			gui.setCpuActive(next);
				if (priorityRrPhase == 1) {
					endSwitchOrIo(p, maxCpuTime);
				} else if (priorityRrPhase == 2){
					endSwitchOrIo(p, p.getCpuTimeNeeded());
				}
		}
		
	}
	
	private void processIoRequestPriority() {
		//insert active cpu process into ioQueue
		boolean runIo = false;
		if (cpu.getActiveProcess() != null) {
			//if activeProcess is empty (and ioQueue), add event to immediately run the inserted process
			if (io.ioQueueIsEmpty() && io.getActiveProcess() == null) {
				io.insertProcess(cpu.getActiveProcess());
				runIo = true;
				
			} else {
				io.insertProcess(cpu.getActiveProcess());
			}
		} else {
			System.err.println("IOrequest with empty cpu device");
		}
		
		//activate next process in readyQueue
		Process p;
		if (priorityRrPhase == 1) {
			p = cpu.activateNext(clock);
		} else if (priorityRrPhase == 2) {
			p = cpu.activateNextPrioritized(clock);
		} else {
			p = cpu.activateNext(clock);
		}
		if (runIo) {
			runProcessIo();
		}
		
		//set marker if no process could be activated (empty queue)
		if (p == null) {
			if (cpu.getActiveProcess() != null) {
				System.err.println("Error in processIO");
			}
			cpu.setActiveProcess(null);
			gui.setCpuActive(null);
		} else {
			itemsInPhase--;
			if (itemsInPhase <= 0) {
				itemsInPhase = cpu.addReadyQueueToPq();
				if (priorityRrPhase == 1) {
					priorityRrPhase = 2;
				} else if (priorityRrPhase == 2) {
					priorityRrPhase = 1;
				}
			}
			gui.setCpuActive(p);
			if (priorityRrPhase == 1) {
				endSwitchOrIo(p, maxCpuTime);
			} else if (priorityRrPhase == 2){
				endSwitchOrIo(p, p.getCpuTimeNeeded());
			}
		}
		
	}
	
	private void endIoOperationPriority() {
		statistics.nofProcessedIoOperations++;
		boolean runCPU = false;
		//Inserting finished process back into cpu queue
		if (cpu.readyQueueIsEmpty() && cpu.getActiveProcess() == null) {
			cpu.insertProcess(io.getActiveProcess());
			runCPU = true;
		} else {
			cpu.insertProcess(io.getActiveProcess());
		}
		//Activating the next process in the ioQueue
		Process	p = io.activateNext(clock);
		if (runCPU) {
			runProcessCpuPriority();
		}

		
		//Set a marker if no process could be activated (empty queue)
		if (p == null) {
			if (io.getActiveProcess() != null) {
				System.err.println("Error in endIo");
			}
			io.setActiveProcess(null);
			gui.setIoActive(null);
		} else {
			gui.setIoActive(p);
			io.updateNextIoTime(clock);
			eventQueue.insertEvent(new Event(END_IO, io.getNextIoTime(clock), clock));
		}
	}
	
	private void runProcessCpuPriority() {
		System.out.println("Ran prioritized runProcess");
		if (cpu.getActiveProcess() == null) {
			Process p;
			if (priorityRrPhase == 1) {
				p = cpu.activateProcess(clock);			
			} else if (priorityRrPhase == 2) {
				p = cpu.activateProcessPrioritized(clock);
			} else {
				p = cpu.activateProcess(clock);
			}
			if (p == null) {
				System.err.println("Tried to run process when readyQueue was empty");
				gui.setCpuActive(null);
			} else {
				itemsInPhase--;
				if (itemsInPhase <= 0) {
					itemsInPhase = cpu.addReadyQueueToPq();
					if (priorityRrPhase == 1) {
						priorityRrPhase = 2;
					} else if (priorityRrPhase == 2) {
						priorityRrPhase = 1;
					}
				}
				gui.setCpuActive(p);
				if (priorityRrPhase == 1) {
					endSwitchOrIo(p, maxCpuTime);
				} else if (priorityRrPhase == 2){
					endSwitchOrIo(p, p.getCpuTimeNeeded());
				}

			}
		} else {
			System.err.println("fill empty cpu when cpu was filled");
		}
	}
	
	
	

	/**
	 * Reads a number from the an input reader.
	 * @param reader	The input reader from which to read a number.
	 * @return			The number that was inputted.
	 */
	public static long readLong(BufferedReader reader) {
		try {
			return Long.parseLong(reader.readLine());
		} catch (IOException ioe) {
			return 100;
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	/**
	 * The startup method. Reads relevant parameters from the standard input,
	 * and starts up the GUI. The GUI will then start the simulation when
	 * the user clicks the "Start simulation" button.
	 * @param args	Parameters from the command line, they are ignored.
	 */
	public static void main(String args[]) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Please input system parameters: ");

		System.out.print("Memory size (KB): ");
		long memorySize = readLong(reader);
		while(memorySize < 400) {
			System.out.println("Memory size must be at least 400 KB. Specify memory size (KB): ");
			memorySize = readLong(reader);
		}

		System.out.print("Maximum uninterrupted cpu time for a process (ms): ");
		long maxCpuTime = readLong(reader);

		System.out.print("Average I/O operation time (ms): ");
		long avgIoTime = readLong(reader);

		System.out.print("Simulation length (ms): ");
		long simulationLength = readLong(reader);
		while(simulationLength < 1) {
			System.out.println("Simulation length must be at least 1 ms. Specify simulation length (ms): ");
			simulationLength = readLong(reader);
		}

		System.out.print("Average time between process arrivals (ms): ");
		long avgArrivalInterval = readLong(reader);

		SimulationGui gui = new SimulationGui(memorySize, maxCpuTime, avgIoTime, simulationLength, avgArrivalInterval);
	}
}
