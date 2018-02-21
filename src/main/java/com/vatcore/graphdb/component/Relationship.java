package com.vatcore.graphdb.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vatcore.graphdb.json.Response;
import com.vatcore.graphdb.json.response.CreateRelationship;
import com.vatcore.graphdb.util.HttpUtil;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.locks.Condition;

public class Relationship<F, T, D> {

    @JsonIgnore
    private volatile boolean ready;  // 数据库是否已经成功同步
//    @JsonIgnore
//    private volatile boolean flag = true;  // 能否继续操作下去
    @JsonIgnore
    private volatile boolean error;  // 有错误
    @JsonIgnore
    private volatile boolean sync;  // 是否同步
    @JsonIgnore
    private volatile long syncTime;  // 同步时间戳, 只有与时间戳相同才能设置 sync 为 true, 即最后一次



    private volatile Long id;
    private Node<F> from;
    private Node<T> to;
    private Document<D> document;

    private Relationship() {}

    public Relationship(Node<F> from, Node<T> to, Document<D> document) {
        this.from = from;
        this.to = to;
        this.document = document;
    }

    public static <F, T, D> Relationship<F, T, D> build(Node<F> from, Node<T> to, Document<D> document) {
        return new Relationship<>(from, to, document);
    }

    // 默认异步, 需要修改
    public Relationship<F, T, D> create() throws IOException {

        if (this.from.equals(this.to)) throw new IOException();  // 两个节点相同抛自定义异常, 此处修改

        sync = false;
        syncTime = System.nanoTime();

        // 使用线程池?
        new Thread(() -> {
            long tempSyncTime = syncTime;
            synchronized (Relationship.this) {

                try {
                    long time = System.currentTimeMillis();
                    while (from.getId() == null || from.getId() == null) {  // id 要 可见性
                        if (System.currentTimeMillis() - time > 1000 * 10) {   // 10 秒超时
                            error = true;
                            return;
                        }
                        Thread.sleep(50);
                    }

                    String result = HttpUtil.post("createRelationship", this);
                    System.out.println(result);

                    Response<CreateRelationship> response = new ObjectMapper().readValue(result, new TypeReference<Response<CreateRelationship>>() {
                    });

                    if (Response.Code.OK.equals(response.getCode())) {
                        this.setId(response.getData().getRelationshipId());
                        this.getDocument().setId(response.getData().getDocumentId());
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
                    Relationship.this.notifyAll();
                }

            }
        }).start();

        return this;
    }

    // 同步
    public Relationship<F, T, D> sync() throws Exception {
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

    private Relationship<F, T, D> setId(Long id) {
        this.id = id;
        return this;
    }

    public Node<F> getFrom() {
        return from;
    }

    public Relationship<F, T, D> setFrom(Node<F> from) {
        this.from = from;
        return this;
    }

    public Node<T> getTo() {
        return to;
    }

    public Relationship<F, T, D> setTo(Node<T> to) {
        this.to = to;
        return this;
    }

    public Document<D> getDocument() {
        return document;
    }

    public Relationship<F, T, D> setDocument(Document<D> document) {
        this.document = document;
        return this;
    }
}
