package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import game.HuntState;
import game.Hunter;
import game.Node;
import game.NodeStatus;
import game.ScramState;

/** A solution with huntOrb optimized and scram getting out as fast as possible. */
public class Pollack extends Hunter {
    /** The id's of all visited NodeStatus objects. */
    private HashSet<Long> visitedHuntState;

    /** The current state in the Hunt phase. */
    private HuntState currentHuntState;

    /** The current state in the Scram phase. */
    private ScramState currentScramState;

    /** Get to the orb in as few steps as possible. <br>
     * Once you get there, you must return from the function in order to pick it up. <br>
     * If you continue to move after finding the orb rather than returning, it will not count.<br>
     * If you return from this function while not standing on top of the orb, it will count as a
     * failure.
     *
     * There is no limit to how many steps you can take, but you will receive<br>
     * a score bonus multiplier for finding the orb in fewer steps.
     *
     * At every step, you know only your current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the orb at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLocation(), neighbors(), and distanceToOrb() in HuntState.<br>
     * You know you are standing on the orb when distanceToOrb() is 0.
     *
     * Use function moveTo(long id) in HuntState to move to a neighboring<br>
     * tile by its ID. Doing this will change state to reflect your new position.
     *
     * A suggested first implementation that will always find the orb, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first search. <br>
     * Some modification is necessary to make the search better, in general. */
    @Override
    public void huntOrb(HuntState state) {
        // TODO 1: Get the orb
        visitedHuntState= new HashSet<>();
        currentHuntState= state;
        dfsWalk();
    }

    /** This method performs depth-first search of the Orb from the current position. */
    private void dfsWalk() {
        if (currentHuntState.distanceToOrb() == 0) return;
        long currentId= currentHuntState.currentLocation();
        visitedHuntState.add(currentId);
        List<NodeStatus> sortedNeighbors= new ArrayList<>(currentHuntState.neighbors());
        Collections.sort(sortedNeighbors);
        for (NodeStatus n : sortedNeighbors) {
            if (!visitedHuntState.contains(n.getId())) {
                currentHuntState.moveTo(n.getId());
                dfsWalk();
                if (currentHuntState.distanceToOrb() == 0) return;
                currentHuntState.moveTo(currentId);
            }
        }
    }

    /** Get out the cavern before the ceiling collapses, trying to collect as <br>
     * much gold as possible along the way. Your solution must ALWAYS get out <br>
     * before time runs out, and this should be prioritized above collecting gold.
     *
     * You now have access to the entire underlying graph, which can be accessed <br>
     * through ScramState. <br>
     * currentNode() and getExit() will return Node objects of interest, and <br>
     * getNodes() will return a collection of all nodes on the graph.
     *
     * Note that the cavern will collapse in the number of steps given by <br>
     * getStepsRemaining(), and for each step this number is decremented by the <br>
     * weight of the edge taken. <br>
     * Use getStepsRemaining() to get the time still remaining, <br>
     * pickUpGold() to pick up any gold on your current tile <br>
     * (this will fail if no such gold exists), and <br>
     * moveTo() to move to a destination node adjacent to your current node.
     *
     * You must return from this function while standing at the exit. <br>
     * Failing to do so before time runs out or returning from the wrong <br>
     * location will be considered a failed run.
     *
     * You will always have enough time to scram using the shortest path from the <br>
     * starting position to the exit, although this will not collect much gold. <br>
     * For this reason, using Dijkstra's to plot the shortest path to the exit <br>
     * is a good starting solution */
    @Override
    public void scram(ScramState state) {
        // TODO 2: Get out of the cavern before it collapses, picking up gold along the way
        currentScramState= state;
        boolean continueCollectGold= true;

        while (continueCollectGold) {
            Node currentNode= currentScramState.currentNode();
            Heap<Node> sortedNodes= sortedGoldNodes();
            boolean continueSearch= true;

            while (sortedNodes.size() > 0 && continueSearch) {
                Node bestNode= sortedNodes.poll();
                int stepsToGold= getSteps(
                    Path.shortest(currentNode, bestNode));
                int stepsFromGoldToFinal= getSteps(
                    Path.shortest(bestNode, currentScramState.getExit()));

                if (stepsToGold + stepsFromGoldToFinal <= currentScramState.stepsLeft()) {
                    goToNode(bestNode);
                    continueSearch= false;
                }
            }
            // Check if Pollack has moved:
            if (currentNode.equals(currentScramState.currentNode())) {
                // If she hasn't moved then there was no gold she could collect --> exit cavern
                continueCollectGold= false;
            }
        }
        goToNode(currentScramState.getExit());
        return;
    }

    /** Returns a priority max-heap with all nodes with gold in the Scram state where the <br>
     * priority of each node is directly proportional to its gold and inversely <br>
     * proportional to the distance to this node. */
    private Heap<Node> sortedGoldNodes() {
        Heap<Node> sortedNodes= new Heap<>(true);
        for (Node node : currentScramState.allNodes()) {
            if (node.getTile().gold() > 0 && !node.equals(currentScramState.currentNode())) {
                int distance= getSteps(Path.shortest(currentScramState.currentNode(), node));
                double priority= node.getTile().gold() + 52850.0 / distance;
                sortedNodes.add(node, priority);
            }
        }
        return sortedNodes;
    }

    /** Moves Pollack from her current position to Node node along the Dijkstra's shortest path<br>
     * Precondition: node is an object of class Node and it represents one of the reachable <br>
     * tiles in the cavern. */
    private void goToNode(Node node) {
        List<Node> shortestPath= Path.shortest(currentScramState.currentNode(), node);
        for (Node n : shortestPath) {
            if (!n.equals(currentScramState.currentNode())) {
                currentScramState.moveTo(n);
            }
        }
    }

    /** Returns the number of steps it takes to go along this path. <br>
     * Precondition: path is an object of type List<Node> and it represents a path from <br>
     * one tile in the cavern to another. */
    private int getSteps(List<Node> path) {
        if (path.size() == 1) return 0;
        int sum= 0;
        for (int k= 0; k < path.size() - 1; k++ ) {
            Node node1= path.get(k);
            Node node2= path.get(k + 1);
            sum= sum + node1.getEdge(node2).length();
        }
        return sum;
    }

}
