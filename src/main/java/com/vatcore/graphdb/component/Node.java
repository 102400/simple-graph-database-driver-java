package com.vatcore.graphdb.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vatcore.graphdb.Driver;
import com.vatcore.graphdb.json.Response;
import com.vatcore.graphdb.util.HttpUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Node<T> implements java.io.Serializable {

    @JsonIgnore
    private volatile boolean ready;  // 数据库是否已经成功同步
    @JsonIgnore
    private volatile boolean error;  // 有错误
    @JsonIgnore
    private volatile boolean sync;  // 是否同步
    @JsonIgnore
    private volatile long syncTime;  // 同步时间戳, 只有与时间戳相同才能设置 sync 为 true, 即最后一次



    private volatile Long id;
//    private String name;
    private List<Relationship> fromList;
    private List<Relationship> toList;
    private Document<T> document;

//    public Node(Long id) {
//        this.id = id;
//    }

    private Node() {}

    public Node(Document<T> document) {
        this.document = document;
    }

    public static <T> Node<T> build(Document<T> document) {
        return new Node<>(document);
    }

    // 默认异步, 需要修改
    public Node<T> create() throws IOException {
        sync = false;
        syncTime = System.nanoTime();

        // 使用线程池?
        new Thread(() -> {
            long tempSyncTime = syncTime;
            synchronized (Node.this) {
                try {
                    long time = System.currentTimeMillis();

                    String result = HttpUtil.post("createNode", Node.this);
                    System.out.println(result);

                    Response<Node<T>> response = new ObjectMapper().readValue(result, new TypeReference<Response<Node<T>>>() {});


                    if (Response.Code.OK.equals(response.getCode())) {
                        this.setId(response.getData().getId());
                        this.getDocument().setId(response.getData().getDocument().getId());
                        ready = true;
                    }
                    else {
                        error = true;
                        return;
                    }

                    Thread.sleep(100 * new Random().nextInt(5));  // 仅测试

                }
                catch (IOException | InterruptedException e) {
                    error = true;
                    e.printStackTrace();
                }
                finally {
                    if (tempSyncTime == syncTime) sync = true;  // 判断起始时间戳是否同一个
                    Node.this.notifyAll();
                }
            }
        }).start();

        return this;
    }

    // 广度优先
    public void bfs(Document document) {

    }

    // 深度优先
    public void dfs() {

    }

    // 同步
    public Node<T> sync() throws Exception {
        synchronized (this) {
            while (!sync) {
                wait();
            }

            sync = false;
            notifyAll();

//            if (error) throw new Exception();
        }
        return this;
    }



    public Long getId() {
        return id;
    }

    private Node<T> setId(Long id) {
        this.id = id;
        return this;
    }

    public List<Relationship> getFromList() {
        return fromList;
    }

    public Node<T> setFromList(List<Relationship> fromList) {
        this.fromList = fromList;
        return this;
    }

    public List<Relationship> getToList() {
        return toList;
    }

    public Node<T> setToList(List<Relationship> toList) {
        this.toList = toList;
        return this;
    }

    public Document<T> getDocument() {
        return document;
    }

    public Node<T> setDocument(Document<T> document) {
        this.document = document;
        return this;
    }
}
