package com.battlesnake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.battlesnake.data.Move;
import com.battlesnake.data.MoveRequest;
import com.battlesnake.data.MoveResponse;
import com.battlesnake.data.Snake;

public class RequestControllerTest {
    RequestController rc = new RequestController();
    MoveRequest moveRequest = new MoveRequest();

    // test trap avoidance
    private void setUpTrapSnake() {
        String mySnakeUuid = "2c4d4d70-8cca-48e0-ac9d-03ecafca0c98";

        Snake mySnake = makeTrapSnake(mySnakeUuid);

        ArrayList<Snake> snakeList = new ArrayList<Snake>();
        snakeList.add(mySnake);

        moveRequest.setHeight(5);
        moveRequest.setWidth(5);
        moveRequest.setTurn(0);
        moveRequest.setFood(new int[][] { { 1, 2 } });
        moveRequest.setYou(mySnakeUuid);
        moveRequest.setSnakes(snakeList);
        moveRequest.setGameId(UUID.randomUUID().toString()); // I don't think I care...?

    }
    
    private Snake makeTrapSnake(String uuid) {
        Snake trapSnake = new Snake();
        trapSnake.setId(uuid);
        trapSnake.setHealth(100);
        trapSnake.setName("mySnakeName");
        trapSnake.setTaunt("mySnakeTaunt");
        trapSnake.setCoords(new int[][] { { 1, 0 }, { 1, 1 }, { 1, 2 }, { 2, 2 }, { 3, 2 }, { 4, 2 } });
        return trapSnake;
    }

    // test collision avoidance
    private void setUpDangerousEnemySnake() {
        String mySnakeUuid = "2c4d4d70-8cca-48e0-ac9d-03ecafca0c98";
        String enemySnakeUuid = "7e87374a-2ca8-41fc-b56a-86a6f2e74aef";
        
        Snake mySnake = makeSmallSnake(mySnakeUuid);
        Snake enemySnake = makeBigSnake(enemySnakeUuid);

        ArrayList<Snake> snakeList = new ArrayList<Snake>();
        snakeList.add(mySnake);
        snakeList.add(enemySnake);

        moveRequest.setHeight(5);
        moveRequest.setWidth(5);
        moveRequest.setTurn(0);
        moveRequest.setFood(new int[][] { { 4, 4 } });
        moveRequest.setYou(mySnakeUuid);
        moveRequest.setSnakes(snakeList);
        moveRequest.setGameId(UUID.randomUUID().toString()); // I don't think I care...?
    }
    
    // test aggressive
    private void setUpWeakEnemySnake() {
        String mySnakeUuid = "2c4d4d70-8cca-48e0-ac9d-03ecafca0c98";
        String enemySnakeUuid = "7e87374a-2ca8-41fc-b56a-86a6f2e74aef";
        
        Snake mySnake = makeBigSnake(mySnakeUuid);
        Snake enemySnake = makeSmallSnake(enemySnakeUuid);

        ArrayList<Snake> snakeList = new ArrayList<Snake>();
        snakeList.add(mySnake);
        snakeList.add(enemySnake);

        moveRequest.setHeight(5);
        moveRequest.setWidth(5);
        moveRequest.setTurn(0);
        moveRequest.setFood(new int[][] { { 4, 4 } });
        moveRequest.setYou(mySnakeUuid);
        moveRequest.setSnakes(snakeList);
        moveRequest.setGameId(UUID.randomUUID().toString()); // I don't think I care...?
    }
    
    private Snake makeSmallSnake(String uuid) {
        Snake smallSnake = new Snake();
        smallSnake.setId(uuid);
        smallSnake.setHealth(100);
        smallSnake.setName("smallSnakeName");
        smallSnake.setTaunt("smallSnakeTaunt");
        smallSnake.setCoords(new int[][] { { 1, 0 }, { 1, 1 }, { 1, 2 }, { 1, 3 }});
        return smallSnake;
    }
    
    private Snake makeBigSnake(String uuid) {
        Snake bigSnake = new Snake();
        bigSnake.setId(uuid);
        bigSnake.setHealth(100);
        bigSnake.setName("bigSnakeName");
        bigSnake.setTaunt("bigSnakeTaunt");
        bigSnake.setCoords(new int[][] { { 3, 0 }, { 4, 0 }, { 4, 1 }, { 4, 2 }, { 4 , 3}});
        return bigSnake;
    }
    
    @Test
    public void testFindPropopsedPoint() {
        int[] originalPoint = new int[] { 1, 1 };
        Move move = Move.DOWN;
        int[] proposedPoint = rc.findProposedPoint(originalPoint, move);
        Assert.assertEquals(1, proposedPoint[0]);
        Assert.assertEquals(2, proposedPoint[1]);
    }
    
    @Test
    public void testMove() {
    }

    // set up a situation with one snake where
    // there is a potential to be trapped, so avoid
    @Test
    public void testItsATrap() {
        setUpTrapSnake();
        MoveResponse moveResponse = rc.move(moveRequest);
        Assert.assertEquals(Move.LEFT, moveResponse.getMove());
    }

    // set up a situation where we would want to move right to get the food, but
    // realize that a bigger snake could eat us so avoid
    @Test
    public void testDangerousMove() {
        setUpDangerousEnemySnake();
        MoveResponse moveResponse = rc.move(moveRequest);
        Assert.assertEquals(Move.LEFT, moveResponse.getMove());
    }

    // Initial test of aggressive behaviour
    @Test
    public void testAggressiveMove() {
        setUpWeakEnemySnake();
        MoveResponse moveResponse = rc.move(moveRequest);
        Assert.assertEquals(Move.LEFT, moveResponse.getMove());
    }
    
    @Test
    public void testOrderedMoves() {
        Map<Move, Integer> testMap = new HashMap<Move, Integer>();
        testMap.put(Move.DOWN, 1);
        testMap.put(Move.UP, 2);
        testMap.put(Move.LEFT, 3);
        testMap.put(Move.RIGHT, 4);
        
        List<Move> orderedMoves = rc.orderMoves(testMap);
        Assert.assertEquals(Move.RIGHT, orderedMoves.get(0));
        Assert.assertEquals(Move.LEFT, orderedMoves.get(1));
        Assert.assertEquals(Move.UP, orderedMoves.get(2));
        Assert.assertEquals(Move.DOWN, orderedMoves.get(3));
    }
}
