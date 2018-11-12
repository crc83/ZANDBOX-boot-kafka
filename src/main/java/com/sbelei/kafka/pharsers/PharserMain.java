package com.sbelei.kafka.pharsers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.sbelei.pharsers.MarkupParser.Java9Lexer;
import org.sbelei.pharsers.MarkupParser.Java9Parser;

public class PharserMain {

//	public static long lexerTime = 0;
	public static boolean profile = false;
	public static boolean notree = false;
	public static boolean gui = false;
	public static boolean printTree = false;
	public static boolean SLL = false;
	public static boolean diag = false;
	public static boolean bail = false;
	public static boolean x2 = false;
	public static boolean threaded = false;
	public static boolean quiet = false;
//	public static long parserStart;
//	public static long parserStop;
	public static Worker[] workers = new Worker[3];
	static int windex = 0;

	public static CyclicBarrier barrier;

	public static volatile boolean firstPassDone = false;

	public static class Worker implements Runnable {
		public long parserStart;
		public long parserStop;
		List<String> files;
		public Worker(List<String> files) {
			this.files = files;
		}
		@Override
		public void run() {
			parserStart = System.currentTimeMillis();
			for (String f : files) {
				parseFile(f);
			}
			parserStop = System.currentTimeMillis();
			try {
				barrier.await();
			}
			catch (InterruptedException ex) {
				return;
			}
			catch (BrokenBarrierException ex) {
				return;
			}
		}
	}

	public static void main(String[] args) {
		args = new String[] {"C:\\projects\\ZANDBOX-boot-kafka\\src\\main\\java", "-ptree", "-gui", "-2x"};
		doAll(args);
	}

	public static void doAll(String[] args) {
		List<String> inputFiles = new ArrayList<String>();
		long start = System.currentTimeMillis();
		try {
			if (args.length > 0 ) {
				// for each directory/file specified on the command line
				for(int i=0; i< args.length;i++) {
					if ( args[i].equals("-notree") ) notree = true;
					else if ( args[i].equals("-gui") ) gui = true;
					else if ( args[i].equals("-ptree") ) printTree = true;
					else if ( args[i].equals("-SLL") ) SLL = true;
					else if ( args[i].equals("-bail") ) bail = true;
					else if ( args[i].equals("-diag") ) diag = true;
					else if ( args[i].equals("-2x") ) x2 = true;
					else if ( args[i].equals("-threaded") ) threaded = true;
					else if ( args[i].equals("-quiet") ) quiet = true;
					if ( args[i].charAt(0)!='-' ) { // input file name
						inputFiles.add(args[i]);
					}
				}
				List<String> javaFiles = new ArrayList<String>();
				for (String fileName : inputFiles) {
					List<String> files = getFilenames(new File(fileName));
					javaFiles.addAll(files);
				}
				doFiles(javaFiles);

//				DOTGenerator gen = new DOTGenerator(null);
//				String dot = gen.getDOT(Java9Parser._decisionToDFA[112], false);
//				System.out.println(dot);
//				dot = gen.getDOT(Java9Parser._decisionToDFA[81], false);
//				System.out.println(dot);

				if ( x2 ) {
					System.gc();
					System.out.println("waiting for 1st pass");
					if ( threaded ) while ( !firstPassDone ) { } // spin
					System.out.println("2nd pass");
					doFiles(javaFiles);
				}
			}
			else {
				System.err.println("Usage: java Main <directory or file name>");
			}
		}
		catch(Exception e) {
			System.err.println("exception: "+e);
			e.printStackTrace(System.err);   // so we can get stack trace
		}
		long stop = System.currentTimeMillis();
//		System.out.println("Overall time " + (stop - start) + "ms.");
		System.gc();
	}

