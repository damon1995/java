package com.work.sqlLock;

/**
 * @author damon
 * @date: 2017/8/24.
 */
public class SqlLock {
    //数据库排它锁实现分布式锁，数据库引擎为innodb
    //缺点：非重入锁，数据库单点问题，分库分表无法使用，排它锁长时间不提交，占用数据库连接
    //MySql会对查询进行优化，即便在条件中使用了索引字段，但是否使用索引来检索数据是由 MySQL 通过判断不同执行计划的代价来决定的，如果 MySQL 认为全表扫效率更高，则会选择表级锁
    public void Lock(){
        String sql = "select *from test_table where id = 1 for update";
    }
    //建立一张数据库表
    //缺点：非阻塞，非重入
    public void Lock2(){
        //建表语句,对method_name加唯一索引

        //   CREATE TABLE `test_table` (
        //  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
        //  `method_name` varchar(64) NOT NULL DEFAULT '' COMMENT '锁定的方法名',
        //   PRIMARY KEY (`id`),
        //   UNIQUE KEY `uidx_method_name` (`method_name `) USING BTREE
        //) ENGINE=InnoDB DEFAULT CHARSET=utf8;

        //当锁住某个方法时，执行如下操作，执行成功的线程即为获得锁的线程

        //insert into test_table(method_name) values (‘method_name’)

        //当使用完某个方法时，执行如下操作

        //delete from test_table where method_name = 'method_name'

    }
}
