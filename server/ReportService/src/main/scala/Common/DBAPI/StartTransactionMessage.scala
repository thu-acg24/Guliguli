package Common.DBAPI


import Common.API.API
import Global.ServiceCenter.tongWenDBServiceCode

case class StartTransactionMessage() extends API[String](tongWenDBServiceCode)