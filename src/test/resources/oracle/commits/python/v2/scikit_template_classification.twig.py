import arff
import argparse
import json
import numpy as np
import os
import pickle
import sys
import warnings
from scipy.io import arff as scipy_arff
from os.path import join as path_join
import pandas as pd
{{imports}}

OUTPUT_FILE = None

"""
ArffStructure and parse implemented by Amin Faez.
"""

def parse(arff_, is_path=True):
    """ Opens and reads the file located at path.
    May also be called with the content string.
    arff_: either  arff file path or arff file content. treated based on how is_bath is assigned.
    is_path: bool, if true, arff_ is an arff-file path. If false, arff_ is treated as the content of an arff file
    as a string.
    Returns an ArffStructure object or None if there was an error.
    """
    if is_path:
        arff_data = open(arff_, 'r')
    else:
        arff_data = arff_  # here path is actually the content of an arff file.
        
    ## automatically find out whether the file is a sparse file
    seenData = False
    for x in arff_data:
        if x.isspace():
            continue
        if "@data" in x:
            seenData = True
            continue
        if not seenData:
            continue
        dense_mode =  not ("{" in x and "}" in x)
        break
    
    try:
        if is_path: # reload file, because we scanned over it already
            arff_data = open(arff_, 'r')
        if dense_mode:
            dfARFF = pd.DataFrame(scipy_arff.loadarff(arff_data)[0])
        else:
            arff_parsed = arff.load(arff_data, return_type=arff.LOD, encode_nominal=True)
            list_attributes = arff_parsed["attributes"]
            list_data = arff_parsed["data"]
            rows = np.zeros((len(list_data), len(list_attributes)))
            for i, entry in enumerate(list_data):
                for attr_index in entry:
                    rows[i][attr_index] = entry[attr_index]
            dfARFF = pd.DataFrame(rows, columns=[a[0] for a in list_attributes])
        
        # list_attributes must be a list of tuples (name, type)
        # list_data must be either a dictionary (sparse) or a list of lists
        class_attribute = None
        for a in dfARFF.columns:
            if a.lower() == "class":
                class_attribute = a
        if class_attribute is None:
            class_attribute = dfARFF.columns[-1]
        
        
        # replace nan values with 0
        dfARFF = dfARFF.fillna(0)
        return dfARFF, class_attribute
        
    except Exception as e:
        import traceback
        traceback.print_tb(e.__traceback__)
        return None  # return None to signify an error. (Raising Exception might be a better solution)
    finally:
        if is_path:  # close file if necessary
            arff_data.close()


class ArffStructure:
    """ Stores the arff data in a way it can be used by tensorflow.
    Args:
        arff_data: arff_data which is a dictionary returned by the arff.py module
    Instance Attributes:
        class_list: list of all the classes in this arff data
        in_size: size of the input layer of the neural network
        out_size: size of the output layer of the neural network
        entry_size: number of training entries. Its the height of the input matrix.
        input_matrix: a list of a list.  input_matrix[i][j] stores the input values of attribute j of entry i
        from the arff file.
        output_matrix: a list of a list.  output_vector[i] stores the training output of entry i
        from the arff file in a one-hot manner.
    """

    def __init__(self, df, class_attribute, assume_numeric=False):
        """ Reads and encapsulates arff_data which is a dictionary returned by the arff.py module.
        """
        self.input_df = df.drop(columns=[class_attribute])
        if not assume_numeric:
        	print("Computing dummy-representation of data")
        	self.input_df = pd.get_dummies(self.input_df)
        self.input_matrix = self.input_df.values
        self.output_df = df[[class_attribute]]
        self.output_matrix = self.output_df.values
        self.class_attribute = class_attribute

def get_filename(path_of_file):
    """
    Returns the filename of a file that is referenced with its path.
    :param path_of_file: (Absolute) path to the file.
    :return: Name of the file (without file type extension).
    """
    return os.path.splitext(os.path.basename(path_of_file))[0]

