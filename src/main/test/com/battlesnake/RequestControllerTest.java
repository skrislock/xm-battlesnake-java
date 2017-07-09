package com.battlesnake;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

import com.battlesnake.data.Move;
import com.battlesnake.data.MoveRequest;
import com.battlesnake.data.MoveResponse;
import com.battlesnake.data.Snake;

public class RequestControllerTest {
    RequestController rc = new RequestController();
    MoveRequest moveRequest = new MoveRequest();

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
        trapSnake.setCoords(new int[][] { { 1, 0 }, { 1, 1 }, { 2, 1 }, { 3, 1 }, { 4, 1 }, { 4, 0 } });
        return trapSnake;
    }

    @Test
    public void testFindPropopsedPoint() {
        int[] originalPoint = new int[] { 1, 1 };
        Move move = Move.DOWN;
        int[] proposedPoint = rc.findProposedPoint(originalPoint, move);
        Assert.assertEquals(2, proposedPoint[0]);
        Assert.assertEquals(1, proposedPoint[1]);
    }
    
    @Test
    public void testMove() {
    }

    @Test
    public void testItsATrap() {
        setUpTrapSnake();
        MoveResponse moveResponse = rc.move(moveRequest);
    }

}
