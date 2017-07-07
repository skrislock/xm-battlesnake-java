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
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@RestController
public class RequestController {

    @RequestMapping(value = "/start", method = RequestMethod.POST, produces = "application/json")
    public StartResponse start(@RequestBody StartRequest request) {
        return new StartResponse()
                .setName("Bowser Snake")
                .setColor("#FF0000")
                .setHeadUrl("http://vignette1.wikia.nocookie.net/nintendo/images/6/61/Bowser_Icon.png/revision/latest?cb=20120820000805&path-prefix=en")
                .setHeadType(HeadType.DEAD)
                .setTailType(TailType.PIXEL)
                .setTaunt("Roarrrrrrrrr!");
    }

    @RequestMapping(value = "/move", method = RequestMethod.POST, produces = "application/json")
    public MoveResponse move(@RequestBody MoveRequest request) {
        Snake mySnake = findOurSnake(request);

        List<Move> possibleMoves = findValidMoves(request, mySnake.getCoords()[0], mySnake.getCoords()[1]);
        outputMoveList(possibleMoves, "possibleMoves");

        System.out.println("I have " + possibleMoves.size() + " valid moves");

        if (!possibleMoves.isEmpty()) {
            // just forage for now
            List<Move> foodMoves = movesTowardsFood(request, possibleMoves, mySnake);
            outputMoveList(foodMoves, "foodMoves");

            Move myMove = foodMoves.stream().filter(thisFoodMove -> possibleMoves.contains(thisFoodMove)).findFirst().orElse(possibleMoves.get(0));
            System.out.println("picking " + myMove.getName());
            return new MoveResponse().setMove(myMove).setTaunt("foraging " + myMove.getName());
        } else {
            return new MoveResponse()
                    .setMove(Move.DOWN)
                    .setTaunt("Oh Drat!");
        }
    }

    private void outputMoveList(List<Move> moveList, String name) {
        String message = "Here are the moves for " + name;

        for (Move move : moveList) {
            message += " " + move.getName();
        }

        System.out.println(message);

    }

    private Snake findOurSnake(MoveRequest request) {
        String myUuid = request.getYou();

        List<Snake> snakes = request.getSnakes();

        return snakes.stream().filter(thisSnake -> thisSnake.getId().equals(myUuid)).findFirst().orElse(null);
    }

    private List<Move> movesTowardsFood(MoveRequest request, List<Move> possibleMoves, Snake mySnake) {
        ArrayList<Move> returnMe = new ArrayList<>();

        int[] head = mySnake.getCoords()[0];

        // faked for just 1 food pellet for now
        int closestFood = 0;

        int[] closestFoodLocation = request.getFood()[closestFood];

        if (closestFoodLocation[0] < head[0]) {
            returnMe.add(Move.LEFT);
        }

        if (closestFoodLocation[0] > head[0]) {
            returnMe.add(Move.RIGHT);
        }

        if (closestFoodLocation[1] < head[1]) {
            returnMe.add(Move.UP);
        }

        if (closestFoodLocation[1] > head[1]) {
            returnMe.add(Move.DOWN);
        }
        return returnMe;
    }

    /*
     * private Move determineMove(int[] head, int[] move) { System.out.println("Determining move for: head [" + head[0] + "," + head[1] + "], move[" + move[0] + "," + move[1] + "]"); if (head[0] + 1 == move[0] && head[1] == move[1]) { return Move.RIGHT; } else if (head[0] == move[0] && head[1] - 1 == move[1]) { return Move.UP; } else if (head[0] - 1 == move[0] && head[1] == move[1]) { return Move.LEFT; } else if (head[0] == move[0] && head[1] + 1 == move[1]) { return Move.DOWN; } else { return null; } }
     */

    @RequestMapping(value = "/end", method = RequestMethod.POST)
    public Object end() {
        // No response required
        Map<String, Object> responseObject = new HashMap<String, Object>();
        return responseObject;
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

        it = request.getDeadSnakes().iterator();
        while (it.hasNext()) {
            Snake thisSnake = it.next();
            System.out.println("analyzing dead snake : " + thisSnake.getName());
            System.out.println("analyzingMe is : " + analyzeMe[0] + ", " + analyzeMe[1]);
            int[][] thisSnakeCoords = thisSnake.getCoords();

            // System.out.println("Fancy output : " + Arrays.deepToString(thisSnakeCoords));

            for (int i = 0; i < thisSnakeCoords.length; i++) {
                int[] thisCoord = thisSnakeCoords[i];
                // System.out.println("found this coord:" + thisCoord[0] + ", " + thisCoord[1]);
                if (coordinatesEquals(thisCoord, analyzeMe)) {
                    System.out.println("don't hit a dead snake");
                    return false;
                }
            }
        }

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

}
