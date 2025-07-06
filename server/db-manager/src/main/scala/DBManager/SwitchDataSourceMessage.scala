package DBManager

import Common.API.API
import Global.ServiceCenter.tongWenDBServiceCode

case class SwitchDataSourceMessage(projectName: String) extends API[String](tongWenDBServiceCode)