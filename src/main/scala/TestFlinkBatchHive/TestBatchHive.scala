package TestFlinkBatchHive

import org.apache.flink.table.api._
import org.apache.flink.table.catalog.hive.HiveCatalog

import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

/**
 *  flink 批量读写hive
 */
object TestBatchHive extends App {
  override def main(args: Array[String]): Unit = {

    val settings: EnvironmentSettings = EnvironmentSettings
      .newInstance()
      .useBlinkPlanner()
      .inBatchMode()
      .build()

    val tableEnv: TableEnvironment = TableEnvironment.create(settings)

    // 测试flink sql  好不好用
    val res = tableEnv.sqlQuery("select '1', '1', 1, '1', '1', '1'")
    val r = res.execute()
    r.print()


    val name = "myhive" // Catalog名称，定义一个唯一的名称表示
    val defaultDatabase = "default" // 默认数据库名称
    val hiveConfDir = "/etc/hive/conf" // hive-site.xml路径
    val version = "1.1.0" // Hive版本号

    val hive = new HiveCatalog(name, defaultDatabase, hiveConfDir, version)

    val statementSet: StatementSet = tableEnv.createStatementSet()
    tableEnv.registerCatalog(name, hive)

    tableEnv.useCatalog(name)

    tableEnv.getConfig.setSqlDialect(SqlDialect.HIVE)
    tableEnv.useDatabase("default")

    // 设置读取hive的并行度
    val configuration = tableEnv.getConfig.getConfiguration

    configuration.setString("table.exec.hive.infer-source-parallelism", "false")
    configuration.setString("table.exec.hive.infer-source-parallelism.max", "15")
    configuration.setString("table.exec.hive.fallback-mapred-reader", "true")


    val sqlResult: Table = tableEnv.sqlQuery(
      """
        | select username, age, height, dt, hr from fs_table_source
        |""".stripMargin)

    //    这种方式也可以
    //    statementSet.addInsert("fs_table_dst", sqlResult)
    //    statementSet.execute()

    // register the view named 'fs_table_source.View' in the catalog named 'myhive'
    // in the database named 'default'
    tableEnv.createTemporaryView("fs_table_source.View", sqlResult)

    // 统计总的数量
    val total = tableEnv.executeSql("select count(*) from fs_table_source.View")
    total.print()

    // 写入hive的fs_table_dst表
    tableEnv.executeSql("insert into fs_table_dst select * from fs_table_source.View")

  }
}

