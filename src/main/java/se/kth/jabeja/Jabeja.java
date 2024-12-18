package se.kth.jabeja;

import org.apache.log4j.Logger;
import se.kth.jabeja.config.Config;
import se.kth.jabeja.config.NodeSelectionPolicy;
import se.kth.jabeja.io.FileIO;
import se.kth.jabeja.rand.RandNoGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Jabeja {
  final static Logger logger = Logger.getLogger(Jabeja.class);
  private final Config config;
  private final HashMap<Integer/*id*/, Node/*neighbors*/> entireGraph;
  private final List<Integer> nodeIds;
  private int numberOfSwaps;
  private int round;
  private float T;
  private boolean resultFileCreated = false;
  private final HashMap<Integer, Integer> nodeDegrees = new HashMap<>();

  //-------------------------------------------------------------------
  public Jabeja(HashMap<Integer, Node> graph, Config config) {
    this.entireGraph = graph;
    this.nodeIds = new ArrayList(entireGraph.keySet());
    this.round = 0;
    this.numberOfSwaps = 0;
    this.config = config;
    this.T = config.getTemperature();

    // Precompute node degrees
    for (int nodeId : entireGraph.keySet()) {
        nodeDegrees.put(nodeId, entireGraph.get(nodeId).getNeighbours().size());
    }
}


  //-------------------------------------------------------------------
  // public void startJabeja() throws IOException {
  //   for (round = 0; round < config.getRounds(); round++) {
  //     for (int id : entireGraph.keySet()) {
  //       sampleAndSwap(id);
  //     }

  //     // Restart mechanism
  //       if (round % config.getRestartInterval() == 0) {
  //           T = config.getMaxInitialTemperature(); // Restart temperature
  //       }

  //     //one cycle for all nodes have completed.
  //     //reduce the temperature
  //     saCoolDown();
  //     report();
  //   }
  // }

  //BONUS TASK
  public void startJabeja() throws IOException {
      for (round = 0; round < config.getRounds(); round++) {
          // Parallelize node processing
          nodeIds.parallelStream().forEach(this::sampleAndSwap);

          // Restart mechanism
          if (round % config.getRestartInterval() == 0) {
              T = config.getMaxInitialTemperature(); // Restart temperature
          }

          // Reduce the temperature
          saCoolDown();
          report();
      }
  }


  /**
   * Simulated analealing cooling function
   */
  // private void saCoolDown(){
  //   // TODO for second task
  //   if (T > 1)
  //     T -= config.getDelta();
  //   if (T < 1)
  //     T = 1;
  // }

  private void saCoolDown() {
    if (T > 1) {
        T *= config.getCoolingRate();
    }
    if (T < 1) {
        T = 1;
    }
}


  /**
   * Sample and swap algorith at node p
   * @param nodeId
   */
  private void sampleAndSwap(int nodeId) {
    Node partner = null;
    Node nodep = entireGraph.get(nodeId);

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.LOCAL) {
      // swap with random neighbors
      // TODO: Double check this logic - Written by Daniel
      Integer[] neighbors = getNeighbors(nodep);
      partner = findPartner(nodeId, neighbors);
    }

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.RANDOM) {
      // if local policy fails then randomly sample the entire graph
      // TODO: Double check this logic - Written by Daniel
      if (partner == null) {
        Integer[] sample = getSample(nodeId);
        partner = findPartner(nodeId, sample);
      }
    }

    // swap the colors
    // TODO: Double check this logic - Written by Daniel
    if (partner != null) {
      int tempColor = nodep.getColor();
      nodep.setColor(partner.getColor());
      partner.setColor(tempColor);
      // numberOfSwaps++;
      synchronized (this) {
          numberOfSwaps++;
      }
    }
  }

  public Node findPartner(int nodeId, Integer[] nodes){

    Node nodep = entireGraph.get(nodeId);

    Node bestPartner = null;
    double highestBenefit = 0;

    float alpha = config.getAlpha();


    // TODO: Double check this logic - Written by Daniel
    for (int q : nodes) {
      //Get node q from the graph
      Node nodeq = entireGraph.get(q);

      //Calculate the old energy
      int dpp = getDegree(nodep, nodep.getColor());
      int dqq = getDegree(nodeq, nodeq.getColor());
      double oldEnergy = Math.pow(dpp, alpha) + Math.pow(dqq, alpha);

      //Calculate the new energy
      int dpq = getDegree(nodep, nodeq.getColor());
      int dqp = getDegree(nodeq, nodep.getColor());
      double newEnergy = Math.pow(dpq, alpha) + Math.pow(dqp, alpha);

      //Calculate the benefit
      // if (((newEnergy * T) > oldEnergy) && (newEnergy > highestBenefit)) {
      //   bestPartner = nodeq;
      //   highestBenefit = newEnergy;
      // }
      //TODO: END OF DANIEL'S CODE BLOCK

      //TASK 2
      // if (Math.random() < Math.exp((newEnergy - oldEnergy) / T)) {
      //     if (newEnergy > highestBenefit) {
      //         bestPartner = nodeq;
      //         highestBenefit = newEnergy;
      //     }
      // }

      //BONUS TASK
      // Calculate degree weight for the acceptance probability
      float degreeWeight = (float) getDegree(nodeq, nodeq.getColor()) / (float) getAverageDegree();

      // Acceptance probability based on weighted simulated annealing
      if (Math.random() < Math.exp(degreeWeight * (newEnergy - oldEnergy) / T)) {
          bestPartner = nodeq;
          highestBenefit = newEnergy;
      }

      // if ((newEnergy - oldEnergy) / T > -10) {  // Approximation threshold
      //     float probability = 1 + (float) ((newEnergy - oldEnergy) / T);  // Linear approximation
      //     if (Math.random() < probability) {
      //         bestPartner = nodeq;
      //         highestBenefit = newEnergy;
      //     }
      // }

    }

    return bestPartner;
  }

  private float getAverageDegree() {
      int totalDegree = 0;
      for (Node node : entireGraph.values()) {
          totalDegree += node.getNeighbours().size();
      }
      return (float) totalDegree / entireGraph.size();
  }


  /**
   * The the degreee on the node based on color
   * @param node
   * @param colorId
   * @return how many neighbors of the node have color == colorId
   */
  // private int getDegree(Node node, int colorId){
  //   int degree = 0;
  //   for(int neighborId : node.getNeighbours()){
  //     Node neighbor = entireGraph.get(neighborId);
  //     if(neighbor.getColor() == colorId){
  //       degree++;
  //     }
  //   }
  //   return degree;
  // }

  private int getDegree(Node node, int colorId) {
      int degree = 0;
      for (int neighborId : node.getNeighbours()) {
          Node neighbor = entireGraph.get(neighborId);
          if (neighbor.getColor() == colorId) {
              degree++;
          }
      }
      return degree;
  }

  private int getPrecomputedDegree(int nodeId) {
      return nodeDegrees.getOrDefault(nodeId, 0);
  }


  /**
   * Returns a uniformly random sample of the graph
   * @param currentNodeId
   * @return Returns a uniformly random sample of the graph
   */
  private Integer[] getSample(int currentNodeId) {
    int count = config.getUniformRandomSampleSize();
    int rndId;
    int size = entireGraph.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    while (true) {
      rndId = nodeIds.get(RandNoGenerator.nextInt(size));
      if (rndId != currentNodeId && !rndIds.contains(rndId)) {
        rndIds.add(rndId);
        count--;
      }

      if (count == 0)
        break;
    }

    Integer[] ids = new Integer[rndIds.size()];
    return rndIds.toArray(ids);
  }


  // /**
  //  * Get random neighbors. The number of random neighbors is controlled using
  //  * -closeByNeighbors command line argument which can be obtained from the config
  //  * using {@link Config#getRandomNeighborSampleSize()}
  //  * @param node
  //  * @return
  //  */
  // private Integer[] getNeighbors(Node node) {
  //   ArrayList<Integer> list = node.getNeighbours();
  //   int count = config.getRandomNeighborSampleSize();
  //   int rndId;
  //   int index;
  //   int size = list.size();
  //   ArrayList<Integer> rndIds = new ArrayList<Integer>();

  //   if (size <= count)
  //     rndIds.addAll(list);
  //   else {
  //     while (true) {
  //       index = RandNoGenerator.nextInt(size);
  //       rndId = list.get(index);
  //       if (!rndIds.contains(rndId)) {
  //         rndIds.add(rndId);
  //         count--;
  //       }

  //       if (count == 0)
  //         break;
  //     }
  //   }

  //   Integer[] arr = new Integer[rndIds.size()];
  //   return rndIds.toArray(arr);
  // }

  //BONUS TASK
  // private Integer[] getNeighbors(Node node) {
  //     ArrayList<Integer> neighbors = node.getNeighbours();

  //     // Sort neighbors by degree in descending order
  //     neighbors.sort((a, b) -> Integer.compare(
  //         entireGraph.get(b).getNeighbours().size(),
  //         entireGraph.get(a).getNeighbours().size()
  //     ));

  //     // Dynamically choose the top neighbors based on sample size
  //     int dynamicSampleSize = Math.min(config.getRandomNeighborSampleSize(), neighbors.size());
  //     ArrayList<Integer> selectedNeighbors = new ArrayList<>(neighbors.subList(0, dynamicSampleSize));

  //     return selectedNeighbors.toArray(new Integer[0]);
  // }

  private Integer[] getNeighbors(Node node) {
      ArrayList<Integer> neighbors = node.getNeighbours();

      // Sort neighbors by degree in descending order using parallel streams
      neighbors.parallelStream()
              .sorted((a, b) -> Integer.compare(
                  entireGraph.get(b).getNeighbours().size(),
                  entireGraph.get(a).getNeighbours().size()
              ));

      // Dynamically choose the top neighbors based on sample size
      int dynamicSampleSize = Math.min(config.getRandomNeighborSampleSize(), neighbors.size());
      return neighbors.subList(0, dynamicSampleSize).toArray(new Integer[0]);
  }





  /**
   * Generate a report which is stored in a file in the output dir.
   *
   * @throws IOException
   */
  private void report() throws IOException {
    int grayLinks = 0;
    int migrations = 0; // number of nodes that have changed the initial color
    int size = entireGraph.size();

    for (int i : entireGraph.keySet()) {
      Node node = entireGraph.get(i);
      int nodeColor = node.getColor();
      ArrayList<Integer> nodeNeighbours = node.getNeighbours();

      if (nodeColor != node.getInitColor()) {
        migrations++;
      }

      if (nodeNeighbours != null) {
        for (int n : nodeNeighbours) {
          Node p = entireGraph.get(n);
          int pColor = p.getColor();

          if (nodeColor != pColor)
            grayLinks++;
        }
      }
    }

    int edgeCut = grayLinks / 2;

    logger.info("round: " + round +
            ", edge cut:" + edgeCut +
            ", swaps: " + numberOfSwaps +
            ", migrations: " + migrations);

    saveToFile(edgeCut, migrations);
  }

  private void saveToFile(int edgeCuts, int migrations) throws IOException {
    String delimiter = "\t\t";
    String outputFilePath;

    //output file name
    File inputFile = new File(config.getGraphFilePath());
    outputFilePath = config.getOutputDir() +
            File.separator +
            inputFile.getName() + "_" +
            "NS" + "_" + config.getNodeSelectionPolicy() + "_" +
            "GICP" + "_" + config.getGraphInitialColorPolicy() + "_" +
            "T" + "_" + config.getTemperature() + "_" +
            "D" + "_" + config.getDelta() + "_" +
            "RNSS" + "_" + config.getRandomNeighborSampleSize() + "_" +
            "URSS" + "_" + config.getUniformRandomSampleSize() + "_" +
            "A" + "_" + config.getAlpha() + "_" +
            "R" + "_" + config.getRounds() + ".txt";

    if (!resultFileCreated) {
      File outputDir = new File(config.getOutputDir());
      if (!outputDir.exists()) {
        if (!outputDir.mkdir()) {
          throw new IOException("Unable to create the output directory");
        }
      }
      // create folder and result file with header
      String header = "# Migration is number of nodes that have changed color.";
      header += "\n\nRound" + delimiter + "Edge-Cut" + delimiter + "Swaps" + delimiter + "Migrations" + delimiter + "Skipped" + "\n";
      FileIO.write(header, outputFilePath);
      resultFileCreated = true;
    }

    FileIO.append(round + delimiter + (edgeCuts) + delimiter + numberOfSwaps + delimiter + migrations + "\n", outputFilePath);
  }
}
