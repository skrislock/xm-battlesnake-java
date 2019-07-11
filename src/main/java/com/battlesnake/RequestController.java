/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlesnake;

import com.battlesnake.data.*;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RestController
public class RequestController {
    private final String BAD_MOVE_KEY = "bad";
    private final String GOOD_MOVE_KEY = "good";

    @RequestMapping(value = "/start", method = RequestMethod.POST, produces = "application/json")
    public StartResponse start(@RequestBody StartRequest request) {
        return new StartResponse()
                .setName("X Snake")
                .setColor("#4444F0")
                .setHeadUrl("http://vignette1.wikia.nocookie.net/nintendo/images/6/61/Bowser_Icon.png/revision/latest?cb=20120820000805&path-prefix=en")
                .setHeadType(HeadType.DEAD)
                .setTailType(TailType.PIXEL)
                .setTaunt("Am I Smarter?!");
    }
  
    @RequestMapping(value = "/move", method = RequestMethod.POST, produces = "application/json")
    public MoveResponse move(@RequestBody MoveRequest request) {
        MoveResponse moveResponse = new MoveResponse();
        
        Snake mySnake = findOurSnake(request);
        List<Snake> otherSnakes = findOtherSnakes(request);
        
        List<Move> possibleMoves = findValidMoves(request, mySnake.getCoords()[0], mySnake.getCoords()[1]);

        // try to find traps, determine field size for each move recursively
        Map<Move, Integer> moveAndFieldMap = calculateMoveAndFieldMap(request, mySnake, otherSnakes, possibleMoves);
        
        List<Move> trapMoves = determineDangerousMoves(calculateTrapFactor(request, mySnake, otherSnakes), moveAndFieldMap);
        List<Move> orderedMoves = orderMoves(moveAndFieldMap);


        if (!possibleMoves.isEmpty()) {
            // maybe setting traps here
            
            // TODO: add hunting to snake awareness or somewhere else?
            Map<String, List<Move>> snakeAwarenessMap = analyzeCollisions(mySnake, possibleMoves, otherSnakes);
            List<Move> badMoves = snakeAwarenessMap.get(BAD_MOVE_KEY);
            List<Move> goodMoves = snakeAwarenessMap.get(GOOD_MOVE_KEY);

            // if you can attack a smaller snake, go for it
            for (Move goodMove : goodMoves) {
                // do we hunt if that means going into a trap?  Let'a avoid now
                if (trapMoves.contains(goodMove)) {
                    continue;
                }
                
                moveResponse.setTaunt("Hunting " + goodMove.getName());
                moveResponse.setMove(goodMove);
                return moveResponse;
            }
            
            // TODO: analyze when we're going to lose a race then go to the centre of the board, or set traps or...? 
            // STRATEGY PART 1: Go towards food if you can
            // go towards food but don't make a bad move
            List<Move> foodMoves = new ArrayList<Move>();
            // go towards food in your preferred order of moves
            foodMoves.addAll(orderedMoves);
            foodMoves.retainAll(movesTowardsFood(request, mySnake.getCoords()[0]));
            for (Move foodMove : foodMoves) {
                if (badMoves.contains(foodMove)) {
                    moveResponse.setTaunt("Avoiding Collision " + foodMove.getName());
                } else if (trapMoves.contains(foodMove)){
                    moveResponse.setTaunt("Avoiding Trap " + foodMove.getName());
                } else {
                    if (moveResponse.getTaunt() == null) {
                        moveResponse.setTaunt("foraging " + foodMove.getName());
                    }
                    moveResponse.setMove(foodMove);
                    return moveResponse;
                }
            }

            // TODO: I think this needs more testing
            // we can't go towards food, so just make a move and don't make a bad move
            // can possibeMoves lead to food?
            for (Move possibleMove : orderedMoves) {
                if (badMoves.contains(possibleMove) || trapMoves.contains(possibleMove)) {
                    continue;
                }
                int[] nextPossibleMove = findProposedPoint(mySnake.getCoords()[0], possibleMove);
                List<Move> nextValidMoves = findValidMoves(request, nextPossibleMove, mySnake.getCoords()[0]);
                List<Move> nextFoodMoves = movesTowardsFood(request, nextPossibleMove);
                for (Move nextValidMove : nextValidMoves) {
                    if (nextFoodMoves.contains(nextValidMove)) {
                        moveResponse.setTaunt("turning around " + possibleMove.getName());
                        moveResponse.setMove(possibleMove);
                        return moveResponse;
                    }
                }
            }

            // TODO: Maybe try to move towards the centre? 
            // can't seem to move towards food, so don't make a bad move
            for (Move possibleMove : orderedMoves) {
                if (!badMoves.contains(possibleMove) && !trapMoves.contains(possibleMove)) {
                    if (moveResponse.getTaunt() == null) {
                        moveResponse.setTaunt("just moving " + possibleMove.getName());
                    }
                    moveResponse.setMove(possibleMove);
                    return moveResponse;
                }
            }
            
            // all possible moves are bad, so just go
            if (moveResponse.getTaunt() == null) {
                moveResponse.setTaunt("have to make a bad move " + orderedMoves.get(0).getName());
            }
            moveResponse.setMove(orderedMoves.get(0));
            return moveResponse;
        } else {
            moveResponse.setMove(Move.DOWN).setTaunt("Oh Drat!");
            return moveResponse;
        }
    }

