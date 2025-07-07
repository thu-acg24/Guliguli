package Common.Serialize

import Common.API.PlanContext
import Common.ServiceUtils.schemaName
import cats.effect.IO

/**
 * TongWen Table Template Row Trait
 *
 * 除了表的名称和初始化 SQL 语句外
 * 还应该规划:
 * ① 查全量数据;
 * ② 插入数据: 单元素、批量;
 * ③ 根据单字段查找数据; - 所有字段
 * ④ 根据单字段更新数据; - 所有字段
 */
trait TWTableTemplateRowV2 extends CirceSerializable {
  // 定义与表相关的行类型
  type RowType
  
  /***
   * 列归属的表名称, 比如 consilia_info
   * @see [[Process.Init.init]]
   */
  def tableName: String

  /**
   * 表的完整名称, 比如 user_account.user_info，
   * - user_account 是 schemaName
   * - user_info 是表名
   */
  def fullTableName: String = s""" "$schemaName"."$tableName" """

  /**
   * 表初始化的 SQL 语句
   * @see [[Process.Init.init]]
   */
  def initSQL: String

  /**
   * 表初始化的 IO 命令
   */
  def initIO(using PlanContext): IO[Any]

  def addRow(row: RowType)(using PlanContext): IO[Int]
  def addRows(rows: List[RowType])(using PlanContext): IO[List[Int]]

  def getAllRows(using PlanContext): IO[List[RowType]]

}
