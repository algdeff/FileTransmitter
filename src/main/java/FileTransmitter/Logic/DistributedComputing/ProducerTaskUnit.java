package FileTransmitter.Logic.DistributedComputing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


public class ProducerTaskUnit implements Callable<Object>, Serializable {

    private List<Integer> _inputValues = new ArrayList<>();

    public ProducerTaskUnit(List<Integer> inputValues) {
        _inputValues.addAll(inputValues);
    }

    @Override
    public Object call() throws Exception {
        return calculate();
    }

    private Object calculate() {
        Integer result = 0;

        for (Integer value : _inputValues) {
            result += value >> 2;

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(result);

        }


        return result;
    }

}