	public static void doFiles(List<String> files) throws Exception {
		long parserStart = System.currentTimeMillis();
//		lexerTime = 0;
		if ( threaded ) {
			barrier = new CyclicBarrier(3,new Runnable() {
				public void run() {
					report(); firstPassDone = true;
				}
			});
			int chunkSize = files.size() / 3;  // 10/3 = 3
			int p1 = chunkSize; // 0..3
			int p2 = 2 * chunkSize; // 4..6, then 7..10
			workers[0] = new Worker(files.subList(0,p1+1));
			workers[1] = new Worker(files.subList(p1+1,p2+1));
			workers[2] = new Worker(files.subList(p2+1,files.size()));
			new Thread(workers[0], "worker-"+windex++).start();
			new Thread(workers[1], "worker-"+windex++).start();
			new Thread(workers[2], "worker-"+windex++).start();
		}
		else {
			for (String f : files) {
				parseFile(f);
			}
			long parserStop = System.currentTimeMillis();
			System.out.println("Total lexer+parser time " + (parserStop - parserStart) + "ms.");
		}
	}

	private static void report() {
//		parserStop = System.currentTimeMillis();
//		System.out.println("Lexer total time " + lexerTime + "ms.");
		long time = 0;
		if ( workers!=null ) {
			// compute max as it's overlapped time
			for (Worker w : workers) {
				long wtime = w.parserStop - w.parserStart;
				time = Math.max(time,wtime);
				System.out.println("worker time " + wtime + "ms.");
			}
		}
		System.out.println("Total lexer+parser time " + time + "ms.");

		System.out.println("finished parsing OK");
		System.out.println(LexerATNSimulator.match_calls+" lexer match calls");
//		System.out.println(ParserATNSimulator.predict_calls +" parser predict calls");
//		System.out.println(ParserATNSimulator.retry_with_context +" retry_with_context after SLL conflict");
//		System.out.println(ParserATNSimulator.retry_with_context_indicates_no_conflict +" retry sees no conflict");
//		System.out.println(ParserATNSimulator.retry_with_context_predicts_same_alt +" retry predicts same alt as resolving conflict");
	}

	public static List<String> getFilenames(File f) throws Exception {
		List<String> files = new ArrayList<String>();
		getFilenames_(f, files);
		return files;
	}

	public static void getFilenames_(File f, List<String> files) throws Exception {
		// If this is a directory, walk each file/dir in that directory
		if (f.isDirectory()) {
			String flist[] = f.list();
			for(int i=0; i < flist.length; i++) {
				getFilenames_(new File(f, flist[i]), files);
			}
		}

		// otherwise, if this is a java file, parse it!
		else if ( ((f.getName().length()>5) &&
			f.getName().substring(f.getName().length()-5).equals(".java")) )
		{
			files.add(f.getAbsolutePath());
		}
	}

	// This method decides what action to take based on the type of
	//   file we are looking at
//	public static void doFile_(File f) throws Exception {
//		// If this is a directory, walk each file/dir in that directory
//		if (f.isDirectory()) {
//			String files[] = f.list();
//			for(int i=0; i < files.length; i++) {
//				doFile_(new File(f, files[i]));
//			}
//		}
//
//		// otherwise, if this is a java file, parse it!
//		else if ( ((f.getName().length()>5) &&
//			f.getName().substring(f.getName().length()-5).equals(".java")) )
//		{
//			System.err.println(f.getAbsolutePath());
//			parseFile(f.getAbsolutePath());
//		}
//	}

	public static void parseFile(String f) {
		try {
			if ( !quiet ) System.err.println(f);
			// Create a scanner that reads from the input stream passed to us
			Lexer lexer = new Java9Lexer(new ANTLRFileStream(f));

			CommonTokenStream tokens = new CommonTokenStream(lexer);
//			long start = System.currentTimeMillis();
//			tokens.fill(); // load all and check time
//			long stop = System.currentTimeMillis();
//			lexerTime += stop-start;

			// Create a parser that reads from the scanner
			Java9Parser parser = new Java9Parser(tokens);
			if ( diag ) parser.addErrorListener(new DiagnosticErrorListener());
			if ( bail ) parser.setErrorHandler(new BailErrorStrategy());
			if ( SLL ) parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

			// start parsing at the compilationUnit rule
			ParserRuleContext t = parser.compilationUnit();
			if ( notree ) parser.setBuildParseTree(false);
//			if ( gui ) t.inspect(parser);
			if ( printTree ) System.out.println(t.toStringTree(parser));
		}
		catch (Exception e) {
			System.err.println("parser exception: "+e);
			e.printStackTrace();   // so we can get stack trace
		}
	}
}
