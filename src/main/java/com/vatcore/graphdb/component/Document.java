package com.vatcore.graphdb.component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vatcore.graphdb.json.Response;
import com.vatcore.graphdb.json.response.CreateRelationship;
import com.vatcore.graphdb.util.HttpUtil;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

public class Document<T> {

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



    private Long id;
    private String typeName;
    private T data;

    private Document() {}

    public Document(Long id) {
        this.id = id;
    }

//    @SuppressWarnings("unchecked")
    public Document(T data) {
        this.data = data;
    }

    public static <T> Document<T> build(Long id) throws IOException {
        if (id == null) throw new IOException();
        return new Document<>(id);
    }

    public static <T> Document<T> build(T data) {
        return new Document<>(data);
    }




    public Document<T> update() throws IOException {

        sync = false;
        syncTime = System.nanoTime();

        new Thread(() -> {
            long tempSyncTime = syncTime;
            synchronized (Document.this) {
                try {
                    long time = System.currentTimeMillis();
                    while (id == null) {
                        if (System.currentTimeMillis() - time > 1000 * 10) {   // 10 秒超时
                            error = true;
                            return;
                        }
                        wait(1000 * 11);
                    }

                    String result = HttpUtil.post("updateDocument", this);
                    System.out.println(result);

                    Response<Document<T>> response = new ObjectMapper().readValue(result, new TypeReference<Response<Document<T>>>() {
                    });

                    if (Response.Code.OK.equals(response.getCode())) {
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
                    Document.this.notifyAll();
                }
            }
        }).start();


        return this;
    }

    public Document<T> sync() throws Exception {
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


    public synchronized Long idSync() throws InterruptedException {
        long time = System.currentTimeMillis();
        while (id == null) {
            if (System.currentTimeMillis() - time > 1000 * 10) {   // 10 秒超时
                error = true;
                return id;
            }
            this.wait(1000 * 11);
        }
        return id;
    }

    public Long getId() {
        return id;
    }

    public synchronized Document<T> setId(Long id) {
        this.id = id;

        this.notifyAll();
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public T getData() {
        return data;
    }

    public Document<T> setData(T data) {
        this.data = data;
        return this;
    }

}