    private Map<Move, Integer> calculateMoveAndFieldMap(MoveRequest request, Snake mySnake, List<Snake> otherSnakes, List<Move> possibleMoves) {
        Map<Move, Integer> moveAndFieldMap = new HashMap<>();
      
        for (Move possibleMove : possibleMoves) {
            moveAndFieldMap.put(possibleMove, countFieldSize(request, mySnake.getCoords()[0], possibleMove));
        }

        return moveAndFieldMap;
    }

    private int countFieldSize(MoveRequest request, int[] head, Move possibleMove) {
        // analyze this move to see if we get ourselves into a space with less than 10 moves
        int[] proposedPoint = findProposedPoint(head, possibleMove);
        
        System.out.println("Analyzing head : [" + head[0] + "," + head[1] + "] for move: " + possibleMove.getName() 
            + " proposedPoint: [" + proposedPoint[0] + "," + proposedPoint[1] + "]");

        int boardSize = request.getWidth() * request.getHeight();

        // maybe run this on every valid move? one less step?
        ArrayList<int[]> initialCoveredPoints = new ArrayList<>();
        initialCoveredPoints.add(head);
        
        int possiblePathCount = recursePathFindTraps(request, proposedPoint, head, initialCoveredPoints, boardSize, 0);
        System.out.println("Counted: " + possiblePathCount);
        if (possiblePathCount < boardSize) {
            System.out.println("Analyzing head : [" + head[0] + "," + head[1] + "], found : " + possiblePathCount + " moves");
            return possiblePathCount;
        }

        return boardSize;
    }
    
    // maybe this can be used for more than finding traps
    private int recursePathFindTraps(MoveRequest request, int[] newHead, int [] newNeck,
            List<int[]> coveredPoints, int limit, int counter) {
        System.out.println("counter is: " + counter + ", newHead is [" + newHead[0] + "," + newHead[1] + "]");

        // if we are over the limit, return counter without incrementing
        if (counter >= limit) {
            return counter;
        }
        
        // if we have already checked this point, return counter - 1 because this point is not valid
        for (int[] thisCoveredPoint : coveredPoints) {
            if (coordinatesEquals(newHead, thisCoveredPoint)) {
                System.out.println("Already checked this point [" + newHead[0] + "," + newHead[1] + "]");
                return counter - 1;
            }
        }
        
        ArrayList<Move> validMoves = findValidMoves(request, newHead, newNeck);
        
        if(validMoves.isEmpty()) { // no valid moves here
            System.out.println("No valid moves");
            return counter;
        }

        coveredPoints.add(newHead);

        for(Move validMove : validMoves) {
            int[] proposedHead = findProposedPoint(newHead, validMove);

            counter = recursePathFindTraps(request, proposedHead, newHead, coveredPoints, limit, counter + 1);
            
            if (counter >= limit) { // we're already over the limit so stop counting
                return counter;
            }
        }
        
        return counter;
    }

    public int[] findProposedPoint(int[] point, Move move) {
        int[] proposedPoint = point.clone();
        if (move.equals(Move.RIGHT)) {
            proposedPoint[0] = proposedPoint[0] + 1;
        } else if (move.equals(Move.UP)) {
            proposedPoint[1] = proposedPoint[1] - 1;
        } else if (move.equals(Move.LEFT)) {
            proposedPoint[0] = proposedPoint[0] - 1;
        } else if (move.equals(Move.DOWN)) {
            proposedPoint[1] = proposedPoint[1] + 1;
        }
        return proposedPoint;
    }


