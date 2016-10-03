package davidRichardson;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ThreadAnalyzer implements Runnable 
{
	private static final Logger m_Logger = Logger.getLogger( MyLogger.class.getName() );

	// Separate thread for Analyzer to run
	private Thread                   m_AnalyzerThread;

	private String                   m_ExcelFilename = new String("");
	private ArrayList <DBResult>     m_DBResultList  = null;

	// Analyzer used by thread
	private Analyzer                 m_Analyzer;

	private Boolean                  m_AnalyzerRunning;
	//	static Object                    m_Lock = new Object();
	private Object                   m_Lock;
	
	
	private boolean                  m_SummaryOnly       = false;
	private boolean                  m_FullRangeAnalysis = false;
	
	private Analyzer.AnalyzerMode    m_AnalyzerMode      = Analyzer.AnalyzerMode.normal;

	// Thread Synchronization
	public void waitUntilFree()
	{
		synchronized(m_Lock)
		{
			while (m_AnalyzerRunning)
			{
				try
				{
					m_Logger.log( Level.FINE, "ThreadAnalyzer Wait - Running & about to try lock: " + this );
					m_Lock.wait();
					m_Logger.log( Level.FINE, "ThreadAnalyzer Wait - Running & notified : " + this );
				} 
				catch (InterruptedException e) 
				{					
					m_Logger.log(Level.SEVERE, "<"+this.getClass().getName()+">" + "ThreadAnalyzer Wait - EXCEPTION CAUGHT.", e);
				}
			}
			m_Logger.log( Level.FINE, "ThreadAnalyzer Wait - No longer running: " + this);
		}
	}
	// Handler to notify when analyzer completes
	AnalyzerCompleteHander           m_CompleteHandler;

	// Thread Handler for resynchronization
	public static abstract class AnalyzerCompleteHander 
	{
		private Object m_Object;
		public AnalyzerCompleteHander(Object obj)
		{
			m_Object = obj;
		}
		public abstract void analyzeResultsComplete(Object obj);
		public abstract void exceptionRaised(String message);

		public Object getM_Object()
		{
			return m_Object;
		}
	}

	public ThreadAnalyzer(Analyzer analyzer)
	{
		m_AnalyzerRunning = true;  // Initialise the thread in running state
		m_AnalyzerThread  = new Thread(this);
		m_Analyzer        = analyzer;
		m_CompleteHandler = null;

		// Thread synchronization
		m_Lock            = new Object();
	}

	public ThreadAnalyzer(ArrayList <DBResult> results)
	{
		m_AnalyzerRunning = true;  // Initialise the thread in running state
		m_AnalyzerThread  = new Thread(this);
		m_Analyzer        = new Analyzer(results);
		m_CompleteHandler = null;

		// Thread synchronization
		m_Lock            = new Object();
	}
	
	public ThreadAnalyzer(ArrayList <DBResult> results, boolean summaryOnly)
	{
		m_AnalyzerRunning = true;  // Initialise the thread in running state
		m_AnalyzerThread  = new Thread(this);
		m_Analyzer        = new Analyzer(results, summaryOnly);
		m_CompleteHandler = null;

		// Thread synchronization
		m_Lock            = new Object();
		m_SummaryOnly     = summaryOnly;
	}

	public ThreadAnalyzer(ArrayList <DBResult> results, Analyzer.AnalyzerMode mode)
	{
		m_AnalyzerRunning = true;  // Initialise the thread in running state
		m_AnalyzerThread  = new Thread(this);
		m_Analyzer        = new Analyzer(results, mode);
		m_CompleteHandler = null;

		// Thread synchronization
		m_Lock            = new Object();
		
		m_AnalyzerMode    = mode;
	}

	public void analyzeResults(AnalyzerCompleteHander completeHandler)
	{
		m_CompleteHandler = completeHandler;
		m_AnalyzerThread.start();
	}

	public void run() 
	{
		m_AnalyzerRunning = true;		
		synchronized(m_Lock)
		{
			// Launch the analyze method
			
			Analyzer.AnalyzerResult analyzerResult = m_Analyzer.analyzeResults(m_DBResultList, m_ExcelFilename);
			m_Logger.log(Level.FINE, "analyzeResults(m_DBResultList, m_ExcelFilename) returned: " + analyzerResult);
			
			if (analyzerResult == Analyzer.AnalyzerResult.analysisComplete)
			{
				m_CompleteHandler.analyzeResultsComplete(m_CompleteHandler.getM_Object());
			}
			else if (analyzerResult == Analyzer.AnalyzerResult.datesAreReversed)
			{
				m_Logger.log(Level.FINE, "Raising exception now");
				m_CompleteHandler.exceptionRaised("Analysis did not run since start and end dates are reversed.  Please check and try again.");
			}
			else if (analyzerResult == Analyzer.AnalyzerResult.noDataToAnalyze)
			{
				m_Logger.log(Level.FINE, "Raising exception now");
				m_CompleteHandler.exceptionRaised("There are no results to analyze");
			}

			m_AnalyzerRunning = false;
			m_Lock.notifyAll();
		}
	}

	/**
	 * @return the m_ExcelFilename
	 */
	public synchronized String getM_ExcelFilename() {
		return m_ExcelFilename;
	}

	/**
	 * @param m_ExcelFilename the m_ExcelFilename to set
	 */
	public synchronized void setM_ExcelFilename(String m_ExcelFilename) {
		this.m_ExcelFilename = m_ExcelFilename;
	}

	/**
	 * @return the m_DBResultList
	 */
	public synchronized ArrayList<DBResult> getM_DBResultList() {
		return m_DBResultList;
	}

	/**
	 * @param m_DBResultList the m_DBResultList to set
	 */
	public synchronized void setM_DBResultList(ArrayList<DBResult> m_DBResultList) {
		this.m_DBResultList = m_DBResultList;
	}

	/**
	 * @return the m_Analyzer
	 */
	public synchronized Analyzer getM_Analyzer() {
		return m_Analyzer;
	}

	/**
	 * @param m_Analyzer the m_Analyzer to set
	 */
	public synchronized void setM_Analyzer(Analyzer m_Analyzer) {
		this.m_Analyzer = m_Analyzer;
	}

	/**
	 * @return the m_AnalyzerRunning
	 */
	public synchronized Boolean getM_AnalyzerRunning() {
		return m_AnalyzerRunning;
	}

	/**
	 * @param m_AnalyzerRunning the m_AnalyzerRunning to set
	 */
	public synchronized void setM_AnalyzerRunning(Boolean m_AnalyzerRunning) {
		this.m_AnalyzerRunning = m_AnalyzerRunning;
	}

	
}
