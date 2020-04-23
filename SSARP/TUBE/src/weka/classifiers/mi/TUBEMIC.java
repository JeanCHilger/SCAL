/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * TUBEMIC.java
 * (formally known as TUBEC)
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.clusterers.MultiTUBEClusterer;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.Debug.DBO;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.estimators.MultiBin;
import weka.estimators.MultiBinningUtils;
import weka.estimators.MultiEstimator;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MultiInstanceToPropositional;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

/**
 <!-- globalinfo-start -->
 * Multiple-instance Classifier 
 * directly based on the binning generated by Multi-TUBE 
 * 
 * Uses MultiTUBEClusterer to find clusters which could be the concept that defines
 * the positive instances.<br/>
 * <br/>
 * 
 * @author Gabi Schmidberger (gabi dot schmidberger at gmail dot com)
 * @version $Revision: 1.0 $ 
 */
public class TUBEMIC
  extends Classifier 
  implements OptionHandler, MultiInstanceCapabilitiesHandler,
             TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = -9039042691334245579L;

  /** additional output element (for verbose modes) */
  DBO dbo = new DBO();
 
  /** print the info about the centre clusters */
  public static int D_INFOCENTRES         = 1;
   
  /** follow the selection of the best cluster */
  public static int D_TESTCLUSTER         = 2;
   
   /** follow the selection of the best cluster */
  public static int D_CHECKINST           = 3;

  /** follow the selection of the best cluster */
  public static int D_NUMCENTRES          = 4;

  /** output the percentage of pos instances in bins */
  public static int D_PERCENTOFBINS      = 25;

  /** Trainings data transformed to propositional and with bag no's */
  protected Instances m_trainPropositional = null;
  
  /** the centre selected as concept for the positive instances */
  protected int m_posConceptCentre; 
  
  /** The index of the class attribute */
  protected int m_ClassIndex;

  /** The number of the class labels */
  protected int m_NumClasses;
  
  /** the percentage a bin is still part of positive cluster */
  double m_minPercent = 99.0;

  /** Whether the concept can be in either of the centres (OR). */
  protected boolean m_orFlag = false;

  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter = null;

  /** Whether to normalize/standardize/neither, default:standardize */
  protected int m_filterType = FILTER_STANDARDIZE;

  /** Normalize training data */
  public static final int FILTER_NORMALIZE = 0;
  /** Standardize training data */
  public static final int FILTER_STANDARDIZE = 1;
  /** No normalization/standardization */
  public static final int FILTER_NONE = 2;
  /** The filter to apply to the training data */
  public static final Tag [] TAGS_FILTER = {
    new Tag(FILTER_NORMALIZE, "Normalize training data"),
    new Tag(FILTER_STANDARDIZE, "Standardize training data"),
    new Tag(FILTER_NONE, "No normalization/standardization"),
  };

  /** The multidimensional binning estimator estimators. */
  protected MultiEstimator m_estimator = new weka.estimators.MultiTUBE();
  
  /** filename used for output */
  protected String m_fileName = null;
  
  /** The filter used to get rid of missing values. */
  protected ReplaceMissingValues m_Missing = new ReplaceMissingValues();

  /** the single-instance weight setting method */
  protected int m_WeightMethod = MultiInstanceToPropositional.WEIGHTMETHOD_INVERSE2;
  
  /** Filter used to convert MI dataset into single-instance dataset */
  protected MultiInstanceToPropositional m_convertToProp = new MultiInstanceToPropositional();

  /** Clusterer used to find centres */
  protected Clusterer m_clusterer = null;
  
  /** the centre bins */
  private Vector m_centreBins = null;
  
  /** the centre bins */
  private int m_numCentres = 0;
  
  /** the cluster bin lists */
  private Vector [] m_clusterBinList = null;
  
  /** the centrepoints */
  //private Instance [] m_centrePoints = null;
  
  /*=== the start methods  ===*/
  /** take the centre point in the centre bins that has the cluster
   * with the most instances  */
  //public static final int MOST_INSTANCES           = 1;
  /** take the centre point in the centre bins that has the cluster
   * with the most volume  */
  //public static final int MOST_VOLUME              = 2;
  
   /** take all centre points of all centre bins */
  //public static final int ALL_CENTRES              = 3;
  
  /** the start method */
  //protected int m_startMethod = 1; 
  
  /** the attribute taken as valid */
  boolean [] m_validAttributes = null;
  
  /** number of attributes (only valid ones) */
  int m_numAtt = -1;
  
  /** model of the original trainings data */
  Instances m_trainModel = null;
  
 /** model used for transformation */
  Instances m_baglessModel = null;
  
  /** the flags if attribute is valid */
  public TUBEMIC () {
    dbo.initializeRanges(30);
    String [] estSpec = null;
    String estName = "weka.clusterers.MultiTUBEClusterer";
    String estString = "weka.clusterers.MultiTUBEClusterer -L last -C 3 -Y 2 -X "+
    "-E \"weka.estimators.MultiTUBE -B 10 -N\"";
    try {
      estSpec = Utils.splitOptions(estString);
      estName = estSpec[0];
      estSpec[0] = "";
      m_clusterer = (MultiTUBEClusterer) Utils.forName(Clusterer.class, estName, estSpec);
    } catch (Exception ex) {
      ex.printStackTrace();
      DBO.pln(ex.getMessage());
      throw new IllegalStateException("Error while setting clusterer");          
    }

   }
  
  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Re-implement the Diverse Density algorithm, changes the testing "
      + "procedure.\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.PHDTHESIS);
    result.setValue(Field.AUTHOR, "Gabi Schmidberger");
    result.setValue(Field.YEAR, "2007");
    result.setValue(Field.TITLE, "Tree based Density Estimation");
    result.setValue(Field.SCHOOL, "University of Waikato");
        
    return result;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

 
    result.addElement(new Option(
        "\tTurn on OR for conceptareas.",
        "R", 0, "-R"));

    result.addElement(new Option(
          "\tWhether to 0=normalize/1=standardize/2=neither.\n"
          + "\t(default 1=standardize)",
          "N", 1, "-N <num>"));

    result.addElement(new Option(
        "\tSet the minimal percent value of positive instances in a bin."
	+" with which it is still accepted as part of the positive cluster\n"
        + "\t(default 99.0)",
        "P", 1, "-P <value>"));

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *     
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Turn on debugging output.</pre>
   * 
   * <pre> -N &lt;num&gt;
   *  Whether to 0=normalize/1=standardize/2=neither.
   *  (default 1=standardize)</pre>
   * 
   <!-- options-leftEnd -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    // output info data 
    String outputRange = Utils.getOption('V', options);
    setVerboseLevels(outputRange);
 
    //String startString = Utils.getOption('S', options);
    //if (startString.length() != 0) {
    //  setStartMethod(Integer.parseInt(startString));
    //}
 
    String weightString = Utils.getOption('A', options);
    if (weightString.length() != 0) {
      setWeightMethod(
          new SelectedTag(
            Integer.parseInt(weightString), 
            MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
    } else {
      setWeightMethod(
          new SelectedTag(
            MultiInstanceToPropositional.WEIGHTMETHOD_INVERSE2, 
            MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
    }	
    
    setDebug(Utils.getFlag('D', options));

    setOrFlag(Utils.getFlag('R', options));
    
    String nString = Utils.getOption('N', options);
    if (nString.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(nString), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_STANDARDIZE, TAGS_FILTER));
    }     
    
    String pString = Utils.getOption('P', options);
    if (pString.length() != 0) {
      setMinPercent(Double.parseDouble(pString));
    }
  
      // set multidmensional binning estimator and it options
    String [] estSpec = null;
    String estName = "weka.clusterers.MultiTUBEClusterer";
    String estString = Utils.getOption('C', options);
    if (estString.length() != 0) {
      estSpec = Utils.splitOptions(estString);
      if (estSpec.length == 0) {
        throw new IllegalArgumentException("Invalid clusterer specification string");
      }
      estName = estSpec[0];
      estSpec[0] = "";
      
      setClusterer((Clusterer) Utils.forName(Clusterer.class, estName, estSpec));
    }

    // filename for output of transformed data set
    String fileName = Utils.getOption('O', options);
    if (fileName.length() > 0)
      setFileName(fileName);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    
    result = new Vector();

    if (getDebug())
      result.add("-D");
    
    result.add("-A");
    result.add("" + m_WeightMethod);

   if (getOrFlag())
      result.add("-R");
    
    result.add("-N");
    result.add("" + m_filterType);

    result.add("-P");
    result.add("" + m_minPercent);
    
    //result.add("-S");
    //result.add("" + m_startMethod);

    // estimator
    result.add("-C");
    result.add("" + getClustererSpec());
      
    // verbose settings
    String verboseLevels = getVerboseLevels();
    if (verboseLevels.length() > 0) {
      result.add("-V");
      result.add("" + verboseLevels);
    }

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String filterTypeTipText() {
    return "The filter type for transforming the training data.";
  }

  /**
   * Gets how the training data will be transformed. Will be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   *
   * @return the filtering mode
   */
  public SelectedTag getFilterType() {
    return new SelectedTag(m_filterType, TAGS_FILTER);
  }

  /**
   * Sets how the training data will be transformed. Should be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   *
   * @param newType the new filtering mode
   */
  public void setFilterType(SelectedTag newType) {

    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

/*  public int getStartMethod() {
    return m_startMethod;
  }
  
  /**
   * 
   * @param st
   *
  public void setStartMethod(int st) {
    m_startMethod = st;
  }*/
  
  public double getMinPercent() {
    return m_minPercent;
  }
  
  /**
   * 
   * @param minPercent
   */
  public void setMinPercent(double minPercent) {
    m_minPercent = minPercent;
  }
  
  /**
   * Get whether all clusters are considered. Positive concept is in one of the clusters
   *
   * @return true if debugging output is on
   */
  public boolean getOrFlag() {

    return m_orFlag;
  }
  
  public void setOrFlag(boolean flag) {

    m_orFlag = flag;
  }

  /**
   * Sets the filename for output
   * @param n the new file name
   */
  public void setFileName(String n) {
    m_fileName = n;
  }

  /**
   * Returns the filename for all info output
   * @return filename that would be used for info output
   */
  public String getFileName() {
    return m_fileName;
  }

  /**
   * Switches the outputs on that are requested from the option V
   * if list is empty switches on the verbose mode only
   * 
   * @param list list of integers, all are used for an output type
   */
  public void setVerboseLevels(String list) { 
    dbo.setOutputTypes(list);
  }

  /**
   * Gets the current output type selection
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getVerboseLevels() {
    return dbo.getOutputTypes();
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String weightMethodTipText() {
    return "The method used for weighting the instances.";
  }

  /**
   * The new method for weighting the instances.
   *
   * @param method      the new method
   */
  public void setWeightMethod(SelectedTag method){
    if (method.getTags() == MultiInstanceToPropositional.TAGS_WEIGHTMETHOD)
      m_WeightMethod = method.getSelectedTag().getID();
  }

  /**
   * Returns the current weighting method for instances.
   * 
   * @return the current weighting method
   */
  public SelectedTag getWeightMethod(){
    return new SelectedTag(
                  m_WeightMethod, MultiInstanceToPropositional.TAGS_WEIGHTMETHOD);
  }

  /**
   * Sets the clusterer
     *
     * @param clusterer the clusterer with all options set.
     */
    public void setClusterer(Clusterer clusterer) {
      
      m_clusterer = (Clusterer)clusterer;
    }
    
    /**
     * Gets the clusterer used.
     *
     * @return the clusterer
     */
    public Clusterer getClusterer() {
      
      return m_clusterer;
    }
    
    /**
     * Gets the clusterer specification string.
     *
     * @return the estimator string.
     */
    protected String getClustererSpec() {
      
      Clusterer e = getClusterer();
      if (e == null) return "";
      if (e instanceof OptionHandler) {
        return e.getClass().getName() + " "
        + Utils.joinOptions(((OptionHandler) e).getOptions());
      }
      return e.getClass().getName();
    }

 /**
   * Sets the estimator
   *
   * @param estimator the estimator with all options set.
   *
  public void setEstimator(MultiEstimator estimator) {
    
    m_estimator = (MultiEstimator)estimator;
  }
  
  /**
   * Gets the estimator used.
   *
   * @return the estimator
   *
  public MultiEstimator getEstimator() {
    
    return m_estimator;
  } 
  
  /**
   * Gets the estimator specification string.
   *
   * @return the estimator string.
   *
  protected String getEstimatorSpec() {
    
    MultiEstimator e = getEstimator();
    if (e == null) return "";
    if (e instanceof OptionHandler) {
      return e.getClass().getName() + " "
      + Utils.joinOptions(((OptionHandler) e).getOptions());
    }
    return e.getClass().getName();
  }*/

    
  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);
    
    return result;
  }

  /**
   * Returns the capabilities of this multi-instance classifier for the
   * relational data.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = super.getCapabilities();
    
    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);
    
    return result;
  }

  /**
   * Builds the classifier
   *
   * @param train the training data to be used for generating the
   * boosted classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances train) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(train);

    // remove instances with missing class
    train = new Instances(train);
    m_trainModel = new Instances(train, 0);
    train.deleteWithMissingClass();
       
    m_ClassIndex = train.classIndex();
    m_NumClasses = train.numClasses();

    int numInst = train.numInstances();

    // using TUBE clusterer find positive centres 
    findPositiveCentres(train);
       
    // get the list of valid attributes from the clusterer
    //m_validAttributes = findValidAttributes();
    m_validAttributes = new boolean[train.attribute(1).relation().numAttributes()];
    for (int i = 0; i < m_validAttributes.length; i++) {
      m_validAttributes[i] = true;
    }
    
    // count the number of valid attributes
    int numAtt = 0;
    for (int i = 0; i < m_validAttributes.length; i++) {
      if (m_validAttributes[i]) { numAtt++; }
    }
    m_numAtt = numAtt;
    
    // set one centre if not all centres are supposed to be seen as positive centre
    if (!m_orFlag) {
      m_posConceptCentre = classifyTrainOnCentres();
    }
    
    m_numCentres = m_centreBins.size();
  }
  
  private boolean [] findValidAttributes() {
    boolean valid [] = ((MultiTUBEClusterer)m_clusterer).listOfCuttingAtts();
    return valid;
  }
  
  private void findPositiveCentres(Instances train) throws Exception {
    Instances data = new Instances(train);
    //String relationName = data.relationName();
    //Instances transformedDataModel = null;
    //m_numBags = data.attribute(0).numValues();

    // convert the training dataset into single-instance dataset
    try {
      m_trainPropositional = convertToPropositional(data);
    } catch (Exception ex) {
      ex.printStackTrace();
      DBO.pln(ex.getMessage());
      throw new IllegalStateException("Error while transforming to propositional");          
    }

    // for debug information output
    //transformedDataModel = new Instances(data, 0);

  // delete the bag number attribute
    
    data = new Instances(m_trainPropositional);
    data.deleteAttributeAt(0);
    
    // filter as given by parameter
    m_baglessModel = new Instances(data, 0);
    if (m_filterType == FILTER_STANDARDIZE)
      m_Filter = new Standardize();
    else if (m_filterType == FILTER_NORMALIZE)
      m_Filter = new Normalize();
    else 
      m_Filter = null; 

    if (m_Filter!=null) {
      m_Filter.setInputFormat(data);
      data = Filter.useFilter(data, m_Filter); 	
    }

    // delete missing values
    m_Missing.setInputFormat(data);
    data = Filter.useFilter(data, m_Missing);

    // debug outbut
    /*
    String fname = new String("data.arff");
    PrintWriter output = new PrintWriter(new FileOutputStream(fname));
    output.println(data.toString());
    output.close();
*/
    // make splits (bins) with estimator
    //((MultiTUBEClusterer)m_clusterer).setCutMethod(3);
    //((MultiTUBEClusterer)m_clusterer).setCompareMethod(2);
    //((MultiTUBEClusterer)m_clusterer).setXORClusters(true);
    //((MultiTUBEClusterer)m_clusterer).setEstimator(m_estimator);
    //m_clusterer.setVerboseLevels("7");
    // build the clusters
    m_clusterer.buildClusterer(data);
    
    // gather centre infos, centre bins and list of bins for each cluster
    m_centreBins = ((MultiTUBEClusterer)m_clusterer).getCentreBins();
    //if (dbo.dl(D_DIFFAB_WIDEPICT)) {
    // get max density
    Vector bins = ((MultiTUBEClusterer)m_clusterer).getBins();
    double maxDensity = MultiBinningUtils.getMaxDensity(bins);
    double maxABDensity = MultiBinningUtils.getMaxABDensity(bins);
 
    //dbo.pln("\ncentre bins");
    //dbo.dpln(MultiBinningUtils.binsToPictStringRow(cb, true, true, true, true, true,
    //maxDensity, maxABDensity, false));
    //}
   
    // PROBLEM TODO: first cluster is always the largest
    m_clusterBinList = ((MultiTUBEClusterer)m_clusterer).getClusterBinList();
    
    // delete centres with 0 positive instances
    Vector centrePoints = new Vector();
    MultiTUBEClusterer.tidyupCentreList(dbo.dl(D_NUMCENTRES), m_centreBins, centrePoints);
    
    // delete all bins in the bclusters that have less than min percent and are not the centres
    MultiTUBEClusterer.tidyupBinLists(dbo.dl(D_NUMCENTRES), m_minPercent,
	m_centreBins, m_clusterBinList);
    
    return;
  }
   
  /*
   * Tests the centres found with the training data
   * and returns the best centre that should be used as positive concept centre
   * @returns the index of the centre that should be used as positive concept
   */
  private int classifyTrainOnCentres() {
    
    int numCentres = m_centreBins.size();
    dbo.dpln(D_TESTCLUSTER, "num centres found: " + numCentres);
    
    // found only one centre; so the first one is the positive concept 
    if (numCentres == 1) {
      return 0;
    }
    
    int[] numCorrect = new int [numCentres];
    int[] numPosCorrect = new int [numCentres];
    int[] numNegCorrect = new int [numCentres];
    int[] numIncorrect = new int [numCentres];
    boolean[] positiv = new boolean [numCentres];
    int numMIInst = m_trainPropositional.numInstances();
    int[] numBins = new int[numCentres];
  
    for (int j = 0; j < numCentres; j++) {
      numCorrect[j] = 0;
      numPosCorrect[j] = 0;
      numNegCorrect[j] = 0;
      numBins[j] = m_clusterBinList[j].size();
    }
    
    if (numMIInst == 0)  return -1;
    double nextClassValue = -1.0; 

    // classify all instances
    Instance inst = m_trainPropositional.instance(0);
     
    double classValue = inst.classValue();
    double bagId = inst.value(0);
    double thisBag = bagId;
    int i = 0;
    while (i < numMIInst) {

      dbo.dpln(D_CHECKINST, "Instance "+inst);
      dbo.dpln(D_CHECKINST, "Bag "+bagId);
      dbo.dpln(D_CHECKINST, "class "+classValue);
     // reset values for new bag
      for (int j = 0; j < numCentres; j++) {
	positiv[j] = false;
	//numCorrect[j] = 0;
	//numIncorrect[j] = 0;
      }
      int numPos = 0;
      // check for all instances of bag if they fit into one of the centres cluster bins
      while (bagId == thisBag) {  
	
	// already positive for all centres
	if (numPos < numCentres) {
	  inst.setDataset(null);
	  inst.deleteAttributeAt(0);
	  inst.setDataset(m_baglessModel);
	  dbo.dpln(D_CHECKINST, "Reduced Instance "+inst);
	  for (int j = 0; j < numCentres; j++) {

	    // try all bins in cluster of centre
	    int k = 0;
	    // cannot get more positiv
	    while ((!positiv[j])    
		&& (k < numBins[j])) {
	      if (((MultiBin)m_clusterBinList[j].elementAt(k)).fitsInto(inst)) {
		dbo.dpln(D_CHECKINST, "fits into bag " + k + " of cluster " + j);
		positiv[j] = true; 
		numPos++;
	      }
	      k++;
	    }      
	  }
	}

	// get next instance
	i++;
	if (i < numMIInst) {
	  inst = m_trainPropositional.instance(i);
	  // maybe next classvalue
	  nextClassValue = inst.classValue();
	  bagId = inst.value(0);
	} else {
	  bagId = -1.0;
	}
      } // while instances of one bag
      
      // leftEnd of bag; set numCorrect
      if (classValue == 1.0) { // positive
	for (int j = 0; j < numCentres; j++) {
	  if (positiv[j]) {
	    numCorrect[j]++;
	    numPosCorrect[j]++;
	  }
	  else numIncorrect[j]++;
	}	
      } else {
	for (int j = 0; j < numCentres; j++) {
	  if (!positiv[j]) {
	    numCorrect[j]++;
	    numNegCorrect[j]++;
	  }
	  else numIncorrect[j]++;
	}	
      }     
      //for (int j = 0; j < numCentres; j++) {
      //	dbo.dpln(D_NUMCENTRES, "num centre: "+j+" has "+numCorrect[j]);
      //    }

      thisBag = bagId;
      classValue = nextClassValue;
    } // while, all instances

    // take bag with most numCorrect
    int maxNumCorrect = -1;
    int bestCentre = -1;
    for (int j = 0; j < numCentres; j++) {
      dbo.dpln(D_TESTCLUSTER, "num centre: "+j+" has "+numCorrect[j]+" instances correct"
	  +" neg "+numNegCorrect[j]+" pos "+numPosCorrect[j]);
      if (numCorrect[j] > maxNumCorrect) {
	maxNumCorrect = numCorrect[j];
	bestCentre = j;
      }
    }	

    // save memory
    m_trainPropositional = null;
    return bestCentre;
  }
 