def parse_args():
    """
    Parses the arguments that are given to the script and overwrites sys.argv with this parsed representation that is
    accessable as a list.
    """
    parser = argparse.ArgumentParser()
    parser.add_argument('--mode', choices=['train','test','traintest'], required=True, help="Selecting whether a train or a test is run.")
    parser.add_argument('--arff', required=True, help="Path or ARFF to use for training/ testing.")
    parser.add_argument('--testarff', required=False, help="Path or ARFF to use for testing when running with traintest mode.")
    parser.add_argument('--output', required=True, help="In train mode set the file where the model shall be dumped; in test mode set the file where the prediction results shall be serialized to.")
    parser.add_argument('--model', help="Path to the trained model (in .pcl format) that shall be used for testing.")
    parser.add_argument('--regression', action='store_true', help="If set, the data is assumed to be a regression problem instead of a categorical one.")
    parser.add_argument('--targets', nargs='*', type=int, help="Declare which of the columns of the ARFF to use as targets. Default is only the last column.")
    parser.add_argument('--seed', required=True, help="Sets the seed.")
    sys.argv = vars(parser.parse_args())

def load_arff_file(arff_path):
    """
    Loads an arff file from disk.
    Returns content.
    """
    # Load the arff dataset and convert the data into array.
    df, class_attribute = parse(arff_path)
    data =  ArffStructure(df, class_attribute)
    return data
    
def load_arff_files(arff_path_train, arff_path_test):
    """
    Loads an arff file from disk.
    Returns content.
    """
    # Load the arff dataset and convert the data into array.
    print("Reading in train data from ", arff_path_train, " and test data from ", arff_path_test)
    dfTrain, class_attribute = parse(arff_path_train)
    dfTest, class_attribute = parse(arff_path_test)
    print("Dataframes parsed.")
    if len(dfTrain.drop(columns=[class_attribute]).select_dtypes(include='number').columns) == len(dfTrain.columns) - 1:
        print("This is a numeric dataset, no binarization required.")
        dfBinarizedTrain = dfTrain
        dfBinarizedTest = dfTest
    else:
        print("Data required binarization. Merging the data frames")
        dfUnion = pd.concat([dfTrain, dfTest])
        print("Deriving ArffStructure from the union")
        data = ArffStructure(dfUnion, class_attribute)
        print("Ready. Size of input matrix is ", len(data.input_matrix), " and size of output matrix is ", len(data.output_matrix), ". Now creating train/test split over the binarized dataset if necessary.")
        dfBinarized = pd.concat([data.input_df, data.output_df], axis=1)
        dfBinarizedTrain = dfBinarized[:len(dfTrain)]
        dfBinarizedTest = dfBinarized[len(dfTrain):]
        
    print("Now computing ArffStructures for train and test")
    out1 = ArffStructure(dfBinarizedTrain, class_attribute, assume_numeric=True)
    out2 = ArffStructure(dfBinarizedTest, class_attribute, assume_numeric=True)
    print("ready.")
    return out1, out2

def get_feature_target_matrices(data):
    """
    If the given map contains the key 'target_indices', then these indices are used
    to define the matrices and the entry is removed from the map. Otherwise the last column is assumed to be the target.
    Returns a target and feature matrix.
    """
    # If target indices are given, use them...
    if sys.argv["targets"]:
        target_indices = sys.argv["targets"]
        target_columns = []
        # Stitch all the target columns together
        for index in target_indices:
            target_columns.append(data[:, index])
        targets = np.transpose(np.array(target_columns))
        # The feature columns are the remaining columns that are not a target
        feature_indices = list({j for j in range(len(data[0]))} - set(target_indices))
        feature_columns = []
        # Stitch the feature columns together
        for index in feature_indices:
            feature_columns.append(data[:, index])
        features = np.transpose(np.array(feature_columns))
    # Else assume last column is target.
    else:
        targets = [row[-1] for row in data]
        features = [row[:-1] for row in data]
    return features, targets


def serialize_model(classifier_instance):
    """
    Serialize trained model.
    Returns path to serialized model.
    """
    # Safe model on disk.
    print("dump model to file ", OUTPUT_FILE)
    with open(OUTPUT_FILE, 'wb') as file:
        pickle.dump(classifier_instance, file)

