package com.example.android.activitymonitor.feature;

import org.tensorflow.Graph;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import android.content.Context;

public class Classifier {

    private TensorFlowInferenceInterface infr;
    //our model
    private static final String MODEL_FILE = "file:///android_asset/har-model.pb";
    //the name of the input node
    private static final String iptNode = "inputs";
    //the name of the output node
    private static final String[] optNodes = {"y_pred"};
    private static final String output = "y_pred";
    //it takes 128 variables for x y and z
    private static final long[] iptSize = {1, 128, 9};
    private static final int optSize = 2; //change to 2

    //set up the tensorflow library
    static {
        System.loadLibrary("tensorflow_inference");
    }

    public Classifier(final Context context) {
        //initialize a new inference interface
        infr = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
    }

    public float[] predict(float[] data) {
        float[] result = new float[optSize];
        //send the data to our model

        infr.feed(iptNode, data, iptSize);
        //run the data through the model
        infr.run(optNodes);
        //get the results from the model
        infr.fetch(output, result);

        return result;
    }
}
