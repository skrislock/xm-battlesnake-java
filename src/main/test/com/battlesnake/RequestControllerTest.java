package com.battlesnake;

import org.junit.Assert;
import org.junit.Test;
// import org.springframework.beans.factory.annotation.Autowired;

import com.battlesnake.data.Move;

public class RequestControllerTest {
    RequestController rc = new RequestController();
    
    @Test
    public void testFindPropopsedPoint() {
        int[] originalPoint = new int[] { 1, 1 };
        Move move = Move.DOWN;
        int[] proposedPoint = rc.findProposedPoint(originalPoint, move);
        Assert.assertEquals(2, proposedPoint[0]);
        Assert.assertEquals(1, proposedPoint[1]);
    }
    
}
