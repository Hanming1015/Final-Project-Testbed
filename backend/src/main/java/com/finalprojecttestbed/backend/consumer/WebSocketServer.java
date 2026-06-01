package com.finalprojecttestbed.backend.consumer;

import com.alibaba.fastjson.JSONObject;
import com.finalprojecttestbed.backend.consumer.utils.Game;
import com.finalprojecttestbed.backend.consumer.utils.JwtAuthentication;
import com.finalprojecttestbed.backend.mapper.UserMapper;
import com.finalprojecttestbed.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/websocket/{token}")
public class WebSocketServer {

    final public static ConcurrentHashMap<String, WebSocketServer> users = new ConcurrentHashMap<>();
    final private static CopyOnWriteArraySet<User> matchPool = new CopyOnWriteArraySet<>();
    private User user;
    private Session session = null;

    private static UserMapper userMapper;
    private Game game = null;

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketServer.userMapper = userMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        this.session = session;

        String userId = JwtAuthentication.getUserId(token);
        this.user = userMapper.selectById(userId);
        if (user != null) {
            System.out.println("connected");
            users.put(userId, this);
        } else {
            try {
                this.session.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @OnClose
    public void onClose() {
        System.out.println("disconnected");
        if (this.user != null) {
            users.remove(this.user.getId());
        }
    }

    public void startMatching() {
        System.out.println("start matching");
        matchPool.add(this.user);

        while (matchPool.size() >= 2) {
            Iterator<User> it = matchPool.iterator();
            User userA = it.next();
            User userB = it.next();
            matchPool.remove(userA);
            matchPool.remove(userB);

            Game game = new Game(13,14,0, userA.getId(), userB.getId());
            game.createMap();

            if (users.get(userA.getId()) != null) {
                users.get(userA.getId()).game = game;
            }
            if (users.get(userB.getId()) != null) {
                users.get(userB.getId()).game = game;
            }

            game.start();

            JSONObject respGame = new JSONObject();
            respGame.put("a_id", game.getPlayerA().getId());
            respGame.put("a_sx", game.getPlayerA().getSx());
            respGame.put("a_sy", game.getPlayerA().getSy());
            respGame.put("b_id", game.getPlayerB().getId());
            respGame.put("b_sx", game.getPlayerB().getSx());
            respGame.put("b_sy", game.getPlayerB().getSy());
            respGame.put("map", game.getG());

            users.get(userA.getId()).game = game;
            users.get(userB.getId()).game = game;

            JSONObject respA = new JSONObject();
            respA.put("event", "matching-success");
            respA.put("opponent_username", userB.getUsername());
            respA.put("game", respGame);
            users.get(userA.getId()).sendMessage(respA.toJSONString());

            JSONObject respB = new JSONObject();
            respB.put("event", "matching-success");
            respB.put("opponent_username", userA.getUsername());
            respB.put("game", respGame);
            users.get(userB.getId()).sendMessage(respB.toJSONString());
        }
    }

    public void stopMatching() {
        System.out.println("stop matching");
        matchPool.add(this.user);
    }

    private void move(int direction) {
        if (game.getPlayerA().getId().equals(this.user.getId())) {
            game.setNextStepA(direction);
        } else if (game.getPlayerB().getId().equals(this.user.getId())){
            game.setNextStepB(direction);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("receive message");
        JSONObject jsonObject = JSONObject.parseObject(message);
        String event = jsonObject.getString("event");
        if (event.equals("start-matching")) {
            startMatching();
        } else if (event.equals("stop-matching")) {
            stopMatching();
        } else if (event.equals("move")) {
            //System.out.println(jsonObject.getInteger("direction"));
            move(jsonObject.getInteger("direction"));
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) {
        synchronized (this.session) {
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