    // look around at other snakes and determine good and bad moves
    private Map<String, List<Move>> analyzeCollisions(Snake mySnake, List<Move> possibleMoves, List<Snake> otherSnakes) {
        Map<String, List<Move>> moveMap = new HashMap<>();
        List<Move> badMoveList = new ArrayList<>();
        List<Move> goodMoveList = new ArrayList<>();
        
        // find other snake heads, and their length

        Map<int[], Integer> otherSnakeHeadsAndLength = new HashMap<>();
        for(Snake otherSnake : otherSnakes) {
            otherSnakeHeadsAndLength.put(otherSnake.getCoords()[0], otherSnake.getCoords().length);
        }
        
        // for each possible move, check if there is a snake head in a dangerous spot
        int[] myHead = mySnake.getCoords()[0];
        int myLength = mySnake.getCoords().length;
        Set<int[]> otherSnakeHeadPoints = otherSnakeHeadsAndLength.keySet();
        for(Move possibleMove : possibleMoves) {
            int[] proposedPoint = findProposedPoint(myHead, possibleMove);
            // check around the proposed point to see if there are any snake heads
            for(int[] otherSnakeHeadPoint : otherSnakeHeadPoints) {
                if(coordinatesEquals(otherSnakeHeadPoint, findProposedPoint(proposedPoint, Move.UP))
                        || coordinatesEquals(otherSnakeHeadPoint, findProposedPoint(proposedPoint, Move.DOWN))
                        || coordinatesEquals(otherSnakeHeadPoint, findProposedPoint(proposedPoint, Move.LEFT))
                        || coordinatesEquals(otherSnakeHeadPoint, findProposedPoint(proposedPoint, Move.RIGHT))) {
                    int otherSnakeLength = otherSnakeHeadsAndLength.get(otherSnakeHeadPoint);
                    if (otherSnakeLength < myLength) {
                        goodMoveList.add(possibleMove);
                    } else {
                        badMoveList.add(possibleMove);
                    }
                }
            }
        }
        
        // if there's something that's both a good and bad move, it's a bad move
        goodMoveList.removeAll(badMoveList);
        
        moveMap.put(BAD_MOVE_KEY, badMoveList);
        moveMap.put(GOOD_MOVE_KEY, goodMoveList);
        
        return moveMap;
    }
/*
    private void outputMoveList(List<Move> moveList, String name) {
        String message = "Here are the moves for " + name;

        for (Move move : moveList) {
            message += " " + move.getName();
        }

        System.out.println(message);

    }
    */

    private Snake findOurSnake(MoveRequest request) {
        String myUuid = request.getYou();

        List<Snake> snakes = request.getSnakes();

        return snakes.stream().filter(thisSnake -> thisSnake.getId().equals(myUuid)).findFirst().orElse(null);
    }
    
    private List<Snake> findOtherSnakes(MoveRequest request) {
        String myUuid = request.getYou();

        List<Snake> snakes = request.getSnakes();

        return snakes.stream().filter(thisSnake -> !thisSnake.getId().equals(myUuid)).collect(toList());
    }

    private List<Move> movesTowardsFood(MoveRequest request, int[] mySnakeHead) {
        ArrayList<Move> returnMe = new ArrayList<>();

        // int[] head = mySnake.getCoords()[0];

        // faked for just 1 food pellet for now
        int closestFood = 0;

        int[] closestFoodLocation = request.getFood()[closestFood];

        if (closestFoodLocation[0] < mySnakeHead[0]) {
            returnMe.add(Move.LEFT);
        }

        if (closestFoodLocation[0] > mySnakeHead[0]) {
            returnMe.add(Move.RIGHT);
        }

        if (closestFoodLocation[1] < mySnakeHead[1]) {
            returnMe.add(Move.UP);
        }

        if (closestFoodLocation[1] > mySnakeHead[1]) {
            returnMe.add(Move.DOWN);
        }
        return returnMe;
    }

    public ArrayList<Move> findValidMoves(MoveRequest request, int[] head, int[] previous) {

        ArrayList<Move> returnMe = new ArrayList<>();

        // analyze right
        int[] right = new int[] {head[0] + 1, head[1]};
        boolean keepRight = analyze(request, head, previous, right);
        if (keepRight) {
            // System.out.println("right is OK");
            returnMe.add(Move.RIGHT);
        }

        // analyze top
        int[] up = new int[] { head[0], head[1] - 1 };
        boolean keepUp = analyze(request, head, previous, up);
        if (keepUp) {
            // System.out.println("top is OK");
            returnMe.add(Move.UP);
        }

        // analyze left
        int[] left = new int[] { head[0] - 1, head[1] };
        boolean keepLeft = analyze(request, head, previous, left);
        if (keepLeft) {
            // System.out.println("left is OK");
            returnMe.add(Move.LEFT);
        }

        // analyze down
        int[] down = new int[] { head[0], head[1] + 1 };
        boolean keepDown = analyze(request, head, previous, down);
        if (keepDown) {
            // System.out.println("bottom is OK");
            returnMe.add(Move.DOWN);
        }

        return returnMe;
    }

