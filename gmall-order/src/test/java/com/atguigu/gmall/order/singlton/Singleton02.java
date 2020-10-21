package com.atguigu.gmall.order.singlton;


/**
 * 在整个工程中永远只有一个实例
 *  编写规则：
 *      1、不允许其他类在使用该类的时候通过new去创建
 *      2、通过调用公共的静态的返回本类实例对象的一个方法
 *      3、是否线程安全
 *          操作共享资源的代码有多条的时候
 *  分类：
 *      饿汉式：
 *          提前将对象创建好
 *      懒汉式
 *          需要的时候再创建
 */
public class Singleton02 {

    private Singleton02(){};

    private static Singleton02 singleton02 =  null;

    public static synchronized Singleton02 getInstance(){
        if (singleton02 == null){
            singleton02 = new Singleton02();
        }
        return singleton02;
    }


}
