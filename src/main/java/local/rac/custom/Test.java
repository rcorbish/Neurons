package local.rac.custom;

import java.util.Random;

import org.apache.log4j.Level;

public class Test {
	static Random rng = new Random( 200 );

	public static void main(String[] args) {
		final int INPUT_COUNT = 2 ;
		final int OUTPUT_COUNT = 1 ;
		int clock = 0 ;
		try {
			Brain brain = new Brain( Brain.STANDARD, INPUT_COUNT, OUTPUT_COUNT, new int[] { 6, 8 } ) ;

			/*RestServer server = */new WebServer( brain ) ;
			double inputs[] = new double[INPUT_COUNT] ;
			double outputs[][] = new double[50][OUTPUT_COUNT] ;
			
			int n = 0 ;
			for( ; ; ) {
				if( ++n > 40 ) n = 0 ;
				outputs[clock][0] = 0.0 ;
				for( int i=0 ; i<INPUT_COUNT ; i++ ) {
					inputs[i] = rng.nextInt( clock+1 ) ;
					outputs[clock][0] += inputs[i] ;
				}
				double err = brain.train( inputs, outputs[clock] ) ;
				System.out.println( "Error:" + err ) ;

				clock++ ;
				if( clock == outputs.length ) {
					clock=10 ;					
				}
		
				Thread.sleep(100);
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}


	static {
		org.apache.log4j.LogManager.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
		final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("org.eclipse.jetty");
		if ((logger instanceof ch.qos.logback.classic.Logger)) {
			ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) logger;
			logbackLogger.setLevel(ch.qos.logback.classic.Level.WARN);
		}
	}
}