/*private Vector findStartInstances(Instances train) throws Exception {
    int numCentres = m_centreBins.size();
    //int [] timeFitsInto = new int[numCentres];
    //int [] zerosInCentre = new int[numCentres];
    Vector centreInstances = new Vector();
    
    
    // simplest, take all
    if (m_startMethod == ALL_CENTRES) {
      dbo.dpln(D_INFOCENTRES, "*** added ALL centre points ");
      // take the centre of the centre bin in the cluster with the most positive instances
      for (int i = 0; i < numCentres; i++){
	Instance inst = m_centrePoints[i];
	centreInstances.add(inst); 
      }
      return centreInstances;
   }         
    
    int [] numInstInCluster = new int[numCentres];
    double [] volumeInCluster = new double[numCentres];
    int maxNumInst = 0;
    double maxVolume = 0.0;
    int maxNumInstBin = -1;
    int maxVolumeBin = -1;
      
    dbo.dpln(D_INFOCENTRES, "*** " + numCentres + " centres");
 
    /** reduce the clusterBinLists to the bins with pos only 
     * or just the centre *
    for (int i = 0; i < numCentres; i++){
      numInstInCluster[i] = 0;
      MultiBin centreBin = (MultiBin)m_centreBins.elementAt(i);
      Vector bins = new Vector();
      bins.add(centreBin);
      numInstInCluster[i] += centreBin.getNumB_Inst();
      volumeInCluster[i] += centreBin.getVolume();
      // start from one, first is centre bin
      dbo.dpln(D_INFOCENTRES, "* cluster: "+i+" started with "+m_clusterBinList[i].size()+" bins");
      for (int j = 1; j < m_clusterBinList[i].size(); j++) {
	
         MultiBin bin = (MultiBin)m_clusterBinList[i].elementAt(j);
         double numNeg = bin.getNumInst(); ////hier
         if (numNeg <= 0.0) {
           bins.add(bin);
           numInstInCluster[i] += bin.getNumB_Inst();
         }
      }
      if (numInstInCluster[i] > maxNumInst) {
	maxNumInst = numInstInCluster[i];
	maxNumInstBin = i;
      }
      if (volumeInCluster[i] > maxVolume) {
	maxVolume = volumeInCluster[i];
	maxVolumeBin = i;
      }
           
      m_clusterBinList[i] = bins;
      dbo.dpln(D_INFOCENTRES, "*** cluster: "+i+" has "+bins.size()+" bins");
      dbo.dpln(D_INFOCENTRES, "    instances: "+numInstInCluster[i]);
      dbo.dpln(D_INFOCENTRES, "       volume: "+volumeInCluster[i]);
    }

    if (m_startMethod == MOST_INSTANCES) {
      dbo.dpln(D_INFOCENTRES, "*** most instances");
      dbo.dpln(D_INFOCENTRES, "*** added centre point from centre "+maxNumInstBin);
      // take the centre of the centre bin in the cluster with the most positive instances
      Instance inst = m_centrePoints[maxNumInstBin];
      centreInstances.add(inst); 
    }
    if (m_startMethod == MOST_VOLUME) {
      dbo.dpln(D_INFOCENTRES, "*** most volume");
      dbo.dpln(D_INFOCENTRES, "*** added centre point from centre "+maxVolumeBin);
      // take the centre of the centre bin in the cluster with the most positive instances
      Instance inst = m_centrePoints[maxVolumeBin];
      centreInstances.add(inst); 
   }        
    
    return centreInstances;
  }**/

  /**
   * Converts dataset into single-instance dataset
   * this means one instance per bag
   * @param data the instances to convert
   * @return the converted instances
   * @exception if exception in the filterposconcept
   */
  private Instances convertToPropositional(Instances data) throws Exception {
    Instances newInst = new Instances(data, 0);
    m_convertToProp.setWeightMethod(getWeightMethod());
    m_convertToProp.setInputFormat(data);

    newInst = Filter.useFilter(data, m_convertToProp);
    //m_propositionalFormat = new Instances(newInst, 0);
    return newInst;
  }
  
  /**
   * Computes the distribution for a given exemplar
   *
   * @param exemplar the exemplar for which distribution is computed
   * @return the distribution
   * @throws Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance exemplar) 
  throws Exception {

    // Extract the instances
    Instances data = new Instances(m_trainModel, 0);
    data.add(exemplar);

    // convert the training dataset into single-instance dataset
    try {
      data = convertToPropositional(data);
    } catch (Exception ex) {
      ex.printStackTrace();
      DBO.pln(ex.getMessage());
      throw new IllegalStateException("Error while transforming to propositional");          
    }

    // delete the bag number attribute
    data.deleteAttributeAt(0);

    // apply standardize or normalize filter
    if(m_Filter != null)
      data = Filter.useFilter(data, m_Filter);

    data = Filter.useFilter(data, m_Missing);
    int numInst = data.numInstances(); 
    int numBins = m_clusterBinList[m_posConceptCentre].size();

    boolean positiv = false;
    
    int fromCentre = m_posConceptCentre;
    int toCentre = m_posConceptCentre;
    if (m_orFlag) {
      fromCentre = 0;
      toCentre = m_numCentres - 1;
    }
    
    for (int centreIndex = fromCentre; centreIndex <= toCentre && !positiv;
    centreIndex++) {
      // test if one instance fits into a pos centre bin, if yes than is positive
      for (int i = 0; i < numInst && !positiv; i++) {
	Instance inst = data.instance(i);
	// over all bins in the psoitve centre
	for (int k = 0; k < numBins && !positiv; k++) {

	  // try all bins in cluster of positive concept centre
	  if (((MultiBin)m_clusterBinList[m_posConceptCentre].elementAt(k)).fitsInto(inst)) {
	    positiv = true; 
	  }

	}      
      }
    }

    // Compute distribution
    double [] distribution = new double[2];
    distribution[0] = 0.0;  // log-Prob. for class 0
    distribution[1] = 0.0;

    if (!positiv)
      distribution[0] = 1.0;
    else
      distribution[1] = 1.0;

    return distribution;
  }

  /**
   * Gets a string describing the classifier.
   *
   * @return a string describing the classifer built.
   */
  public String toString() {

    //double CSq = m_LLn - m_LL;
    //int df = m_NumPredictors;
    String result = "Diverse Density";
    
    result += "Clusterer used \n"+ m_clusterer.toString() + "\n";
    
    if (!m_orFlag) {
      result += "The positive concept was in centre "+m_posConceptCentre+"\n";
    }
    
    if (m_centreBins == null) {
      return result + ": No model built yet.";
    }

  
    return result;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String[] argv) {
    runClassifier(new TUBEMIC(), argv);
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.00 $");
  }

}