    public boolean analyze(MoveRequest request, int[] head, int[] previous, int[] analyzeMe) {
        if (coordinatesEquals(previous, analyzeMe)) {
            System.out.println("don't go backwards");
            return false;
        }

        // don't hit the walls
        if (analyzeMe[0] < 0 || analyzeMe[0] >= request.getWidth()) {
            System.out.println("don't hit the wall");
            return false;
        }

        if (analyzeMe[1] < 0 || analyzeMe[1] >= request.getHeight()) {
            System.out.println("don't hit the wall");
            return false;
        }

        // don't hit another snake
        List<Snake> snakes = request.getSnakes();
        // System.out.println("there are : " + snakes.size() + "snakes");

        Iterator<Snake> it = snakes.iterator();
        while (it.hasNext()) {
            Snake thisSnake = it.next();
            // System.out.println("analyzing snake : " + thisSnake.getName());
            // System.out.println("analyzingMe is : " + analyzeMe[0] + ", " + analyzeMe[1]);
            int[][] thisSnakeCoords = thisSnake.getCoords();

            // System.out.println("Fancy output : " + Arrays.deepToString(thisSnakeCoords));

            for (int i = 0; i < thisSnakeCoords.length; i++) {
                int[] thisCoord = thisSnakeCoords[i];
                // System.out.println("found this coord:" + thisCoord[0] + ", " + thisCoord[1]);
                if (coordinatesEquals(thisCoord, analyzeMe)) {
                    System.out.println("don't hit another snake");
                    return false;
                }
            }
        }

        // I guess you dont' die if you hit a dead snake
        /*
         * it = request.getDeadSnakes().iterator(); while (it.hasNext()) { Snake thisSnake = it.next(); System.out.println("analyzing dead snake : " + thisSnake.getName()); System.out.println("analyzingMe is : " + analyzeMe[0] + ", " + analyzeMe[1]); int[][] thisSnakeCoords = thisSnake.getCoords();
         * 
         * // System.out.println("Fancy output : " + Arrays.deepToString(thisSnakeCoords));
         * 
         * for (int i = 0; i < thisSnakeCoords.length; i++) { int[] thisCoord = thisSnakeCoords[i]; // System.out.println("found this coord:" + thisCoord[0] + ", " + thisCoord[1]); if (coordinatesEquals(thisCoord, analyzeMe)) { System.out.println("don't hit a dead snake"); return false; } } }
         */
        return true;
    }

    public boolean coordinatesEquals(int[] oneArray, int[] secondArray) {
        if (oneArray.length != secondArray.length) {
            return false;
        }

        for (int x = 0; x < oneArray.length; x++) {
            if (oneArray[x] != secondArray[x]) {
                return false;
            }
        }

        return true;
    }

    private Integer calculateTrapFactor(MoveRequest request, Snake mySnake, List<Snake> otherSnakes) {
        float boardSizeFactor = 0.036f;
        float lengthFactor = 0.2f;
        
        int boardSize = request.getWidth() * request.getHeight();
        int snakeLengthCounter = mySnake.getCoords().length;
        int snakeCounter = 1; // mySnake
        for (Snake otherSnake : otherSnakes) {
            snakeLengthCounter += otherSnake.getCoords().length;
            snakeCounter++;
        }
        float averageLength = snakeLengthCounter / snakeCounter;
        
        return Math.round((boardSize * boardSizeFactor) + (averageLength * lengthFactor));
    }
    
    private List<Move> determineDangerousMoves(Integer trapFactor, Map<Move, Integer> moveAndFieldMap) {
        List<Move> dangerousMoves = new ArrayList<Move>();
        Set<Move> moves = moveAndFieldMap.keySet();
        for (Move thisMove : moves) {
            if (moveAndFieldMap.get(thisMove) < trapFactor) {
                dangerousMoves.add(thisMove);
            }
        }
        return dangerousMoves;
    }
    
    
    List<Move> orderMoves(Map<Move, Integer> moveAndFieldMap) {
        List<Move> orderedMoves = new ArrayList<Move>();
        Map<Move, Integer> sorted = moveAndFieldMap
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                    toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                        LinkedHashMap::new));
        
        sorted.entrySet().forEach(entry -> {
            orderedMoves.add(entry.getKey());
        });
        return orderedMoves;
    }
    
    @RequestMapping(value = "/end", method = RequestMethod.POST)
    public Object end() {
        // No response required
        Map<String, Object> responseObject = new HashMap<String, Object>();
        return responseObject;
    }

}
