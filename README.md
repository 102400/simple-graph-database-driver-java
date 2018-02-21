# Simple Graph Database Driver Java
- RESTful 的封装
- 默认异步, 同步调用 sync()

## example
```java
class Example {
    public static void main(String[] args) {
        Class.forName("com.vatcore.graphdb.Driver");  // 加载配置
        
        Document<TestNodeData> documentDalao = Document.build(new TestNodeData()
                        .setName("da lao")
                        .setSex((byte) 0)
                        .setAge((short) 66)
        );  // 一个文档
        
        Node<TestNodeData> nodeDaLao = Node.build(documentDalao).create();  // 创建节点与文档
        
        Node<TestNodeData> nodeLaoWang = Node.build(Document.build(new TestNodeData()
                        .setName("lao wang")
                        .setSex((byte) 1)
                        .setAge((short) 23)
        )).create();  // 创建节点与文档
        
        Node.build(Document.<TestNodeData>build(
                documentDalao.idSync()  // 等待id不为null
        )).create();  // 创建节点, 节点中的文档为documentDalao, name为da lao
        
        documentDalao.setData(new TestNodeData()
                        .setName("da lao clone")
                        .setSex((byte) 1)
                        .setAge((short) 88)
        ).update().sync();  // 更新文档, 此处同步
        
        Node.build(Document.<TestNodeData>build(
                nodeDaLao.getDocument().idSync()
        )).create();  // 创建节点, 节点中的文档为documentDalao, name为da lao clone
        
        Relationship.build(nodeDaLao, nodeLaoWang, Document.build(new TestRelationshipData()
                .setFather(true)
        )).create();  // 创建关系和文档, 也可传入id不为null的文档不新建, nodeDaLao -> nodeLaoWang
    }
}
```