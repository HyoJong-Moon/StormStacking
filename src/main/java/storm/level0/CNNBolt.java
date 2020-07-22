package storm.level0;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.LoggerFactory;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.util.Map;
import java.util.logging.Logger;

public class CNNBolt extends BaseRichBolt {
    private Log log = LogFactory.getLog(CNNBolt.class);
    private OutputCollector outputCollector;
    private SavedModelBundle savedModelBundle;
    private Session sess;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.outputCollector = outputCollector;
        this.savedModelBundle = SavedModelBundle.load("/home/hjmoon/models/url/","serve");
        this.sess = savedModelBundle.session();
    }

    @Override
    public void execute(Tuple tuple) {
        int[] data = (int[]) tuple.getValueByField("url");
        int[][] input = new int[1][80];
        for(int i = 0; i < 80; i++) {
            input[0][i] = data[i];
        }
        Tensor x = Tensor.create(input);
        Tensor result = sess.runner()
                .feed("cnn_input:0", x)
                .fetch("cnn_output/Sigmoid:0")
                .run()
                .get(0);

        float[][] prob = (float[][]) result.copyTo(new float[1][1]);

        outputCollector.emit(new Values(data, prob));
        outputCollector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("url", "cnn"));
    }
}
