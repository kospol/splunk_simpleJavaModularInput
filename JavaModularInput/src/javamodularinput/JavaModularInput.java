package javamodularinput;

import com.splunk.modularinput.Argument;
import com.splunk.modularinput.Event;
import com.splunk.modularinput.EventWriter;
import com.splunk.modularinput.InputDefinition;
import com.splunk.modularinput.MalformedDataException;
import com.splunk.modularinput.Scheme;
import com.splunk.modularinput.Script;
import com.splunk.modularinput.SingleValueParameter;
import com.splunk.modularinput.ValidationDefinition;
import java.io.IOException;
import java.util.Random;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author kospol
 */
public class JavaModularInput extends Script {

    /**
     * Starts the Script
     */
    public static void main(String[] args) {
        new JavaModularInput().run(args);
    }

    /**
     * Creates the Scheme for the Modular Input
     * @return the Scheme
     */
    @Override
    public Scheme getScheme() {
        Scheme scheme = new Scheme("Java RandomNumbers ModInput");
        scheme.setDescription("Generates events containing a random number.");
        scheme.setUseExternalValidation(true);
        scheme.setUseSingleInstance(true);

        Argument minArgument = new Argument("min");
        minArgument.setDataType(Argument.DataType.NUMBER);
        minArgument.setDescription("Minimum value to be produced by this input.");
        minArgument.setRequiredOnCreate(true);
        scheme.addArgument(minArgument);

        Argument maxArgument = new Argument("max");
        maxArgument.setDataType(Argument.DataType.NUMBER);
        maxArgument.setDescription("Maximum value to be produced by this input.");
        maxArgument.setRequiredOnCreate(true);
        scheme.addArgument(maxArgument);

        Argument timeArgument = new Argument("waitfor");
        timeArgument.setDataType(Argument.DataType.NUMBER);
        timeArgument.setDescription("How often the input will produce the random numbers (in seconds)");
        timeArgument.setRequiredOnCreate(true);
        scheme.addArgument(timeArgument);

        return scheme;
    }

    @Override
    public void validateInput(ValidationDefinition definition) throws Exception {
        // Get the values of the parameters
        int min = ((SingleValueParameter) definition.getParameters().get("min")).getInt();
        int max = ((SingleValueParameter) definition.getParameters().get("max")).getInt();
        int waitfor = ((SingleValueParameter) definition.getParameters().get("waitfor")).getInt();
        
        //Validate the parameters
        if (min >= max) {
            throw new Exception("min must be less than max; found min=" + Double.toString(min)
                    + ", max=" + Double.toString(max));
        }

        //Validate the parameters
        if (waitfor <= 0) {
            throw new Exception("waitfor must be at least 1 second");
        }
    }

    @Override
    public void streamEvents(InputDefinition inputs, EventWriter ew) throws MalformedDataException,
            XMLStreamException, IOException {
        for (String inputName : inputs.getInputs().keySet()) {
            // We get the parameters for each input and start a new thread for each one. 
            // All the real work happens in the class for event generation.
            int min = ((SingleValueParameter) inputs.getInputs().get(inputName).get("min")).getInt();
            int max = ((SingleValueParameter) inputs.getInputs().get(inputName).get("max")).getInt();
            int waitfor = ((SingleValueParameter) inputs.getInputs().get(inputName).get("waitfor")).getInt();

            Thread t = new Thread(new Generator(ew, inputName, min, max, waitfor));
            t.run();
        }
    }

    final Random randomGenerator = new Random(System.currentTimeMillis());

    /**
     * A class that generates random numbers and sends the events to Splunk
     */
    class Generator implements Runnable {

        private final int min, max, waitfor;
        private final EventWriter ew;
        private final String inputName;

        public Generator(EventWriter ew, String inputName, int min, int max, int waitfor) {
            super();
            this.min = min;
            this.max = max;
            this.ew = ew;
            this.waitfor = waitfor;
            this.inputName = inputName;
        }

        @Override
        public void run() {
            ew.synchronizedLog(EventWriter.INFO, "Random number generator " + inputName
                    + " started, generating numbers between "
                    + Integer.toString(min) + " and " + Integer.toString(max)
                    + " with time set to: " + Integer.toString(waitfor));

            while (true) {
                Event event = new Event();
                event.setStanza(inputName);
                event.setData("number=" + (randomGenerator.nextInt((max - min) + 1) + min));
                try {
                    ew.writeEvent(event);
                } catch (MalformedDataException e) {
                    ew.synchronizedLog(EventWriter.ERROR, "MalformedDataException in writing event to input"
                            + inputName + ": " + e.toString());
                }

                try {
                    Thread.sleep(waitfor * 1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}