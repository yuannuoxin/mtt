package com.mtd.tt.utils;

import lombok.Data;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ListToTreeUtil {


    public static <T> List<Tree<T>> convertListToTree(List<T> nodeList, TreeConverter<T> converter) {
        if (nodeList == null || nodeList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Object, Tree<T>> nodeMap = new HashMap<>();
        List<Tree<T>> rootNodes = new ArrayList<>();

        // 将节点放入Map中，方便后续查找和创建树
        for (T item : nodeList) {
            Object itemId = converter.getItemId(item);
            Tree<T> treeNode = new Tree<>(itemId, item);
            nodeMap.put(itemId, treeNode);
        }

        // 遍历节点列表，将每个节点添加到其父节点的子节点列表中，或者作为根节点
        for (T item : nodeList) {
            Object parentId = converter.getParentId(item);

            if (parentId == null || !nodeMap.containsKey(parentId)) {
                // 没有父节点，将其视为根节点
                Tree<T> rootNode = nodeMap.get(converter.getItemId(item));
                rootNodes.add(rootNode);
            } else {
                Tree<T> parent = nodeMap.get(parentId);
                Tree<T> childNode = nodeMap.get(converter.getItemId(item));
                parent.addChild(childNode);
            }
        }

        return rootNodes;
    }

    public interface TreeConverter<T> {
        /**
         * 获取节点的唯一标识ID
         *
         * @param item 节点对象
         * @return 节点ID
         */
        Object getItemId(T item);

        /**
         * 获取节点的父节点ID
         *
         * @param item 节点对象
         * @return 父节点ID
         */
        Object getParentId(T item);
    }

    @Data
    public static class Tree<T> {
        private Object id;
        private T data;
        private List<Tree<T>> children;

        public Tree(Object id, T data) {
            this.id = id;
            this.data = data;
            this.children = new ArrayList<>();
        }

        public void addChild(Tree<T> child) {
            children.add(child);
        }
    }


    public static <T> List<T> convertListToTree(List<T> nodeList, TreeIdAccessor<T> idAccessor, TreeParentIdAccessor<T> parentIdAccessor, TreeChildrenAccessor<T> childrenAccessor, SetChildFunction<T> setChildFunction) {
        if (nodeList == null || nodeList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Object, T> nodeMap = new HashMap<>();
        List<T> rootNodes = new ArrayList<>();

        // 将节点放入Map中，方便后续查找和创建树
        for (T node : nodeList) {
            Object nodeId = idAccessor.getId(node);
//            Object nodeId = idAccessor.apply(node);
            nodeMap.put(nodeId, node);
        }

        // 遍历节点列表，将每个节点添加到其父节点的子节点列表中，或者作为根节点
        for (T node : nodeList) {
            Object parentId = parentIdAccessor.getParentId(node);
            if (parentId == null) {
                // 没有父节点，将其视为根节点
                rootNodes.add(node);
            } else {
                T parent = nodeMap.get(parentId);
                if (parent != null) {
                    List<T> children = childrenAccessor.getChildren(parent);
//                    List<T> children = childrenAccessor.apply(parent);
                    if (Objects.isNull(children)) {
                        children = new LinkedList<>();
                        setChildFunction.setChildren(parent, children);
//                        setChildFunction.accept(parent,children);
                    }
                    children.add(node);
                } else {
                    // 父节点不存在，将当前节点视为根节点
                    rootNodes.add(node);
                }
            }
        }

        return rootNodes;
    }


    public static <T> List<T> convertListToTreeV2(List<T> nodeList, Function<T, Object> idAccessor, Function<T, Object> parentIdAccessor, Function<T, List<T>> childrenAccessor, BiConsumer<T, List<T>> setChildFunction) {
        if (nodeList == null || nodeList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Object, T> nodeMap = new HashMap<>();
        List<T> rootNodes = new ArrayList<>();

        // 将节点放入Map中，方便后续查找和创建树
        for (T node : nodeList) {
            Object nodeId = idAccessor.apply(node);
            nodeMap.put(nodeId, node);
        }

        // 遍历节点列表，将每个节点添加到其父节点的子节点列表中，或者作为根节点
        for (T node : nodeList) {
            Object parentId = parentIdAccessor.apply(node);
            if (parentId == null) {
                // 没有父节点，将其视为根节点
                rootNodes.add(node);
            } else {
                T parent = nodeMap.get(parentId);
                if (parent != null) {
                    List<T> children = childrenAccessor.apply(parent);
                    if (Objects.isNull(children)) {
                        children = new LinkedList<>();
                        setChildFunction.accept(parent, children);
                    }
                    children.add(node);
                } else {
                    // 父节点不存在，将当前节点视为根节点
                    rootNodes.add(node);
                }
            }
        }

        return rootNodes;
    }


    /**
     * 标识节点的唯一ID
     * 可以用 {@link Function} 替代  Function<T,Object> idAccessor   Object nodeId = idAccessor.apply(node);
     *
     * @param <T>
     */
    public interface TreeIdAccessor<T> {
        Object getId(T node);
    }

    // 获取节点的父节点ID
    public interface TreeParentIdAccessor<T> {
        Object getParentId(T node);
    }


    /**
     * 获取节点的子节点列表
     * 可以用 {@link Function} 替代  Function<T,List<T>> childrenAccessor   Object nodeId = idAccessor.apply(node);
     *
     * @param <T>
     */
    public interface TreeChildrenAccessor<T> {
        List<T> getChildren(T node);
    }

    /**
     * 设置节点的子节点列表
     * 可以用 {@link BiConsumer} 替代  BiConsumer<T,List<T>> setChildFunction   setChildFunction.accept(parent,children);
     */
    public interface SetChildFunction<T> {
        void setChildren(T node, List<T> childNodeList);
    }
}

