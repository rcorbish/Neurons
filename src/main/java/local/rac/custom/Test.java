package local.rac.custom;

import java.util.Random;

import org.apache.log4j.Level;

public class Test {
	static Random rng = new Random( 200 );

	public static void main(String[] args) {
		final int INPUT_COUNT = 10 ;
		final int OUTPUT_COUNT = 6 ;
		try {
			Brain brain = new Brain( 0.2, Brain.STANDARD, INPUT_COUNT, OUTPUT_COUNT, new int[] { 3, 6, 3 } ) ;

			/*RestServer server = */new WebServer( brain ) ;
			double inputs[] = new double[INPUT_COUNT] ;
			for( ; ; ) {
				if( rng.nextInt( 10 ) == 0 ) {
					for( int i=0 ; i<INPUT_COUNT ; i++ ) {
						inputs[i] = rng.nextDouble() * 100 ;
					}
					brain.clock( inputs ); 
				} else {
					brain.clock();
				}
				Thread.sleep(50);
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