def serialize_prediction(prediction):
    """
    Serialize prediction results.
    Returns path to serialized predictions.
    """
    # Make sure the predictions are in a list
    prediction = prediction.tolist()
    # Convert possible integers to floats (nescassary for Weka signature)
    if isinstance(prediction[0],int):
        prediction = [float(i) for i in prediction]
    elif isinstance(prediction[0],list):
        for sublist in prediction:
            sublist = [float(i) for i in sublist]
    if not isinstance(prediction[0],list):
        prediction = [prediction]
    prediction_json = json.dumps(prediction)
    # Safe prediction on disk.
    print("write prediction to file ", OUTPUT_FILE)
    with open(OUTPUT_FILE, 'w') as file:
        file.write(prediction_json)


def run_train_mode(data):
    """
    Trains a model of the demanded classifier with the given data. The classifier is instantiated with the constructor
    parameters that the were used for the template and the classifiers can run it training with either parameters that
    the script was started with or those that were given to the template.
    Returns path to serialized model.
    """
    features, targets = data.input_matrix, data.output_matrix
    if targets.shape[1] != 1:
        raise Exception("Can currently only work with single targets.")
    X = features
    if not sys.argv["regression"]:
        y = targets[:,0].astype("str")
    else:
        y = targets[:,0]
    # Create instance of classifier with given parameters.
    classifier_instance = {{classifier_construct}}
    classifier_instance.fit(X, y)
    serialize_model(classifier_instance)

def run_train_test_mode(data, testdata):
    """
    Trains a model of the demanded classifier with the given data. The classifier is instantiated with the constructor
    parameters that the were used for the template and the classifiers can run it training with either parameters that
    the script was started with or those that were given to the template.
    Returns path to serialized model.
    """
    features, targets = data.input_matrix, data.output_matrix
    if targets.shape[1] != 1:
        raise Exception("Can currently only work with single targets.")
    X = features
    if not sys.argv["regression"]:
        y = targets[:,0].astype("str")
    else:
        y = targets[:,0]

    print("Now training on data with ", len(X), "instances")
    if (len(X) != len(y)):
    	raise Exception("Input matrix and prediction vector have different sizes. Prediction vector has length " + str(len(y)))
    	
    # Create instance of classifier with given parameters.
    classifier_instance = {{classifier_construct}}
    classifier_instance.fit(X, y)
    
    test_features = np.array(testdata.input_matrix)
    
    if sys.argv["regression"]:
        prediction = classifier_instance.predict(test_features)
    else:
        try:
            prediction = classifier_instance.predict_proba(test_features)
        except:
            prediction = classifier_instance.predict(test_features)
    serialize_prediction(prediction)

def run_test_mode(testdata):
    """
    Tests the model that is referenced by the model argument with the given data.
    Returns path to prediction results.
    """
    with open(sys.argv["model"], 'rb') as file:
        classifier_instance = pickle.load(file)

    test_features = np.array(testdata.input_matrix)
    
    if sys.argv["regression"]:
        prediction = classifier_instance.predict(test_features)
    else:
        try:
            prediction = classifier_instance.predict_proba(test_features)
        except:
            prediction = classifier_instance.predict(test_features)
    serialize_prediction(prediction)


def main():
    print("using seed ", sys.argv["seed"])
    np.random.seed(int(sys.argv["seed"]))
    print("load arff file from ", sys.argv["arff"])
    print("run script in mode ", sys.argv["mode"])
    if sys.argv["mode"] == "train":
        data = load_arff_file(sys.argv["arff"])
        run_train_mode(data)
    elif sys.argv["mode"] == "test":
        assert sys.argv["model"]
        data = load_arff_file(sys.argv["arff"])
        run_test_mode(data)
    elif sys.argv["mode"] == "traintest":
        traindata, testdata = load_arff_files(sys.argv["arff"], sys.argv["testarff"]);
        run_train_test_mode(traindata, testdata)


if __name__ == "__main__":
    parse_args()
    OUTPUT_FILE = sys.argv["output"]
    if not sys.argv["regression"] and sys.argv["targets"] and len(sys.argv["targets"]) > 1:
        raise RuntimeError("Multiple targets are not supported for categorical problems.")
    main()
